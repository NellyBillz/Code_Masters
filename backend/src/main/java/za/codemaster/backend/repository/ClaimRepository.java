package za.codemaster.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
     * Finds a project's claims in a given status (via {@code claim.issue.project}).
     * Backs {@code activeClaims} in {@code GET /users/me/maintainer-activity} (API-03.7).
     */
    List<Claim> findByIssueProjectIdAndStatus(Long projectId, ClaimStatus status);

    /**
     * Counts a project's claims in a given status (via {@code claim.issue.project}).
     * Backs {@code ProjectContributionContext.completedContributionsCount}
     * (API-04.1), always called with {@code ClaimStatus.COMPLETED}.
     */
    long countByIssueProjectIdAndStatus(Long projectId, ClaimStatus status);

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
     * Counts distinct users who have ever held a claim (any status) OR were
     * ever an accepted collaborator on someone else's claim (any status of
     * that claim). Backs {@code PlatformStats.totalContributorsEngaged}
     * (API-03.12) — a collaborator did real work and should count as an
     * engaged contributor even on the (uncommon) path where they never
     * separately owned a claim of their own.
     */
    @Query(value = "SELECT COUNT(DISTINCT engaged_user_id) FROM ("
            + "SELECT user_id AS engaged_user_id FROM claims "
            + "UNION "
            + "SELECT requester_user_id AS engaged_user_id FROM claim_collaboration_requests WHERE status = 'accepted'"
            + ") AS engaged_users", nativeQuery = true)
    long countDistinctUsersIncludingCollaborators();

    /**
     * Every credited contributor, platform-wide, with their contribution
     * count — the raw material for the Recognition Leaderboard (wow-feature,
     * 2026-09-24), before {@code LeaderboardService} assigns rank numbers.
     * Same crediting rule as {@link #countCreditedContributions}, just
     * computed for every user in one query instead of one at a time: a
     * completed claim credits its owner, plus any accepted collaborator on
     * that same completed claim. The two inner {@code SELECT}s are deduped
     * by {@code UNION} on the {@code (user, claim)} pair, so a claim can
     * never double-count the same user twice. Deliberately unbounded — this
     * platform's real user count is small enough that ranking the full list
     * in memory is simpler and safer to demo than DB-side pagination, same
     * reasoning as {@code IssueRepository#findOpenIssuesOnPublishedAcceptingProjects}.
     * Ties (equal counts) break on username alphabetically, so rank order is
     * always deterministic across requests — not a "fair" tie-breaking rule,
     * just a stable one.
     */
    @Query(value = """
            SELECT u.id AS user_id, u.username AS username, u.display_name AS display_name,
                   u.avatar_url AS avatar_url, COUNT(*) AS contribution_count
            FROM (
                SELECT c.user_id AS engaged_user_id, c.id AS claim_id
                FROM claims c WHERE c.status = 'completed'
                UNION
                SELECT ccr.requester_user_id AS engaged_user_id, ccr.claim_id AS claim_id
                FROM claim_collaboration_requests ccr
                JOIN claims c ON c.id = ccr.claim_id
                WHERE ccr.status = 'accepted' AND c.status = 'completed'
            ) AS credited
            JOIN users u ON u.id = credited.engaged_user_id
            GROUP BY u.id, u.username, u.display_name, u.avatar_url
            ORDER BY contribution_count DESC, u.username ASC
            """, nativeQuery = true)
    List<LeaderboardRow> findLeaderboardRows();

    /**
     * A user's real, credited contribution count: claims they own with
     * status {@code completed}, plus claims they were an accepted
     * collaborator on with status {@code completed}. Backs
     * {@code PublicUserProfile.contributionsCount} — superseding a plain
     * {@code countByUserIdAndStatus(userId, COMPLETED)}, which only ever
     * counted ownership.
     */
    @Query("SELECT COUNT(DISTINCT c) FROM Claim c WHERE c.status = za.codemaster.backend.domain.model.ClaimStatus.COMPLETED "
            + "AND (c.user.id = :userId OR EXISTS ("
            + "  SELECT 1 FROM ClaimCollaborationRequest ccr "
            + "  WHERE ccr.claim = c AND ccr.requester.id = :userId "
            + "  AND ccr.status = za.codemaster.backend.domain.model.CollaborationRequestStatus.ACCEPTED"
            + "))")
    long countCreditedContributions(@Param("userId") Long userId);

    /**
     * How many distinct projects a user has a real, credited contribution on
     * — same eligibility rule as {@link #countCreditedContributions}, just
     * counting distinct {@code issue.project} instead of distinct claims.
     * Backs {@code PublicUserProfile.projectsCount} (Contributor Passport
     * wow-feature, 2026-09-24), previously a documented dead field nothing
     * computed.
     */
    @Query("SELECT COUNT(DISTINCT c.issue.project.id) FROM Claim c WHERE c.status = za.codemaster.backend.domain.model.ClaimStatus.COMPLETED "
            + "AND (c.user.id = :userId OR EXISTS ("
            + "  SELECT 1 FROM ClaimCollaborationRequest ccr "
            + "  WHERE ccr.claim = c AND ccr.requester.id = :userId "
            + "  AND ccr.status = za.codemaster.backend.domain.model.CollaborationRequestStatus.ACCEPTED"
            + "))")
    long countDistinctCreditedProjects(@Param("userId") Long userId);

    /**
     * A user's real, credited contribution history: claims they own with
     * status {@code completed}, plus claims they were an accepted
     * collaborator on with status {@code completed}, most recent
     * {@code completedAt} first, paginated. Backs
     * {@code GET /users/{username}/contributions} (API-03.6) — superseding
     * {@link #findByUserIdAndStatusOrderByCompletedAtDesc}, which only ever
     * returned owned claims.
     */
    @Query(value = "SELECT DISTINCT c FROM Claim c WHERE c.status = za.codemaster.backend.domain.model.ClaimStatus.COMPLETED "
            + "AND (c.user.id = :userId OR EXISTS ("
            + "  SELECT 1 FROM ClaimCollaborationRequest ccr "
            + "  WHERE ccr.claim = c AND ccr.requester.id = :userId "
            + "  AND ccr.status = za.codemaster.backend.domain.model.CollaborationRequestStatus.ACCEPTED"
            + ")) ORDER BY c.completedAt DESC",
            countQuery = "SELECT COUNT(DISTINCT c) FROM Claim c WHERE c.status = za.codemaster.backend.domain.model.ClaimStatus.COMPLETED "
            + "AND (c.user.id = :userId OR EXISTS ("
            + "  SELECT 1 FROM ClaimCollaborationRequest ccr "
            + "  WHERE ccr.claim = c AND ccr.requester.id = :userId "
            + "  AND ccr.status = za.codemaster.backend.domain.model.CollaborationRequestStatus.ACCEPTED"
            + "))")
    Page<Claim> findCreditedContributions(@Param("userId") Long userId, Pageable pageable);
}
