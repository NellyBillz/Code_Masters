package za.codemaster.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import za.codemaster.backend.BackendApplication;
import za.codemaster.backend.domain.model.Claim;
import za.codemaster.backend.domain.model.ClaimStatus;
import za.codemaster.backend.domain.model.Issue;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.dto.user.PublicUserProfile;
import za.codemaster.backend.dto.user.UpdateUserRequest;
import za.codemaster.backend.dto.user.UserProfile;
import za.codemaster.backend.exception.ApiException;
import za.codemaster.backend.repository.ClaimRepository;
import za.codemaster.backend.repository.IssueRepository;
import za.codemaster.backend.repository.ProjectMaintainerRepository;
import za.codemaster.backend.repository.ProjectRepository;
import za.codemaster.backend.repository.UserRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies API-02.9's acceptance criteria (round2-tickets.md): the caller's own
 * profile includes {@code email}, which the public profile endpoint never returns.
 * Also verifies API-02.10's: updating only one field leaves the others untouched.
 * <p>
 * Runs against a real (test) Postgres database, same pattern as
 * {@code CommentServiceTest}/{@code ClaimServiceTest}.
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
class UserServiceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMaintainerRepository projectMaintainerRepository;

    private UserService service;

    @BeforeEach
    void setUp() {
        ProjectQueryService projectQueryService =
                new ProjectQueryService(projectRepository, issueRepository, claimRepository, projectMaintainerRepository);
        service = new UserService(userRepository, claimRepository, projectQueryService);
    }

    private User createUser(String username, String email) {
        return userRepository.save(User.builder()
                .githubId(System.nanoTime())
                .username(username)
                .displayName("Display " + username)
                .email(email)
                .bio("bio")
                .location("Cape Town")
                .skills(new String[]{"Java", "TypeScript"})
                .build());
    }

    @Test
    void currentUserProfileIncludesEmailAndGithubAccess() {
        String suffix = String.valueOf(System.nanoTime());
        User user = createUser("me_" + suffix, "me_" + suffix + "@example.com");

        UserProfile profile = service.getCurrentUserProfile(user);

        assertEquals("me_" + suffix + "@example.com", profile.email());
        assertTrue(profile.githubAccess());
        assertEquals(user.getUsername(), profile.publicProfile().username());
        assertEquals(user.getId(), profile.publicProfile().id());
    }

    @Test
    void publicProfileNeverIncludesEmail() {
        String suffix = String.valueOf(System.nanoTime());
        User user = createUser("pub_" + suffix, "pub_" + suffix + "@example.com");

        PublicUserProfile profile = service.getPublicProfile(user.getUsername());

        assertEquals(user.getUsername(), profile.username());
        assertEquals(user.getDisplayName(), profile.displayName());
        assertEquals(user.getBio(), profile.bio());
        assertEquals(user.getLocation(), profile.location());
        assertEquals(java.util.List.of("Java", "TypeScript"), profile.skills());
        // PublicUserProfile has no email field at all — the type system itself
        // enforces this isn't returned, this just documents the intent.
    }

    @Test
    void publicProfileOnUnknownUsernameThrowsUserNotFound() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.getPublicProfile("no-such-user-xyz"));

        assertEquals("USER_NOT_FOUND", ex.getCode());
        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, ex.getStatus());
    }

    private Issue seedIssue() {
        ProjectQueryServiceFixtures fixtures = ProjectQueryServiceFixtures.seed(projectRepository, issueRepository);
        return issueRepository.findById(fixtures.issueId(0)).orElseThrow();
    }

    private Claim claim(Issue issue, User user, ClaimStatus status) {
        Claim claim = new Claim();
        claim.setIssue(issue);
        claim.setUser(user);
        claim.setStatus(status);
        return claimRepository.save(claim);
    }

    @Test
    void contributionsCountOnlyCountsCompletedClaims() {
        String suffix = String.valueOf(System.nanoTime());
        User user = createUser("contrib_" + suffix, "contrib_" + suffix + "@example.com");
        Issue issue = seedIssue();

        claim(issue, user, ClaimStatus.ACTIVE);
        claim(issue, user, ClaimStatus.CHANGES_REQUESTED);
        claim(issue, user, ClaimStatus.RELEASED);
        claim(issue, user, ClaimStatus.COMPLETED);

        PublicUserProfile profile = service.getPublicProfile(user.getUsername());

        assertEquals(1, profile.contributionsCount(),
                "only the single completed claim should count, not the active/changes_requested/released ones");
    }

    @Test
    void contributionsCountIncrementsWithEachAdditionalCompletedClaim() {
        String suffix = String.valueOf(System.nanoTime());
        User user = createUser("multicontrib_" + suffix, "multicontrib_" + suffix + "@example.com");
        Issue issueA = seedIssue();
        Issue issueB = seedIssue();

        claim(issueA, user, ClaimStatus.COMPLETED);
        assertEquals(1, service.getPublicProfile(user.getUsername()).contributionsCount());

        claim(issueB, user, ClaimStatus.COMPLETED);
        assertEquals(2, service.getPublicProfile(user.getUsername()).contributionsCount());
    }

    @Test
    void contributionsCountIsZeroForAUserWithNoCompletedClaims() {
        String suffix = String.valueOf(System.nanoTime());
        User user = createUser("nocontrib_" + suffix, "nocontrib_" + suffix + "@example.com");
        Issue issue = seedIssue();

        claim(issue, user, ClaimStatus.ACTIVE);

        assertEquals(0, service.getPublicProfile(user.getUsername()).contributionsCount());
    }

    @Test
    void projectsCountCountsEachDistinctProjectOnceEvenWithMultipleCompletedIssuesOnIt() {
        String suffix = String.valueOf(System.nanoTime());
        User user = createUser("projectscount_" + suffix, "projectscount_" + suffix + "@example.com");
        ProjectQueryServiceFixtures fixtures = ProjectQueryServiceFixtures.seed(projectRepository, issueRepository);
        // issueId(0) and issueId(1) both belong to the same fixture project (OpenLearn SA).
        Issue sameProjectIssueA = issueRepository.findById(fixtures.issueId(0)).orElseThrow();
        Issue sameProjectIssueB = issueRepository.findById(fixtures.issueId(1)).orElseThrow();

        completedClaim(sameProjectIssueA, user, java.time.OffsetDateTime.now());
        assertEquals(1, service.getPublicProfile(user.getUsername()).projectsCount());

        completedClaim(sameProjectIssueB, user, java.time.OffsetDateTime.now());
        assertEquals(1, service.getPublicProfile(user.getUsername()).projectsCount(),
                "a second completed contribution on the SAME project must not double-count it");
    }

    @Test
    void projectsCountIncrementsOnlyWhenACompletedContributionLandsOnANewProject() {
        String suffix = String.valueOf(System.nanoTime());
        User user = createUser("multiproj_" + suffix, "multiproj_" + suffix + "@example.com");
        Issue issueOnProjectA = seedIssue();
        Issue issueOnProjectB = seedIssue(); // a fresh seed() call, so this is a distinct project row

        assertEquals(0, service.getPublicProfile(user.getUsername()).projectsCount());

        completedClaim(issueOnProjectA, user, java.time.OffsetDateTime.now());
        assertEquals(1, service.getPublicProfile(user.getUsername()).projectsCount());

        completedClaim(issueOnProjectB, user, java.time.OffsetDateTime.now());
        assertEquals(2, service.getPublicProfile(user.getUsername()).projectsCount());
    }

    private Claim completedClaim(Issue issue, User user, java.time.OffsetDateTime completedAt) {
        Claim claim = new Claim();
        claim.setIssue(issue);
        claim.setUser(user);
        claim.setStatus(ClaimStatus.COMPLETED);
        claim.setCompletionSource(za.codemaster.backend.domain.model.CompletionSource.MAINTAINER_CONFIRMED);
        claim.setCompletedAt(completedAt);
        return claimRepository.save(claim);
    }

    @Test
    void contributionsShowsExactlyTheCompletedClaimsAmongAMixOfStatuses() {
        String suffix = String.valueOf(System.nanoTime());
        User user = createUser("history_" + suffix, "history_" + suffix + "@example.com");
        java.time.OffsetDateTime now = java.time.OffsetDateTime.now();

        completedClaim(seedIssue(), user, now.minusDays(2));
        completedClaim(seedIssue(), user, now.minusDays(1));
        claim(seedIssue(), user, ClaimStatus.ACTIVE);
        claim(seedIssue(), user, ClaimStatus.RELEASED);

        za.codemaster.backend.dto.user.PagedContributions contributions =
                service.getContributions(user.getUsername(), null, null);

        assertEquals(2, contributions.items().size(),
                "only the 2 completed claims should appear, not the active or released ones");
        assertTrue(contributions.items().stream()
                .allMatch(c -> c.status() == za.codemaster.backend.dto.claim.ClaimStatusDto.COMPLETED));
        assertEquals(2, contributions.meta().total());
    }

    @Test
    void contributionsAreOrderedMostRecentCompletedAtFirst() {
        String suffix = String.valueOf(System.nanoTime());
        User user = createUser("order_" + suffix, "order_" + suffix + "@example.com");
        java.time.OffsetDateTime now = java.time.OffsetDateTime.now();

        Claim older = completedClaim(seedIssue(), user, now.minusDays(5));
        Claim newer = completedClaim(seedIssue(), user, now.minusDays(1));

        za.codemaster.backend.dto.user.PagedContributions contributions =
                service.getContributions(user.getUsername(), null, null);

        assertEquals(newer.getCompletedAt(), contributions.items().get(0).completedAt());
        assertEquals(older.getCompletedAt(), contributions.items().get(1).completedAt());
    }

    @Test
    void contributionShapeIncludesIssueProjectAndVerificationDetails() {
        String suffix = String.valueOf(System.nanoTime());
        User user = createUser("shape_" + suffix, "shape_" + suffix + "@example.com");
        Issue issue = seedIssue();
        Claim completed = completedClaim(issue, user, java.time.OffsetDateTime.now());
        completed.setPullRequestUrl("https://github.com/example-org/repo/pull/42");
        claimRepository.save(completed);

        za.codemaster.backend.dto.user.Contribution contribution =
                service.getContributions(user.getUsername(), null, null).items().get(0);

        assertEquals(issue.getId(), contribution.issue().id());
        assertEquals(issue.getProject().getId(), contribution.project().id());
        assertEquals("https://github.com/example-org/repo/pull/42", contribution.pullRequestUrl());
        assertEquals(za.codemaster.backend.dto.claim.ClaimCompletionSourceDto.MAINTAINER_CONFIRMED,
                contribution.completionSource());
    }

    @Test
    void contributionsForUnknownUsernameThrowsUserNotFound() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.getContributions("no-such-user-xyz", null, null));

        assertEquals("USER_NOT_FOUND", ex.getCode());
        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void contributionsForAUserWithNoneIsAnEmptyList() {
        String suffix = String.valueOf(System.nanoTime());
        User user = createUser("empty_" + suffix, "empty_" + suffix + "@example.com");

        za.codemaster.backend.dto.user.PagedContributions contributions =
                service.getContributions(user.getUsername(), null, null);

        assertTrue(contributions.items().isEmpty());
        assertEquals(0, contributions.meta().total());
    }

    @Test
    void updatingOnlyBioLeavesOtherFieldsUntouched() {
        String suffix = String.valueOf(System.nanoTime());
        User user = createUser("update_" + suffix, "update_" + suffix + "@example.com");

        UserProfile updated = service.updateCurrentUserProfile(user, new UpdateUserRequest(null, "new bio", null, null));

        assertEquals("new bio", updated.publicProfile().bio());
        assertEquals("Display update_" + suffix, updated.publicProfile().displayName(), "displayName wasn't provided, should be untouched");
        assertEquals("Cape Town", updated.publicProfile().location(), "location wasn't provided, should be untouched");
        assertEquals(List.of("Java", "TypeScript"), updated.publicProfile().skills(), "skills weren't provided, should be untouched");
    }

    @Test
    void updateCanChangeAllFieldsAtOnce() {
        String suffix = String.valueOf(System.nanoTime());
        User user = createUser("updateall_" + suffix, "updateall_" + suffix + "@example.com");

        UpdateUserRequest request = new UpdateUserRequest(
                "New Display Name", "New bio", "Nairobi", List.of("Go", "Rust"));
        UserProfile updated = service.updateCurrentUserProfile(user, request);

        assertEquals("New Display Name", updated.publicProfile().displayName());
        assertEquals("New bio", updated.publicProfile().bio());
        assertEquals("Nairobi", updated.publicProfile().location());
        assertEquals(List.of("Go", "Rust"), updated.publicProfile().skills());
    }

    @Test
    void updatePersistsToTheDatabase() {
        String suffix = String.valueOf(System.nanoTime());
        User user = createUser("persist_" + suffix, "persist_" + suffix + "@example.com");

        service.updateCurrentUserProfile(user, new UpdateUserRequest("Persisted Name", null, null, null));

        User reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertEquals("Persisted Name", reloaded.getDisplayName());
    }

    @Test
    void deleteAnonymizesAllPersonalFields() {
        String suffix = String.valueOf(System.nanoTime());
        String originalUsername = "delete_" + suffix;
        User user = createUser(originalUsername, "delete_" + suffix + "@example.com");
        Long originalGithubId = user.getGithubId();

        service.deleteCurrentUser(user);

        User reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertEquals("deleted-user-" + user.getId(), reloaded.getUsername());
        assertNull(reloaded.getDisplayName());
        assertNull(reloaded.getAvatarUrl());
        assertNull(reloaded.getBio());
        assertNull(reloaded.getLocation());
        assertNull(reloaded.getSkills());
        assertNull(reloaded.getEmail());
        assertNotEquals(originalGithubId, reloaded.getGithubId());
        assertTrue(reloaded.getGithubId() < 0, "githubId must be replaced with a sentinel a real GitHub id can never equal");
    }

    @Test
    void deletedUsersOldUsernameNoLongerResolves() {
        String suffix = String.valueOf(System.nanoTime());
        String originalUsername = "gone_" + suffix;
        User user = createUser(originalUsername, "gone_" + suffix + "@example.com");

        service.deleteCurrentUser(user);

        ApiException ex = assertThrows(ApiException.class, () -> service.getPublicProfile(originalUsername));
        assertEquals("USER_NOT_FOUND", ex.getCode());
    }

    @Test
    void deletedUsersAnonymizedProfileIsStillReachableByItsNewUsername() {
        String suffix = String.valueOf(System.nanoTime());
        User user = createUser("stillhere_" + suffix, "stillhere_" + suffix + "@example.com");

        service.deleteCurrentUser(user);

        PublicUserProfile profile = service.getPublicProfile("deleted-user-" + user.getId());
        assertEquals("deleted-user-" + user.getId(), profile.username());
    }

    @Test
    void deletionPreservesCompletedContributionsUnderTheAnonymizedIdentity() {
        String suffix = String.valueOf(System.nanoTime());
        User user = createUser("shipper_" + suffix, "shipper_" + suffix + "@example.com");
        Issue issue = seedIssue();
        Claim completed = completedClaim(issue, user, java.time.OffsetDateTime.now());

        service.deleteCurrentUser(user);

        // The claim itself is untouched — still completed, still attached to the
        // same (now-anonymized) user row. This is the fact GET /stats (API-03.12,
        // not yet built) will depend on to keep counting this contribution.
        Claim reloadedClaim = claimRepository.findById(completed.getId()).orElseThrow();
        assertEquals(ClaimStatus.COMPLETED, reloadedClaim.getStatus());
        assertEquals(user.getId(), reloadedClaim.getUser().getId());

        // Still fetchable as a contribution under the new (anonymized) username.
        var contributions = service.getContributions("deleted-user-" + user.getId(), null, null);
        assertEquals(1, contributions.items().size());
    }

    @Test
    void deletingAMissingUserThrowsUserNotFound() {
        User ghostUser = User.builder().id(-999L).githubId(-999L).username("ghost").build();

        ApiException ex = assertThrows(ApiException.class, () -> service.deleteCurrentUser(ghostUser));

        assertEquals("USER_NOT_FOUND", ex.getCode());
        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, ex.getStatus());
    }
}
