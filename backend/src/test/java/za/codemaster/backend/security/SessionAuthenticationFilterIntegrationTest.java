package za.codemaster.backend.security;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import za.codemaster.backend.domain.model.Session;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.repository.SessionRepository;
import za.codemaster.backend.repository.UserRepository;
import za.codemaster.testsupport.ThrowawayWriteController;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Acceptance tests for API-02.2 (round2-tickets.md): the real
 * {@code SecurityFilterChain}/{@link SessionAuthenticationFilter} replacing Round 1's
 * {@code permitAll()} placeholder.
 * <p>
 * Runs against a throwaway {@code @AuthenticatedUser}-annotated endpoint registered
 * only for this test, per the ticket's own suggestion ("test against a throwaway
 * endpoint if every real write endpoint is still pending") — this keeps the test from
 * depending on any specific write controller's business logic.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ThrowawayWriteController.class)
@Transactional
class SessionAuthenticationFilterIntegrationTest {

    private static final String SESSION_COOKIE = "CODEMASTERS_SESSION";
    private static final String CSRF_HEADER = "X-CSRF-Token";
    private static final String THROWAWAY_PATH = ThrowawayWriteController.PATH;

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
                .username("secuser_" + suffix)
                .displayName("Security Test User")
                .build());

        return sessionRepository.save(Session.builder()
                .user(user)
                .csrToken("csrf-" + suffix)
                .expiresAt(OffsetDateTime.now().plusHours(1))
                .build());
    }

    @Test
    @DisplayName("GET requests stay public with no session cookie at all")
    void getRequestIsPublicWithNoSession() throws Exception {
        mockMvc.perform(get(THROWAWAY_PATH))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST with no session cookie -> 401 UNAUTHENTICATED")
    void postWithNoSessionCookieIsRejected() throws Exception {
        mockMvc.perform(post(THROWAWAY_PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("POST with a valid session but no X-CSRF-Token header -> rejected")
    void postWithValidSessionButMissingCsrfHeaderIsRejected() throws Exception {
        Session session = createActiveSession();

        mockMvc.perform(post(THROWAWAY_PATH)
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_TOKEN_MISMATCH"));
    }

    @Test
    @DisplayName("POST with a valid session but the wrong X-CSRF-Token header -> rejected")
    void postWithValidSessionAndWrongCsrfHeaderIsRejected() throws Exception {
        Session session = createActiveSession();

        mockMvc.perform(post(THROWAWAY_PATH)
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString()))
                        .header(CSRF_HEADER, "not-the-right-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_TOKEN_MISMATCH"));
    }

    @Test
    @DisplayName("POST with a valid session and matching X-CSRF-Token header reaches the controller")
    void postWithValidSessionAndCorrectCsrfHeaderReachesController() throws Exception {
        Session session = createActiveSession();

        mockMvc.perform(post(THROWAWAY_PATH)
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString()))
                        .header(CSRF_HEADER, session.getCsrToken()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST with an unknown/garbage session cookie value -> 401 UNAUTHENTICATED, not a 500")
    void postWithGarbageSessionCookieIsUnauthenticated() throws Exception {
        mockMvc.perform(post(THROWAWAY_PATH)
                        .cookie(new Cookie(SESSION_COOKIE, "not-a-uuid")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }
}
