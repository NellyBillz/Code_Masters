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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack acceptance tests for API-02.8 (round2-tickets.md), run through the real
 * {@code SecurityFilterChain}. Covers the ticket's own acceptance criteria verbatim:
 * attempting to remove the sole owner -> 409 and the project still has that owner
 * afterward, and a second invite of the same user -> 409.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MaintainerControllerIntegrationTest {

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
    private Long projectId;
    private Session ownerSession;

    @BeforeEach
    void setUp() {
        fixtures = ProjectQueryServiceFixtures.seed(projectRepository, issueRepository);
        projectId = fixtures.projectId(0);
        ownerSession = createSession();
        addMaintainer(ownerSession.getUser(), "owner");
    }

    private Session createSession() {
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

    private void addMaintainer(User user, String role) {
        Project project = projectRepository.findById(projectId).orElseThrow();
        ProjectMaintainer relationship = new ProjectMaintainer();
        relationship.setProject(project);
        relationship.setUser(user);
        relationship.setRole(role);
        projectMaintainerRepository.save(relationship);
    }

    private String inviteJson(String username) {
        return "{\"username\":\"" + username + "\"}";
    }

    @Test
    @DisplayName("POST maintainers while unauthenticated -> 401")
    void inviteUnauthenticatedIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/projects/{projectId}/maintainers", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inviteJson("someone")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("Owner invites a user; role defaults to maintainer")
    void ownerInvitesMaintainerWithDefaultRole() throws Exception {
        Session invitee = createSession();

        mockMvc.perform(post("/api/v1/projects/{projectId}/maintainers", projectId)
                        .cookie(new Cookie(SESSION_COOKIE, ownerSession.getId().toString()))
                        .header(CSRF_HEADER, ownerSession.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inviteJson(invitee.getUser().getUsername())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("maintainer"))
                .andExpect(jsonPath("$.user.username").value(invitee.getUser().getUsername()));

        mockMvc.perform(get("/api/v1/projects/{projectId}", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maintainers.length()").value(2));
    }

    @Test
    @DisplayName("A non-owner maintainer attempting to invite -> 403")
    void nonOwnerInviteIsForbidden() throws Exception {
        Session plainMaintainerSession = createSession();
        addMaintainer(plainMaintainerSession.getUser(), "maintainer");
        Session invitee = createSession();

        mockMvc.perform(post("/api/v1/projects/{projectId}/maintainers", projectId)
                        .cookie(new Cookie(SESSION_COOKIE, plainMaintainerSession.getId().toString()))
                        .header(CSRF_HEADER, plainMaintainerSession.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inviteJson(invitee.getUser().getUsername())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("A second invite of the same user -> 409 MAINTAINER_ALREADY_EXISTS")
    void secondInviteOfSameUserIsRejected() throws Exception {
        Session invitee = createSession();

        mockMvc.perform(post("/api/v1/projects/{projectId}/maintainers", projectId)
                        .cookie(new Cookie(SESSION_COOKIE, ownerSession.getId().toString()))
                        .header(CSRF_HEADER, ownerSession.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inviteJson(invitee.getUser().getUsername())))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/projects/{projectId}/maintainers", projectId)
                        .cookie(new Cookie(SESSION_COOKIE, ownerSession.getId().toString()))
                        .header(CSRF_HEADER, ownerSession.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inviteJson(invitee.getUser().getUsername())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MAINTAINER_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("Inviting an unknown username -> 404 USER_NOT_FOUND")
    void inviteUnknownUsernameIs404() throws Exception {
        mockMvc.perform(post("/api/v1/projects/{projectId}/maintainers", projectId)
                        .cookie(new Cookie(SESSION_COOKIE, ownerSession.getId().toString()))
                        .header(CSRF_HEADER, ownerSession.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inviteJson("no-such-user-xyz")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    @DisplayName("DELETE maintainer while unauthenticated -> 401")
    void removeUnauthenticatedIsRejected() throws Exception {
        mockMvc.perform(delete("/api/v1/projects/{projectId}/maintainers/{userId}",
                        projectId, ownerSession.getUser().getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("Attempting to remove the sole owner -> 409, project still has that owner afterward")
    void removingSoleOwnerIsRejectedAndOwnerRemains() throws Exception {
        mockMvc.perform(delete("/api/v1/projects/{projectId}/maintainers/{userId}",
                        projectId, ownerSession.getUser().getId())
                        .cookie(new Cookie(SESSION_COOKIE, ownerSession.getId().toString()))
                        .header(CSRF_HEADER, ownerSession.getCsrToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CANNOT_REMOVE_LAST_OWNER"));

        mockMvc.perform(get("/api/v1/projects/{projectId}", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maintainers.length()").value(1))
                .andExpect(jsonPath("$.maintainers[0].user.username").value(ownerSession.getUser().getUsername()));
    }

    @Test
    @DisplayName("Owner can remove a plain maintainer")
    void ownerCanRemovePlainMaintainer() throws Exception {
        Session plainMaintainerSession = createSession();
        addMaintainer(plainMaintainerSession.getUser(), "maintainer");

        mockMvc.perform(delete("/api/v1/projects/{projectId}/maintainers/{userId}",
                        projectId, plainMaintainerSession.getUser().getId())
                        .cookie(new Cookie(SESSION_COOKIE, ownerSession.getId().toString()))
                        .header(CSRF_HEADER, ownerSession.getCsrToken()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/projects/{projectId}", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maintainers.length()").value(1));
    }

    @Test
    @DisplayName("A non-owner attempting to remove a maintainer -> 403")
    void nonOwnerRemoveIsForbidden() throws Exception {
        Session plainMaintainerSession = createSession();
        addMaintainer(plainMaintainerSession.getUser(), "maintainer");
        Session anotherMaintainerSession = createSession();
        addMaintainer(anotherMaintainerSession.getUser(), "maintainer");

        mockMvc.perform(delete("/api/v1/projects/{projectId}/maintainers/{userId}",
                        projectId, anotherMaintainerSession.getUser().getId())
                        .cookie(new Cookie(SESSION_COOKIE, plainMaintainerSession.getId().toString()))
                        .header(CSRF_HEADER, plainMaintainerSession.getCsrToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Removing a non-maintainer -> 404 MAINTAINER_NOT_FOUND")
    void removeNonMaintainerIs404() throws Exception {
        Session notAMaintainer = createSession();

        mockMvc.perform(delete("/api/v1/projects/{projectId}/maintainers/{userId}",
                        projectId, notAMaintainer.getUser().getId())
                        .cookie(new Cookie(SESSION_COOKIE, ownerSession.getId().toString()))
                        .header(CSRF_HEADER, ownerSession.getCsrToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MAINTAINER_NOT_FOUND"));
    }

    @Test
    @DisplayName("Invite/remove on a missing project -> 404 PROJECT_NOT_FOUND")
    void operationsOnMissingProjectAre404() throws Exception {
        mockMvc.perform(post("/api/v1/projects/{projectId}/maintainers", -999L)
                        .cookie(new Cookie(SESSION_COOKIE, ownerSession.getId().toString()))
                        .header(CSRF_HEADER, ownerSession.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inviteJson(ownerSession.getUser().getUsername())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
    }
}
