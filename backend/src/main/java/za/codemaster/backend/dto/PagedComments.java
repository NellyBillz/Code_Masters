package za.codemaster.backend.dto;

import java.util.List;

/**
 * Response shape for {@code GET /projects/{projectId}/comments} and
 * {@code GET /issues/{issueId}/comments}.
 * <p>
 * Matches the {@code PagedComments} schema in codemasters-api-spec.yaml v2.1.
 * Same {@code items}/{@code meta} pattern as {@link PagedProjects}/{@link PagedIssues} —
 * this shape is fixed for the frontend's comment list (FE-02.6).
 */
public record PagedComments(
        List<Comment> items,
        PageMeta meta
) {
}
