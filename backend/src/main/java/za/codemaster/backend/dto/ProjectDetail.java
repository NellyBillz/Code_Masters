package za.codemaster.backend.dto;


import com.fasterxml.jackson.annotation.JsonUnwrapped;

import java.util.List;

/**
 * Response shape for {@code GET /api/v1/projects/{projectId}}.
 * <p>
 * Matches the {@code ProjectDetail} schema in codemasters-api-spec.yaml v2.1,
 * which is defined as an {@code allOf}: every {@link Project} field, merged
 * flat into the same JSON object, plus maintainers/featuredIssues/recentComments.
 * <p>
 * Java records can't extend another record the way {@code allOf} implies, so
 * this wraps a {@link Project} and uses {@code @JsonUnwrapped} to flatten its
 * fields into the same level at serialization time — the JSON output has no
 * nested {@code "project": {...}} object, exactly matching the spec.
 */
public record ProjectDetail(
        @JsonUnwrapped Project project,
        List<ProjectMaintainer> maintainers,
        List<Issue> featuredIssues,
        List<Comment> recentComments
) {
}
