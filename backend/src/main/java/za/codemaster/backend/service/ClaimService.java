package za.codemaster.backend.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.codemaster.backend.domain.model.Claim;
import za.codemaster.backend.domain.model.ClaimStatus;
import za.codemaster.backend.domain.model.CompletionSource;
import za.codemaster.backend.domain.model.Issue;
import za.codemaster.backend.domain.model.PullRequestState;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.dto.claim.ClaimCompletionSourceDto;
import za.codemaster.backend.dto.claim.ClaimDto;
import za.codemaster.backend.dto.claim.ClaimReviewDecision;
import za.codemaster.backend.dto.claim.ClaimReviewRequest;
import za.codemaster.backend.dto.claim.ClaimStatusDto;
import za.codemaster.backend.dto.claim.PullRequestStateDto;
import za.codemaster.backend.dto.user.PublicUserProfile;
import za.codemaster.backend.exception.ApiException;
import za.codemaster.backend.repository.ClaimRepository;
import za.codemaster.backend.repository.IssueRepository;
import za.codemaster.backend.repository.ProjectMaintainerRepository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Service orchestrating issue claims (API-02.5, design doc §8 step 6): a lightweight
 * local signal of contributor intent, deliberately non-exclusive (design doc §7) —
 * any number of different users may each hold an active claim on the same issue.
 */
@Service
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final IssueRepository issueRepository;
    private final ProjectMaintainerRepository projectMaintainerRepository;

    public ClaimService(ClaimRepository claimRepository, IssueRepository issueRepository,
                         ProjectMaintainerRepository projectMaintainerRepository) {
        this.claimRepository = claimRepository;
        this.issueRepository = issueRepository;
        this.projectMaintainerRepository = projectMaintainerRepository;
    }

    /**
     * Creates an active claim on an issue for the caller.
     * <p>
     * Uniqueness of "one active claim per (issue, user)" is enforced by a partial
     * unique index in the database (see {@link Claim}'s Javadoc), not pre-checked
     * here with a separate query that could race — this method always attempts the
     * insert and translates a resulting constraint violation into a clean 409,
     * per this ticket's explicit instruction.
     *
     * @param issueId the issue id from the path
     * @param note    an optional short note (already length-validated by {@code @Valid} on the request DTO)
     * @param caller  the authenticated caller, injected via {@code @AuthenticatedUser}
     * @return the created claim, in API shape
     * @throws ApiException with code {@code ISSUE_NOT_FOUND} (404) if the issue doesn't exist, or
     *                       {@code CLAIM_ALREADY_ACTIVE} (409) if the caller already holds an active
     *                       claim on this issue
     */
    @Transactional
    public ClaimDto createClaim(Long issueId, String note, User caller) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ApiException(
                        "ISSUE_NOT_FOUND", "No issue exists with id " + issueId, HttpStatus.NOT_FOUND));

        Claim claim = new Claim();
        claim.setIssue(issue);
        claim.setUser(caller);
        claim.setNote(note);
        claim.setStatus(ClaimStatus.ACTIVE);

        try {
            // saveAndFlush forces the insert (and any constraint violation) to
            // happen right here, rather than being deferred to a later flush.
            return toDto(claimRepository.saveAndFlush(claim));
        } catch (DataIntegrityViolationException ex) {
            throw new ApiException(
                    "CLAIM_ALREADY_ACTIVE",
                    "You already have an active claim on this issue.",
                    HttpStatus.CONFLICT);
        }
    }

    /**
     * Releases the caller's own active claim on an issue.
     *
     * @param issueId the issue id from the path
     * @param caller  the authenticated caller, injected via {@code @AuthenticatedUser}
     * @throws ApiException with code {@code ISSUE_NOT_FOUND} (404) if the issue doesn't exist, or
     *                       {@code CLAIM_NOT_FOUND} (404) if the caller has no active claim to release
     */
    @Transactional
    public void releaseClaim(Long issueId, User caller) {
        if (!issueRepository.existsById(issueId)) {
            throw new ApiException(
                    "ISSUE_NOT_FOUND", "No issue exists with id " + issueId, HttpStatus.NOT_FOUND);
        }

        Claim claim = claimRepository.findByIssueIdAndUserIdAndStatus(issueId, caller.getId(), ClaimStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(
                        "CLAIM_NOT_FOUND", "You do not have an active claim on this issue.", HttpStatus.NOT_FOUND));

        claim.setStatus(ClaimStatus.RELEASED);
        // Flushed immediately, not just saved: Hibernate's default flush order runs
        // inserts before updates, so a later createClaim() in the same flush cycle
        // could send its INSERT to Postgres before this release UPDATE lands — tripping
        // the partial unique index even though the release logically happened first.
        claimRepository.saveAndFlush(claim);
    }

    /**
     * Attaches or updates the pull request link on the caller's own claim
     * (API-03.3, design doc §13/§7): the contributor's way of showing real
     * progress on an issue they claimed. Sets {@code pullRequestState: OPEN} as
     * an honest immediate default — GitHub sync (GH-03.2) is what later
     * corrects it to {@code MERGED}/{@code CLOSED_UNMERGED} once it can check
     * the real state on GitHub.
     *
     * @param issueId        the issue id from the path
     * @param claimId        the claim id from the path
     * @param pullRequestUrl the contributor's own open pull request for this issue
     * @param caller         the authenticated caller, injected via {@code @AuthenticatedUser}
     * @return the updated claim, in API shape
     * @throws ApiException with code {@code ISSUE_NOT_FOUND} (404) if the issue doesn't exist,
     *                       {@code CLAIM_NOT_FOUND} (404) if no claim with that id exists on that
     *                       issue, or {@code FORBIDDEN} (403) if the caller doesn't own the claim
     */
    @Transactional
    public ClaimDto attachPullRequest(Long issueId, Long claimId, String pullRequestUrl, User caller) {
        if (!issueRepository.existsById(issueId)) {
            throw new ApiException(
                    "ISSUE_NOT_FOUND", "No issue exists with id " + issueId, HttpStatus.NOT_FOUND);
        }

        Claim claim = claimRepository.findById(claimId)
                .filter(c -> c.getIssue().getId().equals(issueId))
                .orElseThrow(() -> new ApiException(
                        "CLAIM_NOT_FOUND", "No claim exists with id " + claimId + " on issue " + issueId,
                        HttpStatus.NOT_FOUND));

        if (!claim.getUser().getId().equals(caller.getId())) {
            throw new ApiException(
                    "FORBIDDEN", "Only the claim's own owner may attach a pull request to it.",
                    HttpStatus.FORBIDDEN);
        }

        claim.setPullRequestUrl(pullRequestUrl);
        claim.setPullRequestState(PullRequestState.OPEN);

        return toDto(claimRepository.save(claim));
    }

    /**
     * A maintainer's review decision on a claim (API-03.4): either request changes
     * with feedback, or manually confirm completion (the fallback for cases GitHub
     * sync can't verify itself — design doc §13.3).
     * <p>
     * A {@code released} claim can never be reviewed — there's nothing left to act
     * on. A {@code request_changes} decision requires non-blank {@code feedback},
     * enforced here rather than as a bean-validation constraint since it's a
     * cross-field rule, not a property of {@code feedback} alone. Per this file's
     * own decision #2 (round3-tickets.md): {@code confirm_completed} on an
     * already-{@code completed} claim is a no-op that returns the claim unchanged,
     * not an error — a maintainer re-confirming shouldn't be punished for it.
     * <p>
     * Judgment call beyond what the ticket pins down: {@code request_changes} on
     * an already-{@code completed} claim is also rejected as
     * {@code CLAIM_NOT_REVIEWABLE}, the same as a released claim — allowing a
     * maintainer to move a verified, already-counted-in-{@code /stats} claim back
     * to {@code changes_requested} would silently undo a real, counted
     * contribution. The ticket only pins down the {@code confirm_completed}
     * re-confirmation case; this extends the same "completed is terminal"
     * reasoning to the other decision.
     *
     * @param issueId the issue id from the path
     * @param claimId the claim id from the path
     * @param request the review decision and, for {@code request_changes}, feedback
     * @param caller  the authenticated caller, injected via {@code @AuthenticatedUser}
     * @return the reviewed (or, for an idempotent re-confirmation, unchanged) claim, in API shape
     * @throws ApiException with code {@code ISSUE_NOT_FOUND} (404) if the issue doesn't exist,
     *                       {@code CLAIM_NOT_FOUND} (404) if no claim with that id exists on that issue,
     *                       {@code FORBIDDEN} (403) if the caller isn't a maintainer of the project,
     *                       {@code VALIDATION_ERROR} (400) if {@code request_changes} has no feedback, or
     *                       {@code CLAIM_NOT_REVIEWABLE} (409) if the claim is {@code released}, or
     *                       {@code completed} and the decision is {@code request_changes}
     */
    @Transactional
    public ClaimDto reviewClaim(Long issueId, Long claimId, ClaimReviewRequest request, User caller) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ApiException(
                        "ISSUE_NOT_FOUND", "No issue exists with id " + issueId, HttpStatus.NOT_FOUND));

        Claim claim = claimRepository.findById(claimId)
                .filter(c -> c.getIssue().getId().equals(issueId))
                .orElseThrow(() -> new ApiException(
                        "CLAIM_NOT_FOUND", "No claim exists with id " + claimId + " on issue " + issueId,
                        HttpStatus.NOT_FOUND));

        if (!projectMaintainerRepository.existsByProjectIdAndUserId(issue.getProject().getId(), caller.getId())) {
            throw new ApiException(
                    "FORBIDDEN", "Only a maintainer of this project may review a claim.", HttpStatus.FORBIDDEN);
        }

        if (claim.getStatus() == ClaimStatus.RELEASED) {
            throw new ApiException(
                    "CLAIM_NOT_REVIEWABLE", "A released claim can no longer be reviewed.", HttpStatus.CONFLICT);
        }

        if (request.decision() == ClaimReviewDecision.REQUEST_CHANGES) {
            if (claim.getStatus() == ClaimStatus.COMPLETED) {
                throw new ApiException(
                        "CLAIM_NOT_REVIEWABLE", "A completed claim can no longer be reviewed.", HttpStatus.CONFLICT);
            }
            if (request.feedback() == null || request.feedback().isBlank()) {
                throw new ApiException(
                        "VALIDATION_ERROR", "feedback is required when decision is request_changes.",
                        HttpStatus.BAD_REQUEST);
            }
            claim.setStatus(ClaimStatus.CHANGES_REQUESTED);
            claim.setMaintainerFeedback(request.feedback());
            return toDto(claimRepository.save(claim));
        }

        // decision == CONFIRM_COMPLETED
        if (claim.getStatus() == ClaimStatus.COMPLETED) {
            return toDto(claim);
        }
        claim.setStatus(ClaimStatus.COMPLETED);
        claim.setCompletionSource(CompletionSource.MAINTAINER_CONFIRMED);
        claim.setCompletedAt(OffsetDateTime.now());
        return toDto(claimRepository.save(claim));
    }

    /**
     * Lists every claim on an issue, regardless of status, oldest first
     * (API-03.5, spec's updated description of this endpoint) — a contributor
     * or maintainer should see the issue's full claim history, including
     * claims that already resolved to {@code changes_requested}, {@code completed},
     * or {@code released}, not just who currently holds an active one.
     *
     * @throws ApiException with code {@code ISSUE_NOT_FOUND} (404) if the issue doesn't exist
     */
    @Transactional(readOnly = true)
    public List<ClaimDto> getClaims(Long issueId) {
        if (!issueRepository.existsById(issueId)) {
            throw new ApiException(
                    "ISSUE_NOT_FOUND", "No issue exists with id " + issueId, HttpStatus.NOT_FOUND);
        }

        return claimRepository.findByIssueIdOrderByCreatedAtAsc(issueId).stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Maps a persisted claim row to the API's {@link ClaimDto} shape.
     * Public: reused by {@code MaintainerActivityService} (API-03.7) rather
     * than duplicating this mapping there.
     */
    public ClaimDto toDto(Claim entity) {
        return new ClaimDto(
                entity.getId(),
                entity.getIssue().getId(),
                toPublicProfile(entity.getUser()),
                ClaimStatusDto.valueOf(entity.getStatus().name()),
                entity.getNote(),
                entity.getPullRequestUrl(),
                PullRequestStateDto.valueOf(entity.getPullRequestState().name()),
                entity.getMaintainerFeedback(),
                entity.getCompletionSource() == null
                        ? null
                        : ClaimCompletionSourceDto.valueOf(entity.getCompletionSource().name()),
                entity.getCompletedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    /**
     * Maps a claim's holder to the API's {@link PublicUserProfile} shape.
     * {@code projectsCount} is still a placeholder — nothing computes it yet.
     * {@code contributionsCount} (API-03.5) counts only this user's
     * {@code completed} claims.
     */
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
                (int) claimRepository.countByUserIdAndStatus(user.getId(), ClaimStatus.COMPLETED),
                user.getReputation()
        );
    }
}
