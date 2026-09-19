package za.codemaster.backend.dto;

/**
 * Request body for {@code PATCH /issues/{issueId}}.
 * <p>
 * Matches the {@code UpdateIssueRequest} schema in codemasters-api-spec.yaml v2.1.
 * All fields optional; only provided (non-null) fields are changed — see
 * {@code IssueService.updateClassification} for how a partial update is applied.
 */
public record UpdateIssueRequest(
        Difficulty difficulty,
        Boolean isBeginnerFriendly
) {
}
