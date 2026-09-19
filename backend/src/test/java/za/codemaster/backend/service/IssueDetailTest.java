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
import za.codemaster.backend.dto.issue.IssueDetail;
import za.codemaster.backend.exception.ApiException;
import za.codemaster.backend.repository.ClaimRepository;
import za.codemaster.backend.repository.IssueRepository;
import za.codemaster.backend.repository.ProjectRepository;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies API-01.6's acceptance criteria: valid id returns full IssueDetail
 * shape, invalid id throws ISSUE_NOT_FOUND.
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
class IssueDetailTest {

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
    void validIdReturnsIssueDetailWithAllThreeExtraFieldsPresent() {
        Long existingId = fixtures.issueId(0);

        IssueDetail detail = service.getIssueDetail(existingId);

        assertNotNull(detail.issue(), "issue fields should be present (via @JsonUnwrapped)");
        assertEquals(existingId, detail.issue().id());
        assertNotNull(detail.project(), "project should be populated, not null, since we have the data");
        assertEquals(detail.issue().projectId(), detail.project().id(),
                "project returned should actually be the issue's own project");
        assertNotNull(detail.comments(), "comments should be present, even if empty");
        assertNotNull(detail.claims(), "claims should be present, even if empty");
    }

    @Test
    void invalidIdThrowsIssueNotFound() {
        Long missingId = -999L;

        ApiException ex = assertThrows(ApiException.class, () -> service.getIssueDetail(missingId));

        assertEquals("ISSUE_NOT_FOUND", ex.getCode());
        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, ex.getStatus());
    }
}
