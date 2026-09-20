package za.codemaster.backend.service;

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
import za.codemaster.backend.dto.search.SearchType;
import za.codemaster.backend.exception.ApiException;
import za.codemaster.backend.repository.IssueRepository;
import za.codemaster.backend.repository.ProjectRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Unified cross-resource search (API-02.11, product doc Feature 17): one
 * {@code q} returning both projects and issues in a single ranked, paginated list.
 * <p>
 * Loads and filters projects/issues in Java, same reasoning as {@link ProjectQueryService#search}
 * ("project counts are small enough for a hackathon MVP that this is not a real
 * bottleneck") — merging two different tables into one paginated list isn't a
 * plain SQL query anyway, so there's no simpler DB-level alternative here.
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
        String needle = params.q().trim().toLowerCase(Locale.ROOT);

        List<SearchResultItem> results = new ArrayList<>();
        if (type == SearchType.ALL || type == SearchType.PROJECTS) {
            results.addAll(searchProjects(needle, params));
        }
        if (type == SearchType.ALL || type == SearchType.ISSUES) {
            results.addAll(searchIssues(needle, params));
        }

        int size = clampSize(params.size());
        int page = clampPage(params.page());
        List<SearchResultItem> pageItems = paginate(results, page, size);

        return new PagedSearchResults(pageItems, new PageMeta(page, size, results.size()));
    }

    private List<SearchResultItem> searchProjects(String needle, SearchParams params) {
        return projectRepository.findAll().stream()
                .filter(p -> matchesProjectQuery(p, needle))
                .filter(p -> params.language() == null || params.language().equalsIgnoreCase(p.getPrimaryLanguage()))
                .filter(p -> matchesCountry(p, params.country()))
                .map(projectQueryService::toDto)
                .map(dto -> (SearchResultItem) new ProjectSearchResult(dto))
                .toList();
    }

    private List<SearchResultItem> searchIssues(String needle, SearchParams params) {
        return issueRepository.findAll().stream()
                .filter(i -> matchesIssueQuery(i, needle))
                .filter(i -> params.difficulty() == null
                        || params.difficulty().getWireValue().equalsIgnoreCase(i.getDifficulty()))
                .map(projectQueryService::toDto)
                .map(dto -> (SearchResultItem) new IssueSearchResult(dto))
                .toList();
    }

    /** True if {@code needle} is found in the project's name, description, owner, or tags. */
    private boolean matchesProjectQuery(Project project, String needle) {
        return containsIgnoreCase(project.getName(), needle)
                || containsIgnoreCase(project.getDescription(), needle)
                || containsIgnoreCase(project.getGithubOwner(), needle)
                || (project.getTags() != null && project.getTags().stream().anyMatch(tag -> containsIgnoreCase(tag, needle)));
    }

    /** True if {@code needle} is found in the issue's title or body excerpt. */
    private boolean matchesIssueQuery(Issue issue, String needle) {
        return containsIgnoreCase(issue.getTitle(), needle) || containsIgnoreCase(issue.getBodyExcerpt(), needle);
    }

    private boolean matchesCountry(Project project, String country) {
        return country == null
                || (project.getCountryCodes() != null
                        && project.getCountryCodes().stream().anyMatch(c -> c.equalsIgnoreCase(country)));
    }

    private boolean containsIgnoreCase(String haystack, String needle) {
        return haystack != null && haystack.toLowerCase(Locale.ROOT).contains(needle);
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
