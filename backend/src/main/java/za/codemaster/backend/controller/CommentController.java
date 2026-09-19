package za.codemaster.backend.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.dto.Comment;
import za.codemaster.backend.dto.CreateCommentRequest;
import za.codemaster.backend.dto.PagedComments;
import za.codemaster.backend.security.AuthenticatedUser;
import za.codemaster.backend.service.CommentService;

/**
 * The comment half of the demo flow (design doc §8, step 5): {@code POST}/{@code GET}
 * on both projects and issues (API-02.3, round2-tickets.md).
 * <p>
 * Kept separate from {@link ProjectController}/{@link IssueController} — same
 * reasoning as {@link SyncController} — since it's one self-contained concern
 * (the {@code Comments} tag in codemasters-api-spec.yaml) spanning two parent
 * resources, not an extension of either resource's own endpoint set.
 */
@RestController
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * {@code POST /api/v1/projects/{projectId}/comments}: start or continue project discussion.
     * Requires authentication (401 {@code UNAUTHENTICATED} if not) — enforced by
     * {@code SecurityFilterChain} before this method is reached, with
     * {@code @AuthenticatedUser} resolving the caller as a safety net.
     */
    @PostMapping("/api/v1/projects/{projectId}/comments")
    public ResponseEntity<Comment> createProjectComment(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticatedUser User currentUser) {
        Comment created = commentService.createProjectComment(projectId, request.body(), currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * {@code GET /api/v1/projects/{projectId}/comments}: list project discussion, paginated.
     */
    @GetMapping("/api/v1/projects/{projectId}/comments")
    public PagedComments getProjectComments(
            @PathVariable Long projectId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return commentService.getProjectComments(projectId, page, size);
    }

    /**
     * {@code POST /api/v1/issues/{issueId}/comments}: comment on an issue.
     */
    @PostMapping("/api/v1/issues/{issueId}/comments")
    public ResponseEntity<Comment> createIssueComment(
            @PathVariable Long issueId,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticatedUser User currentUser) {
        Comment created = commentService.createIssueComment(issueId, request.body(), currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * {@code GET /api/v1/issues/{issueId}/comments}: list issue discussion, paginated.
     */
    @GetMapping("/api/v1/issues/{issueId}/comments")
    public PagedComments getIssueComments(
            @PathVariable Long issueId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return commentService.getIssueComments(issueId, page, size);
    }
}
