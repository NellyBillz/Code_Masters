package za.codemaster.backend.dto;

import java.time.OffsetDateTime;


/**
 * Code Masters discussion attached to a project or an issue.
 * <p>
 * Matches the {@code Comment} schema in codemasters-api-spec.yaml v2.1.
 * Note the schema itself has no {@code projectId}/{@code issueId} field —
 * which comment a {@code Comment} belongs to is determined by which list
 * it's returned in (e.g. {@code ProjectDetail.recentComments} vs
 * {@code IssueDetail.comments}), not by a field on the object itself.
 */
public record Comment(
        Long id,
        PublicUserProfile author,
        String body,
        boolean edited,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
