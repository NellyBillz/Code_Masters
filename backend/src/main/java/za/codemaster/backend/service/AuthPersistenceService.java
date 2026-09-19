package za.codemaster.backend.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.codemaster.backend.client.github.dto.GitHubOAuthUser;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class AuthPersistenceService {
    private final JdbcTemplate jdbcTemplate;

    public AuthPersistenceService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public LoginSession login(GitHubOAuthUser githubUser, String csrfToken, OffsetDateTime expiresAt) {
        Long userId = jdbcTemplate.queryForObject("""
                INSERT INTO users (github_id, username, display_name, avatar_url, email, bio, location)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (github_id) DO UPDATE SET
                    username = EXCLUDED.username,
                    display_name = EXCLUDED.display_name,
                    avatar_url = EXCLUDED.avatar_url,
                    email = EXCLUDED.email,
                    bio = EXCLUDED.bio,
                    location = EXCLUDED.location,
                    updated_at = NOW()
                RETURNING id
                """, Long.class,
                githubUser.id(), githubUser.login(), githubUser.name(), githubUser.avatarUrl(),
                githubUser.email(), githubUser.bio(), githubUser.location());

        UUID sessionId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO sessions (id, user_id, csrf_token, expires_at) VALUES (?, ?, ?, ?)",
                sessionId, userId, csrfToken, expiresAt);
        return new LoginSession(sessionId, userId);
    }

    public record LoginSession(UUID sessionId, long userId) {}
}
