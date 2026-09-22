package za.codemaster.backend.dto.issue;

/**
 * Response shape for {@code GET /api/v1/issues/{issueId}/contribution-context}
 * (API-04.1). Evidence-based contribution context: plain facts and counts a
 * contributor can weigh themselves — deliberately no combined score or
 * ranking, so nobody has to defend a weighting formula to a judge. Rendered
 * verbatim by {@code FE-04.6}.
 */
public record IssueContributionContext(
        ProjectContributionContext project,
        IssueContributionContextIssue issue
) {
}
