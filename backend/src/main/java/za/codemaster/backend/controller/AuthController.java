package za.codemaster.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import jakarta.servlet.http.Cookie;
import za.codemaster.backend.repository.SessionRepository;
import java.util.Arrays;
import java.util.UUID;
import za.codemaster.backend.client.github.dto.GitHubOAuthUser;
import za.codemaster.backend.service.AuthPersistenceService;
import za.codemaster.backend.service.GitHubOAuthService;

import java.net.URI;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;

@Controller
public class AuthController {
    public static final String SESSION_COOKIE = "CODEMASTERS_SESSION";
    public static final String CSRF_COOKIE = "CODEMASTERS_CSRF";
    private static final String OAUTH_STATE_SESSION_KEY = "github_oauth_state";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final GitHubOAuthService gitHubOAuthService;
    private final AuthPersistenceService authPersistenceService;
    private final String frontendUrl;
    private final Duration sessionTtl;
    private final boolean secureCookies;
    private final SessionRepository sessionRepository;

    public AuthController(
            GitHubOAuthService gitHubOAuthService,
            AuthPersistenceService authPersistenceService,
            @Value("${app.frontend-url:http://localhost:3000}") String frontendUrl,
            @Value("${app.session-ttl:PT168H}") Duration sessionTtl,
            @Value("${app.cookies.secure:false}") boolean secureCookies,
            SessionRepository sessionRepository) {
        this.gitHubOAuthService = gitHubOAuthService;
        this.authPersistenceService = authPersistenceService;
        this.frontendUrl = frontendUrl;
        this.sessionTtl = sessionTtl;
        this.secureCookies = secureCookies;
        this.sessionRepository = sessionRepository;
    }

    @GetMapping("/auth/github")
    public ResponseEntity<Void> startGitHubLogin(HttpServletRequest request) {
        String state = randomToken();
        request.getSession(true).setAttribute(OAUTH_STATE_SESSION_KEY, state);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(gitHubOAuthService.authorizationUri(state))
                .build();
    }

    @GetMapping("/auth/github/callback")
    public ResponseEntity<Void> gitHubCallback(
            @RequestParam String code,
            @RequestParam String state,
            HttpServletRequest request) {
        HttpSession oauthSession = request.getSession(false);
        String expectedState = oauthSession == null ? null : (String) oauthSession.getAttribute(OAUTH_STATE_SESSION_KEY);
        if (oauthSession != null) {
            oauthSession.removeAttribute(OAUTH_STATE_SESSION_KEY);
        }
        if (expectedState == null || !constantTimeEquals(expectedState, state)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        String accessToken = gitHubOAuthService.exchangeCode(code);
        GitHubOAuthUser githubUser = gitHubOAuthService.fetchUser(accessToken);
        String csrfToken = randomToken();
        AuthPersistenceService.LoginSession login = authPersistenceService.login(
                githubUser, csrfToken, OffsetDateTime.now().plus(sessionTtl));

        if (oauthSession != null) {
            oauthSession.invalidate();
        }

        ResponseCookie sessionCookie = ResponseCookie.from(SESSION_COOKIE, login.sessionId().toString())
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite("Lax")
                .path("/")
                .maxAge(sessionTtl)
                .build();
        ResponseCookie csrfCookie = ResponseCookie.from(CSRF_COOKIE, csrfToken)
                .httpOnly(false)
                .secure(secureCookies)
                .sameSite("Lax")
                .path("/")
                .maxAge(sessionTtl)
                .build();

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.SET_COOKIE, sessionCookie.toString())
                .header(HttpHeaders.SET_COOKIE, csrfCookie.toString())
                .location(URI.create(frontendUrl))
                .build();
    }


    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        sessionCookieValue(request).ifPresent(value -> {
            try {
                sessionRepository.deleteById(UUID.fromString(value));
            } catch (IllegalArgumentException ignored) {
                // An invalid/stale cookie is already logged out from the server's perspective.
            }
        });

        ResponseCookie clearSession = expiredCookie(SESSION_COOKIE, true);
        ResponseCookie clearCsrf = expiredCookie(CSRF_COOKIE, false);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearSession.toString())
                .header(HttpHeaders.SET_COOKIE, clearCsrf.toString())
                .build();
    }

    private java.util.Optional<String> sessionCookieValue(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return java.util.Optional.empty();
        return Arrays.stream(cookies)
                .filter(cookie -> SESSION_COOKIE.equals(cookie.getName()))
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

    private static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        if (actual == null) return false;
        byte[] a = expected.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] b = actual.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return java.security.MessageDigest.isEqual(a, b);
    }
}
