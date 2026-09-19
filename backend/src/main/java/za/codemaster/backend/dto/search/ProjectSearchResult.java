package za.codemaster.backend.dto.search;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import za.codemaster.backend.dto.project.ProjectDto;

/**
 * Matches the {@code ProjectSearchResult} schema in codemasters-api-spec.yaml v2.1:
 * an {@code allOf} of {@code resultType: "project"} and every {@link ProjectDto}
 * field, flattened via {@code @JsonUnwrapped} — same technique as {@code ProjectDetail}.
 */
public record ProjectSearchResult(
        SearchResultType resultType,
        @JsonUnwrapped ProjectDto project
) implements SearchResultItem {

    public ProjectSearchResult(ProjectDto project) {
        this(SearchResultType.PROJECT, project);
    }
}
