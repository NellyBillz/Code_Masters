package za.codemaster.backend.service;

import za.codemaster.backend.dto.issue.Difficulty;
import za.codemaster.backend.dto.search.SearchType;

/**
 * Request parameters for {@code GET /search} (API-02.11). Any field except
 * {@code q} may be {@code null}; see {@link SearchService#search} for how each
 * one is applied. Same pattern as {@link ProjectSearchParams}/{@link ProjectIssuesSearchParams}.
 */
public record SearchParams(
        String q,
        SearchType type,
        String language,
        Difficulty difficulty,
        String country,
        Integer page,
        Integer size
) {
}
