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
import za.codemaster.backend.domain.model.Session;
import za.codemaster.backend.domain.model.User;
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
 * Full-stack acceptance tests for API-02.7 (round2-tickets.md), run through the real
 * {@code SecurityFilterChain}. Covers the ticket's own acceptance criteria verbatim:
 * a successful POST /projects immediately shows the caller in the project's maintainer
 * list, and a duplicate githubUrl -> 409, not a raw DB exception.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProjectControllerIntegrationTest {

    private static final String SESSION_COOKIE = "CODEMASTERS_SESSION";
    private static final String CSRF_HEADER = "X-CSRF-Token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionRepository sessionRepository;

    private Session createActiveSession() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User user = userRepository.save(User.builder()
                .githubId(System.nanoTime())
                .username("submitter_" + suffix)
                .displayName("Submitter " + suffix)
                .build());

        return sessionRepository.save(Session.builder()
                .user(user)
                .csrToken("csrf-" + suffix)
                .expiresAt(OffsetDateTime.now().plusHours(1))
                .build());
    }

    private String uniqueGithubUrl() {
        return "https://github.com/example-org/repo-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * A submission URL whose owner segment matches {@code ownerUsername} — the free,
     * no-API-call verification signal (security audit finding, 2026-09-24), so the
     * submitter actually ends up as the project's owner maintainer. Tests exercising
     * post-submission maintainer/ownership behavior need this instead of
     * {@link #uniqueGithubUrl()}, whose {@code example-org} owner never matches any
     * test session's username and so now leaves the project maintainer-less.
     */
    private String uniqueGithubUrl(String ownerUsername) {
        return "https://github.com/" + ownerUsername + "/repo-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String createProjectJson(String githubUrl) {
        return "{\"githubUrl\":\"" + githubUrl + "\",\"connection\":\"south_african\",\"category\":\"Developer Tools\"}";
    }

    private Long extractId(String responseBody) {
        Matcher matcher = Pattern.compile("\"id\":(\\d+)").matcher(responseBody);
        if (!matcher.find()) {
            throw new IllegalStateException("No id field found in response: " + responseBody);
        }
        return Long.valueOf(matcher.group(1));
    }

    @Test
    @DisplayName("POST /projects while unauthenticated -> 401")
    void submitProjectUnauthenticatedIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createProjectJson(uniqueGithubUrl())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("A successful POST /projects immediately shows the caller in the maintainer list")
    void successfulSubmissionShowsCallerAsMaintainer() throws Exception {
        Session session = createActiveSession();
        String githubUrl = uniqueGithubUrl(session.getUser().getUsername());

        String response = mockMvc.perform(post("/api/v1/projects")
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString()))
                        .header(CSRF_HEADER, session.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createProjectJson(githubUrl)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.githubUrl").value(githubUrl))
                .andExpect(jsonPath("$.connection").value("south_african"))
                .andReturn().getResponse().getContentAsString();

        Long projectId = extractId(response);

        // A freshly submitted project is `pending` (API-03.1) — the submitter
        // must be authenticated as themselves to still see it.
        mockMvc.perform(get("/api/v1/projects/{projectId}", projectId)
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maintainers.length()").value(1))
                .andExpect(jsonPath("$.maintainers[0].user.username").value(session.getUser().getUsername()))
                .andExpect(jsonPath("$.maintainers[0].role").value("owner"));
    }

    @Test
    @DisplayName("A duplicate githubUrl -> 409 PROJECT_ALREADY_EXISTS, not a raw DB exception")
    void duplicateGithubUrlIsRejectedCleanly() throws Exception {
        Session session = createActiveSession();
        String githubUrl = uniqueGithubUrl();

        mockMvc.perform(post("/api/v1/projects")
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString()))
                        .header(CSRF_HEADER, session.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createProjectJson(githubUrl)))
                .andExpect(status().isCreated());

        // Last DB-touching action in this test — see ProjectServiceTest's equivalent note.
        mockMvc.perform(post("/api/v1/projects")
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString()))
                        .header(CSRF_HEADER, session.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createProjectJson(githubUrl)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROJECT_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("A non-GitHub URL -> 400 VALIDATION_ERROR")
    void nonGithubUrlIsRejectedWith400() throws Exception {
        Session session = createActiveSession();

        mockMvc.perform(post("/api/v1/projects")
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString()))
                        .header(CSRF_HEADER, session.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createProjectJson("https://gitlab.com/example-org/repo")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("PATCH /projects/{id} while unauthenticated -> 401")
    void updateProjectUnauthenticatedIsRejected() throws Exception {
        mockMvc.perform(patch("/api/v1/projects/{projectId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"Fintech\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("The maintainer who submitted a project can PATCH it")
    void maintainerCanUpdateOwnProject() throws Exception {
        Session session = createActiveSession();
        String response = mockMvc.perform(post("/api/v1/projects")
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString()))
                        .header(CSRF_HEADER, session.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createProjectJson(uniqueGithubUrl(session.getUser().getUsername()))))
                .andReturn().getResponse().getContentAsString();
        Long projectId = extractId(response);

        mockMvc.perform(patch("/api/v1/projects/{projectId}", projectId)
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString()))
                        .header(CSRF_HEADER, session.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"Fintech\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("Fintech"));
    }

    @Test
    @DisplayName("Toggling acceptingContributions is reflected on the next GET; listingStatus in the same body has no effect")
    void togglingAcceptingContributionsIsReflectedButListingStatusIsIgnored() throws Exception {
        Session session = createActiveSession();
        String response = mockMvc.perform(post("/api/v1/projects")
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString()))
                        .header(CSRF_HEADER, session.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createProjectJson(uniqueGithubUrl(session.getUser().getUsername()))))
                .andExpect(jsonPath("$.acceptingContributions").value(true))
                .andReturn().getResponse().getContentAsString();
        Long projectId = extractId(response);

        mockMvc.perform(patch("/api/v1/projects/{projectId}", projectId)
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString()))
                        .header(CSRF_HEADER, session.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"acceptingContributions\":false,\"listingStatus\":\"published\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acceptingContributions").value(false))
                .andExpect(jsonPath("$.listingStatus").value("pending"));

        mockMvc.perform(get("/api/v1/projects/{projectId}", projectId)
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acceptingContributions").value(false))
                .andExpect(jsonPath("$.listingStatus").value("pending"));
    }

    @Test
    @DisplayName("A non-maintainer attempting to PATCH -> 403")
    void nonMaintainerUpdateIsForbidden() throws Exception {
        Session owner = createActiveSession();
        Session stranger = createActiveSession();
        String response = mockMvc.perform(post("/api/v1/projects")
                        .cookie(new Cookie(SESSION_COOKIE, owner.getId().toString()))
                        .header(CSRF_HEADER, owner.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createProjectJson(uniqueGithubUrl())))
                .andReturn().getResponse().getContentAsString();
        Long projectId = extractId(response);

        mockMvc.perform(patch("/api/v1/projects/{projectId}", projectId)
                        .cookie(new Cookie(SESSION_COOKIE, stranger.getId().toString()))
                        .header(CSRF_HEADER, stranger.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"Hijacked\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("PATCH on a missing project -> 404 PROJECT_NOT_FOUND")
    void patchMissingProjectIs404() throws Exception {
        Session session = createActiveSession();

        mockMvc.perform(patch("/api/v1/projects/{projectId}", -999L)
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString()))
                        .header(CSRF_HEADER, session.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"X\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
    }
}
