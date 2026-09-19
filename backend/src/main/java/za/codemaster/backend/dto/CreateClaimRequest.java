package za.codemaster.backend.dto;

import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /issues/{issueId}/claim}.
 * <p>
 * Matches the {@code CreateClaimRequest} schema in codemasters-api-spec.yaml v2.1.
 * The whole body is optional (a bare claim needs no note), and {@code note} itself
 * is optional within it — only its max length is validated.
 */
public record CreateClaimRequest(
        @Size(max = 280, message = "note must be at most 280 characters")
        String note
) {
}
