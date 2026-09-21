package za.codemaster.backend.service;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack acceptance tests for API-03.10 (round3-tickets.md), run through the
 * real {@code SecurityFilterChain} with deliberately low configured limits (2 per
 * action, via {@code @TestPropertySource}) so the acceptance criteria can be
 * proven with a handful of requests instead of waiting an hour or sending 20+.
 * Covers the ticket's own acceptance criteria verbatim: scripted rapid-fire
 * requests past each configured limit -> 429 RATE_LIMITED; requests under the
 * limit are unaffected; GET endpoints are never rate-limited.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.rate-limit.comments-per-hour=2",
        "app.rate-limit.claims-per-hour=2",
        "app.rate-limit.reports-per-hour=2"
})
@Transactional
class RateLimitControllerIntegrationTest {

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

    @BeforeEach
    void setUp() {
        fixtures = ProjectQueryServiceFixtures.seed(projectRepository, issueRepository);
    }

    private Session createSession() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User user = userRepository.save(User.builder()
                .githubId(System.nanoTime())
                .username("ratelimituser_" + suffix)
                .displayName("Rate Limit User")
                .build());

        return sessionRepository.save(Session.builder()
                .user(user)
                .csrToken("csrf-" + suffix)
                .expiresAt(OffsetDateTime.now().plusHours(1))
                .build());
    }

    @Test
    @DisplayName("Comment creation: 2 succeed, the 3rd -> 429 RATE_LIMITED")
    void commentCreationPastLimitIs429() throws Exception {
        Session session = createSession();
        Long issueIdA = fixtures.issueId(0);
        Long issueIdB = fixtures.issueId(1);
        Long issueIdC = fixtures.issueId(2);

        mockMvc.perform(post("/api/v1/issues/{issueId}/comments", issueIdA)
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString()))
                        .header(CSRF_HEADER, session.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"First comment\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/issues/{issueId}/comments", issueIdB)
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString()))
                        .header(CSRF_HEADER, session.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Second comment\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/issues/{issueId}/comments", issueIdC)
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString()))
                        .header(CSRF_HEADER, session.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Third comment\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"));
    }

    @Test
    @DisplayName("Claim creation: 2 succeed, the 3rd -> 429 RATE_LIMITED")
    void claimCreationPastLimitIs429() throws Exception {
        Session session = createSession();

        mockMvc.perform(post("/api/v1/issues/{issueId}/claim", fixtures.issueId(0))
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString()))
                        .header(CSRF_HEADER, session.getCsrToken()))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/issues/{issueId}/claim", fixtures.issueId(1))
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString()))
                        .header(CSRF_HEADER, session.getCsrToken()))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/issues/{issueId}/claim", fixtures.issueId(2))
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString()))
                        .header(CSRF_HEADER, session.getCsrToken()))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"));
    }

    @Test
    @DisplayName("Report creation: 2 succeed, the 3rd -> 429 RATE_LIMITED")
    void reportCreationPastLimitIs429() throws Exception {
        Session session = createSession();
        Issue issue = issueRepository.findById(fixtures.issueId(0)).orElseThrow();

        Comment commentA = saveComment(issue, "Comment A");
        Comment commentB = saveComment(issue, "Comment B");
        Comment commentC = saveComment(issue, "Comment C");

        mockMvc.perform(post("/api/v1/comments/{commentId}/reports", commentA.getId())
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString()))
                        .header(CSRF_HEADER, session.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Spam A\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/comments/{commentId}/reports", commentB.getId())
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString()))
                        .header(CSRF_HEADER, session.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Spam B\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/comments/{commentId}/reports", commentC.getId())
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString()))
                        .header(CSRF_HEADER, session.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Spam C\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"));
    }

    @Test
    @DisplayName("GET endpoints are never rate-limited, however many times they're hit")
    void getEndpointsAreNeverRateLimited() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/v1/projects"))
                    .andExpect(status().isOk());
        }
    }

    private Comment saveComment(Issue issue, String body) {
        Comment comment = new Comment();
        comment.setIssue(issue);
        comment.setUser(createSession().getUser());
        comment.setBody(body);
        return commentRepository.save(comment);
    }
}
