package za.codemaster.backend.dto.claim;

import za.codemaster.backend.dto.user.PublicUserProfile;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * A lightweight signal that a contributor intends to work on an issue.
 * <p>
 * Matches the {@code Claim} schema in codemasters-api-spec.yaml v2.1.
 * Named {@code ClaimDto} (not {@code Claim}) to avoid colliding with the JPA
 * entity {@link za.codemaster.backend.domain.model.Claim} of the same spec name.
 * Multiple different users may each hold an {@code active} claim on the same
 * issue simultaneously — this is a deliberate product decision (design doc
 * §7), not something to "fix" by treating claims as exclusive.
 * <p>
 * {@code collaborators} is additive: every user with an accepted
 * {@link za.codemaster.backend.domain.model.ClaimCollaborationRequest} on
 * this claim. The claim keeps exactly one owner ({@code user}) — a
 * collaborator never replaces or shares that field, they're credited
 * alongside it once the claim completes.
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
        OffsetDateTime updatedAt,
        List<PublicUserProfile> collaborators
) {
}
