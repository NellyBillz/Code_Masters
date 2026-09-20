package za.codemaster.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import za.codemaster.backend.controller.AuthController;
import za.codemaster.backend.domain.model.Session;
import za.codemaster.backend.exception.ApiException;
import za.codemaster.backend.service.SessionUserResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The auth resolver from API-02.2 (round2-tickets.md): populates the
 * {@link org.springframework.security.core.context.SecurityContext} with the
 * {@link za.codemaster.backend.domain.model.User} matching the request's
 * {@code CODEMASTERS_SESSION} cookie, and — for state-changing requests — rejects
 * the request before it reaches a controller unless the {@code X-CSRF-Token} header
 * matches that session's stored {@code csrf_token} (design doc §4).
 * <p>
 * A request with no session cookie (or an invalid/expired one) is simply left
 * anonymous here; {@code SecurityConfig}'s {@code authorizeHttpRequests} rule is what
 * turns "anonymous on a non-GET request" into the 401 {@code UNAUTHENTICATED} response,
 * via {@link ApiAuthenticationEntryPoint}.
 */
@Component
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    private static final Set<String> STATE_CHANGING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final String CSRF_HEADER = "X-CSRF-Token";

    private final SessionUserResolver sessionUserResolver;
    private final HandlerExceptionResolver exceptionResolver;

    public SessionAuthenticationFilter(SessionUserResolver sessionUserResolver,
                                        @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
        this.sessionUserResolver = sessionUserResolver;
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Optional<Session> session = sessionUserResolver.resolveSession(sessionCookieValue(request));

        if (session.isPresent()) {
            Session activeSession = session.get();
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(activeSession.getUser(), null, List.of()));

            if (isStateChanging(request) && !csrfHeaderMatches(request, activeSession)) {
                exceptionResolver.resolveException(request, response, null, new ApiException(
                        "CSRF_TOKEN_MISMATCH",
                        "Missing or invalid X-CSRF-Token header.",
                        HttpStatus.FORBIDDEN));
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private static boolean isStateChanging(HttpServletRequest request) {
        return STATE_CHANGING_METHODS.contains(request.getMethod());
    }

    private static boolean csrfHeaderMatches(HttpServletRequest request, Session session) {
        String header = request.getHeader(CSRF_HEADER);
        if (header == null) {
            return false;
        }
        byte[] expected = session.getCsrToken().getBytes(StandardCharsets.UTF_8);
        byte[] actual = header.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }

    private static String sessionCookieValue(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
                .filter(cookie -> AuthController.SESSION_COOKIE.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
