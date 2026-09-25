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
import za.codemaster.backend.domain.model.Claim;
import za.codemaster.backend.domain.model.ClaimStatus;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.dto.claim.ClaimDto;
import za.codemaster.backend.dto.collaboration.ClaimCollaborationRequestDto;
import za.codemaster.backend.dto.collaboration.CollaborationRequestStatusDto;
import za.codemaster.backend.dto.collaboration.CollaborationResponseDecision;
import za.codemaster.backend.exception.ApiException;
import za.codemaster.backend.repository.ClaimCollaborationRequestRepository;
import za.codemaster.backend.repository.ClaimRepository;
import za.codemaster.backend.repository.IssueRepository;
import za.codemaster.backend.repository.NotificationRepository;
import za.codemaster.backend.repository.ProjectMaintainerRepository;
import za.codemaster.backend.repository.ProjectRepository;
import za.codemaster.backend.repository.UserRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the collaboration-request flow end to end against a real (test)
 * Postgres database, same pattern as {@code ClaimServiceTest}: a request to
 * join a claim, the owner's accept/decline, and the two rules that make this
 * genuinely "join a claim" and not "a second independent claim" —
 * (1) accepting releases the requester's own separate active claim on the
 * same issue, if they hold one, and (2) once a claim completes, both the
 * owner and every accepted collaborator are credited for it.
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
class ClaimCollaborationServiceTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private ProjectMaintainerRepository projectMaintainerRepository;

    @Autowired
    private ClaimCollaborationRequestRepository collaborationRequestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    private ClaimCollaborationService service;
    private ClaimService claimService;
    private ProjectQueryServiceFixtures fixtures;
    private Long issueId;
    private User owner;
    private User requester;

    @BeforeEach
    void setUp() {
        // A generous limit — this class isn't testing API-03.10's rate limiting.
        RateLimitService unlimitedRateLimitService = new RateLimitService(1_000_000, 1_000_000, 1_000_000, 1_000_000, 1_000_000, 1_000_000);
        service = new ClaimCollaborationService(
                collaborationRequestRepository, claimRepository, issueRepository, unlimitedRateLimitService,
                notificationService);
        claimService = new ClaimService(claimRepository, issueRepository, projectMaintainerRepository,
                unlimitedRateLimitService, collaborationRequestRepository, notificationService);
        fixtures = ProjectQueryServiceFixtures.seed(projectRepository, issueRepository);
        issueId = fixtures.issueId(0);
        owner = newUser("owner");
        requester = newUser("requester");
    }

    private User newUser(String label) {
        return userRepository.save(User.builder()
                .githubId(System.nanoTime())
                .username(label + "_" + System.nanoTime())
                .displayName(label)
                .build());
    }

    private Claim ownedClaim(User claimant, ClaimStatus status) {
        Claim claim = new Claim();
        claim.setIssue(issueRepository.findById(issueId).orElseThrow());
        claim.setUser(claimant);
        claim.setStatus(status);
        return claimRepository.saveAndFlush(claim);
    }

    @Test
    void requestingCollaborationCreatesAPendingRequest() {
        Claim claim = ownedClaim(owner, ClaimStatus.ACTIVE);

        ClaimCollaborationRequestDto created = service.requestCollaboration(issueId, claim.getId(), requester);

        assertEquals(CollaborationRequestStatusDto.PENDING, created.status());
        assertEquals(requester.getUsername(), created.requester().username());
        assertEquals(claim.getId(), created.claimId());
        assertEquals(issueId, created.issueId());
    }

    @Test
    void cannotRequestToCollaborateOnYourOwnClaim() {
        Claim claim = ownedClaim(owner, ClaimStatus.ACTIVE);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.requestCollaboration(issueId, claim.getId(), owner));

        assertEquals("VALIDATION_ERROR", ex.getCode());
    }

    @Test
    void cannotRequestToJoinACompletedClaim() {
        Claim claim = ownedClaim(owner, ClaimStatus.COMPLETED);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.requestCollaboration(issueId, claim.getId(), requester));

        assertEquals("CLAIM_NOT_JOINABLE", ex.getCode());
    }

    @Test
    void cannotRequestToJoinAReleasedClaim() {
        Claim claim = ownedClaim(owner, ClaimStatus.RELEASED);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.requestCollaboration(issueId, claim.getId(), requester));

        assertEquals("CLAIM_NOT_JOINABLE", ex.getCode());
    }

    @Test
    void aSecondLiveRequestFromTheSameRequesterIsRejected() {
        Claim claim = ownedClaim(owner, ClaimStatus.ACTIVE);
        service.requestCollaboration(issueId, claim.getId(), requester);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.requestCollaboration(issueId, claim.getId(), requester));

        assertEquals("COLLABORATION_ALREADY_REQUESTED", ex.getCode());
    }

    @Test
    void requestingIsAllowedEvenWithoutHoldingYourOwnClaimOnTheIssue() {
        // requester holds no claim at all on this issue — the product rule is
        // "regardless of whether they have their own claim or not."
        Claim claim = ownedClaim(owner, ClaimStatus.ACTIVE);

        ClaimCollaborationRequestDto created = service.requestCollaboration(issueId, claim.getId(), requester);

        assertEquals(CollaborationRequestStatusDto.PENDING, created.status());
    }

    @Test
    void ownerAcceptingCreatesAnAcceptedCollaborator() {
        Claim claim = ownedClaim(owner, ClaimStatus.ACTIVE);
        ClaimCollaborationRequestDto pending = service.requestCollaboration(issueId, claim.getId(), requester);

        ClaimCollaborationRequestDto accepted = service.respondToRequest(
                issueId, claim.getId(), pending.id(), CollaborationResponseDecision.ACCEPT, owner);

        assertEquals(CollaborationRequestStatusDto.ACCEPTED, accepted.status());

        ClaimDto claimDto = claimService.toDto(claimRepository.findById(claim.getId()).orElseThrow());
        assertEquals(1, claimDto.collaborators().size());
        assertEquals(requester.getUsername(), claimDto.collaborators().get(0).username());
    }

    @Test
    void ownerDecliningLeavesTheRequesterUncredited() {
        Claim claim = ownedClaim(owner, ClaimStatus.ACTIVE);
        ClaimCollaborationRequestDto pending = service.requestCollaboration(issueId, claim.getId(), requester);

        ClaimCollaborationRequestDto declined = service.respondToRequest(
                issueId, claim.getId(), pending.id(), CollaborationResponseDecision.DECLINE, owner);

        assertEquals(CollaborationRequestStatusDto.DECLINED, declined.status());
        ClaimDto claimDto = claimService.toDto(claimRepository.findById(claim.getId()).orElseThrow());
        assertTrue(claimDto.collaborators().isEmpty());
    }

    @Test
    void onlyTheClaimOwnerCanRespondToARequest() {
        Claim claim = ownedClaim(owner, ClaimStatus.ACTIVE);
        ClaimCollaborationRequestDto pending = service.requestCollaboration(issueId, claim.getId(), requester);
        User someoneElse = newUser("someone_else");

        ApiException ex = assertThrows(ApiException.class,
                () -> service.respondToRequest(issueId, claim.getId(), pending.id(),
                        CollaborationResponseDecision.ACCEPT, someoneElse));

        assertEquals("FORBIDDEN", ex.getCode());
    }

    @Test
    void respondingTwiceToTheSameRequestIsRejected() {
        Claim claim = ownedClaim(owner, ClaimStatus.ACTIVE);
        ClaimCollaborationRequestDto pending = service.requestCollaboration(issueId, claim.getId(), requester);
        service.respondToRequest(issueId, claim.getId(), pending.id(), CollaborationResponseDecision.DECLINE, owner);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.respondToRequest(issueId, claim.getId(), pending.id(),
                        CollaborationResponseDecision.ACCEPT, owner));

        assertEquals("COLLABORATION_NOT_PENDING", ex.getCode());
    }

    @Test
    void acceptingReleasesTheRequestersOwnSeparateActiveClaimOnTheSameIssue() {
        Claim ownerClaim = ownedClaim(owner, ClaimStatus.ACTIVE);
        Claim requesterOwnClaim = ownedClaim(requester, ClaimStatus.ACTIVE);

        ClaimCollaborationRequestDto pending =
                service.requestCollaboration(issueId, ownerClaim.getId(), requester);
        service.respondToRequest(issueId, ownerClaim.getId(), pending.id(), CollaborationResponseDecision.ACCEPT, owner);

        Claim reloaded = claimRepository.findById(requesterOwnClaim.getId()).orElseThrow();
        assertEquals(ClaimStatus.RELEASED, reloaded.getStatus(),
                "the requester's own separate claim on the same issue should be released on acceptance");
    }

    @Test
    void bothOwnerAndAcceptedCollaboratorAreCreditedOnceTheClaimCompletes() {
        Claim claim = ownedClaim(owner, ClaimStatus.ACTIVE);
        ClaimCollaborationRequestDto pending = service.requestCollaboration(issueId, claim.getId(), requester);
        service.respondToRequest(issueId, claim.getId(), pending.id(), CollaborationResponseDecision.ACCEPT, owner);

        assertEquals(0, claimRepository.countCreditedContributions(owner.getId()));
        assertEquals(0, claimRepository.countCreditedContributions(requester.getId()));

        claim.setStatus(ClaimStatus.COMPLETED);
        claimRepository.save(claim);

        assertEquals(1, claimRepository.countCreditedContributions(owner.getId()),
                "the claim's owner should be credited once it completes");
        assertEquals(1, claimRepository.countCreditedContributions(requester.getId()),
                "the accepted collaborator should be credited too, automatically, once it completes");
    }

    @Test
    void requesterCanCancelTheirOwnPendingRequest() {
        Claim claim = ownedClaim(owner, ClaimStatus.ACTIVE);
        ClaimCollaborationRequestDto pending = service.requestCollaboration(issueId, claim.getId(), requester);

        service.cancelRequest(issueId, claim.getId(), pending.id(), requester);

        List<ClaimCollaborationRequestDto> history = service.listRequests(issueId, claim.getId());
        assertEquals(1, history.size());
        assertEquals(CollaborationRequestStatusDto.CANCELLED, history.get(0).status());
    }

    @Test
    void onlyTheRequesterCanCancelTheirOwnRequest() {
        Claim claim = ownedClaim(owner, ClaimStatus.ACTIVE);
        ClaimCollaborationRequestDto pending = service.requestCollaboration(issueId, claim.getId(), requester);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.cancelRequest(issueId, claim.getId(), pending.id(), owner));

        assertEquals("FORBIDDEN", ex.getCode());
    }

    @Test
    void aCancelledRequestCanBeFollowedByANewRequestFromTheSamePerson() {
        Claim claim = ownedClaim(owner, ClaimStatus.ACTIVE);
        ClaimCollaborationRequestDto first = service.requestCollaboration(issueId, claim.getId(), requester);
        service.cancelRequest(issueId, claim.getId(), first.id(), requester);

        ClaimCollaborationRequestDto second = service.requestCollaboration(issueId, claim.getId(), requester);

        assertEquals(CollaborationRequestStatusDto.PENDING, second.status());
        assertFalse(second.id().equals(first.id()));
    }

    @Test
    void listRequestsReturnsFullHistoryRegardlessOfStatus() {
        Claim claim = ownedClaim(owner, ClaimStatus.ACTIVE);
        User secondRequester = newUser("second_requester");
        ClaimCollaborationRequestDto declined = service.requestCollaboration(issueId, claim.getId(), requester);
        service.respondToRequest(issueId, claim.getId(), declined.id(), CollaborationResponseDecision.DECLINE, owner);
        service.requestCollaboration(issueId, claim.getId(), secondRequester);

        List<ClaimCollaborationRequestDto> history = service.listRequests(issueId, claim.getId());

        assertEquals(2, history.size());
    }

    @Test
    void requestingCollaborationNotifiesTheClaimOwner() {
        Claim claim = ownedClaim(owner, ClaimStatus.ACTIVE);

        service.requestCollaboration(issueId, claim.getId(), requester);

        var notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(owner.getId(), PageRequest.of(0, 10));
        assertEquals(1, notifications.getTotalElements());
        assertEquals("collaboration_requested", notifications.getContent().get(0).getType().getValue());
        assertEquals("/issues/" + issueId, notifications.getContent().get(0).getLink());
    }

    @Test
    void acceptingNotifiesTheRequester() {
        Claim claim = ownedClaim(owner, ClaimStatus.ACTIVE);
        ClaimCollaborationRequestDto pending = service.requestCollaboration(issueId, claim.getId(), requester);

        service.respondToRequest(issueId, claim.getId(), pending.id(), CollaborationResponseDecision.ACCEPT, owner);

        var notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(requester.getId(), PageRequest.of(0, 10));
        assertEquals(1, notifications.getTotalElements());
        assertEquals("collaboration_responded", notifications.getContent().get(0).getType().getValue());
        assertTrue(notifications.getContent().get(0).getMessage().contains("accepted"));
    }

    @Test
    void decliningNotifiesTheRequester() {
        Claim claim = ownedClaim(owner, ClaimStatus.ACTIVE);
        ClaimCollaborationRequestDto pending = service.requestCollaboration(issueId, claim.getId(), requester);

        service.respondToRequest(issueId, claim.getId(), pending.id(), CollaborationResponseDecision.DECLINE, owner);

        var notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(requester.getId(), PageRequest.of(0, 10));
        assertEquals(1, notifications.getTotalElements());
        assertTrue(notifications.getContent().get(0).getMessage().contains("declined"));
    }

    @Test
    void bothOwnerAndCollaboratorAreNotifiedWhenTheClaimCompletes() {
        Claim claim = ownedClaim(owner, ClaimStatus.ACTIVE);
        ClaimCollaborationRequestDto pending = service.requestCollaboration(issueId, claim.getId(), requester);
        service.respondToRequest(issueId, claim.getId(), pending.id(), CollaborationResponseDecision.ACCEPT, owner);
        notificationRepository.deleteAll(); // isolate: only care about notifications from completion itself

        claim.setStatus(ClaimStatus.COMPLETED);
        claimRepository.save(claim);
        claimService.notifyClaimCompleted(claim, issueRepository.findById(issueId).orElseThrow());

        assertEquals(1, notificationRepository.findByUserIdOrderByCreatedAtDesc(owner.getId(), PageRequest.of(0, 10)).getTotalElements());
        assertEquals(1, notificationRepository.findByUserIdOrderByCreatedAtDesc(requester.getId(), PageRequest.of(0, 10)).getTotalElements());
    }
}
