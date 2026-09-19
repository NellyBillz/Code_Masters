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
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.dto.project.CreateProjectRequest;
import za.codemaster.backend.dto.project.ProjectConnection;
import za.codemaster.backend.dto.project.ProjectDetail;
import za.codemaster.backend.dto.project.ProjectDto;
import za.codemaster.backend.dto.project.UpdateProjectRequest;
import za.codemaster.backend.exception.ApiException;
import za.codemaster.backend.repository.ClaimRepository;
import za.codemaster.backend.repository.IssueRepository;
import za.codemaster.backend.repository.ProjectMaintainerRepository;
import za.codemaster.backend.repository.ProjectRepository;
import za.codemaster.backend.repository.UserRepository;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies API-02.7's acceptance criteria (round2-tickets.md): a successful
 * {@code POST /projects} immediately shows the caller in that project's
 * maintainer list, and a duplicate {@code githubUrl} produces a clean 409
 * (caught from the DB's unique constraint, not pre-checked, same pattern as
 * {@code ClaimService.createClaim}).
 * <p>
 * Runs against a real (test) Postgres database, same pattern as
 * {@code CommentServiceTest}/{@code ClaimServiceTest}.
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
class ProjectServiceTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private ProjectMaintainerRepository projectMaintainerRepository;

    @Autowired
    private UserRepository userRepository;

    private ProjectService service;
    private ProjectQueryService projectQueryService;
    private User submitter;

    @BeforeEach
    void setUp() {
        projectQueryService = new ProjectQueryService(projectRepository, issueRepository, claimRepository, projectMaintainerRepository);
        service = new ProjectService(projectRepository, projectMaintainerRepository, projectQueryService);

        submitter = userRepository.save(User.builder()
                .githubId(System.nanoTime())
                .username("submitter_" + System.nanoTime())
                .displayName("Submitter")
                .build());
    }

    private String uniqueGithubUrl() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return "https://github.com/example-org/repo-" + suffix;
    }

    @Test
    void createProjectPersistsAndReturnsSubmittedFields() {
        String url = uniqueGithubUrl();
        CreateProjectRequest request = new CreateProjectRequest(
                url, ProjectConnection.SOUTH_AFRICAN, "Developer Tools", List.of("cli"), List.of("ZA"));

        ProjectDto created = service.createProject(request, submitter);

        assertNotNull(created.id());
        assertEquals(url, created.githubUrl());
        assertEquals(ProjectConnection.SOUTH_AFRICAN, created.connection());
        assertEquals("Developer Tools", created.category());
        assertEquals(List.of("cli"), created.tags());
        assertEquals(List.of("ZA"), created.countryCodes());
    }

    @Test
    void createdProjectSubmitterAppearsAsOwnerInMaintainerList() {
        CreateProjectRequest request = new CreateProjectRequest(
                uniqueGithubUrl(), ProjectConnection.AFRICA_LED, "Data", null, null);

        ProjectDto created = service.createProject(request, submitter);

        ProjectDetail detail = projectQueryService.getProjectDetail(created.id());
        assertEquals(1, detail.maintainers().size());
        assertEquals(submitter.getUsername(), detail.maintainers().get(0).user().username());
        assertEquals(za.codemaster.backend.dto.project.MaintainerRole.OWNER, detail.maintainers().get(0).role());
    }

    @Test
    void createProjectWithoutOptionalFieldsSucceeds() {
        CreateProjectRequest request = new CreateProjectRequest(
                uniqueGithubUrl(), ProjectConnection.COMMUNITY_VERIFIED, "Health", null, null);

        ProjectDto created = service.createProject(request, submitter);

        assertTrue(created.tags().isEmpty());
        assertTrue(created.countryCodes().isEmpty());
    }

    @Test
    void createProjectWithInvalidGithubUrlThrowsBadRequest() {
        CreateProjectRequest request = new CreateProjectRequest(
                "https://gitlab.com/example-org/repo", ProjectConnection.SOUTH_AFRICAN, "Education", null, null);

        ApiException ex = assertThrows(ApiException.class, () -> service.createProject(request, submitter));

        assertEquals("INVALID_GITHUB_URL", ex.getCode());
        assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void duplicateGithubUrlThrows409NotRawDbException() {
        String url = uniqueGithubUrl();
        CreateProjectRequest request = new CreateProjectRequest(
                url, ProjectConnection.SOUTH_AFRICAN, "Education", null, null);
        service.createProject(request, submitter);

        // Kept as the last DB-touching action in this test — same reasoning as
        // ClaimServiceTest's duplicate-claim test (constraint-violation flush
        // aborts the shared test transaction for any further statement).
        ApiException ex = assertThrows(ApiException.class, () -> service.createProject(request, submitter));

        assertEquals("PROJECT_ALREADY_EXISTS", ex.getCode());
        assertEquals(org.springframework.http.HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void maintainerCanUpdateProjectMetadata() {
        CreateProjectRequest createRequest = new CreateProjectRequest(
                uniqueGithubUrl(), ProjectConnection.SOUTH_AFRICAN, "Education", List.of("old"), List.of("ZA"));
        ProjectDto created = service.createProject(createRequest, submitter);

        UpdateProjectRequest updateRequest = new UpdateProjectRequest(
                "Fintech", List.of("payments"), ProjectConnection.AFRICA_FOCUSED, List.of("KE"));
        ProjectDto updated = service.updateProject(created.id(), updateRequest, submitter);

        assertEquals("Fintech", updated.category());
        assertEquals(List.of("payments"), updated.tags());
        assertEquals(ProjectConnection.AFRICA_FOCUSED, updated.connection());
        assertEquals(List.of("KE"), updated.countryCodes());
    }

    @Test
    void updateOnlyChangesProvidedFields() {
        CreateProjectRequest createRequest = new CreateProjectRequest(
                uniqueGithubUrl(), ProjectConnection.SOUTH_AFRICAN, "Education", List.of("original"), List.of("ZA"));
        ProjectDto created = service.createProject(createRequest, submitter);

        UpdateProjectRequest updateRequest = new UpdateProjectRequest("UpdatedCategory", null, null, null);
        ProjectDto updated = service.updateProject(created.id(), updateRequest, submitter);

        assertEquals("UpdatedCategory", updated.category());
        assertEquals(List.of("original"), updated.tags(), "tags weren't provided, should be untouched");
        assertEquals(ProjectConnection.SOUTH_AFRICAN, updated.connection(), "connection wasn't provided, should be untouched");
    }

    @Test
    void nonMaintainerUpdateIsForbidden() {
        CreateProjectRequest createRequest = new CreateProjectRequest(
                uniqueGithubUrl(), ProjectConnection.SOUTH_AFRICAN, "Education", null, null);
        ProjectDto created = service.createProject(createRequest, submitter);

        User stranger = userRepository.save(User.builder()
                .githubId(System.nanoTime())
                .username("stranger_" + System.nanoTime())
                .displayName("Stranger")
                .build());

        UpdateProjectRequest updateRequest = new UpdateProjectRequest("Hijacked", null, null, null);
        ApiException ex = assertThrows(ApiException.class,
                () -> service.updateProject(created.id(), updateRequest, stranger));

        assertEquals("FORBIDDEN", ex.getCode());
        assertEquals(org.springframework.http.HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void updateOnMissingProjectThrowsProjectNotFound() {
        UpdateProjectRequest updateRequest = new UpdateProjectRequest("X", null, null, null);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.updateProject(-999L, updateRequest, submitter));

        assertEquals("PROJECT_NOT_FOUND", ex.getCode());
        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, ex.getStatus());
    }
}
