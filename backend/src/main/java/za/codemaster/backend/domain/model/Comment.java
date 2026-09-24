package za.codemaster.backend.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Represents a comment attached to either a {@link Project} or an {@link Issue}.
 * <p>
 * Exactly one parent association ({@code project} OR {@code issue}) must be non-null.
 * This constraint is enforced at the database level by {@code chk_comments_target_exclusive}
 * and pre-validated in the service layer before persistence to provide clean 400 Bad Request responses.
 */
@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
public class Comment {

    /**
     * Primary key identifier for the comment.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The author of the comment.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Target project association. Nullable; must be set if {@code issue} is null.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    /**
     * Target issue association. Nullable; must be set if {@code project} is null.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id")
    private Issue issue;

    /**
     * The text content of the comment.
     */
    @Column(name = "body", nullable = false)
    private String body;

    /**
     * Indicates whether the comment content has been edited post-creation.
     */
    @Column(name = "edited")
    private Boolean edited = false;

    /**
     * Soft-deletion timestamp. Null if the comment is active.
     */
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    /**
     * Flags this comment as a blocking question — "I need this answered
     * before I can start" — distinct from general discussion. Only
     * meaningful on an issue comment (never set by
     * {@code CommentService#createProjectComment}); surfaces in the
     * maintainer activity rollup as its own triaged queue until resolved.
     */
    @Column(name = "is_question", nullable = false)
    private Boolean isQuestion = false;

    /** When a maintainer marked this question resolved. Null while still open. */
    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    /**
     * Creation timestamp generated automatically by the database default.
     */
    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Last update timestamp generated automatically by the database default.
     */
    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;
}