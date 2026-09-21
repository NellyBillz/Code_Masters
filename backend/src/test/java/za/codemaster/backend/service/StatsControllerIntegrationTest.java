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
import za.codemaster.backend.domain.model.Issue;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack acceptance tests for API-03.12 (round3-tickets.md), run through the
 * real {@code SecurityFilterChain}. Covers the ticket's own acceptance criteria
 * verbatim: approving a pending project increments {@code publishedProjects};
 * a claim reaching {@code completed} increments {@code totalContributionsCompleted}
 * on the next call; a pending project counts toward nothing. Drives these
 * transitions through the real moderation (API-03.1) and claim-review (API-03.4)
 * endpoints, not direct repository writes, so this proves the whole loop.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StatsControllerIntegrationTest {

    private static final String SESSION_COOKIE = "CODEMASTERS_SESSION";
    private static final String CSRF_HEADER = "X-CSRF-Token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private ProjectMaintainerRepository projectMaintainerRepository;

    private Session createSession(boolean isSiteAdmin) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User user = userRepository.save(User.builder()
                .githubId(System.nanoTime())
                .username("statsapiuser_" + suffix)
                .displayName("Stats API User")
                .isSiteAdmin(isSiteAdmin)
                .build());

        return sessionRepository.save(Session.builder()
                .user(user)
                .csrToken("csrf-" + suffix)
                .expiresAt(OffsetDateTime.now().plusHours(1))
                .build());
    }

    private void addMaintainer(Issue issue, User user) {
        ProjectMaintainer relationship = new ProjectMaintainer();
        relationship.setProject(issue.getProject());
        relationship.setUser(user);
        relationship.setRole("maintainer");
        projectMaintainerRepository.save(relationship);
    }

    private long statsField(String field) throws Exception {
        String body = mockMvc.perform(get("/api/v1/stats"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Matcher matcher = Pattern.compile("\"" + field + "\":(\\d+)").matcher(body);
        if (!matcher.find()) {
            throw new IllegalStateException("No " + field + " field found in response: " + body);
        }
        return Long.parseLong(matcher.group(1));
    }

    private Long extractId(String responseBody) {
        Matcher matcher = Pattern.compile("\"id\":(\\d+)").matcher(responseBody);
        if (!matcher.find()) {
            throw new IllegalStateException("No id field found in response: " + responseBody);
        }
        return Long.valueOf(matcher.group(1));
    }

    private String uniqueGithubUrl() {
        return "https://github.com/stats-org/repo-" + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    @DisplayName("GET /stats is public — no session required")
    void statsIsPublic() throws Exception {
        mockMvc.perform(get("/api/v1/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publishedProjects").exists())
                .andExpect(jsonPath("$.activeProjectsAcceptingContributions").exists())
                .andExpect(jsonPath("$.totalContributorsEngaged").exists())
                .andExpect(jsonPath("$.totalActiveClaims").exists())
                .andExpect(jsonPath("$.totalContributionsCompleted").exists())
                .andExpect(jsonPath("$.generatedAt").exists());
    }

    @Test
    @DisplayName("A pending project counts toward nothing; approving it increments publishedProjects")
    void approvingAPendingProjectIncrementsPublishedProjects() throws Exception {
        Session submitter = createSession(false);
        Session admin = createSession(true);

        long beforeSubmission = statsField("publishedProjects");

        String response = mockMvc.perform(post("/api/v1/projects")
                        .cookie(new Cookie(SESSION_COOKIE, submitter.getId().toString()))
                        .header(CSRF_HEADER, submitter.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"githubUrl\":\"" + uniqueGithubUrl()
                                + "\",\"connection\":\"south_african\",\"category\":\"Developer Tools\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long projectId = extractId(response);

        // Still pending — must count toward nothing.
        assertEquals(beforeSubmission, statsField("publishedProjects"));

        mockMvc.perform(post("/api/v1/admin/projects/{projectId}/moderation", projectId)
                        .cookie(new Cookie(SESSION_COOKIE, admin.getId().toString()))
                        .header(CSRF_HEADER, admin.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"approve\"}"))
                .andExpect(status().isOk());

        assertEquals(beforeSubmission + 1, statsField("publishedProjects"));
    }

    @Test
    @DisplayName("A claim reaching completed increments totalContributionsCompleted on the next call")
    void claimReachingCompletedIncrementsTotalContributionsCompleted() throws Exception {
        Session contributor = createSession(false);
        Session maintainerSession = createSession(false);

        ProjectQueryServiceFixtures fixtures = ProjectQueryServiceFixtures.seed(projectRepository, issueRepository);
        Long issueId = fixtures.issueId(0);
        Issue issue = issueRepository.findById(issueId).orElseThrow();
        addMaintainer(issue, maintainerSession.getUser());

        mockMvc.perform(post("/api/v1/issues/{issueId}/claim", issueId)
                        .cookie(new Cookie(SESSION_COOKIE, contributor.getId().toString()))
                        .header(CSRF_HEADER, contributor.getCsrToken()))
                .andExpect(status().isCreated());

        String claimsResponse = mockMvc.perform(get("/api/v1/issues/{issueId}/claims", issueId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long claimId = extractId(claimsResponse);

        long before = statsField("totalContributionsCompleted");

        mockMvc.perform(post("/api/v1/issues/{issueId}/claims/{claimId}/review", issueId, claimId)
                        .cookie(new Cookie(SESSION_COOKIE, maintainerSession.getId().toString()))
                        .header(CSRF_HEADER, maintainerSession.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"confirm_completed\"}"))
                .andExpect(status().isOk());

        assertEquals(before + 1, statsField("totalContributionsCompleted"));
    }
}
