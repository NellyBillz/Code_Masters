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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack acceptance tests for API-03.9's admin moderation queue
 * (round3-tickets.md), run through the real {@code SecurityFilterChain}.
 * Covers the ticket's own acceptance criteria verbatim: a non-admin hitting
 * {@code /admin/reports} -> 403; resolving a report leaves the reported
 * comment untouched.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminReportControllerIntegrationTest {

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

    private Session createSession(boolean isSiteAdmin) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User user = userRepository.save(User.builder()
                .githubId(System.nanoTime())
                .username("adminreportuser_" + suffix)
                .displayName("Admin Report User")
                .isSiteAdmin(isSiteAdmin)
                .build());

        return sessionRepository.save(Session.builder()
                .user(user)
                .csrToken("csrf-" + suffix)
                .expiresAt(OffsetDateTime.now().plusHours(1))
                .build());
    }

    private Comment createComment() {
        ProjectQueryServiceFixtures fixtures = ProjectQueryServiceFixtures.seed(projectRepository, issueRepository);
        Issue issue = issueRepository.findById(fixtures.issueId(0)).orElseThrow();
        Comment comment = new Comment();
        comment.setIssue(issue);
        comment.setUser(createSession(false).getUser());
        comment.setBody("A comment worth reporting");
        return commentRepository.save(comment);
    }

    private Long fileReport(Comment comment, Session reporter) throws Exception {
        String response = mockMvc.perform(post("/api/v1/comments/{commentId}/reports", comment.getId())
                        .cookie(new Cookie(SESSION_COOKIE, reporter.getId().toString()))
                        .header(CSRF_HEADER, reporter.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"This is spam.\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Matcher matcher = Pattern.compile("\"id\":(\\d+)").matcher(response);
        if (!matcher.find()) {
            throw new IllegalStateException("No id field found in response: " + response);
        }
        return Long.valueOf(matcher.group(1));
    }

    @Test
    @DisplayName("GET /admin/reports while unauthenticated -> 401")
    void listReportsUnauthenticatedIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reports"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("A non-admin hitting GET /admin/reports -> 403")
    void listReportsByNonAdminIsForbidden() throws Exception {
        Session nonAdmin = createSession(false);

        mockMvc.perform(get("/api/v1/admin/reports")
                        .cookie(new Cookie(SESSION_COOKIE, nonAdmin.getId().toString())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("An admin sees a filed report in the default (open) queue")
    void adminSeesFiledReportInDefaultQueue() throws Exception {
        Comment comment = createComment();
        Session reporter = createSession(false);
        Session admin = createSession(true);
        Long reportId = fileReport(comment, reporter);

        mockMvc.perform(get("/api/v1/admin/reports")
                        .cookie(new Cookie(SESSION_COOKIE, admin.getId().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id == " + reportId + ")]").exists());
    }

    @Test
    @DisplayName("A non-admin hitting PATCH /admin/reports/{id} -> 403")
    void reviewByNonAdminIsForbidden() throws Exception {
        Comment comment = createComment();
        Session reporter = createSession(false);
        Session nonAdmin = createSession(false);
        Long reportId = fileReport(comment, reporter);

        mockMvc.perform(patch("/api/v1/admin/reports/{reportId}", reportId)
                        .cookie(new Cookie(SESSION_COOKIE, nonAdmin.getId().toString()))
                        .header(CSRF_HEADER, nonAdmin.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"dismissed\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Resolving a report leaves the reported comment untouched, and moves it out of the open queue")
    void resolvingReportLeavesCommentUntouchedAndLeavesOpenQueue() throws Exception {
        Comment comment = createComment();
        Session reporter = createSession(false);
        Session admin = createSession(true);
        Long reportId = fileReport(comment, reporter);

        mockMvc.perform(patch("/api/v1/admin/reports/{reportId}", reportId)
                        .cookie(new Cookie(SESSION_COOKIE, admin.getId().toString()))
                        .header(CSRF_HEADER, admin.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"resolved\",\"resolution\":\"Agreed, flagging to maintainer.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("resolved"))
                .andExpect(jsonPath("$.resolution").value("Agreed, flagging to maintainer."))
                .andExpect(jsonPath("$.resolvedAt").exists());

        mockMvc.perform(get("/api/v1/admin/reports")
                        .cookie(new Cookie(SESSION_COOKIE, admin.getId().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id == " + reportId + ")]").doesNotExist());

        mockMvc.perform(get("/api/v1/issues/{issueId}/comments", comment.getIssue().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].body").value("A comment worth reporting"));
    }

    @Test
    @DisplayName("Reviewing a missing report -> 404 REPORT_NOT_FOUND")
    void reviewingMissingReportIs404() throws Exception {
        Session admin = createSession(true);

        mockMvc.perform(patch("/api/v1/admin/reports/{reportId}", -999L)
                        .cookie(new Cookie(SESSION_COOKIE, admin.getId().toString()))
                        .header(CSRF_HEADER, admin.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"dismissed\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REPORT_NOT_FOUND"));
    }
}
