package za.codemaster.backend.service;

import org.junit.jupiter.api.Test;
import za.codemaster.backend.dto.*;
import za.codemaster.backend.exception.ApiException;
import za.codemaster.backend.mock.MockDataStore;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies API-01.5's acceptance criteria: one test per filter param against
 * the mock set, plus a 404 test matching API-01.4's PROJECT_NOT_FOUND exactly.
 */
class ProjectIssuesTest {

    private final MockDataStore mockDataStore = new MockDataStore();
    private final ProjectQueryService service = new ProjectQueryService(mockDataStore);

    private ProjectIssuesSearchParams emptyParams() {
        return new ProjectIssuesSearchParams(null, null, null, null, null);
    }

    /** Project 1 (OpenLearn SA) has 2 mock issues: one BEGINNER/OPEN, one ADVANCED/OPEN. */
    private static final Long PROJECT_WITH_ISSUES = 1L;

    @Test
    void filtersByDifficulty() {
        PagedIssues all = service.getProjectIssues(PROJECT_WITH_ISSUES, emptyParams());
        PagedIssues filtered = service.getProjectIssues(PROJECT_WITH_ISSUES,
                new ProjectIssuesSearchParams(null, null, Difficulty.BEGINNER, null, null));

        assertTrue(filtered.items().size() < all.items().size());
        assertTrue(filtered.items().stream().allMatch(i -> i.difficulty() == Difficulty.BEGINNER));
    }

    @Test
    void filtersByLabel() {
        PagedIssues filtered = service.getProjectIssues(PROJECT_WITH_ISSUES,
                new ProjectIssuesSearchParams(null, null, null, "good-first-issue", null));

        assertTrue(filtered.items().size() > 0);
        assertTrue(filtered.items().stream()
                .allMatch(i -> i.labels().stream().anyMatch(l -> l.equalsIgnoreCase("good-first-issue"))));
    }

    @Test
    void filtersByStatus() {
        // Project 2 (Naija DevTools) has one CLAIMED and one OPEN issue in mock data.
        PagedIssues filtered = service.getProjectIssues(2L,
                new ProjectIssuesSearchParams(null, null, null, null, IssueStatus.CLAIMED));

        assertTrue(filtered.items().size() > 0);
        assertTrue(filtered.items().stream().allMatch(i -> i.status() == IssueStatus.CLAIMED));
    }

    @Test
    void onlyReturnsIssuesBelongingToTheRequestedProject() {
        PagedIssues result = service.getProjectIssues(PROJECT_WITH_ISSUES, emptyParams());

        assertTrue(result.items().stream().allMatch(i -> i.projectId().equals(PROJECT_WITH_ISSUES)));
    }

    @Test
    void invalidProjectIdThrowsProjectNotFoundMatchingApi014() {
        Long missingId = -999L;

        ApiException ex = assertThrows(ApiException.class,
                () -> service.getProjectIssues(missingId, emptyParams()));

        assertEquals("PROJECT_NOT_FOUND", ex.getCode());
        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, ex.getStatus());
    }
}