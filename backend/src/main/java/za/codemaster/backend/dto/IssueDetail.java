package za.codemaster.backend.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import java.util.List;

/**
 * Response shape for {@code GET /api/v1/issues/{issueId}}.
 * <p>
 * Matches the {@code IssueDetail} schema in codemasters-api-spec.yaml v2.1:
 * an {@code allOf} of {@link Issue}'s fields (flattened via
 * {@code @JsonUnwrapped}, same technique as {@link ProjectDetail}), plus a
 * single nested {@code project} object, plus {@code comments}/{@code claims}
 * arrays.
 * <p>
 * Unlike {@code comments}/{@code claims} (empty against mock data per
 * API-01.6), {@code project} is populated with the real matching project;
 * we already have it via the issue's {@code projectId}, no reason to fake it.
 */
public record IssueDetail(
        @JsonUnwrapped Issue issue,
        Project project,
        List<Comment> comments,
        List<Claim> claims
) {
}