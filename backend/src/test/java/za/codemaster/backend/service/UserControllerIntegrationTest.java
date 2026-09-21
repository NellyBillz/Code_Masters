package za.codemaster.backend.service;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import za.codemaster.backend.domain.model.Claim;
import za.codemaster.backend.domain.model.ClaimStatus;
import za.codemaster.backend.domain.model.CompletionSource;
import za.codemaster.backend.domain.model.Issue;
import za.codemaster.backend.domain.model.Session;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.repository.ClaimRepository;
import za.codemaster.backend.repository.IssueRepository;
import za.codemaster.backend.repository.ProjectRepository;
import za.codemaster.backend.repository.SessionRepository;
import za.codemaster.backend.repository.UserRepository;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack acceptance tests for API-02.9/API-02.10 (round2-tickets.md), run through
 * the real {@code SecurityFilterChain}. Covers both tickets' acceptance criteria
 * verbatim: {@code GET /users/me} with no session -> 401; with a valid session -> the
 * caller's own profile, including {@code email}, which the public endpoint never
 * returns; updating only {@code bio} leaves other fields untouched; no session -> 401;
 * {@code bio} over 1000 chars -> 400 VALIDATION_ERROR.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerIntegrationTest {

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
    private ClaimRepository claimRepository;

    private Session createActiveSession(String email) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User user = userRepository.save(User.builder()
                .githubId(System.nanoTime())
                .username("user_" + suffix)
                .displayName("User " + suffix)
                .email(email)
                .build());

        return sessionRepository.save(Session.builder()
                .user(user)
                .csrToken("csrf-" + suffix)
                .expiresAt(OffsetDateTime.now().plusHours(1))
                .build());
    }

    @Test
    @DisplayName("GET /users/me with no session -> 401")
    void getCurrentUserWithNoSessionIsUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("GET /users/me with a garbage session cookie -> 401, not a 500")
    void getCurrentUserWithGarbageSessionIsUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .cookie(new Cookie(SESSION_COOKIE, "not-a-uuid")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("GET /users/me with a valid session -> the caller's own profile, including email")
    void getCurrentUserWithValidSessionReturnsProfileWithEmail() throws Exception {
        Session session = createActiveSession("caller@example.com");

        mockMvc.perform(get("/api/v1/users/me")
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(session.getUser().getUsername()))
                .andExpect(jsonPath("$.email").value("caller@example.com"))
                .andExpect(jsonPath("$.githubAccess").value(true));
    }

    @Test
    @DisplayName("GET /users/{username} is public and never returns email")
    void getPublicProfileNeverReturnsEmail() throws Exception {
        Session session = createActiveSession("shouldnotleak@example.com");

        mockMvc.perform(get("/api/v1/users/{username}", session.getUser().getUsername()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(session.getUser().getUsername()))
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    @DisplayName("GET /users/{username} for an unknown username -> 404 USER_NOT_FOUND")
    void getPublicProfileForUnknownUsernameIs404() throws Exception {
        mockMvc.perform(get("/api/v1/users/{username}", "no-such-user-xyz"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    @DisplayName("PATCH /users/me with no session -> 401")
    void updateCurrentUserWithNoSessionIsUnauthenticated() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bio\":\"new bio\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("Updating only bio leaves other fields untouched")
    void updatingOnlyBioLeavesOtherFieldsUntouched() throws Exception {
        Session session = createActiveSession("caller@example.com");
        String originalDisplayName = session.getUser().getDisplayName();

        mockMvc.perform(patch("/api/v1/users/me")
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString()))
                        .header(CSRF_HEADER, session.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bio\":\"new bio\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value("new bio"))
                .andExpect(jsonPath("$.displayName").value(originalDisplayName));
    }

    @Test
    @DisplayName("A bio over 1000 chars -> 400 VALIDATION_ERROR")
    void bioOverMaxLengthIsRejectedWith400() throws Exception {
        Session session = createActiveSession("caller@example.com");
        String tooLong = "a".repeat(1001);

        mockMvc.perform(patch("/api/v1/users/me")
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString()))
                        .header(CSRF_HEADER, session.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bio\":\"" + tooLong + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("DELETE /users/me with no session -> 401")
    void deleteCurrentUserWithNoSessionIsUnauthenticated() throws Exception {
        mockMvc.perform(delete("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("Deleting an account: 204, old username 404s afterward, old session cookie stops authenticating")
    void deletingAccountAnonymizesAndLogsOut() throws Exception {
        Session session = createActiveSession("caller@example.com");
        String oldUsername = session.getUser().getUsername();

        mockMvc.perform(delete("/api/v1/users/me")
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString()))
                        .header(CSRF_HEADER, session.getCsrToken()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users/{username}", oldUsername))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));

        // The old session row is gone, so the same cookie no longer authenticates.
        mockMvc.perform(get("/api/v1/users/me")
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("A deleted user's prior comment still renders, under the anonymized name")
    void deletedUsersCommentStillRendersUnderAnonymizedName() throws Exception {
        ProjectQueryServiceFixtures fixtures = ProjectQueryServiceFixtures.seed(projectRepository, issueRepository);
        Long issueId = fixtures.issueId(0);
        Session session = createActiveSession("caller@example.com");
        Long userId = session.getUser().getId();

        mockMvc.perform(post("/api/v1/issues/{issueId}/comments", issueId)
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString()))
                        .header(CSRF_HEADER, session.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"A comment worth keeping\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/v1/users/me")
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString()))
                        .header(CSRF_HEADER, session.getCsrToken()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/issues/{issueId}/comments", issueId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].body").value("A comment worth keeping"))
                .andExpect(jsonPath("$.items[0].author.username").value("deleted-user-" + userId));
    }

    @Test
    @DisplayName("A deleted user's completed contribution survives, attached to the anonymized identity")
    void deletedUsersCompletedContributionSurvives() throws Exception {
        ProjectQueryServiceFixtures fixtures = ProjectQueryServiceFixtures.seed(projectRepository, issueRepository);
        Issue issue = issueRepository.findById(fixtures.issueId(0)).orElseThrow();
        Session session = createActiveSession("caller@example.com");
        Long userId = session.getUser().getId();

        Claim claim = new Claim();
        claim.setIssue(issue);
        claim.setUser(session.getUser());
        claim.setStatus(ClaimStatus.COMPLETED);
        claim.setCompletionSource(CompletionSource.GITHUB_VERIFIED);
        claim.setCompletedAt(OffsetDateTime.now());
        claimRepository.save(claim);

        mockMvc.perform(delete("/api/v1/users/me")
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString()))
                        .header(CSRF_HEADER, session.getCsrToken()))
                .andExpect(status().isNoContent());

        // GET /stats (API-03.12) isn't built yet — this is the fact it will depend
        // on: the completed claim itself is untouched and still attached to a real
        // (now-anonymized) user, still discoverable as a verified contribution.
        mockMvc.perform(get("/api/v1/users/{username}/contributions", "deleted-user-" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1));
    }
}
