package za.codemaster.backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import za.codemaster.backend.dto.common.ErrorResponse;

import java.time.OffsetDateTime;

/** Development-only guidance for a missing local GitHub OAuth configuration. */
@Profile("dev")
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class DevOAuthConfigurationExceptionHandler {

    @ExceptionHandler(OAuthNotConfiguredException.class)
    public ResponseEntity<ErrorResponse> handleOAuthNotConfigured(
            OAuthNotConfiguredException exception,
            HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                "OAUTH_NOT_CONFIGURED",
                exception.getMessage(),
                OffsetDateTime.now(),
                request.getRequestURI(),
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
