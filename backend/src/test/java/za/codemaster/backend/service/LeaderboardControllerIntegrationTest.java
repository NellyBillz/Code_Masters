package za.codemaster.backend.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import za.codemaster.backend.domain.model.Session;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.repository.SessionRepository;
import za.codemaster.backend.repository.UserRepository;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import jakarta.servlet.http.Cookie;

/**
 * Full-stack acceptance tests for the Recognition Leaderboard (wow-feature,
 * 2026-09-24), run through the real {@code SecurityFilterChain}: the public
 * list needs no session, the personalized rank does.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LeaderboardControllerIntegrationTest {

    private static final String SESSION_COOKIE = "CODEMASTERS_SESSION";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Test
    @DisplayName("GET /leaderboard is public — no session required")
    void leaderboardIsPublic() throws Exception {
        mockMvc.perform(get("/api/v1/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /users/me/leaderboard-rank without a session -> 401")
    void myRankRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/leaderboard-rank"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("GET /users/me/leaderboard-rank with a session -> the caller's own entry")
    void myRankReturnsTheCallersOwnEntry() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User user = userRepository.save(User.builder()
                .githubId(System.nanoTime())
                .username("rankcheck_" + suffix)
                .displayName("Rank Check")
                .build());
        Session session = sessionRepository.save(Session.builder()
                .user(user)
                .csrToken("csrf-" + suffix)
                .expiresAt(OffsetDateTime.now().plusHours(1))
                .build());

        mockMvc.perform(get("/api/v1/users/me/leaderboard-rank")
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getId()))
                .andExpect(jsonPath("$.username").value(user.getUsername()))
                .andExpect(jsonPath("$.rank").value(org.hamcrest.Matchers.nullValue()));
    }
}
