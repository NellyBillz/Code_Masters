package za.codemaster.backend.dto.collaboration;

import za.codemaster.backend.dto.user.PublicUserProfile;

import java.time.OffsetDateTime;

/**
 * A request from a contributor to join another user's claim as a
 * collaborator. Matches the {@code ClaimCollaborationRequest} schema.
 */
public record ClaimCollaborationRequestDto(
        Long id,
        Long claimId,
        Long issueId,
        PublicUserProfile requester,
        CollaborationRequestStatusDto status,
        OffsetDateTime createdAt,
        OffsetDateTime respondedAt
) {
}
