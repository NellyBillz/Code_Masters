package za.codemaster.backend.service;

import org.springframework.stereotype.Component;
import za.codemaster.backend.domain.model.Session;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.repository.SessionRepository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Shared session authentication boundary for API-02.2 and later authenticated endpoints.
 * Invalid, missing and expired session values deliberately resolve to Optional.empty().
 */
@Component
public class SessionUserResolver {
    private final SessionRepository sessionRepository;

    public SessionUserResolver(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    public Optional<User> resolve(String sessionCookieValue) {
        return resolveSession(sessionCookieValue).map(Session::getUser);
    }

    /**
     * Resolves the full {@link Session} row (not just its user), since CSRF
     * enforcement needs the session's stored {@code csrf_token} to compare
     * against the request's {@code X-CSRF-Token} header.
     */
    public Optional<Session> resolveSession(String sessionCookieValue) {
        if (sessionCookieValue == null || sessionCookieValue.isBlank()) {
            return Optional.empty();
        }
        try {
            return sessionRepository.findActiveSession(UUID.fromString(sessionCookieValue), OffsetDateTime.now());
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
