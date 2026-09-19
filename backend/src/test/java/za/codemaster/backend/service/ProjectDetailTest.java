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
import za.codemaster.backend.dto.ProjectDetail;
import za.codemaster.backend.exception.ApiException;
import za.codemaster.backend.repository.ClaimRepository;
import za.codemaster.backend.repository.IssueRepository;
import za.codemaster.backend.repository.ProjectRepository;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies API-01.4's two acceptance criteria against
 * ProjectQueryService.getProjectDetail: a valid id returns full ProjectDetail
 * shape, an invalid id throws PROJECT_NOT_FOUND.
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
class ProjectDetailTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private ClaimRepository claimRepository;

    private ProjectQueryService service;
    private ProjectQueryServiceFixtures fixtures;

    @BeforeEach
    void setUp() {
        service = new ProjectQueryService(projectRepository, issueRepository, claimRepository);
        fixtures = ProjectQueryServiceFixtures.seed(projectRepository, issueRepository);
    }

    @Test
    void validIdReturnsProjectDetailWithAllFourExtraFieldsPresent() {
        Long existingId = fixtures.projectId(0);

        ProjectDetail detail = service.getProjectDetail(existingId);

        assertNotNull(detail.project(), "project fields should be present (via @JsonUnwrapped)");
        assertEquals(existingId, detail.project().id());
        assertNotNull(detail.maintainers(), "maintainers should be present, even if empty");
        assertNotNull(detail.featuredIssues(), "featuredIssues should be present, even if empty");
        assertNotNull(detail.recentComments(), "recentComments should be present, even if empty");
    }

    @Test
    void invalidIdThrowsProjectNotFound() {
        Long missingId = -999L;

        ApiException ex = assertThrows(ApiException.class, () -> service.getProjectDetail(missingId));

        assertEquals("PROJECT_NOT_FOUND", ex.getCode());
        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, ex.getStatus());
    }
}
