package za.codemaster.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
}