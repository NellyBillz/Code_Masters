package za.codemaster.backend.dto.issue;

import za.codemaster.backend.dto.common.PageMeta;

import java.util.List;

/**
 * Response shape for {@code GET /api/v1/projects/{projectId}/issues}.
 * <p>
 * Matches the {@code PagedIssues} schema in codemasters-api-spec.yaml v2.1.
 * Same {@code items}/{@code meta} pattern as {@link PagedProjects} — this
 * shape is fixed for the frontend's project detail page issue list (Round 2).
 */
public record PagedIssues(
        List<IssueDto> items,
        PageMeta meta
) {
}
