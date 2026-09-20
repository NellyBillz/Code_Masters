package za.codemaster.backend.dto.common;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Standard error response shape returned by every endpoint in this API.
 * <p>
 * Matches the {@code Error} schema in {@code codemasters-api-spec.yaml} v2.1. Every
 * controller in this application should rely on {@link za.codemaster.backend.exception.GlobalExceptionHandler}
 * to produce this shape rather than building it manually.
 *
 * @param code      a stable, machine-readable identifier for the error, e.g. {@code "PROJECT_NOT_FOUND"}
 * @param message   a human-readable description of what went wrong
 * @param timestamp the server time at which the error occurred
 * @param path      the request path that produced the error, e.g. {@code "/api/v1/projects/42"}
 * @param details   optional free-form structured context about the error; may be {@code null}
 */
public record ErrorResponse(
        String code,
        String message,
        OffsetDateTime timestamp,
        String path,
        Map<String, Object> details
) {
}
