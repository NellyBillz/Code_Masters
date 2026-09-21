package za.codemaster.backend.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.dto.user.PagedContributions;
import za.codemaster.backend.dto.user.PublicUserProfile;
import za.codemaster.backend.dto.user.UpdateUserRequest;
import za.codemaster.backend.dto.user.UserProfile;
import za.codemaster.backend.repository.SessionRepository;
import za.codemaster.backend.security.AuthenticatedUser;
import za.codemaster.backend.service.UserService;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

/**
 * Developer profile endpoints: the read surface the frontend needs to show
 * "logged in as X" after OAuth, plus public profiles (API-02.9), self-editing
 * (API-02.10), and account deletion (API-03.11).
 */
@RestController
public class UserController {

    private final UserService userService;
    private final SessionRepository sessionRepository;
    private final boolean secureCookies;

    public UserController(UserService userService,
                           SessionRepository sessionRepository,
                           @Value("${app.cookies.secure:false}") boolean secureCookies) {
        this.userService = userService;
        this.sessionRepository = sessionRepository;
        this.secureCookies = secureCookies;
    }

    /**
     * {@code GET /api/v1/users/me}: the caller's own profile, including {@code email}.
     * Session-authenticated; {@code @AuthenticatedUser} throws {@code UNAUTHENTICATED}
     * (401) if there's no valid session — GET requests are public by default at the
     * {@code SecurityFilterChain} level, so this resolver is the actual enforcement
     * point here, not just a safety net.
     */
    @GetMapping("/api/v1/users/me")
    public UserProfile getCurrentUser(@AuthenticatedUser User currentUser) {
        return userService.getCurrentUserProfile(currentUser);
    }

    /**
     * {@code GET /api/v1/users/{username}}: a public developer profile. Never
     * includes {@code email}. Throws {@code USER_NOT_FOUND} (404) via
     * GlobalExceptionHandler if the username doesn't exist.
     */
    @GetMapping("/api/v1/users/{username}")
    public PublicUserProfile getPublicProfile(@PathVariable String username) {
        return userService.getPublicProfile(username);
    }

    /**
     * {@code GET /api/v1/users/{username}/contributions}: a developer's verified
     * contribution history (API-03.6) — only {@code completed} claims, most
     * recent first. Throws {@code USER_NOT_FOUND} (404) via GlobalExceptionHandler
     * if the username doesn't exist.
     */
    @GetMapping("/api/v1/users/{username}/contributions")
    public PagedContributions getContributions(
            @PathVariable String username,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return userService.getContributions(username, page, size);
    }

    /**
     * {@code PATCH /api/v1/users/me}: edit the caller's own profile (API-02.10).
     * Only {@code displayName}/{@code bio}/{@code location}/{@code skills} are
     * editable — GitHub-derived/system-managed fields aren't accepted here.
     */
    @PatchMapping("/api/v1/users/me")
    public UserProfile updateCurrentUser(
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticatedUser User currentUser) {
        return userService.updateCurrentUserProfile(currentUser, request);
    }

    /**
     * {@code DELETE /api/v1/users/me}: anonymize the caller's account (API-03.11).
     * Deletes the caller's session and clears both cookies here — same pattern as
     * {@code AuthController.logout} — so the old session cookie stops authenticating
     * immediately; the anonymization itself is {@link UserService#deleteCurrentUser}'s job.
     */
    @DeleteMapping("/api/v1/users/me")
    public ResponseEntity<Void> deleteCurrentUser(
            @AuthenticatedUser User currentUser,
            HttpServletRequest request) {
        userService.deleteCurrentUser(currentUser);

        sessionCookieValue(request).ifPresent(value -> {
            try {
                sessionRepository.deleteById(UUID.fromString(value));
            } catch (IllegalArgumentException ignored) {
                // An invalid/stale cookie has no session row to delete.
            }
        });

        ResponseCookie clearSession = expiredCookie(AuthController.SESSION_COOKIE, true);
        ResponseCookie clearCsrf = expiredCookie(AuthController.CSRF_COOKIE, false);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearSession.toString())
                .header(HttpHeaders.SET_COOKIE, clearCsrf.toString())
                .build();
    }

    private Optional<String> sessionCookieValue(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> AuthController.SESSION_COOKIE.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    private ResponseCookie expiredCookie(String name, boolean httpOnly) {
        return ResponseCookie.from(name, "")
                .httpOnly(httpOnly)
                .secure(secureCookies)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }
}
