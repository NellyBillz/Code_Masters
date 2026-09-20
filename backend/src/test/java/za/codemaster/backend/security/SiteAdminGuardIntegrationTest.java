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
import za.codemaster.testsupport.ThrowawayAdminController;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Acceptance tests for API-03.2 (round3-tickets.md): the reusable
 * {@link SiteAdminGuard}, verified in isolation against a throwaway
 * admin-guarded endpoint — same reasoning as
 * {@code SessionAuthenticationFilterIntegrationTest} (API-02.2), so this test
 * doesn't depend on any specific real {@code /admin/*} feature's business logic.
 * <p>
 * Covers the ticket's own acceptance criteria verbatim: no session -> 401;
 * valid session, {@code isSiteAdmin: false} -> 403; valid session,
 * {@code isSiteAdmin: true} -> reaches the handler. Also confirms the 403 uses
 * the existing {@code FORBIDDEN} shape, not a new error code, per the ticket's
 * contract note.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ThrowawayAdminController.class)
@Transactional
class SiteAdminGuardIntegrationTest {

    private static final String SESSION_COOKIE = "CODEMASTERS_SESSION";
    private static final String THROWAWAY_ADMIN_PATH = ThrowawayAdminController.PATH;

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
                .username("guarduser_" + suffix)
                .displayName("Guard Test User")
                .isSiteAdmin(isSiteAdmin)
                .build());

        return sessionRepository.save(Session.builder()
                .user(user)
                .csrToken("csrf-" + suffix)
                .expiresAt(OffsetDateTime.now().plusHours(1))
                .build());
    }

    @Test
    @DisplayName("No session -> 401 UNAUTHENTICATED")
    void noSessionIsUnauthenticated() throws Exception {
        mockMvc.perform(get(THROWAWAY_ADMIN_PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("Valid session, isSiteAdmin: false -> 403 FORBIDDEN, the same shape every maintainer-only endpoint uses")
    void validSessionNonAdminIsForbidden() throws Exception {
        Session session = createSession(false);

        mockMvc.perform(get(THROWAWAY_ADMIN_PATH)
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Valid session, isSiteAdmin: true -> reaches the handler")
    void validSessionAdminReachesHandler() throws Exception {
        Session session = createSession(true);

        mockMvc.perform(get(THROWAWAY_ADMIN_PATH)
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString())))
                .andExpect(status().isOk());
    }
}
