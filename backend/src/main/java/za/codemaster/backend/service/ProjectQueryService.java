package za.codemaster.backend.service;


import org.springframework.stereotype.Service;
import za.codemaster.backend.dto.PageMeta;
import za.codemaster.backend.dto.Project;
import za.codemaster.backend.dto.PagedProjects;
import za.codemaster.backend.mock.MockDataStore;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Applies filtering, sorting, and pagination for {@code GET /api/v1/projects}
 * against {@link MockDataStore}.
 * <p>
 * Kept separate from {@code ProjectController} so filtering logic can be unit
 * tested directly, without starting a web server. In Round 2, this class (or
 * its query logic) moves to a real repository query — the {@link #search}
 * method's return shape ({@link PagedProjects}) does not change.
 */

@Service
public class ProjectQueryService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final MockDataStore mockDataStore;

    public ProjectQueryService(MockDataStore mockDataStore) {
        this.mockDataStore = mockDataStore;
    }

    /**
     * Filters, sorts, and paginates the mock project list.
     *
     * @param params the requested filters/sort/pagination; any field may be {@code null}
     * @return a {@link PagedProjects} containing one page of matching results
     */
    public PagedProjects search(ProjectSearchParams params) {
        List<Project> filtered = mockDataStore.projects().stream()
                .filter(p -> matchesQuery(p, params.q()))
                .filter(p -> matchesLanguage(p, params.language()))
                .filter(p -> matchesCategory(p, params.category()))
                .filter(p -> matchesTag(p, params.tag()))
                .filter(p -> matchesCountry(p, params.country()))
                .filter(p -> matchesHasBeginnerIssues(p, params.hasBeginnerIssues()))
                .toList();

        List<Project> sorted = sort(filtered, params.sort());

        int size = clampSize(params.size());
        int page = clampPage(params.page());
        List<Project> pageItems = paginate(sorted, page, size);

        return new PagedProjects(pageItems, new PageMeta(page, size, sorted.size()));
    }

    /** True if {@code q} is blank/null, or found in the project's name, description, owner, or tags (case-insensitive). */
    private boolean matchesQuery(Project p, String q) {
        if (q == null || q.isBlank()) {
            return true;
        }
        String needle = q.toLowerCase(Locale.ROOT);
        return p.name().toLowerCase(Locale.ROOT).contains(needle)
                || p.description().toLowerCase(Locale.ROOT).contains(needle)
                || p.owner().toLowerCase(Locale.ROOT).contains(needle)
                || p.tags().stream().anyMatch(t -> t.toLowerCase(Locale.ROOT).contains(needle));
    }

    /** True if {@code language} is null, or matches the project's primaryLanguage (case-insensitive). */
    private boolean matchesLanguage(Project p, String language) {
        return language == null || p.primaryLanguage().equalsIgnoreCase(language);
    }

    /** True if {@code category} is null, or matches the project's category (case-insensitive). */
    private boolean matchesCategory(Project p, String category) {
        return category == null || p.category().equalsIgnoreCase(category);
    }

    /** True if {@code tag} is null, or found among the project's tags (case-insensitive). */
    private boolean matchesTag(Project p, String tag) {
        return tag == null || p.tags().stream().anyMatch(t -> t.equalsIgnoreCase(tag));
    }

    /** True if {@code country} is null, or found among the project's countryCodes (case-insensitive). */
    private boolean matchesCountry(Project p, String country) {
        return country == null || p.countryCodes().stream().anyMatch(c -> c.equalsIgnoreCase(country));
    }

    /** True if {@code hasBeginnerIssues} is null, or equals the project's hasBeginnerFriendlyIssues flag. */
    private boolean matchesHasBeginnerIssues(Project p, Boolean hasBeginnerIssues) {
        return hasBeginnerIssues == null || p.hasBeginnerFriendlyIssues() == hasBeginnerIssues;
    }

    /**
     * "relevance" has no real scoring yet (Phase 2 territory, design doc §9)
     * so it's a no-op that preserves the filtered order. Everything else
     * sorts descending; newest, most stars, or most contributors first.
     */

    private List<Project> sort(List<Project> projects, String sortParam) {
        if (sortParam == null || sortParam.equals("relevance")) {
            return projects;
        }
        Comparator<Project> comparator = switch (sortParam) {
            case "recent" -> Comparator.comparing(Project::lastActivityAt).reversed();
            case "stars" -> Comparator.comparing(Project::stars).reversed();
            case "contributors" -> Comparator.comparing(Project::contributors).reversed();
            default -> throw new IllegalArgumentException("Unknown sort value: " + sortParam);
        };
        return projects.stream().sorted(comparator).toList();
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

    /** Slices {@code projects} to the requested page; returns an empty list if {@code page} is past the end. */
    private List<Project> paginate(List<Project> projects, int page, int size) {
        int fromIndex = page * size;
        if (fromIndex >= projects.size()) {
            return List.of();
        }
        int toIndex = Math.min(fromIndex + size, projects.size());
        return projects.subList(fromIndex, toIndex);
    }
}
