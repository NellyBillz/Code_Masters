package za.codemaster.backend.dto.common;
import java.time.OffsetDateTime;

/**
 * Response body for {@code GET /api/v1/health}.
 * <p>
 * Matches the {@code HealthResponse} schema in {@code codemasters-api-spec.yaml} v2.1.
 *
 * @param status    always {@code "UP"} while the application is running and able to respond
 * @param timestamp the server time at which the health check was evaluated
 */
public record HealthResponse(
        String status,
        OffsetDateTime timestamp
) {
}
