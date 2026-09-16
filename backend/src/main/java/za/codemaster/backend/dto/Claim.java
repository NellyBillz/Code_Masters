package za.codemaster.backend.dto;

import java.time.OffsetDateTime;

/**
 * A lightweight signal that a contributor intends to work on an issue.
 * <p>
 * Matches the {@code Claim} schema in codemasters-api-spec.yaml v2.1.
 * Multiple different users may each hold an {@code active} claim on the same
 * issue simultaneously — this is a deliberate product decision (design doc
 * §7), not something to "fix" by treating claims as exclusive.
 */
public record Claim(
        Long id,
        Long issueId,
        PublicUserProfile user,
        ClaimStatus status,
        String note,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
