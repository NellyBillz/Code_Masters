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
import za.codemaster.backend.domain.model.Project;
import za.codemaster.backend.domain.model.ProjectMaintainer;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.dto.issue.Difficulty;
import za.codemaster.backend.dto.issue.IssueDto;
import za.codemaster.backend.dto.issue.UpdateIssueRequest;
import za.codemaster.backend.exception.ApiException;
import za.codemaster.backend.repository.ClaimRepository;
import za.codemaster.backend.repository.IssueRepository;
import za.codemaster.backend.repository.ProjectMaintainerRepository;
import za.codemaster.backend.repository.ProjectRepository;
import za.codemaster.backend.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies API-02.6's acceptance criteria (round2-tickets.md): a non-maintainer
 * gets 403, and after a successful override {@code difficultyOverriddenByUser}
 * is set to the caller — the entire reason that column exists (design doc §6),
 * since it's what tells a future GitHub sync (GH-02.3) not to clobber the
 * human correction.
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
class IssueServiceTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectMaintainerRepository projectMaintainerRepository;

    private IssueService service;
    private ProjectQueryServiceFixtures fixtures;
    private Long issueId;
    private Long projectId;
    private User maintainer;

    @BeforeEach
    void setUp() {
        ProjectQueryService projectQueryService =
                new ProjectQueryService(projectRepository, issueRepository, claimRepository);
        service = new IssueService(issueRepository, projectMaintainerRepository, projectQueryService);

        fixtures = ProjectQueryServiceFixtures.seed(projectRepository, issueRepository);
        issueId = fixtures.issueId(0);
        projectId = fixtures.projectId(0);

        maintainer = userRepository.save(User.builder()
                .githubId(System.nanoTime())
                .username("maintainer_" + System.nanoTime())
                .displayName("Maintainer")
                .build());

        Project project = projectRepository.findById(projectId).orElseThrow();
        ProjectMaintainer relationship = new ProjectMaintainer();
        relationship.setProject(project);
        relationship.setUser(maintainer);
        relationship.setRole("maintainer");
        projectMaintainerRepository.save(relationship);
    }

    private User nonMaintainer() {
        return userRepository.save(User.builder()
                .githubId(System.nanoTime())
                .username("stranger_" + System.nanoTime())
                .displayName("Stranger")
                .build());
    }

    @Test
    void maintainerCanOverrideDifficulty() {
        IssueDto updated = service.updateClassification(
                issueId, new UpdateIssueRequest(Difficulty.ADVANCED, null), maintainer);

        assertEquals(Difficulty.ADVANCED, updated.difficulty());
        assertTrue(updated.difficultyOverridden());
    }

    @Test
    void maintainerCanOverrideBeginnerFriendlyOnly() {
        IssueDto updated = service.updateClassification(
                issueId, new UpdateIssueRequest(null, true), maintainer);

        assertTrue(updated.isBeginnerFriendly());
        assertTrue(updated.difficultyOverridden());
    }

    @Test
    void maintainerCanOverrideBothFieldsAtOnce() {
        IssueDto updated = service.updateClassification(
                issueId, new UpdateIssueRequest(Difficulty.INTERMEDIATE, true), maintainer);

        assertEquals(Difficulty.INTERMEDIATE, updated.difficulty());
        assertTrue(updated.isBeginnerFriendly());
    }

    @Test
    void overrideSetsDifficultyOverriddenByUserToCaller() {
        service.updateClassification(issueId, new UpdateIssueRequest(Difficulty.BEGINNER, null), maintainer);

        var issueEntity = issueRepository.findById(issueId).orElseThrow();
        assertNotNull(issueEntity.getDifficultyOverriddenByUser());
        assertEquals(maintainer.getId(), issueEntity.getDifficultyOverriddenByUser().getId());
    }

    @Test
    void nonMaintainerOverrideIsForbidden() {
        User stranger = nonMaintainer();

        ApiException ex = assertThrows(ApiException.class,
                () -> service.updateClassification(issueId, new UpdateIssueRequest(Difficulty.ADVANCED, null), stranger));

        assertEquals("FORBIDDEN", ex.getCode());
        assertEquals(org.springframework.http.HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void nonMaintainerAttemptDoesNotSetOverrideFlag() {
        User stranger = nonMaintainer();

        assertThrows(ApiException.class,
                () -> service.updateClassification(issueId, new UpdateIssueRequest(Difficulty.ADVANCED, null), stranger));

        var issueEntity = issueRepository.findById(issueId).orElseThrow();
        assertNull(issueEntity.getDifficultyOverriddenByUser());
    }

    @Test
    void overrideOnMissingIssueThrowsIssueNotFound() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.updateClassification(-999L, new UpdateIssueRequest(Difficulty.ADVANCED, null), maintainer));

        assertEquals("ISSUE_NOT_FOUND", ex.getCode());
        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, ex.getStatus());
    }
}
