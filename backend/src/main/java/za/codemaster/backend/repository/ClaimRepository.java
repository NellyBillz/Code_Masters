package za.codemaster.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import za.codemaster.backend.domain.model.Claim;
import za.codemaster.backend.domain.model.ClaimStatus;
import za.codemaster.backend.domain.model.PullRequestState;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing {@link Claim} records.
 */
@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {

    /**
     * Looks up an existing claim by issue, user, and status.
     * Used by the service layer to catch duplicate active claims prior to persisting.
     */
    Optional<Claim> findByIssueIdAndUserIdAndStatus(Long issueId, Long userId, ClaimStatus status);

    /**
     * Verifies if a user already holds an active claim on the specified issue.
     */
    boolean existsByIssueIdAndUserIdAndStatus(Long issueId, Long userId, ClaimStatus status);

    /**
     * Finds all active claims for a given issue.
     */
    List<Claim> findByIssueIdAndStatus(Long issueId, ClaimStatus status);

    /** Claims that are still eligible for automatic GitHub verification. */
    List<Claim> findByIssueIdAndStatusIn(Long issueId, Collection<ClaimStatus> statuses);

    /**
     * Finds all claims on an issue in a given status, oldest first.
     */
    List<Claim> findByIssueIdAndStatusOrderByCreatedAtAsc(Long issueId, ClaimStatus status);

    /**
     * Finds every claim on an issue regardless of status, oldest first. Backs
     * {@code GET /issues/{issueId}/claims} (API-03.5 — the spec's updated
     * description: "returns every claim on the issue regardless of status...
     * so a contributor or maintainer can see the issue's full history, not
     * just who is currently active").
     */
    List<Claim> findByIssueIdOrderByCreatedAtAsc(Long issueId);

    /**
     * Finds all claims associated with a given user.
     */
    List<Claim> findByUserId(Long userId);

    /**
     * Counts claims on an issue in a given status. Used to populate
     * {@code Issue.claimCount} without loading full claim rows.
     */
    long countByIssueIdAndStatus(Long issueId, ClaimStatus status);

    /**
     * Counts claims on an issue whose status is one of {@code statuses}.
     * Backs {@code Issue.claimCount} (API-03.5): in-flight claims only —
     * {@code active}/{@code changes_requested} — not {@code completed}/{@code released}.
     */
    long countByIssueIdAndStatusIn(Long issueId, Collection<ClaimStatus> statuses);

    /**
     * Counts a user's claims in a given status. Backs
     * {@code PublicUserProfile.contributionsCount} (API-03.5): verified
     * contributions only, i.e. status {@code completed} — not a raw claim count.
     */
    long countByUserIdAndStatus(Long userId, ClaimStatus status);

    /**
     * Finds a user's claims in a given status, most recent {@code completedAt}
     * first, paginated. Backs {@code GET /users/{username}/contributions}
     * (API-03.6) — always called with {@code ClaimStatus.COMPLETED}.
     */
    Page<Claim> findByUserIdAndStatusOrderByCompletedAtDesc(Long userId, ClaimStatus status, Pageable pageable);

    /**
     * Finds a project's claims in a given status (via {@code claim.issue.project}).
     * Backs {@code activeClaims} in {@code GET /users/me/maintainer-activity} (API-03.7).
     */
    List<Claim> findByIssueProjectIdAndStatus(Long projectId, ClaimStatus status);

    /**
     * Finds a project's claims in a given status with a given pull-request state.
     * Backs {@code claimsAwaitingReview} in {@code GET /users/me/maintainer-activity}
     * (API-03.7): a PR is attached ({@code pullRequestState: open}) and the claim
     * hasn't been reviewed yet ({@code status: active}) — once reviewed, the claim
     * moves to {@code changes_requested}/{@code completed} and drops out of this
     * specific query, even though it still appears elsewhere.
     */
    List<Claim> findByIssueProjectIdAndStatusAndPullRequestState(
            Long projectId, ClaimStatus status, PullRequestState pullRequestState);

    /**
     * Counts claims in a given status, platform-wide. Backs
     * {@code PlatformStats.totalActiveClaims}/{@code totalContributionsCompleted}
     * (API-03.12) — a plain {@code COUNT()}, not a full row load.
     */
    long countByStatus(ClaimStatus status);

    /**
     * Counts distinct users who have ever held a claim, in any status. Backs
     * {@code PlatformStats.totalContributorsEngaged} (API-03.12).
     */
    @Query("SELECT COUNT(DISTINCT c.user.id) FROM Claim c")
    long countDistinctUsers();
}
