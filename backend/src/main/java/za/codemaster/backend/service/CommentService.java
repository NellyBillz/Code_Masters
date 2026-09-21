package za.codemaster.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.codemaster.backend.domain.model.ClaimStatus;
import za.codemaster.backend.domain.model.Comment;
import za.codemaster.backend.domain.model.Issue;
import za.codemaster.backend.domain.model.Project;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.dto.common.PageMeta;
import za.codemaster.backend.dto.comment.CommentDto;
import za.codemaster.backend.dto.comment.PagedComments;
import za.codemaster.backend.dto.user.PublicUserProfile;
import za.codemaster.backend.exception.ApiException;
import za.codemaster.backend.exception.CommentValidationException;
import za.codemaster.backend.repository.ClaimRepository;
import za.codemaster.backend.repository.CommentRepository;
import za.codemaster.backend.repository.IssueRepository;
import za.codemaster.backend.repository.ProjectMaintainerRepository;
import za.codemaster.backend.repository.ProjectRepository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Service orchestrating comment persistence and retrieval for both projects and
 * issues (API-02.3, design doc §8 step 5), with application-level target validation.
 */
@Service
public class CommentService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final CommentRepository commentRepository;
    private final ProjectRepository projectRepository;
    private final IssueRepository issueRepository;
    private final ProjectMaintainerRepository projectMaintainerRepository;
    private final ClaimRepository claimRepository;
    private final RateLimitService rateLimitService;

    public CommentService(CommentRepository commentRepository,
                           ProjectRepository projectRepository,
                           IssueRepository issueRepository,
                           ProjectMaintainerRepository projectMaintainerRepository,
                           ClaimRepository claimRepository,
                           RateLimitService rateLimitService) {
        this.commentRepository = commentRepository;
        this.projectRepository = projectRepository;
        this.issueRepository = issueRepository;
        this.projectMaintainerRepository = projectMaintainerRepository;
        this.claimRepository = claimRepository;
        this.rateLimitService = rateLimitService;
    }

    /**
     * Creates a comment on a project.
     *
     * @param projectId the project id from the path
     * @param body      the comment text, already length-validated by {@code @Valid} on the request DTO
     * @param author    the authenticated caller, injected via {@code @AuthenticatedUser}
     * @return the created comment, in API shape
     * @throws ApiException with code {@code RATE_LIMITED} (429, API-03.10) if the caller has posted
     *                       too many comments in the last hour, or
     *                       {@code PROJECT_NOT_FOUND} (404) if the project doesn't exist
     */
    @Transactional
    public CommentDto createProjectComment(Long projectId, String body, User author) {
        rateLimitService.checkCommentLimit(author.getId());

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException(
                        "PROJECT_NOT_FOUND", "No project exists with id " + projectId, HttpStatus.NOT_FOUND));

        Comment comment = new Comment();
        comment.setUser(author);
        comment.setProject(project);
        comment.setBody(body);

        return toDto(persist(comment));
    }

    /**
     * Creates a comment on an issue.
     *
     * @param issueId the issue id from the path
     * @param body    the comment text, already length-validated by {@code @Valid} on the request DTO
     * @param author  the authenticated caller, injected via {@code @AuthenticatedUser}
     * @return the created comment, in API shape
     * @throws ApiException with code {@code RATE_LIMITED} (429, API-03.10) if the caller has posted
     *                       too many comments in the last hour, or
     *                       {@code ISSUE_NOT_FOUND} (404) if the issue doesn't exist
     */
    @Transactional
    public CommentDto createIssueComment(Long issueId, String body, User author) {
        rateLimitService.checkCommentLimit(author.getId());

        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ApiException(
                        "ISSUE_NOT_FOUND", "No issue exists with id " + issueId, HttpStatus.NOT_FOUND));

        Comment comment = new Comment();
        comment.setUser(author);
        comment.setIssue(issue);
        comment.setBody(body);

        return toDto(persist(comment));
    }

    /**
     * Lists a project's active (non-deleted) comments, oldest first, paginated.
     *
     * @throws ApiException with code {@code PROJECT_NOT_FOUND} (404) if the project doesn't exist
     */
    @Transactional(readOnly = true)
    public PagedComments getProjectComments(Long projectId, Integer page, Integer size) {
        if (!projectRepository.existsById(projectId)) {
            throw new ApiException(
                    "PROJECT_NOT_FOUND", "No project exists with id " + projectId, HttpStatus.NOT_FOUND);
        }
        Page<Comment> result = commentRepository.findByProjectIdAndDeletedAtIsNullOrderByCreatedAtAsc(
                projectId, pageable(page, size));
        return toPagedDto(result);
    }

    /**
     * Lists an issue's active (non-deleted) comments, oldest first, paginated.
     *
     * @throws ApiException with code {@code ISSUE_NOT_FOUND} (404) if the issue doesn't exist
     */
    @Transactional(readOnly = true)
    public PagedComments getIssueComments(Long issueId, Integer page, Integer size) {
        if (!issueRepository.existsById(issueId)) {
            throw new ApiException(
                    "ISSUE_NOT_FOUND", "No issue exists with id " + issueId, HttpStatus.NOT_FOUND);
        }
        Page<Comment> result = commentRepository.findByIssueIdAndDeletedAtIsNullOrderByCreatedAtAsc(
                issueId, pageable(page, size));
        return toPagedDto(result);
    }

    /**
     * Edits a comment's body, author-only (API-02.4, design doc's comment-moderation note).
     *
     * @param commentId the comment id from the path
     * @param newBody   the replacement text, already length-validated by {@code @Valid} on the request DTO
     * @param caller    the authenticated caller, injected via {@code @AuthenticatedUser}
     * @return the updated comment, in API shape, with {@code edited: true}
     * @throws ApiException with code {@code COMMENT_NOT_FOUND} (404) if the comment doesn't exist or is
     *                       already soft-deleted, or {@code FORBIDDEN} (403) if the caller isn't the author
     */
    @Transactional
    public CommentDto editComment(Long commentId, String newBody, User caller) {
        Comment comment = findActiveCommentOrThrow(commentId);

        if (!comment.getUser().getId().equals(caller.getId())) {
            throw new ApiException(
                    "FORBIDDEN", "Only the comment's author may edit it.", HttpStatus.FORBIDDEN);
        }

        comment.setBody(newBody);
        comment.setEdited(true);

        return toDto(commentRepository.save(comment));
    }

    /**
     * Soft-deletes a comment (author or the parent project's maintainer), per design doc §6's note:
     * a deleted comment's body must never be returned by any subsequent read. That's enforced here by
     * setting {@code deletedAt} rather than removing the row — {@link #getProjectComments} and
     * {@link #getIssueComments} already filter on {@code deletedAtIsNull} at the query layer, so a
     * deleted comment disappears from every list entirely (chosen over a "[deleted]" placeholder for
     * simplicity, per the ticket's own note — flag in review if the team wants the placeholder instead).
     *
     * @param commentId the comment id from the path
     * @param caller    the authenticated caller, injected via {@code @AuthenticatedUser}
     * @throws ApiException with code {@code COMMENT_NOT_FOUND} (404) if the comment doesn't exist or is
     *                       already soft-deleted, or {@code FORBIDDEN} (403) if the caller is neither the
     *                       author nor a maintainer of the comment's parent project
     */
    @Transactional
    public void deleteComment(Long commentId, User caller) {
        Comment comment = findActiveCommentOrThrow(commentId);

        boolean isAuthor = comment.getUser().getId().equals(caller.getId());
        if (!isAuthor && !isMaintainerOfParentProject(comment, caller.getId())) {
            throw new ApiException(
                    "FORBIDDEN",
                    "Only the comment's author or the parent project's maintainer may delete it.",
                    HttpStatus.FORBIDDEN);
        }

        comment.setDeletedAt(OffsetDateTime.now());
        commentRepository.save(comment);
    }

    /**
     * Shared lookup for {@link #editComment}/{@link #deleteComment}: a soft-deleted comment is treated
     * as not found (404), same as one that was never created — not a 403/409 on an already-gone row.
     */
    private Comment findActiveCommentOrThrow(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ApiException(
                        "COMMENT_NOT_FOUND", "No comment exists with id " + commentId, HttpStatus.NOT_FOUND));

        if (comment.getDeletedAt() != null) {
            throw new ApiException(
                    "COMMENT_NOT_FOUND", "No comment exists with id " + commentId, HttpStatus.NOT_FOUND);
        }

        return comment;
    }

    /** A comment's parent project is either its own project, or its issue's project. */
    private boolean isMaintainerOfParentProject(Comment comment, Long userId) {
        Project parentProject = comment.getProject() != null ? comment.getProject() : comment.getIssue().getProject();
        return projectMaintainerRepository.existsByProjectIdAndUserId(parentProject.getId(), userId);
    }

    /**
     * Persists a comment after enforcing that exactly one of Project or Issue is assigned.
     * Clients passing both or neither receive a 400 Bad Request rather than an internal DB error.
     *
     * @param comment the comment entity to persist
     * @return the persisted comment
     * @throws CommentValidationException if both or neither parent associations are provided
     */
    private Comment persist(Comment comment) {
        boolean hasProject = comment.getProject() != null && comment.getProject().getId() != null;
        boolean hasIssue = comment.getIssue() != null && comment.getIssue().getId() != null;

        if (hasProject && hasIssue) {
            throw new CommentValidationException("A comment cannot be attached to both a project and an issue.");
        }
        if (!hasProject && !hasIssue) {
            throw new CommentValidationException("A comment must be attached to either a project or an issue.");
        }

        return commentRepository.save(comment);
    }

    private Pageable pageable(Integer page, Integer size) {
        return PageRequest.of(clampPage(page), clampSize(size));
    }

    /** Clamps {@code size} to the spec's max of 50; defaults to 20 if not provided. */
    private int clampSize(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        return Math.max(1, Math.min(size, MAX_SIZE));
    }

    /** Defaults to page 0 if not provided or negative. */
    private int clampPage(Integer page) {
        if (page == null || page < 0) {
            return DEFAULT_PAGE;
        }
        return page;
    }

    private PagedComments toPagedDto(Page<Comment> result) {
        List<CommentDto> items = result.getContent().stream().map(this::toDto).toList();
        return new PagedComments(items, new PageMeta(result.getNumber(), result.getSize(), (int) result.getTotalElements()));
    }

    /**
     * Maps a persisted comment row to the API's {@link CommentDto} shape.
     * Public: reused by {@code MaintainerActivityService} (API-03.7) rather
     * than duplicating this mapping there.
     */
    public CommentDto toDto(Comment entity) {
        return new CommentDto(
                entity.getId(),
                toPublicProfile(entity.getUser()),
                entity.getBody(),
                Boolean.TRUE.equals(entity.getEdited()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    /**
     * Maps a comment's author to the API's {@link PublicUserProfile} shape.
     * {@code projectsCount} is not yet computed anywhere in the codebase (no
     * ticket populates it); left {@code null} rather than a made-up value.
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
