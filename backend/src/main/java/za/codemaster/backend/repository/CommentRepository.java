package za.codemaster.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.codemaster.backend.domain.model.Comment;

import java.util.List;

/**
 * Repository interface for managing {@link Comment} persistence and retrieval.
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * Retrieves all comments attached to a specific project, excluding soft-deleted rows.
     */
    List<Comment> findByProjectIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long projectId);

    /**
     * Retrieves all comments attached to a specific issue, excluding soft-deleted rows.
     */
    List<Comment> findByIssueIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long issueId);

    /**
     * Paginated, oldest-first retrieval of active comments for a project.
     * Backs {@code GET /projects/{projectId}/comments} (API-02.3).
     */
    Page<Comment> findByProjectIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long projectId, Pageable pageable);

    /**
     * Paginated, oldest-first retrieval of active comments for an issue.
     * Backs {@code GET /issues/{issueId}/comments} (API-02.3).
     */
    Page<Comment> findByIssueIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long issueId, Pageable pageable);

    /**
     * The most recent active comments on a project — attached directly to the
     * project, or to any of its issues — newest first. Backs {@code recentComments}
     * in {@code GET /users/me/maintainer-activity} (API-03.7); {@code pageable}
     * caps the result (e.g. {@code PageRequest.of(0, 10)}) without needing a
     * full {@code Page} count query.
     * <p>
     * Uses an explicit {@code LEFT JOIN} on {@code c.issue}, not implicit
     * {@code c.issue.project.id} dot-navigation: a project-attached comment has
     * a null {@code issue}, and implicit navigation through a null-valued
     * association compiles to an inner join, which would silently drop that
     * row from the whole query (including the {@code c.project.id} side of the
     * {@code OR}) before the {@code WHERE} clause is even evaluated.
     */
    @Query("""
        SELECT c FROM Comment c
        LEFT JOIN c.issue i
        WHERE c.deletedAt IS NULL
          AND (c.project.id = :projectId OR i.project.id = :projectId)
        ORDER BY c.createdAt DESC
    """)
    List<Comment> findRecentByProjectOrItsIssues(@Param("projectId") Long projectId, Pageable pageable);

    /**
     * Unresolved blocking questions across a project's issues, oldest first
     * (a maintainer should clear the longest-waiting one first) — the "Ask
     * before you claim" triage queue. Backs {@code unansweredQuestions} in
     * {@code GET /users/me/maintainer-activity} (mirrors
     * {@code claimsAwaitingReview}'s "flagged, not yet actioned" shape).
     */
    List<Comment> findByIssueProjectIdAndIsQuestionTrueAndResolvedAtIsNullAndDeletedAtIsNullOrderByCreatedAtAsc(Long projectId);
}