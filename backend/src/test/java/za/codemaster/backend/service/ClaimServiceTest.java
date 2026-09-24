package za.codemaster.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import za.codemaster.backend.BackendApplication;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.dto.claim.ClaimDto;
import za.codemaster.backend.dto.claim.ClaimStatusDto;
import za.codemaster.backend.dto.claim.PullRequestStateDto;
import za.codemaster.backend.exception.ApiException;
import za.codemaster.backend.repository.ClaimCollaborationRequestRepository;
import za.codemaster.backend.repository.ClaimRepository;
import za.codemaster.backend.repository.IssueRepository;
import za.codemaster.backend.repository.NotificationRepository;
import za.codemaster.backend.repository.ProjectMaintainerRepository;
import za.codemaster.backend.repository.ProjectRepository;
import za.codemaster.backend.repository.UserRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies API-02.5's service-layer behavior (round2-tickets.md): claims can be
 * created, released, and listed, non-exclusivity across different users holds
 * (design doc §7 — the whole point of the ticket), and a second active claim by
 * the same user is rejected with {@code CLAIM_ALREADY_ACTIVE} (409), caught from
 * the real partial unique index rather than pre-checked.
 * <p>
 * Runs against a real (test) Postgres database, same pattern as
 * {@code CommentServiceTest}.
 * <p>
 * Note on the duplicate-claim test: once {@link ClaimService#createClaim}'s
 * {@code saveAndFlush} fails on the partial unique index, PostgreSQL aborts the
 * current transaction — any further statement on that same connection fails until
 * rollback. Since this whole test class shares one transaction per test method
 * (rolled back at the end for isolation), the duplicate-claim assertion is kept
 * as the last thing each such test does, with no further DB access afterward.
 */
@SpringBootTest(
    classes = BackendApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@EnableAutoConfiguration(exclude = {
    SecurityAutoConfiguration.class,
    OAuth2ResourceServerAutoConfiguration.class,
    OAuth2ClientAutoConfiguration.class
})
@Transactional
class ClaimServiceTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private ProjectMaintainerRepository projectMaintainerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClaimCollaborationRequestRepository claimCollaborationRequestRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    private ClaimService service;
    private ProjectQueryServiceFixtures fixtures;
    private User claimant;

    @BeforeEach
    void setUp() {
        // A generous limit — this class isn't testing API-03.10's rate limiting.
        service = new ClaimService(claimRepository, issueRepository, projectMaintainerRepository,
                new RateLimitService(1_000_000, 1_000_000, 1_000_000, 1_000_000), claimCollaborationRequestRepository,
                notificationService);
        fixtures = ProjectQueryServiceFixtures.seed(projectRepository, issueRepository);
        claimant = userRepository.save(User.builder()
                .githubId(System.nanoTime())
                .username("claimant_" + System.nanoTime())
                .displayName("Claimant")
                .build());
    }

    private User otherUser() {
        return userRepository.save(User.builder()
                .githubId(System.nanoTime())
                .username("other_" + System.nanoTime())
                .displayName("Other User")
                .build());
    }

    /** Makes {@code user} a maintainer of the project the given issue belongs to. */
    private void addMaintainer(Long issueId, User user) {
        za.codemaster.backend.domain.model.Issue issue = issueRepository.findById(issueId).orElseThrow();
        za.codemaster.backend.domain.model.ProjectMaintainer relationship =
                new za.codemaster.backend.domain.model.ProjectMaintainer();
        relationship.setProject(issue.getProject());
        relationship.setUser(user);
        relationship.setRole("maintainer");
        projectMaintainerRepository.save(relationship);
    }

    @Test
    void createClaimPersistsAndReturnsActiveStatus() {
        Long issueId = fixtures.issueId(0);

        ClaimDto created = service.createClaim(issueId, "I'll take this one.", claimant);

        assertNotNull(created.id());
        assertEquals(issueId, created.issueId());
        assertEquals(ClaimStatusDto.ACTIVE, created.status());
        assertEquals("I'll take this one.", created.note());
        assertEquals(claimant.getUsername(), created.user().username());
    }

    @Test
    void createClaimWithoutNoteSucceeds() {
        Long issueId = fixtures.issueId(0);

        ClaimDto created = service.createClaim(issueId, null, claimant);

        assertNull(created.note());
    }

    @Test
    void createClaimOnMissingIssueThrowsIssueNotFound() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.createClaim(-999L, null, claimant));

        assertEquals("ISSUE_NOT_FOUND", ex.getCode());
        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void createClaimOnClosedIssueThrowsIssueNotOpen() {
        Long closedIssueId = fixtures.issueId(5); // "Investigate flaky ETL pipeline test", status = "closed"

        ApiException ex = assertThrows(ApiException.class,
                () -> service.createClaim(closedIssueId, null, claimant));

        assertEquals("ISSUE_NOT_OPEN", ex.getCode());
        assertEquals(org.springframework.http.HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void createClaimOnNonOpenStatusIssueThrowsIssueNotOpen() {
        Long claimedStatusIssueId = fixtures.issueId(2); // "Fix flag parsing edge case in CLI", status = "claimed"

        ApiException ex = assertThrows(ApiException.class,
                () -> service.createClaim(claimedStatusIssueId, null, claimant));

        assertEquals("ISSUE_NOT_OPEN", ex.getCode());
    }

    @Test
    void createClaimOnProjectNotAcceptingContributionsThrowsError() {
        Long issueId = fixtures.issueId(0);
        za.codemaster.backend.domain.model.Project project =
                projectRepository.findById(fixtures.projectId(0)).orElseThrow();
        project.setAcceptingContributions(false);
        projectRepository.saveAndFlush(project);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.createClaim(issueId, null, claimant));

        assertEquals("PROJECT_NOT_ACCEPTING_CONTRIBUTIONS", ex.getCode());
        assertEquals(org.springframework.http.HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void createClaimOnUnpublishedProjectThrowsError() {
        Long issueId = fixtures.issueId(0);
        za.codemaster.backend.domain.model.Project project =
                projectRepository.findById(fixtures.projectId(0)).orElseThrow();
        project.setListingStatus(za.codemaster.backend.domain.model.ListingStatus.PENDING);
        projectRepository.saveAndFlush(project);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.createClaim(issueId, null, claimant));

        assertEquals("PROJECT_NOT_ACCEPTING_CONTRIBUTIONS", ex.getCode());
    }

    @Test
    void twoDifferentUsersCanBothHoldActiveClaimsOnSameIssue() {
        Long issueId = fixtures.issueId(0);
        User userA = claimant;
        User userB = otherUser();

        service.createClaim(issueId, "A is on it", userA);
        service.createClaim(issueId, "B is on it too", userB);

        List<ClaimDto> claims = service.getClaims(issueId);

        assertEquals(2, claims.size(),
                "claims are a non-exclusive signal of interest (design doc §7) — both must be active");
        assertTrue(claims.stream().anyMatch(c -> c.user().username().equals(userA.getUsername())));
        assertTrue(claims.stream().anyMatch(c -> c.user().username().equals(userB.getUsername())));
    }

    @Test
    void claimsAreListedOldestFirst() {
        Long issueId = fixtures.issueId(0);
        User userA = claimant;
        User userB = otherUser();

        ClaimDto first = service.createClaim(issueId, null, userA);
        ClaimDto second = service.createClaim(issueId, null, userB);

        List<ClaimDto> claims = service.getClaims(issueId);

        assertEquals(List.of(first.id(), second.id()), claims.stream().map(ClaimDto::id).toList());
    }

    @Test
    void getClaimsOnMissingIssueThrowsIssueNotFound() {
        ApiException ex = assertThrows(ApiException.class, () -> service.getClaims(-999L));

        assertEquals("ISSUE_NOT_FOUND", ex.getCode());
    }

    @Test
    void releaseClaimMarksItReleasedButItStillAppearsInTheFullClaimsList() {
        Long issueId = fixtures.issueId(0);
        service.createClaim(issueId, null, claimant);

        service.releaseClaim(issueId, claimant);

        // API-03.5: GET /issues/{issueId}/claims returns every status, oldest
        // first — a released claim is part of the issue's history, not erased.
        List<ClaimDto> claims = service.getClaims(issueId);
        assertEquals(1, claims.size());
        assertEquals(ClaimStatusDto.RELEASED, claims.get(0).status());
    }

    @Test
    void releasingWithNoActiveClaimThrowsClaimNotFound() {
        Long issueId = fixtures.issueId(0);

        ApiException ex = assertThrows(ApiException.class, () -> service.releaseClaim(issueId, claimant));

        assertEquals("CLAIM_NOT_FOUND", ex.getCode());
        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void releasingOnMissingIssueThrowsIssueNotFound() {
        ApiException ex = assertThrows(ApiException.class, () -> service.releaseClaim(-999L, claimant));

        assertEquals("ISSUE_NOT_FOUND", ex.getCode());
    }

    @Test
    void afterReleaseTheSameUserCanClaimAgain() {
        Long issueId = fixtures.issueId(0);
        service.createClaim(issueId, "first attempt", claimant);
        service.releaseClaim(issueId, claimant);

        ClaimDto reclaimed = service.createClaim(issueId, "second attempt", claimant);

        assertEquals(ClaimStatusDto.ACTIVE, reclaimed.status());
        // Both the released claim and the new active one are part of the
        // issue's full history now (API-03.5) — exactly one of them is active.
        List<ClaimDto> claims = service.getClaims(issueId);
        assertEquals(2, claims.size());
        assertEquals(1, claims.stream().filter(c -> c.status() == ClaimStatusDto.ACTIVE).count());
    }

    @Test
    void secondActiveClaimBySameUserOnSameIssueThrows409() {
        Long issueId = fixtures.issueId(0);
        service.createClaim(issueId, "first", claimant);

        // Kept as the last action in this test — see class Javadoc note on
        // why nothing touches the DB again after a caught constraint violation.
        ApiException ex = assertThrows(ApiException.class,
                () -> service.createClaim(issueId, "duplicate", claimant));

        assertEquals("CLAIM_ALREADY_ACTIVE", ex.getCode());
        assertEquals(org.springframework.http.HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void attachPullRequestSetsUrlAndDefaultsToOpenState() {
        Long issueId = fixtures.issueId(0);
        ClaimDto created = service.createClaim(issueId, null, claimant);

        ClaimDto updated = service.attachPullRequest(
                issueId, created.id(), "https://github.com/example-org/repo/pull/7", claimant);

        assertEquals("https://github.com/example-org/repo/pull/7", updated.pullRequestUrl());
        assertEquals(PullRequestStateDto.OPEN, updated.pullRequestState());
    }

    @Test
    void attachPullRequestIsReflectedOnNextRead() {
        Long issueId = fixtures.issueId(0);
        ClaimDto created = service.createClaim(issueId, null, claimant);

        service.attachPullRequest(issueId, created.id(), "https://github.com/example-org/repo/pull/7", claimant);

        ClaimDto reread = service.getClaims(issueId).get(0);
        assertEquals("https://github.com/example-org/repo/pull/7", reread.pullRequestUrl());
        assertEquals(PullRequestStateDto.OPEN, reread.pullRequestState());
    }

    @Test
    void attachPullRequestByNonOwnerIsForbidden() {
        Long issueId = fixtures.issueId(0);
        ClaimDto created = service.createClaim(issueId, null, claimant);
        User stranger = otherUser();

        ApiException ex = assertThrows(ApiException.class,
                () -> service.attachPullRequest(issueId, created.id(), "https://github.com/example-org/repo/pull/7", stranger));

        assertEquals("FORBIDDEN", ex.getCode());
        assertEquals(org.springframework.http.HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void attachPullRequestOnMissingClaimThrowsClaimNotFound() {
        Long issueId = fixtures.issueId(0);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.attachPullRequest(issueId, -999L, "https://github.com/example-org/repo/pull/7", claimant));

        assertEquals("CLAIM_NOT_FOUND", ex.getCode());
        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void attachPullRequestOnMissingIssueThrowsIssueNotFound() {
        Long issueId = fixtures.issueId(0);
        ClaimDto created = service.createClaim(issueId, null, claimant);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.attachPullRequest(-999L, created.id(), "https://github.com/example-org/repo/pull/7", claimant));

        assertEquals("ISSUE_NOT_FOUND", ex.getCode());
    }

    @Test
    void attachPullRequestWhenClaimBelongsToADifferentIssueThrowsClaimNotFound() {
        Long issueIdA = fixtures.issueId(0);
        Long issueIdB = fixtures.issueId(1);
        ClaimDto created = service.createClaim(issueIdA, null, claimant);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.attachPullRequest(issueIdB, created.id(), "https://github.com/example-org/repo/pull/7", claimant));

        assertEquals("CLAIM_NOT_FOUND", ex.getCode());
    }

    @Test
    void requestChangesSetsStatusAndFeedback() {
        Long issueId = fixtures.issueId(0);
        ClaimDto created = service.createClaim(issueId, null, claimant);
        User maintainer = otherUser();
        addMaintainer(issueId, maintainer);

        ClaimDto reviewed = service.reviewClaim(issueId, created.id(),
                new za.codemaster.backend.dto.claim.ClaimReviewRequest(
                        za.codemaster.backend.dto.claim.ClaimReviewDecision.REQUEST_CHANGES,
                        "Please add a test for the edge case."),
                maintainer);

        assertEquals(ClaimStatusDto.CHANGES_REQUESTED, reviewed.status());
        assertEquals("Please add a test for the edge case.", reviewed.maintainerFeedback());

        var notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(claimant.getId(), PageRequest.of(0, 10));
        assertEquals(1, notifications.getTotalElements());
        assertEquals("claim_reviewed", notifications.getContent().get(0).getType().getValue());
        assertFalse(notifications.getContent().get(0).getReadAt() != null);
    }

    @Test
    void confirmingCompletedNotifiesTheClaimOwner() {
        Long issueId = fixtures.issueId(0);
        ClaimDto created = service.createClaim(issueId, null, claimant);
        User maintainer = otherUser();
        addMaintainer(issueId, maintainer);

        service.reviewClaim(issueId, created.id(),
                new za.codemaster.backend.dto.claim.ClaimReviewRequest(
                        za.codemaster.backend.dto.claim.ClaimReviewDecision.CONFIRM_COMPLETED, null),
                maintainer);

        var notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(claimant.getId(), PageRequest.of(0, 10));
        assertEquals(1, notifications.getTotalElements());
        assertEquals("claim_reviewed", notifications.getContent().get(0).getType().getValue());
        assertEquals("/issues/" + issueId, notifications.getContent().get(0).getLink());
    }

    @Test
    void requestChangesFeedbackIsVisibleToContributorOnNextRead() {
        Long issueId = fixtures.issueId(0);
        ClaimDto created = service.createClaim(issueId, null, claimant);
        User maintainer = otherUser();
        addMaintainer(issueId, maintainer);

        service.reviewClaim(issueId, created.id(),
                new za.codemaster.backend.dto.claim.ClaimReviewRequest(
                        za.codemaster.backend.dto.claim.ClaimReviewDecision.REQUEST_CHANGES, "Fix the linter warning."),
                maintainer);

        // API-03.5: GET /issues/{issueId}/claims now returns every status, so a
        // changes_requested claim (no longer active) is still visible here.
        ClaimDto reread = service.getClaims(issueId).get(0);
        assertEquals("Fix the linter warning.", reread.maintainerFeedback());
        assertEquals(ClaimStatusDto.CHANGES_REQUESTED, reread.status());
    }

    @Test
    void requestChangesWithoutFeedbackThrowsValidationError() {
        Long issueId = fixtures.issueId(0);
        ClaimDto created = service.createClaim(issueId, null, claimant);
        User maintainer = otherUser();
        addMaintainer(issueId, maintainer);

        ApiException ex = assertThrows(ApiException.class, () -> service.reviewClaim(issueId, created.id(),
                new za.codemaster.backend.dto.claim.ClaimReviewRequest(
                        za.codemaster.backend.dto.claim.ClaimReviewDecision.REQUEST_CHANGES, null),
                maintainer));

        assertEquals("VALIDATION_ERROR", ex.getCode());
        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void confirmCompletedSetsStatusAndMaintainerConfirmedSource() {
        Long issueId = fixtures.issueId(0);
        ClaimDto created = service.createClaim(issueId, null, claimant);
        User maintainer = otherUser();
        addMaintainer(issueId, maintainer);

        ClaimDto reviewed = service.reviewClaim(issueId, created.id(),
                new za.codemaster.backend.dto.claim.ClaimReviewRequest(
                        za.codemaster.backend.dto.claim.ClaimReviewDecision.CONFIRM_COMPLETED, null),
                maintainer);

        assertEquals(ClaimStatusDto.COMPLETED, reviewed.status());
        assertEquals(za.codemaster.backend.dto.claim.ClaimCompletionSourceDto.MAINTAINER_CONFIRMED, reviewed.completionSource());
        assertNotNull(reviewed.completedAt());
    }

    @Test
    void reconfirmingAnAlreadyCompletedClaimIsANoOp() {
        Long issueId = fixtures.issueId(0);
        ClaimDto created = service.createClaim(issueId, null, claimant);
        User maintainer = otherUser();
        addMaintainer(issueId, maintainer);

        ClaimDto firstConfirm = service.reviewClaim(issueId, created.id(),
                new za.codemaster.backend.dto.claim.ClaimReviewRequest(
                        za.codemaster.backend.dto.claim.ClaimReviewDecision.CONFIRM_COMPLETED, null),
                maintainer);

        ClaimDto secondConfirm = service.reviewClaim(issueId, created.id(),
                new za.codemaster.backend.dto.claim.ClaimReviewRequest(
                        za.codemaster.backend.dto.claim.ClaimReviewDecision.CONFIRM_COMPLETED, null),
                maintainer);

        assertEquals(ClaimStatusDto.COMPLETED, secondConfirm.status());
        assertEquals(firstConfirm.completedAt(), secondConfirm.completedAt(),
                "re-confirming must not change completedAt");
        assertEquals(firstConfirm.completionSource(), secondConfirm.completionSource());
    }

    @Test
    void reviewingAReleasedClaimIsRejectedAsNotReviewable() {
        Long issueId = fixtures.issueId(0);
        ClaimDto created = service.createClaim(issueId, null, claimant);
        service.releaseClaim(issueId, claimant);
        User maintainer = otherUser();
        addMaintainer(issueId, maintainer);

        ApiException ex = assertThrows(ApiException.class, () -> service.reviewClaim(issueId, created.id(),
                new za.codemaster.backend.dto.claim.ClaimReviewRequest(
                        za.codemaster.backend.dto.claim.ClaimReviewDecision.CONFIRM_COMPLETED, null),
                maintainer));

        assertEquals("CLAIM_NOT_REVIEWABLE", ex.getCode());
        assertEquals(org.springframework.http.HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void requestingChangesOnAnAlreadyCompletedClaimIsRejectedAsNotReviewable() {
        Long issueId = fixtures.issueId(0);
        ClaimDto created = service.createClaim(issueId, null, claimant);
        User maintainer = otherUser();
        addMaintainer(issueId, maintainer);
        service.reviewClaim(issueId, created.id(),
                new za.codemaster.backend.dto.claim.ClaimReviewRequest(
                        za.codemaster.backend.dto.claim.ClaimReviewDecision.CONFIRM_COMPLETED, null),
                maintainer);

        ApiException ex = assertThrows(ApiException.class, () -> service.reviewClaim(issueId, created.id(),
                new za.codemaster.backend.dto.claim.ClaimReviewRequest(
                        za.codemaster.backend.dto.claim.ClaimReviewDecision.REQUEST_CHANGES, "too late"),
                maintainer));

        assertEquals("CLAIM_NOT_REVIEWABLE", ex.getCode());
    }

    @Test
    void reviewByNonMaintainerIsForbidden() {
        Long issueId = fixtures.issueId(0);
        ClaimDto created = service.createClaim(issueId, null, claimant);
        User stranger = otherUser();

        ApiException ex = assertThrows(ApiException.class, () -> service.reviewClaim(issueId, created.id(),
                new za.codemaster.backend.dto.claim.ClaimReviewRequest(
                        za.codemaster.backend.dto.claim.ClaimReviewDecision.CONFIRM_COMPLETED, null),
                stranger));

        assertEquals("FORBIDDEN", ex.getCode());
        assertEquals(org.springframework.http.HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void reviewOnMissingClaimThrowsClaimNotFound() {
        Long issueId = fixtures.issueId(0);
        User maintainer = otherUser();
        addMaintainer(issueId, maintainer);

        ApiException ex = assertThrows(ApiException.class, () -> service.reviewClaim(issueId, -999L,
                new za.codemaster.backend.dto.claim.ClaimReviewRequest(
                        za.codemaster.backend.dto.claim.ClaimReviewDecision.CONFIRM_COMPLETED, null),
                maintainer));

        assertEquals("CLAIM_NOT_FOUND", ex.getCode());
    }

    @Test
    void reviewOnMissingIssueThrowsIssueNotFound() {
        ApiException ex = assertThrows(ApiException.class, () -> service.reviewClaim(-999L, 1L,
                new za.codemaster.backend.dto.claim.ClaimReviewRequest(
                        za.codemaster.backend.dto.claim.ClaimReviewDecision.CONFIRM_COMPLETED, null),
                claimant));

        assertEquals("ISSUE_NOT_FOUND", ex.getCode());
    }
}
