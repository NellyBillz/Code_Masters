package za.codemaster.backend.dto.project;

import za.codemaster.backend.dto.common.PageMeta;

import java.util.List;

/**
 * Response shape for {@code GET /api/v1/projects}.
 * <p>
 * Matches the {@code PagedProjects} schema in codemasters-api-spec.yaml v2.1.
 * Field names {@code items}/{@code meta} are fixed; the frontend's project
 * list page (FE-01.6) is written against this exact shape.
 */
public record PagedProjects(
        List<ProjectDto> items,
        PageMeta meta
) {
}
