package za.codemaster.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import za.codemaster.backend.exception.ApiException;

/**
 * Turns "no/invalid session on a request that requires one" into the standard
 * {@code ErrorResponse} JSON shape (401 {@code UNAUTHENTICATED}) instead of Spring
 * Security's default behavior, per API-02.2 (round2-tickets.md).
 */
@Component
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final HandlerExceptionResolver exceptionResolver;

    public ApiAuthenticationEntryPoint(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) {
        exceptionResolver.resolveException(request, response, null, new ApiException(
                "UNAUTHENTICATED",
                "Authentication is required.",
                HttpStatus.UNAUTHORIZED));
    }
}
