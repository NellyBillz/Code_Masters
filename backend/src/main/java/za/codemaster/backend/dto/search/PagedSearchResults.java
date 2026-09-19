package za.codemaster.backend.dto.search;

import za.codemaster.backend.dto.common.PageMeta;

import java.util.List;

/**
 * Response shape for {@code GET /api/v1/search}.
 * <p>
 * Matches the {@code PagedSearchResults} schema in codemasters-api-spec.yaml v2.1.
 * Same {@code items}/{@code meta} pattern as {@code PagedProjects}/{@code PagedIssues}/
 * {@code PagedComments} — pagination spans the merged, ranked result set across both
 * resource types, not each type independently.
 */
public record PagedSearchResults(
        List<SearchResultItem> items,
        PageMeta meta
) {
}
