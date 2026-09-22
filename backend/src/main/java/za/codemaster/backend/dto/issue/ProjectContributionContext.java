package za.codemaster.backend.dto.issue;

/**
 * The project-level half of {@link IssueContributionContext} (API-04.1).
 * Plain facts and counts only — no score, no weighting.
 */
public record ProjectContributionContext(
        boolean hasContributingGuide,
        boolean hasCodeOfConduct,
        Integer daysSinceLastActivity,
        long completedContributionsCount
) {
}
