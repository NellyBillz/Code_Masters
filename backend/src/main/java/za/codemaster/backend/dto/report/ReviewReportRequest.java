package za.codemaster.backend.dto.report;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code PATCH /admin/reports/{reportId}} (API-03.9).
 * <p>
 * Matches the {@code ReviewReportRequest} schema in codemasters-api-spec.yaml v3.
 * Updates the report only — resolving a report never itself deletes the
 * reported content; an admin who agrees uses the existing
 * {@code DELETE /comments/{commentId}} separately (design doc §15.4).
 */
public record ReviewReportRequest(
        @NotNull(message = "status is required")
        ReportReviewDecision status,

        @Size(max = 1000, message = "resolution must be at most 1000 characters")
        String resolution
) {
}
