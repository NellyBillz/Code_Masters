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
import za.codemaster.backend.domain.model.Session;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.repository.IssueRepository;
import za.codemaster.backend.repository.ProjectRepository;
import za.codemaster.backend.repository.SessionRepository;
import za.codemaster.backend.repository.UserRepository;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack acceptance tests for API-02.3 (round2-tickets.md), run through the real
 * {@code SecurityFilterChain} (unlike {@code CommentServiceTest}, which bypasses
 * security entirely). Covers the ticket's own acceptance criteria verbatim:
 * unauthenticated POST -> 401 (proves API-02.2 is actually wired in), a body over
 * 5000 chars -> 400, and a successful post appearing in the corresponding GET list.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CommentControllerIntegrationTest {

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

    private ProjectQueryServiceFixtures fixtures;
    private Long projectId;
    private Long issueId;

    @BeforeEach
    void setUp() {
        fixtures = ProjectQueryServiceFixtures.seed(projectRepository, issueRepository);
        projectId = fixtures.projectId(0);
        issueId = fixtures.issueId(0);
    }

    private Session createActiveSession() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User user = userRepository.save(User.builder()
                .githubId(System.nanoTime())
                .username("commenter_" + suffix)
                .displayName("Commenter " + suffix)
                .build());

        return sessionRepository.save(Session.builder()
                .user(user)
                .csrToken("csrf-" + suffix)
                .expiresAt(OffsetDateTime.now().plusHours(1))
                .build());
    }

    private String commentJson(String body) {
        return "{\"body\":\"" + body + "\"}";
    }

    @Test
    @DisplayName("POST project comment while unauthenticated -> 401")
    void postProjectCommentUnauthenticatedIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/projects/{projectId}/comments", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentJson("hello")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("POST issue comment while unauthenticated -> 401")
    void postIssueCommentUnauthenticatedIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/issues/{issueId}/comments", issueId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentJson("hello")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("A body over 5000 chars -> 400 VALIDATION_ERROR")
    void bodyOverMaxLengthIsRejectedWith400() throws Exception {
        Session session = createActiveSession();
        String tooLong = "a".repeat(5001);

        mockMvc.perform(post("/api/v1/projects/{projectId}/comments", projectId)
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString()))
                        .header(CSRF_HEADER, session.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentJson(tooLong)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("A successful project comment post appears in the project's GET comment list")
    void successfulProjectCommentPostAppearsInList() throws Exception {
        Session session = createActiveSession();

        mockMvc.perform(post("/api/v1/projects/{projectId}/comments", projectId)
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString()))
                        .header(CSRF_HEADER, session.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentJson("Nice project, thanks!")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body").value("Nice project, thanks!"))
                .andExpect(jsonPath("$.author.username").value(session.getUser().getUsername()));

        mockMvc.perform(get("/api/v1/projects/{projectId}/comments", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].body").value("Nice project, thanks!"))
                .andExpect(jsonPath("$.meta.total").value(1));
    }

    @Test
    @DisplayName("A successful issue comment post appears in the issue's GET comment list")
    void successfulIssueCommentPostAppearsInList() throws Exception {
        Session session = createActiveSession();

        mockMvc.perform(post("/api/v1/issues/{issueId}/comments", issueId)
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString()))
                        .header(CSRF_HEADER, session.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentJson("I'd like to work on this.")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body").value("I'd like to work on this."));

        mockMvc.perform(get("/api/v1/issues/{issueId}/comments", issueId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].body").value("I'd like to work on this."))
                .andExpect(jsonPath("$.meta.total").value(1));
    }

    @Test
    @DisplayName("POST comment on a missing project -> 404 PROJECT_NOT_FOUND")
    void postCommentOnMissingProjectIs404() throws Exception {
        Session session = createActiveSession();

        mockMvc.perform(post("/api/v1/projects/{projectId}/comments", -999L)
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString()))
                        .header(CSRF_HEADER, session.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentJson("hello")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET comments on a missing issue -> 404 ISSUE_NOT_FOUND")
    void getCommentsOnMissingIssueIs404() throws Exception {
        mockMvc.perform(get("/api/v1/issues/{issueId}/comments", -999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ISSUE_NOT_FOUND"));
    }
}
