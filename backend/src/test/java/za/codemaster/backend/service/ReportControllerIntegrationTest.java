package za.codemaster.backend.service;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import za.codemaster.backend.domain.model.Comment;
import za.codemaster.backend.domain.model.Issue;
import za.codemaster.backend.domain.model.Session;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.repository.CommentRepository;
import za.codemaster.backend.repository.IssueRepository;
import za.codemaster.backend.repository.ProjectRepository;
import za.codemaster.backend.repository.SessionRepository;
import za.codemaster.backend.repository.UserRepository;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack acceptance tests for API-03.9's public reporting endpoints
 * (round3-tickets.md), run through the real {@code SecurityFilterChain}.
 * Covers the ticket's own acceptance criteria verbatim: reporting the same
 * comment twice by the same user -> 409; a second, different user reporting
 * the same comment -> 201.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReportControllerIntegrationTest {

    private static final String SESSION_COOKIE = "CODEMASTERS_SESSION";
    private static final String CSRF_HEADER = "X-CSRF-Token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionRepository sessionRepository;

    private ProjectQueryServiceFixtures fixtures;

    private Session createSession() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User user = userRepository.save(User.builder()
                .githubId(System.nanoTime())
                .username("reportuser_" + suffix)
                .displayName("Report User")
                .build());

        return sessionRepository.save(Session.builder()
                .user(user)
                .csrToken("csrf-" + suffix)
                .expiresAt(OffsetDateTime.now().plusHours(1))
                .build());
    }

    private Comment createComment() {
        fixtures = ProjectQueryServiceFixtures.seed(projectRepository, issueRepository);
        Issue issue = issueRepository.findById(fixtures.issueId(0)).orElseThrow();
        Comment comment = new Comment();
        comment.setIssue(issue);
        comment.setUser(createSession().getUser());
        comment.setBody("A comment worth reporting");
        return commentRepository.save(comment);
    }

    private String reportJson(String reason) {
        return "{\"reason\":\"" + reason + "\"}";
    }

    @Test
    @DisplayName("POST comment report while unauthenticated -> 401")
    void reportCommentUnauthenticatedIsRejected() throws Exception {
        Comment comment = createComment();

        mockMvc.perform(post("/api/v1/comments/{commentId}/reports", comment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportJson("Spam")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("Reporting a comment succeeds with 201")
    void reportCommentSucceeds() throws Exception {
        Comment comment = createComment();
        Session reporter = createSession();

        mockMvc.perform(post("/api/v1/comments/{commentId}/reports", comment.getId())
                        .cookie(new Cookie(SESSION_COOKIE, reporter.getId().toString()))
                        .header(CSRF_HEADER, reporter.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportJson("This is spam.")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.targetType").value("comment"))
                .andExpect(jsonPath("$.targetId").value(comment.getId()))
                .andExpect(jsonPath("$.status").value("open"));
    }

    @Test
    @DisplayName("Reporting the same comment twice by the same user -> 409 REPORT_ALREADY_EXISTS")
    void reportingSameCommentTwiceBySameUserIsRejected() throws Exception {
        Comment comment = createComment();
        Session reporter = createSession();

        mockMvc.perform(post("/api/v1/comments/{commentId}/reports", comment.getId())
                        .cookie(new Cookie(SESSION_COOKIE, reporter.getId().toString()))
                        .header(CSRF_HEADER, reporter.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportJson("This is spam.")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/comments/{commentId}/reports", comment.getId())
                        .cookie(new Cookie(SESSION_COOKIE, reporter.getId().toString()))
                        .header(CSRF_HEADER, reporter.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportJson("Still spam.")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REPORT_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("A second, different user reporting the same comment -> 201")
    void aSecondDifferentUserReportingSameCommentSucceeds() throws Exception {
        Comment comment = createComment();
        Session reporterA = createSession();
        Session reporterB = createSession();

        mockMvc.perform(post("/api/v1/comments/{commentId}/reports", comment.getId())
                        .cookie(new Cookie(SESSION_COOKIE, reporterA.getId().toString()))
                        .header(CSRF_HEADER, reporterA.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportJson("Spam from A.")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/comments/{commentId}/reports", comment.getId())
                        .cookie(new Cookie(SESSION_COOKIE, reporterB.getId().toString()))
                        .header(CSRF_HEADER, reporterB.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportJson("Spam from B.")))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("A reason under 3 characters -> 400 VALIDATION_ERROR")
    void reasonUnderMinLengthIsRejectedWith400() throws Exception {
        Comment comment = createComment();
        Session reporter = createSession();

        mockMvc.perform(post("/api/v1/comments/{commentId}/reports", comment.getId())
                        .cookie(new Cookie(SESSION_COOKIE, reporter.getId().toString()))
                        .header(CSRF_HEADER, reporter.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportJson("Hi")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Reporting a missing comment -> 404 COMMENT_NOT_FOUND")
    void reportingMissingCommentIs404() throws Exception {
        Session reporter = createSession();

        mockMvc.perform(post("/api/v1/comments/{commentId}/reports", -999L)
                        .cookie(new Cookie(SESSION_COOKIE, reporter.getId().toString()))
                        .header(CSRF_HEADER, reporter.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportJson("This is spam.")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"));
    }

    @Test
    @DisplayName("Reporting a project succeeds with 201, and a duplicate -> 409")
    void reportProjectSucceedsThenDuplicateIsRejected() throws Exception {
        fixtures = ProjectQueryServiceFixtures.seed(projectRepository, issueRepository);
        Long projectId = fixtures.projectId(0);
        Session reporter = createSession();

        mockMvc.perform(post("/api/v1/projects/{projectId}/reports", projectId)
                        .cookie(new Cookie(SESSION_COOKIE, reporter.getId().toString()))
                        .header(CSRF_HEADER, reporter.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportJson("Not African open source.")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.targetType").value("project"));

        mockMvc.perform(post("/api/v1/projects/{projectId}/reports", projectId)
                        .cookie(new Cookie(SESSION_COOKIE, reporter.getId().toString()))
                        .header(CSRF_HEADER, reporter.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportJson("Still not African open source.")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REPORT_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("Reporting a missing project -> 404 PROJECT_NOT_FOUND")
    void reportingMissingProjectIs404() throws Exception {
        Session reporter = createSession();

        mockMvc.perform(post("/api/v1/projects/{projectId}/reports", -999L)
                        .cookie(new Cookie(SESSION_COOKIE, reporter.getId().toString()))
                        .header(CSRF_HEADER, reporter.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportJson("This is spam.")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
    }
}
