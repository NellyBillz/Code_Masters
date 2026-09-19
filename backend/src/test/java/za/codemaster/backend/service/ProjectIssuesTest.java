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
import za.codemaster.backend.dto.issue.*;
import za.codemaster.backend.exception.ApiException;
import za.codemaster.backend.repository.ClaimRepository;
import za.codemaster.backend.repository.IssueRepository;
import za.codemaster.backend.repository.ProjectRepository;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies API-01.5's acceptance criteria: one test per filter param, plus a
 * 404 test matching API-01.4's PROJECT_NOT_FOUND exactly.
 * <p>
 * Post API-02.1: runs against a real (test) Postgres database instead of
 * {@code MockDataStore}. Only the fixture setup changed — assertions unchanged.
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
class ProjectIssuesTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private ClaimRepository claimRepository;

    private ProjectQueryService service;
    private ProjectQueryServiceFixtures fixtures;

    /** OpenLearn SA (fixture index 0) has 2 issues: one BEGINNER/OPEN, one ADVANCED/OPEN. */
    private Long projectWithIssues;

    @BeforeEach
    void setUp() {
        service = new ProjectQueryService(projectRepository, issueRepository, claimRepository);
        fixtures = ProjectQueryServiceFixtures.seed(projectRepository, issueRepository);
        projectWithIssues = fixtures.projectId(0);
    }

    private ProjectIssuesSearchParams emptyParams() {
        return new ProjectIssuesSearchParams(null, null, null, null, null);
    }

    @Test
    void filtersByDifficulty() {
        PagedIssues all = service.getProjectIssues(projectWithIssues, emptyParams());
        PagedIssues filtered = service.getProjectIssues(projectWithIssues,
                new ProjectIssuesSearchParams(null, null, Difficulty.BEGINNER, null, null));

        assertTrue(filtered.items().size() < all.items().size());
        assertTrue(filtered.items().stream().allMatch(i -> i.difficulty() == Difficulty.BEGINNER));
    }

    @Test
    void filtersByLabel() {
        PagedIssues filtered = service.getProjectIssues(projectWithIssues,
                new ProjectIssuesSearchParams(null, null, null, "good-first-issue", null));

        assertTrue(filtered.items().size() > 0);
        assertTrue(filtered.items().stream()
                .allMatch(i -> i.labels().stream().anyMatch(l -> l.equalsIgnoreCase("good-first-issue"))));
    }

    @Test
    void filtersByStatus() {
        // Naija DevTools (fixture index 1) has one CLAIMED and one OPEN issue.
        Long naijaDevTools = fixtures.projectId(1);

        PagedIssues filtered = service.getProjectIssues(naijaDevTools,
                new ProjectIssuesSearchParams(null, null, null, null, IssueStatus.CLAIMED));

        assertTrue(filtered.items().size() > 0);
        assertTrue(filtered.items().stream().allMatch(i -> i.status() == IssueStatus.CLAIMED));
    }

    @Test
    void onlyReturnsIssuesBelongingToTheRequestedProject() {
        PagedIssues result = service.getProjectIssues(projectWithIssues, emptyParams());

        assertTrue(result.items().stream().allMatch(i -> i.projectId().equals(projectWithIssues)));
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
