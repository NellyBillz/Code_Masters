package za.codemaster.backend.dto.issue;

/**
 * The issue-level half of {@link IssueContributionContext} (API-04.1).
 * Plain facts and counts only — no score, no weighting. {@code inFlightClaimCount}
 * uses the exact same definition as {@link IssueDto#claimCount()}: status
 * {@code active} or {@code changes_requested}.
 */
public record IssueContributionContextIssue(
        int ageInDays,
        int labelCount,
        boolean isBeginnerFriendly,
        int inFlightClaimCount
) {
}
