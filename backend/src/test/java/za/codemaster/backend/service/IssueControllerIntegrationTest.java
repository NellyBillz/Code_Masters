package za.codemaster.backend.service;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import za.codemaster.backend.domain.model.Project;
import za.codemaster.backend.domain.model.ProjectMaintainer;
import za.codemaster.backend.domain.model.Session;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.repository.IssueRepository;
import za.codemaster.backend.repository.ProjectMaintainerRepository;
import za.codemaster.backend.repository.ProjectRepository;
import za.codemaster.backend.repository.SessionRepository;
import za.codemaster.backend.repository.UserRepository;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack acceptance tests for API-02.6 (round2-tickets.md), run through the real
 * {@code SecurityFilterChain}. Covers the ticket's own acceptance criteria verbatim:
 * a non-maintainer -> 403, and after a successful override,
 * {@code difficultyOverriddenByUser} is set to the caller.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IssueControllerIntegrationTest {

    private static final String SESSION_COOKIE = "CODEMASTERS_SESSION";
    private static final String CSRF_HEADER = "X-CSRF-Token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private ProjectMaintainerRepository projectMaintainerRepository;

    private ProjectQueryServiceFixtures fixtures;
    private Long issueId;
    private Long projectId;

    @BeforeEach
    void setUp() {
        fixtures = ProjectQueryServiceFixtures.seed(projectRepository, issueRepository);
        issueId = fixtures.issueId(0);
        projectId = fixtures.projectId(0);
    }

    private Session createActiveSession() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User user = userRepository.save(User.builder()
                .githubId(System.nanoTime())
                .username("user_" + suffix)
                .displayName("User " + suffix)
                .build());

        return sessionRepository.save(Session.builder()
                .user(user)
                .csrToken("csrf-" + suffix)
                .expiresAt(OffsetDateTime.now().plusHours(1))
                .build());
    }

    private Session createMaintainerSession() {
        Session session = createActiveSession();
        Project project = projectRepository.findById(projectId).orElseThrow();
        ProjectMaintainer maintainer = new ProjectMaintainer();
        maintainer.setProject(project);
        maintainer.setUser(session.getUser());
        maintainer.setRole("maintainer");
        projectMaintainerRepository.save(maintainer);
        return session;
    }

    @Test
    @DisplayName("PATCH issue while unauthenticated -> 401")
    void patchIssueUnauthenticatedIsRejected() throws Exception {
        mockMvc.perform(patch("/api/v1/issues/{issueId}", issueId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"difficulty\":\"advanced\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("A non-maintainer attempting to override -> 403")
    void nonMaintainerOverrideIsForbidden() throws Exception {
        Session session = createActiveSession();

        mockMvc.perform(patch("/api/v1/issues/{issueId}", issueId)
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString()))
                        .header(CSRF_HEADER, session.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"difficulty\":\"advanced\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("A maintainer's override succeeds and marks difficultyOverridden")
    void maintainerOverrideSucceeds() throws Exception {
        Session session = createMaintainerSession();

        mockMvc.perform(patch("/api/v1/issues/{issueId}", issueId)
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString()))
                        .header(CSRF_HEADER, session.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"difficulty\":\"advanced\",\"isBeginnerFriendly\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.difficulty").value("advanced"))
                .andExpect(jsonPath("$.isBeginnerFriendly").value(false))
                .andExpect(jsonPath("$.difficultyOverridden").value(true));
    }

    @Test
    @DisplayName("An unrecognized difficulty value -> 400 VALIDATION_ERROR, not a 500")
    void invalidDifficultyValueIsRejectedWith400() throws Exception {
        Session session = createMaintainerSession();

        mockMvc.perform(patch("/api/v1/issues/{issueId}", issueId)
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString()))
                        .header(CSRF_HEADER, session.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"difficulty\":\"impossible\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("PATCH on a missing issue -> 404 ISSUE_NOT_FOUND")
    void patchMissingIssueIs404() throws Exception {
        Session session = createActiveSession();

        mockMvc.perform(patch("/api/v1/issues/{issueId}", -999L)
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString()))
                        .header(CSRF_HEADER, session.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"difficulty\":\"advanced\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ISSUE_NOT_FOUND"));
    }
}
