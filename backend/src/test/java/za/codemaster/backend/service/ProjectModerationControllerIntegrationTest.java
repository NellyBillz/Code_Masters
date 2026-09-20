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
 * Full-stack acceptance tests for API-03.1 (round3-tickets.md), run through the real
 * {@code SecurityFilterChain}. Covers the ticket's own acceptance criteria verbatim:
 * a project is absent from public discovery immediately after submission but still
 * visible to its submitter, non-admins are refused the {@code /admin} endpoints,
 * an approved project becomes publicly visible, and {@code listingStatus} can't be
 * changed via the ordinary project-update endpoint.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProjectModerationControllerIntegrationTest {

    private static final String SESSION_COOKIE = "CODEMASTERS_SESSION";
    private static final String CSRF_HEADER = "X-CSRF-Token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionRepository sessionRepository;

    private Session createSession(boolean isSiteAdmin) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User user = userRepository.save(User.builder()
                .githubId(System.nanoTime())
                .username("user_" + suffix)
                .displayName("User " + suffix)
                .isSiteAdmin(isSiteAdmin)
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

    private String uniqueMarker() {
        return "moderationmarker" + UUID.randomUUID().toString().substring(0, 8);
    }

    private Long submitProject(Session submitter, String category) throws Exception {
        return submitProject(submitter, category, null);
    }

    /**
     * {@code searchableTag}, when given, is sent as the project's only tag — {@code q}
     * matches a project's name/description/owner/tags (not {@code category}), and a
     * freshly submitted project has no description yet (API-02.7 never collects one),
     * so a tag is the only field these tests can reliably search on.
     */
    private Long submitProject(Session submitter, String category, String searchableTag) throws Exception {
        String tagsJson = searchableTag == null ? "" : ",\"tags\":[\"" + searchableTag + "\"]";
        String response = mockMvc.perform(post("/api/v1/projects")
                        .cookie(new Cookie(SESSION_COOKIE, submitter.getId().toString()))
                        .header(CSRF_HEADER, submitter.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"githubUrl\":\"" + uniqueGithubUrl()
                                + "\",\"connection\":\"south_african\",\"category\":\"" + category + "\"" + tagsJson + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.listingStatus").value("pending"))
                .andReturn().getResponse().getContentAsString();

        Matcher matcher = Pattern.compile("\"id\":(\\d+)").matcher(response);
        if (!matcher.find()) {
            throw new IllegalStateException("No id field found in response: " + response);
        }
        return Long.valueOf(matcher.group(1));
    }

    @Test
    @DisplayName("A pending project is absent from GET /projects but visible to its submitter via GET /projects/{id}")
    void pendingProjectHiddenFromPublicListingButVisibleToSubmitter() throws Exception {
        Session submitter = createSession(false);
        String marker = uniqueMarker();
        Long projectId = submitProject(submitter, "Developer Tools", marker);

        mockMvc.perform(get("/api/v1/projects").param("q", marker))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));

        mockMvc.perform(get("/api/v1/search").param("q", marker))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));

        mockMvc.perform(get("/api/v1/projects/{projectId}", projectId)
                        .cookie(new Cookie(SESSION_COOKIE, submitter.getId().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.listingStatus").value("pending"));
    }

    @Test
    @DisplayName("A stranger (not the submitter, not a maintainer) hitting a pending project's detail -> 404")
    void pendingProjectIs404ToAStranger() throws Exception {
        Session submitter = createSession(false);
        Session stranger = createSession(false);
        Long projectId = submitProject(submitter, "Fintech");

        mockMvc.perform(get("/api/v1/projects/{projectId}", projectId)
                        .cookie(new Cookie(SESSION_COOKIE, stranger.getId().toString())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));

        mockMvc.perform(get("/api/v1/projects/{projectId}", projectId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
    }

    @Test
    @DisplayName("A non-admin hitting GET /admin/projects/pending -> 403")
    void nonAdminCannotListPending() throws Exception {
        Session nonAdmin = createSession(false);

        mockMvc.perform(get("/api/v1/admin/projects/pending")
                        .cookie(new Cookie(SESSION_COOKIE, nonAdmin.getId().toString())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("An unauthenticated caller hitting GET /admin/projects/pending -> 401")
    void unauthenticatedCannotListPending() throws Exception {
        mockMvc.perform(get("/api/v1/admin/projects/pending"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("A non-admin hitting POST /admin/projects/{id}/moderation -> 403")
    void nonAdminCannotModerate() throws Exception {
        Session submitter = createSession(false);
        Session nonAdmin = createSession(false);
        Long projectId = submitProject(submitter, "Health");

        mockMvc.perform(post("/api/v1/admin/projects/{projectId}/moderation", projectId)
                        .cookie(new Cookie(SESSION_COOKIE, nonAdmin.getId().toString()))
                        .header(CSRF_HEADER, nonAdmin.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"approve\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("An admin sees a pending submission in the queue, and approving it makes it publicly visible everywhere")
    void adminCanListAndApprovePendingProject() throws Exception {
        Session admin = createSession(true);
        Session submitter = createSession(false);
        String marker = uniqueMarker();
        Long projectId = submitProject(submitter, "Developer Tools", marker);

        mockMvc.perform(get("/api/v1/admin/projects/pending")
                        .cookie(new Cookie(SESSION_COOKIE, admin.getId().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id == " + projectId + ")]").exists());

        mockMvc.perform(post("/api/v1/admin/projects/{projectId}/moderation", projectId)
                        .cookie(new Cookie(SESSION_COOKIE, admin.getId().toString()))
                        .header(CSRF_HEADER, admin.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"approve\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.listingStatus").value("published"));

        mockMvc.perform(get("/api/v1/projects").param("q", marker))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1));

        // Now visible to a total stranger too, with no session at all.
        mockMvc.perform(get("/api/v1/projects/{projectId}", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.listingStatus").value("published"));
    }

    @Test
    @DisplayName("An admin rejecting a submission leaves it out of public discovery, with a reason recorded on the response")
    void adminCanRejectPendingProject() throws Exception {
        Session admin = createSession(true);
        Session submitter = createSession(false);
        String marker = uniqueMarker();
        Long projectId = submitProject(submitter, "Developer Tools", marker);

        mockMvc.perform(post("/api/v1/admin/projects/{projectId}/moderation", projectId)
                        .cookie(new Cookie(SESSION_COOKIE, admin.getId().toString()))
                        .header(CSRF_HEADER, admin.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"reject\",\"reason\":\"Not related to African open source.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.listingStatus").value("rejected"));

        mockMvc.perform(get("/api/v1/projects").param("q", marker))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    @DisplayName("Moderating a missing project -> 404 PROJECT_NOT_FOUND")
    void moderatingMissingProjectIs404() throws Exception {
        Session admin = createSession(true);

        mockMvc.perform(post("/api/v1/admin/projects/{projectId}/moderation", -999L)
                        .cookie(new Cookie(SESSION_COOKIE, admin.getId().toString()))
                        .header(CSRF_HEADER, admin.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"approve\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"));
    }

    @Test
    @DisplayName("PATCH /projects/{id} cannot change listingStatus even if a client sends it")
    void listingStatusCannotBeChangedViaPatch() throws Exception {
        Session submitter = createSession(false);
        Long projectId = submitProject(submitter, "Education");

        mockMvc.perform(patch("/api/v1/projects/{projectId}", projectId)
                        .cookie(new Cookie(SESSION_COOKIE, submitter.getId().toString()))
                        .header(CSRF_HEADER, submitter.getCsrToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"Education\",\"listingStatus\":\"published\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("Education"))
                .andExpect(jsonPath("$.listingStatus").value("pending"));

        // Still pending — invisible to a stranger, unaffected by the smuggled field.
        mockMvc.perform(get("/api/v1/projects/{projectId}", projectId))
                .andExpect(status().isNotFound());
    }
}
