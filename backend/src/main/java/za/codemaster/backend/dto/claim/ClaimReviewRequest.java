package za.codemaster.backend.dto.claim;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /issues/{issueId}/claims/{claimId}/review}.
 * <p>
 * Matches the {@code ClaimReviewRequest} schema in codemasters-api-spec.yaml v3.
 * {@code feedback} being required specifically when {@code decision} is
 * {@code request_changes} is a cross-field rule the spec deliberately leaves
 * to the service layer to enforce (not a bean-validation annotation here) —
 * see {@code ClaimService.reviewClaim}.
 */
public record ClaimReviewRequest(
        @NotNull(message = "decision is required")
        ClaimReviewDecision decision,

        @Size(max = 2000, message = "feedback must be at most 2000 characters")
        String feedback
) {
}
