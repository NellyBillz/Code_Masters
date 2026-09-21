package za.codemaster.backend.dto.user;

import za.codemaster.backend.dto.claim.ClaimCompletionSourceDto;
import za.codemaster.backend.dto.claim.ClaimStatusDto;
import za.codemaster.backend.dto.issue.IssueDto;
import za.codemaster.backend.dto.project.ProjectDto;

import java.time.OffsetDateTime;

/**
 * A read-only projection of a completed claim, shaped for profile display
 * (API-03.6). Matches the {@code Contribution} schema in codemasters-api-spec.yaml v3.
 * <p>
 * This is specifically the "did this person actually ship something" record
 * (product doc Feature 9) — distinct from a raw claim, which can be
 * {@code active}/{@code changes_requested}/{@code released} and never appears
 * here; {@code GET /users/{username}/contributions} only ever returns
 * {@code completed} ones.
 */
public record Contribution(
        IssueDto issue,
        ProjectDto project,
        ClaimStatusDto status,
        String pullRequestUrl,
        ClaimCompletionSourceDto completionSource,
        OffsetDateTime completedAt
) {
}
