package za.codemaster.backend.dto.claim;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code PATCH /issues/{issueId}/claims/{claimId}}.
 * <p>
 * Matches the {@code UpdateClaimRequest} schema in codemasters-api-spec.yaml v3.
 * {@code pullRequestUrl} is the only field, and it's required — this endpoint
 * exists solely to attach/update the claim owner's own pull request link.
 */
public record UpdateClaimRequest(
        @NotBlank(message = "pullRequestUrl must not be blank")
        String pullRequestUrl
) {
}
