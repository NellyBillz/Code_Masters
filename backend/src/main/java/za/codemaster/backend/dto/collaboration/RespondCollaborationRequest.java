package za.codemaster.backend.dto.collaboration;

import jakarta.validation.constraints.NotNull;

/** Request body for {@code POST /issues/{issueId}/claims/{claimId}/collaboration-requests/{requestId}/response}. */
public record RespondCollaborationRequest(
        @NotNull(message = "decision is required")
        CollaborationResponseDecision decision
) {
}
