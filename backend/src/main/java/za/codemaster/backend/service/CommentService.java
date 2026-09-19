package za.codemaster.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.codemaster.backend.domain.model.Comment;
import za.codemaster.backend.domain.model.Issue;
import za.codemaster.backend.domain.model.Project;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.dto.PageMeta;
import za.codemaster.backend.dto.PagedComments;
import za.codemaster.backend.dto.PublicUserProfile;
import za.codemaster.backend.exception.ApiException;
import za.codemaster.backend.exception.CommentValidationException;
import za.codemaster.backend.repository.CommentRepository;
import za.codemaster.backend.repository.IssueRepository;
import za.codemaster.backend.repository.ProjectRepository;

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

    public CommentService(CommentRepository commentRepository,
                           ProjectRepository projectRepository,
                           IssueRepository issueRepository) {
        this.commentRepository = commentRepository;
        this.projectRepository = projectRepository;
        this.issueRepository = issueRepository;
    }

    /**
     * Creates a comment on a project.
     *
     * @param projectId the project id from the path
     * @param body      the comment text, already length-validated by {@code @Valid} on the request DTO
     * @param author    the authenticated caller, injected via {@code @AuthenticatedUser}
     * @return the created comment, in API shape
     * @throws ApiException with code {@code PROJECT_NOT_FOUND} (404) if the project doesn't exist
     */
    @Transactional
    public za.codemaster.backend.dto.Comment createProjectComment(Long projectId, String body, User author) {
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
     * @throws ApiException with code {@code ISSUE_NOT_FOUND} (404) if the issue doesn't exist
     */
    @Transactional
    public za.codemaster.backend.dto.Comment createIssueComment(Long issueId, String body, User author) {
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
        List<za.codemaster.backend.dto.Comment> items = result.getContent().stream().map(this::toDto).toList();
        return new PagedComments(items, new PageMeta(result.getNumber(), result.getSize(), (int) result.getTotalElements()));
    }

    /** Maps a persisted comment row to the API's {@link za.codemaster.backend.dto.Comment} shape. */
    private za.codemaster.backend.dto.Comment toDto(Comment entity) {
        return new za.codemaster.backend.dto.Comment(
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
     * {@code projectsCount}/{@code contributionsCount} are not yet computed anywhere
     * in the codebase (no ticket populates them); left {@code null} here rather than
     * a made-up value, matching how other unimplemented aggregate fields are handled
     * elsewhere (e.g. {@code ProjectDetail}'s still-empty {@code recentComments}).
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
