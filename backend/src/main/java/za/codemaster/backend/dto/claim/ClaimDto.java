package za.codemaster.backend.dto.claim;

import za.codemaster.backend.dto.user.PublicUserProfile;

import java.time.OffsetDateTime;

/**
 * A lightweight signal that a contributor intends to work on an issue.
 * <p>
 * Matches the {@code Claim} schema in codemasters-api-spec.yaml v2.1.
 * Named {@code ClaimDto} (not {@code Claim}) to avoid colliding with the JPA
 * entity {@link za.codemaster.backend.domain.model.Claim} of the same spec name.
 * Multiple different users may each hold an {@code active} claim on the same
 * issue simultaneously — this is a deliberate product decision (design doc
 * §7), not something to "fix" by treating claims as exclusive.
 */
public record ClaimDto(
        Long id,
        Long issueId,
        PublicUserProfile user,
        ClaimStatusDto status,
        String note,
        String pullRequestUrl,
        PullRequestStateDto pullRequestState,
        String maintainerFeedback,
        ClaimCompletionSourceDto completionSource,
        OffsetDateTime completedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
