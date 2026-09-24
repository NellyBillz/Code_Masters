package za.codemaster.backend.dto.user;

import za.codemaster.backend.dto.claim.ClaimDto;
import za.codemaster.backend.dto.comment.CommentDto;
import za.codemaster.backend.dto.project.ProjectDto;

import java.util.List;

/**
 * One maintained project's activity, as embedded in {@link MaintainerActivitySummary}
 * (API-03.7). Matches the anonymous object schema nested under
 * {@code MaintainerActivitySummary.projects} in codemasters-api-spec.yaml v3.
 */
public record MaintainerProjectActivity(
        ProjectDto project,
        List<ClaimDto> activeClaims,
        List<ClaimDto> claimsAwaitingReview,
        List<CommentDto> recentComments,
        List<CommentDto> unansweredQuestions
) {
}
