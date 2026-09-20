package za.codemaster.backend.dto.project;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /admin/projects/{projectId}/moderation}.
 * <p>
 * Matches the {@code ProjectModerationRequest} schema in codemasters-api-spec.yaml v3.
 * {@code reason} is recommended (not required) when rejecting, so the submitter
 * understands why; it isn't persisted anywhere (no column exists for it — the
 * moderation decision only ever sets {@code listingStatus}, per design doc §15.3).
 */
public record ProjectModerationRequest(
        @NotNull(message = "decision is required")
        ModerationDecision decision,

        @Size(max = 1000, message = "reason must be at most 1000 characters")
        String reason
) {
}
