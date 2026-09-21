package za.codemaster.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import za.codemaster.backend.BackendApplication;
import za.codemaster.backend.domain.model.ListingStatus;
import za.codemaster.backend.domain.model.Project;
import za.codemaster.backend.dto.issue.Difficulty;
import za.codemaster.backend.dto.search.IssueSearchResult;
import za.codemaster.backend.dto.search.PagedSearchResults;
import za.codemaster.backend.dto.search.ProjectSearchResult;
import za.codemaster.backend.dto.search.SearchResultType;
import za.codemaster.backend.dto.search.SearchType;
import za.codemaster.backend.exception.ApiException;
import za.codemaster.backend.repository.ClaimRepository;
import za.codemaster.backend.repository.IssueRepository;
import za.codemaster.backend.repository.ProjectMaintainerRepository;
import za.codemaster.backend.repository.ProjectRepository;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies API-02.11's acceptance criteria (round2-tickets.md): {@code q} under 2
 * characters -> 400; {@code type=projects}/{@code type=issues} return only that
 * resultType; {@code type=all} (or omitted) merges both; {@code language}/
 * {@code difficulty}/{@code country} narrow results consistent with how the
 * equivalent filters behave on {@code GET /projects}/{@code GET /projects/{id}/issues}.
 * <p>
 * Runs against a real (test) Postgres database, same pattern as
 * {@code ProjectQueryServiceTest}.
 */
@SpringBootTest(
    classes = BackendApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@EnableAutoConfiguration(exclude = {
    SecurityAutoConfiguration.class,
    OAuth2ResourceServerAutoConfiguration.class,
    OAuth2ClientAutoConfiguration.class
})
@Transactional
class SearchServiceTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private ProjectMaintainerRepository projectMaintainerRepository;

    private SearchService service;

    @BeforeEach
    void setUp() {
        ProjectQueryService projectQueryService =
                new ProjectQueryService(projectRepository, issueRepository, claimRepository, projectMaintainerRepository);
        service = new SearchService(projectRepository, issueRepository, projectQueryService);
        ProjectQueryServiceFixtures.seed(projectRepository, issueRepository);
    }

    private SearchParams params(String q, SearchType type, String language, Difficulty difficulty, String country) {
        return new SearchParams(q, type, language, difficulty, country, null, null);
    }

    /** Minimal published project for tests that need control over name/description independent of the shared fixtures. */
    private Project newProject(String name, String description) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Project p = new Project();
        p.setGithubOwner("search-rank-org");
        p.setGithubRepo("repo-" + suffix);
        p.setGithubUrl("https://github.com/search-rank-org/repo-" + suffix);
        p.setName(name);
        p.setSlug("search-rank-" + suffix);
        p.setDescription(description);
        p.setCategory("Developer Tools");
        p.setConnection("south_african");
        p.setListingStatus(ListingStatus.PUBLISHED);
        p.setCountryCodes(List.of("ZA"));
        return projectRepository.save(p);
    }

    @Test
    void queryUnderTwoCharactersThrowsValidationError() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.search(params("a", null, null, null, null)));

        assertEquals("VALIDATION_ERROR", ex.getCode());
        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void missingQueryThrowsValidationError() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.search(params(null, null, null, null, null)));

        assertEquals("VALIDATION_ERROR", ex.getCode());
    }

    @Test
    void typeProjectsReturnsOnlyProjectResults() {
        // "open" matches project names (e.g. "OpenLearn SA") and no issue titles/bodies.
        PagedSearchResults results = service.search(params("open", SearchType.PROJECTS, null, null, null));

        assertFalse(results.items().isEmpty());
        assertTrue(results.items().stream().allMatch(item -> item instanceof ProjectSearchResult));
        assertTrue(results.items().stream()
                .allMatch(item -> ((ProjectSearchResult) item).resultType() == SearchResultType.PROJECT));
    }

    @Test
    void typeIssuesReturnsOnlyIssueResults() {
        // "translation" matches an issue title ("Add Afrikaans translation for onboarding flow").
        PagedSearchResults results = service.search(params("translation", SearchType.ISSUES, null, null, null));

        assertFalse(results.items().isEmpty());
        assertTrue(results.items().stream().allMatch(item -> item instanceof IssueSearchResult));
        assertTrue(results.items().stream()
                .allMatch(item -> ((IssueSearchResult) item).resultType() == SearchResultType.ISSUE));
    }

    @Test
    void typeAllMergesBothResultTypesWhenQueryMatchesBoth() {
        // "CLI" matches Naija DevTools' description and its issue title
        // ("Fix flag parsing edge case in CLI") — one query, both resultTypes.
        PagedSearchResults results = service.search(params("CLI", SearchType.ALL, null, null, null));

        boolean hasProject = results.items().stream().anyMatch(item -> item instanceof ProjectSearchResult);
        boolean hasIssue = results.items().stream().anyMatch(item -> item instanceof IssueSearchResult);
        assertTrue(hasProject, "expected at least one project result");
        assertTrue(hasIssue, "expected at least one issue result");
    }

    @Test
    void omittedTypeDefaultsToAll() {
        PagedSearchResults explicitAll = service.search(params("in", SearchType.ALL, null, null, null));
        PagedSearchResults omitted = service.search(params("in", null, null, null, null));

        assertEquals(explicitAll.meta().total(), omitted.meta().total());
    }

    @Test
    void languageFilterNarrowsProjectResultsOnly() {
        // "open" matches OpenLearn SA's description; it's a Java project.
        PagedSearchResults unfiltered = service.search(params("open", SearchType.PROJECTS, null, null, null));
        PagedSearchResults filtered = service.search(params("open", SearchType.PROJECTS, "Java", null, null));

        assertTrue(filtered.items().size() <= unfiltered.items().size());
        assertTrue(filtered.items().stream()
                .allMatch(item -> ((ProjectSearchResult) item).project().primaryLanguage().equalsIgnoreCase("Java")));
    }

    @Test
    void difficultyFilterNarrowsIssueResultsOnly() {
        PagedSearchResults filtered = service.search(params("in", SearchType.ISSUES, null, Difficulty.BEGINNER, null));

        assertTrue(filtered.items().stream()
                .allMatch(item -> ((IssueSearchResult) item).issue().difficulty() == Difficulty.BEGINNER));
    }

    @Test
    void countryFilterNarrowsProjectResultsOnly() {
        // "open" matches OpenLearn SA's description; it's a ZA project.
        PagedSearchResults filtered = service.search(params("open", SearchType.PROJECTS, null, null, "ZA"));

        assertFalse(filtered.items().isEmpty());
        assertTrue(filtered.items().stream()
                .allMatch(item -> ((ProjectSearchResult) item).project().countryCodes().stream()
                        .anyMatch(c -> c.equalsIgnoreCase("ZA"))));
    }

    @Test
    void nameMatchRanksAboveDescriptionOnlyMatch() {
        // "Zephyrine" is a made-up token planted in one project's name and another's
        // description only, so ranking can only be explained by tsv weight (A vs B).
        newProject("Zephyrine Toolkit", "A generic developer utility");
        newProject("Generic Project", "Powered by the Zephyrine engine under the hood");

        PagedSearchResults results = service.search(params("Zephyrine", SearchType.PROJECTS, null, null, null));

        assertTrue(results.items().size() >= 2, "expected both planted projects to match");
        ProjectSearchResult first = (ProjectSearchResult) results.items().get(0);
        assertEquals("Zephyrine Toolkit", first.project().name(),
                "the name match should rank above the description-only match");
    }

    @Test
    void typoInQueryStillMatchesViaTrigramFallback() {
        // "OpenLarn" (missing the 'e') doesn't tokenize to anything in "OpenLearn SA"'s
        // tsvector, but pg_trgm similarity on the name is well above threshold.
        PagedSearchResults results = service.search(params("OpenLarn", SearchType.PROJECTS, null, null, null));

        assertTrue(results.items().stream()
                        .anyMatch(item -> ((ProjectSearchResult) item).project().name().equals("OpenLearn SA")),
                "expected the typo query to still surface OpenLearn SA via trigram fallback");
    }

    @Test
    void noMatchesReturnsEmptyResultsNotAnError() {
        PagedSearchResults results = service.search(params("zzzznomatchzzzz", SearchType.ALL, null, null, null));

        assertTrue(results.items().isEmpty());
        assertEquals(0, results.meta().total());
    }
}
