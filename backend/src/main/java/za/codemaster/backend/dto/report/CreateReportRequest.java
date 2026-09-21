package za.codemaster.backend.dto.report;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /comments/{commentId}/reports} and
 * {@code POST /projects/{projectId}/reports} (API-03.9).
 * <p>
 * Matches the {@code CreateReportRequest} schema in codemasters-api-spec.yaml v3.
 */
public record CreateReportRequest(
        @NotBlank(message = "reason is required")
        @Size(min = 3, max = 500, message = "reason must be between 3 and 500 characters")
        String reason
) {
}
