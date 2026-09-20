package za.codemaster.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import za.codemaster.backend.dto.issue.Difficulty;
import za.codemaster.backend.dto.search.PagedSearchResults;
import za.codemaster.backend.dto.search.SearchType;
import za.codemaster.backend.service.SearchParams;
import za.codemaster.backend.service.SearchService;

/**
 * Public unified search endpoint (API-02.11, round2-tickets.md).
 */
@RestController
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * {@code GET /api/v1/search}: cross-resource project+issue search.
     * <p>
     * Matches the {@code /search} GET operation in codemasters-api-spec.yaml v2.1.
     * {@code q} is required (validated in {@link SearchService#search}, not here,
     * so a missing {@code q} and a too-short {@code q} both produce the same
     * clean {@code VALIDATION_ERROR} rather than needing two error paths).
     */
    @GetMapping("/api/v1/search")
    public PagedSearchResults search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) SearchType type,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        SearchParams params = new SearchParams(q, type, language, difficulty, country, page, size);
        return searchService.search(params);
    }
}
