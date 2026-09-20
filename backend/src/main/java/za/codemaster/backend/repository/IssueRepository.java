package za.codemaster.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.codemaster.backend.domain.model.Issue;

import java.util.Optional;

/**
 * Repository interface for querying and syncing {@link Issue} entities.
 */
@Repository
public interface IssueRepository extends JpaRepository<Issue, Long> {

    /**
     * Contract method for GH-2.3 GitHub issue synchronization.
     * Used by sync tasks to identify whether an issue already exists (update) or needs to be inserted.
     *
     * @param projectId the project repository database ID
     * @param githubIssueNumber the issue number in GitHub (e.g., 42)
     * @return an Optional containing the issue if it already exists
     */
    Optional<Issue> findByProjectIdAndGithubIssueNumber(Long projectId, Integer githubIssueNumber);

    /**
     * Contract method for API-01.5: dynamic filtering of issues scoped to a parent project.
     * Supports filtering by status, difficulty, beginner-friendly flag, and text[] label containment.
     *
     * @param projectId mandatory parent project identifier
     * @param status optional filter for issue status ('open', 'closed', 'claimed')
     * @param difficulty optional filter for issue difficulty level
     * @param isBeginnerFriendly optional flag for beginner friendly issues
     * @param label optional label to check for presence in the issue's labels array
     * @param pageable pagination parameters
     * @return paged list of matching issues
     */
    @Query(
        value = """
            SELECT * FROM issues i
            WHERE i.project_id = :projectId
              AND (:status IS NULL OR i.status = :status)
              AND (:difficulty IS NULL OR i.difficulty = :difficulty)
              AND (:isBeginnerFriendly IS NULL OR i.is_beginner_friendly = :isBeginnerFriendly)
              AND (:label IS NULL OR :label = ANY(i.labels))
        """,
        countQuery = """
            SELECT count(*) FROM issues i
            WHERE i.project_id = :projectId
              AND (:status IS NULL OR i.status = :status)
              AND (:difficulty IS NULL OR i.difficulty = :difficulty)
              AND (:isBeginnerFriendly IS NULL OR i.is_beginner_friendly = :isBeginnerFriendly)
              AND (:label IS NULL OR :label = ANY(i.labels))
        """,
        nativeQuery = true
    )
    Page<Issue> findWithFilters(
        @Param("projectId") Long projectId,
        @Param("status") String status,
        @Param("difficulty") String difficulty,
        @Param("isBeginnerFriendly") Boolean isBeginnerFriendly,
        @Param("label") String label,
        Pageable pageable
    );

    /**
     * Native PostgreSQL relevance-ranked search query.
     * Combines full-text search over the generated tsvector (title + body_excerpt)
     * with trigram similarity fallback on title for typo tolerance.
     *
     * @param q optional free-text search query (supports typos via pg_trgm)
     * @param projectId optional parent project filter
     * @param status optional status filter ('open', 'closed', 'claimed')
     * @param difficulty optional difficulty filter ('beginner', 'intermediate', 'advanced', 'unknown')
     * @param isBeginnerFriendly optional flag filter
     * @param label optional label filter matching against PostgreSQL text[]
     * @param sortByRelevance if true, sorts descending by ts_rank + trigram similarity
     * @param pageable pagination parameters
     * @return paged list of matching issues
     */
    @Query(
        value = """
            SELECT i.*
            FROM issues i
            WHERE (:projectId IS NULL OR i.project_id = :projectId)
              AND (:status IS NULL OR :status = '' OR i.status = :status)
              AND (:difficulty IS NULL OR :difficulty = '' OR i.difficulty = :difficulty)
              AND (:isBeginnerFriendly IS NULL OR i.is_beginner_friendly = :isBeginnerFriendly)
              AND (:label IS NULL OR :label = '' OR :label = ANY(i.labels))
              AND (
                    :q IS NULL OR :q = ''
                    OR i.tsv @@ plainto_tsquery('english', :q)
                    OR similarity(i.title, :q) >= 0.2
                    OR word_similarity(:q, i.title) >= 0.25
                    OR i.title % :q
                  )
            ORDER BY
              CASE WHEN :sortByRelevance = true THEN (
                ts_rank(i.tsv, plainto_tsquery('english', coalesce(:q, ''))) + similarity(i.title, coalesce(:q, ''))
              ) END DESC NULLS LAST,
              i.id DESC
        """,
        countQuery = """
            SELECT count(*)
            FROM issues i
            WHERE (:projectId IS NULL OR i.project_id = :projectId)
              AND (:status IS NULL OR :status = '' OR i.status = :status)
              AND (:difficulty IS NULL OR :difficulty = '' OR i.difficulty = :difficulty)
              AND (:isBeginnerFriendly IS NULL OR i.is_beginner_friendly = :isBeginnerFriendly)
              AND (:label IS NULL OR :label = '' OR :label = ANY(i.labels))
              AND (
                    :q IS NULL OR :q = ''
                    OR i.tsv @@ plainto_tsquery('english', :q)
                    OR similarity(i.title, :q) >= 0.2
                    OR word_similarity(:q, i.title) >= 0.25
                    OR i.title % :q
                  )
        """,
        nativeQuery = true
    )
    Page<Issue> searchIssues(
        @Param("q") String q,
        @Param("projectId") Long projectId,
        @Param("status") String status,
        @Param("difficulty") String difficulty,
        @Param("isBeginnerFriendly") Boolean isBeginnerFriendly,
        @Param("label") String label,
        @Param("sortByRelevance") boolean sortByRelevance,
        Pageable pageable
    );
}