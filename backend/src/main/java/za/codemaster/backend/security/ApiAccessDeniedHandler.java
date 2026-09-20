package za.codemaster.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import za.codemaster.backend.exception.ApiException;

/**
 * Fallback for the case where Spring Security itself denies a request (not currently
 * reachable via the "authenticated" rule alone, but kept so a future authority-based
 * rule never falls through to Spring's default whitelabel/HTML 403 page — every error
 * response from this API uses the standard {@code ErrorResponse} JSON shape).
 */
@Component
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

    private final HandlerExceptionResolver exceptionResolver;

    public ApiAccessDeniedHandler(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) {
        exceptionResolver.resolveException(request, response, null, new ApiException(
                "FORBIDDEN",
                "You do not have permission to perform this action.",
                HttpStatus.FORBIDDEN));
    }
}
