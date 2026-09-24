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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack acceptance tests for API-02.5 (round2-tickets.md), run through the real
 * {@code SecurityFilterChain}. Covers the ticket's own acceptance criteria verbatim:
 * two different users can each hold an active claim on the same issue simultaneously
 * (design doc §7's whole point), and the same user claiming twice -> 409.
 * <p>
 * See {@code ClaimServiceTest}'s class Javadoc for why the duplicate-claim test is
 * kept as the last DB-touching action in its test method.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ClaimControllerIntegrationTest {

    private static final String SESSION_COOKIE = "CODEMASTERS_SESSION";
    private static final String CSRF_HEADER = "X-CSRF-Token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private ProjectMaintainerRepository projectMaintainerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionRepository sessionRepository;

    private ProjectQueryServiceFixtures fixtures;
    private Long issueId;

    @BeforeEach
    void setUp() {
        fixtures = ProjectQueryServiceFixtures.seed(projectRepository, issueRepository);
        issueId = fixtures.issueId(0);
    }

    private Session createActiveSession() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User user = userRepository.save(User.builder()
                .githubId(System.nanoTime())
                .username("claimer_" + suffix)
                .displayName("Claimer " + suffix)
                .build());

        return sessionRepository.save(Session.builder()
                .user(user)
                .csrToken("csrf-" + suffix)
                .expiresAt(OffsetDateTime.now().plusHours(1))
                .build());
    }

    private String claimJson(String note) {
        return note == null ? "{}" : "{\"note\":\"" + note + "\"}";
    }

    private void addMaintainer(User user) {
        Project project = projectRepository.findById(fixtures.projectId(0)).orElseThrow();
        ProjectMaintainer relationship = new ProjectMaintainer();
        relationship.setProject(project);
        relationship.setUser(user);
        relationship.setRole("maintainer");
        projectMaintainerRepository.save(relationship);
    }

    private Long createClaimAndExtractId(Session session) throws Exception {
        String response = mockMvc.perform(post("/api/v1/issues/{issueId}/claim", issueId)
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString()))
                        .header(CSRF_HEADER, session.getCsrToken()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Matcher matcher = Pattern.compile("\"id\":(\\d+)").matcher(response);
        if (!matcher.find()) {
            throw new IllegalStateException("No id field found in response: " + response);
        }
        return Long.valueOf(matcher.group(1));
    }

    @Test
    @DisplayName("POST claim while unauthenticated -> 401")
    void claimUnauthenticatedIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/issues/{issueId}/claim", issueId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("DELETE claim while unauthenticated -> 401")
    void releaseClaimUnauthenticatedIsRejected() throws Exception {
        mockMvc.perform(delete("/api/v1/issues/{issueId}/claim", issueId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("POST claim with no body succeeds (note is entirely optional)")
    void claimWithNoBodySucceeds() throws Exception {
        Session session = createActiveSession();

        mockMvc.perform(post("/api/v1/issues/{issueId}/claim", issueId)
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString()))
                        .header(CSRF_HEADER, session.getCsrToken()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("active"))
                .andExpect(jsonPath("$.user.username").value(session.getUser().getUsername()));
    }

    @Test
    @DisplayName("A successful claim appears in the issue's GET claims list, oldest first")
    void successfulClaimAppearsInList() throws Exception {
        Session session = createActiveSession();

        mockMvc.perform(post("/api/v1/issues/{issueId}/claim", issueId)
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString()))
                        .header(CSRF_HEADER, session.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(claimJson("Working on this")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.note").value("Working on this"));

        mockMvc.perform(get("/api/v1/issues/{issueId}/claims", issueId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].note").value("Working on this"));
    }

    @Test
    @DisplayName("Two different users can each hold an active claim on the same issue simultaneously")
    void twoDifferentUsersCanBothClaimTheSameIssue() throws Exception {
        Session sessionA = createActiveSession();
        Session sessionB = createActiveSession();

        mockMvc.perform(post("/api/v1/issues/{issueId}/claim", issueId)
                        .cookie(new Cookie(SESSION_COOKIE, sessionA.getId().toString()))
                        .header(CSRF_HEADER, sessionA.getCsrToken()))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/issues/{issueId}/claim", issueId)
                        .cookie(new Cookie(SESSION_COOKIE, sessionB.getId().toString()))
                        .header(CSRF_HEADER, sessionB.getCsrToken()))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/issues/{issueId}/claims", issueId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/v1/issues/{issueId}", issueId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claimCount").value(2));
    }

    @Test
    @DisplayName("The same user claiming the same issue twice -> 409 CLAIM_ALREADY_ACTIVE")
    void sameUserClaimingTwiceIsRejected() throws Exception {
        Session session = createActiveSession();

        mockMvc.perform(post("/api/v1/issues/{issueId}/claim", issueId)
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString()))
                        .header(CSRF_HEADER, session.getCsrToken()))
                .andExpect(status().isCreated());

        // Last DB-touching action in this test — see ClaimServiceTest's class Javadoc.
        mockMvc.perform(post("/api/v1/issues/{issueId}/claim", issueId)
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString()))
                        .header(CSRF_HEADER, session.getCsrToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CLAIM_ALREADY_ACTIVE"));
    }

    @Test
    @DisplayName("POST claim on a closed issue -> 409 ISSUE_NOT_OPEN")
    void claimOnClosedIssueIsConflict() throws Exception {
        Session session = createActiveSession();
        Long closedIssueId = fixtures.issueId(5); // "Investigate flaky ETL pipeline test", status = "closed"

        mockMvc.perform(post("/api/v1/issues/{issueId}/claim", closedIssueId)
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString()))
                        .header(CSRF_HEADER, session.getCsrToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ISSUE_NOT_OPEN"));
    }

    @Test
    @DisplayName("A note over 280 chars -> 400 VALIDATION_ERROR")
    void noteOverMaxLengthIsRejectedWith400() throws Exception {
        Session session = createActiveSession();
        String tooLong = "a".repeat(281);

        mockMvc.perform(post("/api/v1/issues/{issueId}/claim", issueId)
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString()))
                        .header(CSRF_HEADER, session.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(claimJson(tooLong)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Releasing an own active claim -> 204, still visible in the claims list as released (API-03.5)")
    void releaseOwnClaimStillAppearsInListAsReleased() throws Exception {
        Session session = createActiveSession();
        mockMvc.perform(post("/api/v1/issues/{issueId}/claim", issueId)
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString()))
                        .header(CSRF_HEADER, session.getCsrToken()))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/v1/issues/{issueId}/claim", issueId)
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString()))
                        .header(CSRF_HEADER, session.getCsrToken()))
                .andExpect(status().isNoContent());

        // API-03.5: GET /issues/{issueId}/claims returns every status, not just
        // active — a released claim is still part of the issue's history.
        mockMvc.perform(get("/api/v1/issues/{issueId}/claims", issueId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("released"));
    }

    @Test
    @DisplayName("Releasing with no active claim -> 404 CLAIM_NOT_FOUND")
    void releaseWithoutActiveClaimIs404() throws Exception {
        Session session = createActiveSession();

        mockMvc.perform(delete("/api/v1/issues/{issueId}/claim", issueId)
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString()))
                        .header(CSRF_HEADER, session.getCsrToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CLAIM_NOT_FOUND"));
    }

    @Test
    @DisplayName("POST claim on a missing issue -> 404 ISSUE_NOT_FOUND")
    void claimOnMissingIssueIs404() throws Exception {
        Session session = createActiveSession();

        mockMvc.perform(post("/api/v1/issues/{issueId}/claim", -999L)
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString()))
                        .header(CSRF_HEADER, session.getCsrToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ISSUE_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET claims on a missing issue -> 404 ISSUE_NOT_FOUND")
    void getClaimsOnMissingIssueIs404() throws Exception {
        mockMvc.perform(get("/api/v1/issues/{issueId}/claims", -999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ISSUE_NOT_FOUND"));
    }

    @Test
    @DisplayName("PATCH claim while unauthenticated -> 401")
    void attachPullRequestUnauthenticatedIsRejected() throws Exception {
        Session owner = createActiveSession();
        Long claimId = createClaimAndExtractId(owner);

        mockMvc.perform(patch("/api/v1/issues/{issueId}/claims/{claimId}", issueId, claimId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pullRequestUrl\":\"https://github.com/example-org/repo/pull/7\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("The claim owner attaching a pull request -> 200, reflected in the next GET claims list")
    void ownerAttachingPullRequestIsReflectedInList() throws Exception {
        Session owner = createActiveSession();
        Long claimId = createClaimAndExtractId(owner);

        mockMvc.perform(patch("/api/v1/issues/{issueId}/claims/{claimId}", issueId, claimId)
                        .cookie(new Cookie(SESSION_COOKIE, owner.getId().toString()))
                        .header(CSRF_HEADER, owner.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pullRequestUrl\":\"https://github.com/example-org/repo/pull/7\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pullRequestUrl").value("https://github.com/example-org/repo/pull/7"))
                .andExpect(jsonPath("$.pullRequestState").value("open"));

        mockMvc.perform(get("/api/v1/issues/{issueId}/claims", issueId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pullRequestUrl").value("https://github.com/example-org/repo/pull/7"))
                .andExpect(jsonPath("$[0].pullRequestState").value("open"));
    }

    @Test
    @DisplayName("A non-owner attempting to attach a pull request -> 403, even a maintainer of the project")
    void nonOwnerAttachingPullRequestIsForbiddenEvenForAMaintainer() throws Exception {
        Session owner = createActiveSession();
        Long claimId = createClaimAndExtractId(owner);

        Session maintainerSession = createActiveSession();
        addMaintainer(maintainerSession.getUser());

        mockMvc.perform(patch("/api/v1/issues/{issueId}/claims/{claimId}", issueId, claimId)
                        .cookie(new Cookie(SESSION_COOKIE, maintainerSession.getId().toString()))
                        .header(CSRF_HEADER, maintainerSession.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pullRequestUrl\":\"https://github.com/example-org/repo/pull/7\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("A blank pullRequestUrl -> 400 VALIDATION_ERROR")
    void blankPullRequestUrlIsRejectedWith400() throws Exception {
        Session owner = createActiveSession();
        Long claimId = createClaimAndExtractId(owner);

        mockMvc.perform(patch("/api/v1/issues/{issueId}/claims/{claimId}", issueId, claimId)
                        .cookie(new Cookie(SESSION_COOKIE, owner.getId().toString()))
                        .header(CSRF_HEADER, owner.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pullRequestUrl\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("A missing pullRequestUrl field -> 400 VALIDATION_ERROR")
    void missingPullRequestUrlIsRejectedWith400() throws Exception {
        Session owner = createActiveSession();
        Long claimId = createClaimAndExtractId(owner);

        mockMvc.perform(patch("/api/v1/issues/{issueId}/claims/{claimId}", issueId, claimId)
                        .cookie(new Cookie(SESSION_COOKIE, owner.getId().toString()))
                        .header(CSRF_HEADER, owner.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("PATCH on a missing claim -> 404 CLAIM_NOT_FOUND")
    void attachPullRequestOnMissingClaimIs404() throws Exception {
        Session owner = createActiveSession();

        mockMvc.perform(patch("/api/v1/issues/{issueId}/claims/{claimId}", issueId, -999L)
                        .cookie(new Cookie(SESSION_COOKIE, owner.getId().toString()))
                        .header(CSRF_HEADER, owner.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pullRequestUrl\":\"https://github.com/example-org/repo/pull/7\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CLAIM_NOT_FOUND"));
    }

    @Test
    @DisplayName("PATCH on a missing issue -> 404 ISSUE_NOT_FOUND")
    void attachPullRequestOnMissingIssueIs404() throws Exception {
        Session owner = createActiveSession();
        Long claimId = createClaimAndExtractId(owner);

        mockMvc.perform(patch("/api/v1/issues/{issueId}/claims/{claimId}", -999L, claimId)
                        .cookie(new Cookie(SESSION_COOKIE, owner.getId().toString()))
                        .header(CSRF_HEADER, owner.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pullRequestUrl\":\"https://github.com/example-org/repo/pull/7\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ISSUE_NOT_FOUND"));
    }

    @Test
    @DisplayName("POST review while unauthenticated -> 401")
    void reviewClaimUnauthenticatedIsRejected() throws Exception {
        Session owner = createActiveSession();
        Long claimId = createClaimAndExtractId(owner);

        mockMvc.perform(post("/api/v1/issues/{issueId}/claims/{claimId}/review", issueId, claimId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"confirm_completed\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("A non-maintainer attempting to review -> 403")
    void reviewByNonMaintainerIsForbidden() throws Exception {
        Session owner = createActiveSession();
        Long claimId = createClaimAndExtractId(owner);
        Session stranger = createActiveSession();

        mockMvc.perform(post("/api/v1/issues/{issueId}/claims/{claimId}/review", issueId, claimId)
                        .cookie(new Cookie(SESSION_COOKIE, stranger.getId().toString()))
                        .header(CSRF_HEADER, stranger.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"confirm_completed\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("request_changes with no feedback -> 400 VALIDATION_ERROR")
    void requestChangesWithoutFeedbackIs400() throws Exception {
        Session owner = createActiveSession();
        Long claimId = createClaimAndExtractId(owner);
        Session maintainerSession = createActiveSession();
        addMaintainer(maintainerSession.getUser());

        mockMvc.perform(post("/api/v1/issues/{issueId}/claims/{claimId}/review", issueId, claimId)
                        .cookie(new Cookie(SESSION_COOKIE, maintainerSession.getId().toString()))
                        .header(CSRF_HEADER, maintainerSession.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"request_changes\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("request_changes with feedback -> 200, changes_requested, feedback visible on next GET")
    void requestChangesWithFeedbackIsVisibleOnNextRead() throws Exception {
        Session owner = createActiveSession();
        Long claimId = createClaimAndExtractId(owner);
        Session maintainerSession = createActiveSession();
        addMaintainer(maintainerSession.getUser());

        mockMvc.perform(post("/api/v1/issues/{issueId}/claims/{claimId}/review", issueId, claimId)
                        .cookie(new Cookie(SESSION_COOKIE, maintainerSession.getId().toString()))
                        .header(CSRF_HEADER, maintainerSession.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"request_changes\",\"feedback\":\"Please add tests.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("changes_requested"))
                .andExpect(jsonPath("$.maintainerFeedback").value("Please add tests."));
    }

    @Test
    @DisplayName("confirm_completed -> 200, completed, completionSource maintainer_confirmed")
    void confirmCompletedSetsCompletedWithMaintainerConfirmedSource() throws Exception {
        Session owner = createActiveSession();
        Long claimId = createClaimAndExtractId(owner);
        Session maintainerSession = createActiveSession();
        addMaintainer(maintainerSession.getUser());

        mockMvc.perform(post("/api/v1/issues/{issueId}/claims/{claimId}/review", issueId, claimId)
                        .cookie(new Cookie(SESSION_COOKIE, maintainerSession.getId().toString()))
                        .header(CSRF_HEADER, maintainerSession.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"confirm_completed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("completed"))
                .andExpect(jsonPath("$.completionSource").value("maintainer_confirmed"))
                .andExpect(jsonPath("$.completedAt").exists());
    }

    @Test
    @DisplayName("Re-confirming an already-completed claim -> 200, unchanged")
    void reconfirmingAnAlreadyCompletedClaimIsANoOp() throws Exception {
        Session owner = createActiveSession();
        Long claimId = createClaimAndExtractId(owner);
        Session maintainerSession = createActiveSession();
        addMaintainer(maintainerSession.getUser());

        String firstResponse = mockMvc.perform(post("/api/v1/issues/{issueId}/claims/{claimId}/review", issueId, claimId)
                        .cookie(new Cookie(SESSION_COOKIE, maintainerSession.getId().toString()))
                        .header(CSRF_HEADER, maintainerSession.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"confirm_completed\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(post("/api/v1/issues/{issueId}/claims/{claimId}/review", issueId, claimId)
                        .cookie(new Cookie(SESSION_COOKIE, maintainerSession.getId().toString()))
                        .header(CSRF_HEADER, maintainerSession.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"confirm_completed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("completed"))
                .andExpect(jsonPath("$.completionSource").value("maintainer_confirmed"));

        assertTrue(firstResponse.contains("\"completionSource\":\"maintainer_confirmed\""));
    }

    @Test
    @DisplayName("Reviewing a released claim -> 409 CLAIM_NOT_REVIEWABLE")
    void reviewingAReleasedClaimIs409() throws Exception {
        Session owner = createActiveSession();
        Long claimId = createClaimAndExtractId(owner);
        Session maintainerSession = createActiveSession();
        addMaintainer(maintainerSession.getUser());

        mockMvc.perform(delete("/api/v1/issues/{issueId}/claim", issueId)
                        .cookie(new Cookie(SESSION_COOKIE, owner.getId().toString()))
                        .header(CSRF_HEADER, owner.getCsrToken()))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/issues/{issueId}/claims/{claimId}/review", issueId, claimId)
                        .cookie(new Cookie(SESSION_COOKIE, maintainerSession.getId().toString()))
                        .header(CSRF_HEADER, maintainerSession.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"confirm_completed\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CLAIM_NOT_REVIEWABLE"));
    }

    @Test
    @DisplayName("PATCH review on a missing claim -> 404 CLAIM_NOT_FOUND")
    void reviewOnMissingClaimIs404() throws Exception {
        Session maintainerSession = createActiveSession();
        addMaintainer(maintainerSession.getUser());

        mockMvc.perform(post("/api/v1/issues/{issueId}/claims/{claimId}/review", issueId, -999L)
                        .cookie(new Cookie(SESSION_COOKIE, maintainerSession.getId().toString()))
                        .header(CSRF_HEADER, maintainerSession.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"confirm_completed\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CLAIM_NOT_FOUND"));
    }
}
