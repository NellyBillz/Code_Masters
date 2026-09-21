package za.codemaster.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.codemaster.backend.domain.model.Issue;
import za.codemaster.backend.domain.model.Project;
import za.codemaster.backend.dto.common.PageMeta;
import za.codemaster.backend.dto.search.IssueSearchResult;
import za.codemaster.backend.dto.search.PagedSearchResults;
import za.codemaster.backend.dto.search.ProjectSearchResult;
import za.codemaster.backend.dto.search.SearchResultItem;
import za.codemaster.backend.domain.model.ListingStatus;
import za.codemaster.backend.dto.search.SearchType;
import za.codemaster.backend.exception.ApiException;
import za.codemaster.backend.repository.IssueRepository;
import za.codemaster.backend.repository.ProjectRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Unified cross-resource search (API-02.11, product doc Feature 17): one
 * {@code q} returning both projects and issues in a single ranked, paginated list.
 * <p>
 * Each half now sources from its own real ranked native query (API-03.13:
 * {@link ProjectRepository#searchProjects}/{@link IssueRepository#searchIssues},
 * DB-03.6) instead of an in-memory {@code findAll()} + Java substring filter.
 * {@code q} is always present here (validated below), so both halves always
 * rank by relevance. Merging the two ranked lists is still a simple
 * project-results-then-issue-results concatenation, unchanged from before —
 * neither result type's score is comparable across types, so there's no
 * meaningful single ranking to interleave them by; this ticket's contract is
 * "no response shape change," not a redesign of the merge strategy.
 * <p>
 * {@code language}/{@code country} only narrow project results (issues have
 * neither field); {@code difficulty} only narrows issue results (projects have
 * no difficulty). Neither the spec nor design doc says this explicitly — it's
 * this ticket's own resolution, following the precedent already set by which
 * filters exist on {@code GET /projects} vs {@code GET /projects/{projectId}/issues}.
 * An unrelated filter is simply ignored for the resource type it doesn't apply to,
 * rather than excluding that whole resource type from the results.
 */
@Service
public class SearchService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;
    private static final int MIN_QUERY_LENGTH = 2;

    private final ProjectRepository projectRepository;
    private final IssueRepository issueRepository;
    private final ProjectQueryService projectQueryService;

    public SearchService(ProjectRepository projectRepository,
                          IssueRepository issueRepository,
                          ProjectQueryService projectQueryService) {
        this.projectRepository = projectRepository;
        this.issueRepository = issueRepository;
        this.projectQueryService = projectQueryService;
    }

    /**
     * @param params the search request; only {@code q} is required
     * @return one page of the merged, ranked project+issue result set
     * @throws ApiException with code {@code VALIDATION_ERROR} (400) if {@code q}
     *                       is missing or under 2 characters
     */
    @Transactional(readOnly = true)
    public PagedSearchResults search(SearchParams params) {
        if (params.q() == null || params.q().trim().length() < MIN_QUERY_LENGTH) {
            throw new ApiException(
                    "VALIDATION_ERROR", "q is required and must be at least 2 characters.", HttpStatus.BAD_REQUEST);
        }

        SearchType type = params.type() == null ? SearchType.ALL : params.type();
        String q = params.q().trim();

        List<SearchResultItem> results = new ArrayList<>();
        if (type == SearchType.ALL || type == SearchType.PROJECTS) {
            results.addAll(searchProjects(q, params));
        }
        if (type == SearchType.ALL || type == SearchType.ISSUES) {
            results.addAll(searchIssues(q, params));
        }

        int size = clampSize(params.size());
        int page = clampPage(params.page());
        List<SearchResultItem> pageItems = paginate(results, page, size);

        return new PagedSearchResults(pageItems, new PageMeta(page, size, results.size()));
    }

    /**
     * Ranked project half of {@code GET /search} (API-03.13) — the same native
     * query {@code GET /projects} uses, always ranked by relevance since
     * {@code q} is guaranteed present here. {@code category}/{@code connection}/
     * {@code tag}/{@code hasBeginnerFriendlyIssues} aren't {@code /search}
     * parameters, so {@code null} is passed for those — matching the query's
     * existing "null means unfiltered" convention.
     */
    private List<SearchResultItem> searchProjects(String q, SearchParams params) {
        Page<Project> matched = projectRepository.searchProjects(
                q,
                ListingStatus.PUBLISHED.getValue(),
                params.language(),
                null,
                null,
                null,
                null,
                params.country(),
                true,
                Pageable.unpaged()
        );
        return matched.getContent().stream()
                .map(projectQueryService::toDto)
                .map(dto -> (SearchResultItem) new ProjectSearchResult(dto))
                .toList();
    }

    /**
     * Ranked issue half of {@code GET /search} (API-03.13) — the same native
     * query pattern as {@link #searchProjects}, always ranked by relevance.
     * {@code projectId}/{@code status}/{@code isBeginnerFriendly}/{@code label}
     * aren't {@code /search} parameters, so {@code null} is passed for those.
     */
    private List<SearchResultItem> searchIssues(String q, SearchParams params) {
        String difficulty = params.difficulty() == null ? null : params.difficulty().getWireValue();
        Page<Issue> matched = issueRepository.searchIssues(
                q,
                null,
                null,
                difficulty,
                null,
                null,
                true,
                Pageable.unpaged()
        );
        return matched.getContent().stream()
                .map(projectQueryService::toDto)
                .map(dto -> (SearchResultItem) new IssueSearchResult(dto))
                .toList();
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

    /** Slices {@code items} to the requested page; returns an empty list if {@code page} is past the end. */
    private List<SearchResultItem> paginate(List<SearchResultItem> items, int page, int size) {
        int fromIndex = page * size;
        if (fromIndex >= items.size()) {
            return List.of();
        }
        int toIndex = Math.min(fromIndex + size, items.size());
        return items.subList(fromIndex, toIndex);
    }
}
