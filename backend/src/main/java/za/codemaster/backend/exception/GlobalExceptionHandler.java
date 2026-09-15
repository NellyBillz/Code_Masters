package za.codemaster.backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import za.codemaster.backend.dto.ErrorResponse;


import java.time.OffsetDateTime;

/**
 * Application-wide exception handler. Converts any exception thrown by a controller
 * into the standard {@link ErrorResponse} JSON shape instead of Spring's default
 * whitelabel error page.
 * <p>
 * Controllers should throw {@link ApiException} for any expected/deliberate error
 * case (not found, forbidden, validation failure, etc.). Anything else; an
 * unanticipated runtime exception, is still caught by {@link #handleUnexpected}
 * so the API never leaks a stack trace to the client.
 */

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles any deliberately-thrown {@link ApiException}, translating its
     * code/message/status/details into the standard error response shape.
     *
     * @param exception the exception that was thrown
     * @param request the current request, used to populate the {@code path} field
     * @return an {@link ErrorResponse} with the status carried by {@code ex}
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException exception,
                                                            HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                exception.getCode(),
                exception.getMessage(),
                OffsetDateTime.now(),
                request.getRequestURI(),
                exception.getDetails()
        );

        return ResponseEntity.status(exception.getStatus()).body(body);
    }

    /**
     * Catch-all handler for any exception not explicitly thrown as an {@link ApiException}
     * (e.g. a bug producing a {@code NullPointerException}). Always returns
     * {@code 500 Internal Server Error} with a generic, non-leaking message.
     *
     * @param exception the unexpected exception
     * @param request the current request, used to populate the {@code path} field
     * @return an {@link ErrorResponse} with code {@code "INTERNAL_ERROR"} and status 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                "INTERNAL_ERROR",
                "Something went wrong. Please try again.",
                OffsetDateTime.now(),
                request.getRequestURI(),
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

}
