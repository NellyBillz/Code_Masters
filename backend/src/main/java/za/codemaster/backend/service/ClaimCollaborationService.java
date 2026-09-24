package za.codemaster.backend.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.codemaster.backend.domain.model.Claim;
import za.codemaster.backend.domain.model.ClaimCollaborationRequest;
import za.codemaster.backend.domain.model.ClaimStatus;
import za.codemaster.backend.domain.model.CollaborationRequestStatus;
import za.codemaster.backend.domain.model.NotificationType;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.dto.collaboration.ClaimCollaborationRequestDto;
import za.codemaster.backend.dto.collaboration.CollaborationRequestStatusDto;
import za.codemaster.backend.dto.collaboration.CollaborationResponseDecision;
import za.codemaster.backend.dto.user.PublicUserProfile;
import za.codemaster.backend.exception.ApiException;
import za.codemaster.backend.repository.ClaimCollaborationRequestRepository;
import za.codemaster.backend.repository.ClaimRepository;
import za.codemaster.backend.repository.IssueRepository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Requests to join another contributor's claim as a collaborator. Deliberately
 * not a merge of two claims: a claim keeps exactly one owner ({@link Claim#getUser()},
 * "whoever opened the claim first" — the one a pull request is expected to be
 * attached to on GitHub). Once accepted, the requester is credited alongside
 * the owner: {@link ClaimRepository#countCreditedContributions} and
 * {@link ClaimRepository#findCreditedContributions} both count a completed
 * claim for every accepted collaborator, not just its owner.
 * <p>
 * A contributor may request to join regardless of whether they currently
 * hold their own separate claim on the same issue — if they do, accepting
 * the request releases that separate claim (a person works on an issue
 * either as the sole claimant or as a collaborator on someone else's claim,
 * not both at once).
 */
@Service
public class ClaimCollaborationService {

    private static final List<ClaimStatus> JOINABLE_CLAIM_STATUSES =
            List.of(ClaimStatus.ACTIVE, ClaimStatus.CHANGES_REQUESTED);
    private static final List<CollaborationRequestStatus> LIVE_REQUEST_STATUSES =
            List.of(CollaborationRequestStatus.PENDING, CollaborationRequestStatus.ACCEPTED);

    private final ClaimCollaborationRequestRepository collaborationRequestRepository;
    private final ClaimRepository claimRepository;
    private final IssueRepository issueRepository;
    private final RateLimitService rateLimitService;
    private final NotificationService notificationService;

    public ClaimCollaborationService(ClaimCollaborationRequestRepository collaborationRequestRepository,
                                      ClaimRepository claimRepository,
                                      IssueRepository issueRepository,
                                      RateLimitService rateLimitService,
                                      NotificationService notificationService) {
        this.collaborationRequestRepository = collaborationRequestRepository;
        this.claimRepository = claimRepository;
        this.issueRepository = issueRepository;
        this.rateLimitService = rateLimitService;
        this.notificationService = notificationService;
    }

    /**
     * {@code POST /issues/{issueId}/claims/{claimId}/collaboration-requests}:
     * request to join a claim the caller doesn't own.
     *
     * @throws ApiException with code {@code RATE_LIMITED} (429) if the caller has sent too many
     *                       collaboration requests in the last hour,
     *                       {@code ISSUE_NOT_FOUND} (404) if the issue doesn't exist,
     *                       {@code CLAIM_NOT_FOUND} (404) if no claim exists on that issue,
     *                       {@code VALIDATION_ERROR} (400) if the caller owns the claim,
     *                       {@code CLAIM_NOT_JOINABLE} (409) if the claim is {@code completed}
     *                       or {@code released}, or
     *                       {@code COLLABORATION_ALREADY_REQUESTED} (409) if the caller already
     *                       has a pending or accepted request on this claim
     */
    @Transactional
    public ClaimCollaborationRequestDto requestCollaboration(Long issueId, Long claimId, User caller) {
        rateLimitService.checkCollaborationLimit(caller.getId());

        Claim claim = findClaimOnIssue(issueId, claimId);

        if (claim.getUser().getId().equals(caller.getId())) {
            throw new ApiException(
                    "VALIDATION_ERROR", "You cannot request to collaborate on your own claim.", HttpStatus.BAD_REQUEST);
        }

        if (!JOINABLE_CLAIM_STATUSES.contains(claim.getStatus())) {
            throw new ApiException(
                    "CLAIM_NOT_JOINABLE", "This claim is no longer open to collaborators.", HttpStatus.CONFLICT);
        }

        if (collaborationRequestRepository
                .findByClaimIdAndRequesterIdAndStatusIn(claimId, caller.getId(), LIVE_REQUEST_STATUSES)
                .isPresent()) {
            throw new ApiException(
                    "COLLABORATION_ALREADY_REQUESTED",
                    "You already have a pending or accepted request on this claim.",
                    HttpStatus.CONFLICT);
        }

        ClaimCollaborationRequest request = new ClaimCollaborationRequest();
        request.setClaim(claim);
        request.setRequester(caller);
        request.setStatus(CollaborationRequestStatus.PENDING);

        try {
            // saveAndFlush forces the insert (and any constraint violation) to happen
            // right here, same reasoning as ClaimService.createClaim.
            ClaimCollaborationRequestDto created = toDto(collaborationRequestRepository.saveAndFlush(request));
            notificationService.notify(claim.getUser(), NotificationType.COLLABORATION_REQUESTED,
                    (caller.getDisplayName() != null ? caller.getDisplayName() : caller.getUsername())
                            + " wants to collaborate on your claim for \"" + claim.getIssue().getTitle() + "\"",
                    "/issues/" + issueId);
            return created;
        } catch (DataIntegrityViolationException ex) {
            throw new ApiException(
                    "COLLABORATION_ALREADY_REQUESTED",
                    "You already have a pending or accepted request on this claim.",
                    HttpStatus.CONFLICT);
        }
    }

    /**
     * {@code POST /issues/{issueId}/claims/{claimId}/collaboration-requests/{requestId}/response}:
     * the claim owner's decision on a pending request. Accepting releases the
     * requester's own separate active claim on the same issue, if they hold one.
     *
     * @throws ApiException with code {@code ISSUE_NOT_FOUND} (404), {@code CLAIM_NOT_FOUND} (404),
     *                       {@code COLLABORATION_REQUEST_NOT_FOUND} (404),
     *                       {@code FORBIDDEN} (403) if the caller isn't the claim's owner, or
     *                       {@code COLLABORATION_NOT_PENDING} (409) if the request was already
     *                       responded to
     */
    @Transactional
    public ClaimCollaborationRequestDto respondToRequest(
            Long issueId, Long claimId, Long requestId, CollaborationResponseDecision decision, User caller) {
        Claim claim = findClaimOnIssue(issueId, claimId);

        if (!claim.getUser().getId().equals(caller.getId())) {
            throw new ApiException(
                    "FORBIDDEN", "Only the claim's owner may respond to a collaboration request.", HttpStatus.FORBIDDEN);
        }

        ClaimCollaborationRequest request = findRequestOnClaim(claimId, requestId);

        if (request.getStatus() != CollaborationRequestStatus.PENDING) {
            throw new ApiException(
                    "COLLABORATION_NOT_PENDING", "This request has already been responded to.", HttpStatus.CONFLICT);
        }

        if (decision == CollaborationResponseDecision.ACCEPT) {
            // A person works on an issue either as the sole claimant or as a
            // collaborator on someone else's claim, not both — release their
            // own separate active claim on this issue, if they hold one.
            // Flushed immediately, same flush-ordering reasoning as
            // ClaimService.releaseClaim.
            claimRepository.findByIssueIdAndUserIdAndStatus(issueId, request.getRequester().getId(), ClaimStatus.ACTIVE)
                    .filter(ownClaim -> !ownClaim.getId().equals(claimId))
                    .ifPresent(ownClaim -> {
                        ownClaim.setStatus(ClaimStatus.RELEASED);
                        claimRepository.saveAndFlush(ownClaim);
                    });
            request.setStatus(CollaborationRequestStatus.ACCEPTED);
        } else {
            request.setStatus(CollaborationRequestStatus.DECLINED);
        }

        request.setRespondedAt(OffsetDateTime.now());
        ClaimCollaborationRequestDto updated = toDto(collaborationRequestRepository.save(request));

        notificationService.notify(request.getRequester(), NotificationType.COLLABORATION_RESPONDED,
                decision == CollaborationResponseDecision.ACCEPT
                        ? "Your request to collaborate on \"" + claim.getIssue().getTitle() + "\" was accepted!"
                        : "Your request to collaborate on \"" + claim.getIssue().getTitle() + "\" was declined.",
                "/issues/" + issueId);

        return updated;
    }

    /**
     * {@code DELETE /issues/{issueId}/claims/{claimId}/collaboration-requests/{requestId}}:
     * the requester withdraws their own still-pending request.
     *
     * @throws ApiException with code {@code ISSUE_NOT_FOUND} (404), {@code CLAIM_NOT_FOUND} (404),
     *                       {@code COLLABORATION_REQUEST_NOT_FOUND} (404),
     *                       {@code FORBIDDEN} (403) if the caller isn't the request's own author, or
     *                       {@code COLLABORATION_NOT_PENDING} (409) if the request was already
     *                       responded to
     */
    @Transactional
    public void cancelRequest(Long issueId, Long claimId, Long requestId, User caller) {
        findClaimOnIssue(issueId, claimId);
        ClaimCollaborationRequest request = findRequestOnClaim(claimId, requestId);

        if (!request.getRequester().getId().equals(caller.getId())) {
            throw new ApiException(
                    "FORBIDDEN", "Only the request's own author may cancel it.", HttpStatus.FORBIDDEN);
        }

        if (request.getStatus() != CollaborationRequestStatus.PENDING) {
            throw new ApiException(
                    "COLLABORATION_NOT_PENDING", "Only a pending request can be cancelled.", HttpStatus.CONFLICT);
        }

        request.setStatus(CollaborationRequestStatus.CANCELLED);
        request.setRespondedAt(OffsetDateTime.now());
        collaborationRequestRepository.save(request);
    }

    /**
     * {@code GET /issues/{issueId}/claims/{claimId}/collaboration-requests}:
     * every request on a claim, regardless of status, oldest first — same
     * "full history, not just the live state" philosophy as
     * {@code ClaimService.getClaims}. Public, no auth required, same
     * visibility as the claims themselves.
     *
     * @throws ApiException with code {@code ISSUE_NOT_FOUND} (404) or {@code CLAIM_NOT_FOUND} (404)
     */
    @Transactional(readOnly = true)
    public List<ClaimCollaborationRequestDto> listRequests(Long issueId, Long claimId) {
        findClaimOnIssue(issueId, claimId);
        return collaborationRequestRepository.findByClaimIdOrderByCreatedAtAsc(claimId).stream()
                .map(this::toDto)
                .toList();
    }

    private Claim findClaimOnIssue(Long issueId, Long claimId) {
        if (!issueRepository.existsById(issueId)) {
            throw new ApiException(
                    "ISSUE_NOT_FOUND", "No issue exists with id " + issueId, HttpStatus.NOT_FOUND);
        }
        return claimRepository.findById(claimId)
                .filter(c -> c.getIssue().getId().equals(issueId))
                .orElseThrow(() -> new ApiException(
                        "CLAIM_NOT_FOUND", "No claim exists with id " + claimId + " on issue " + issueId,
                        HttpStatus.NOT_FOUND));
    }

    private ClaimCollaborationRequest findRequestOnClaim(Long claimId, Long requestId) {
        return collaborationRequestRepository.findById(requestId)
                .filter(r -> r.getClaim().getId().equals(claimId))
                .orElseThrow(() -> new ApiException(
                        "COLLABORATION_REQUEST_NOT_FOUND",
                        "No collaboration request exists with id " + requestId + " on claim " + claimId,
                        HttpStatus.NOT_FOUND));
    }

    private ClaimCollaborationRequestDto toDto(ClaimCollaborationRequest entity) {
        return new ClaimCollaborationRequestDto(
                entity.getId(),
                entity.getClaim().getId(),
                entity.getClaim().getIssue().getId(),
                toPublicProfile(entity.getRequester()),
                CollaborationRequestStatusDto.valueOf(entity.getStatus().name()),
                entity.getCreatedAt(),
                entity.getRespondedAt()
        );
    }

    /** Same mapping as {@code ClaimService.toPublicProfile}. */
    private PublicUserProfile toPublicProfile(User user) {
        return new PublicUserProfile(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getBio(),
                user.getLocation(),
                user.getSkills() == null ? List.of() : List.of(user.getSkills()),
                null,
                (int) claimRepository.countCreditedContributions(user.getId()),
                user.getReputation()
        );
    }
}
