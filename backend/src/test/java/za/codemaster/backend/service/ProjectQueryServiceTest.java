package za.codemaster.backend.service;

import org.junit.jupiter.api.Test;
import za.codemaster.backend.dto.PagedProjects;
import za.codemaster.backend.dto.Project;
import za.codemaster.backend.mock.MockDataStore;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * One test per GET /api/v1/projects filter param, per API-01.3's acceptance
 * criteria — each proves the mock result set actually narrows, not that the
 * param is silently ignored.
 */
class ProjectQueryServiceTest {

    private final ProjectQueryService service = new ProjectQueryService(new MockDataStore());

    private PagedProjects search(ProjectSearchParams params) {
        return service.search(params);
    }

    private ProjectSearchParams emptyParams() {
        return new ProjectSearchParams(null, null, null, null, null, null, null, null, null);
    }

    @Test
    void filtersByQ() {
        PagedProjects all = search(emptyParams());
        PagedProjects filtered = search(new ProjectSearchParams(
                null, null, "health", null, null, null, null, null, null));

        assertTrue(filtered.items().size() < all.items().size());
        assertTrue(filtered.items().stream()
                .allMatch(p -> p.name().toLowerCase().contains("health")
                        || p.description().toLowerCase().contains("health")));
    }

    @Test
    void filtersByLanguage() {
        PagedProjects filtered = search(new ProjectSearchParams(
                null, null, null, "Java", null, null, null, null, null));

        assertTrue(filtered.items().size() > 0);
        assertTrue(filtered.items().stream().allMatch(p -> p.primaryLanguage().equals("Java")));
    }

    @Test
    void filtersByCategory() {
        PagedProjects filtered = search(new ProjectSearchParams(
                null, null, null, null, "Education", null, null, null, null));

        assertTrue(filtered.items().size() > 0);
        assertTrue(filtered.items().stream().allMatch(p -> p.category().equals("Education")));
    }

    @Test
    void filtersByTag() {
        PagedProjects filtered = search(new ProjectSearchParams(
                null, null, null, null, null, "education", null, null, null));

        assertTrue(filtered.items().size() > 0);
        assertTrue(filtered.items().stream()
                .allMatch(p -> p.tags().stream().anyMatch(t -> t.equalsIgnoreCase("education"))));
    }

    @Test
    void filtersByCountry() {
        PagedProjects filtered = search(new ProjectSearchParams(
                null, null, null, null, null, null, "ZA", null, null));

        assertTrue(filtered.items().size() > 0);
        assertTrue(filtered.items().stream()
                .allMatch(p -> p.countryCodes().stream().anyMatch(c -> c.equalsIgnoreCase("ZA"))));
    }

    @Test
    void filtersByHasBeginnerIssues() {
        PagedProjects filtered = search(new ProjectSearchParams(
                null, null, null, null, null, null, null, null, true));

        assertTrue(filtered.items().size() > 0);
        assertTrue(filtered.items().stream().allMatch(Project::hasBeginnerFriendlyIssues));
    }

    @Test
    void sortsByStarsDescending() {
        PagedProjects sorted = search(new ProjectSearchParams(
                null, null, null, null, null, null, null, "stars", null));

        List<Integer> starCounts = sorted.items().stream().map(Project::stars).toList();
        List<Integer> expectedOrder = starCounts.stream().sorted((a, b) -> b - a).toList();
        assertEquals(expectedOrder, starCounts);
    }

    @Test
    void clampsSizeAboveFiftyInsteadOfRejecting() {
        PagedProjects result = search(new ProjectSearchParams(
                null, 999, null, null, null, null, null, null, null));

        assertEquals(50, result.meta().size());
    }
}