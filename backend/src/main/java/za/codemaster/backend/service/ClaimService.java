package za.codemaster.backend.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.codemaster.backend.domain.model.Claim;
import za.codemaster.backend.domain.model.ClaimStatus;
import za.codemaster.backend.domain.model.Issue;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.dto.PublicUserProfile;
import za.codemaster.backend.exception.ApiException;
import za.codemaster.backend.repository.ClaimRepository;
import za.codemaster.backend.repository.IssueRepository;

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

    public ClaimService(ClaimRepository claimRepository, IssueRepository issueRepository) {
        this.claimRepository = claimRepository;
        this.issueRepository = issueRepository;
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
    public za.codemaster.backend.dto.Claim createClaim(Long issueId, String note, User caller) {
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
     * Lists an issue's active claims, oldest first (design doc §7).
     *
     * @throws ApiException with code {@code ISSUE_NOT_FOUND} (404) if the issue doesn't exist
     */
    @Transactional(readOnly = true)
    public List<za.codemaster.backend.dto.Claim> getActiveClaims(Long issueId) {
        if (!issueRepository.existsById(issueId)) {
            throw new ApiException(
                    "ISSUE_NOT_FOUND", "No issue exists with id " + issueId, HttpStatus.NOT_FOUND);
        }

        return claimRepository.findByIssueIdAndStatusOrderByCreatedAtAsc(issueId, ClaimStatus.ACTIVE).stream()
                .map(this::toDto)
                .toList();
    }

    /** Maps a persisted claim row to the API's {@link za.codemaster.backend.dto.Claim} shape. */
    private za.codemaster.backend.dto.Claim toDto(Claim entity) {
        return new za.codemaster.backend.dto.Claim(
                entity.getId(),
                entity.getIssue().getId(),
                toPublicProfile(entity.getUser()),
                za.codemaster.backend.dto.ClaimStatus.valueOf(entity.getStatus().name()),
                entity.getNote(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    /**
     * Maps a claim's holder to the API's {@link PublicUserProfile} shape.
     * Same {@code projectsCount}/{@code contributionsCount} placeholder note as
     * {@code CommentService.toPublicProfile} — nothing computes those yet.
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
                null,
                user.getReputation()
        );
    }
}
