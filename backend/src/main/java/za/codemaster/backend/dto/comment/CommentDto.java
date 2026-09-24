package za.codemaster.backend.dto.comment;

import za.codemaster.backend.dto.user.PublicUserProfile;

import java.time.OffsetDateTime;

/**
 * Code Masters discussion attached to a project or an issue.
 * <p>
 * Matches the {@code Comment} schema in codemasters-api-spec.yaml v2.1.
 * Named {@code CommentDto} (not {@code Comment}) to avoid colliding with the
 * JPA entity {@link za.codemaster.backend.domain.model.Comment} of the same
 * spec name.
 * Note the schema itself has no {@code projectId} field — which project a
 * project comment belongs to is determined by which list it's returned in
 * (e.g. {@code ProjectDetail.recentComments}), not by a field on the object
 * itself. {@code issueId} is the one exception: it's populated (non-null)
 * for issue comments so the maintainer dashboard's unanswered-questions
 * queue (API-03.7) can link straight to the issue without a second lookup;
 * it's left {@code null} for project comments.
 */
public record CommentDto(
        Long id,
        PublicUserProfile author,
        String body,
        boolean edited,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        boolean isQuestion,
        boolean resolved,
        Long issueId
) {
}
