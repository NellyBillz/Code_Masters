package za.codemaster.backend.dto.stats;

import java.time.OffsetDateTime;

/**
 * Public, non-personal, aggregate platform-impact metrics (API-03.12, design
 * doc §17) — the hackathon brief's own phrase, "measurable impact," made into
 * a real, live number instead of a claim in a pitch deck.
 * <p>
 * Matches the {@code PlatformStats} schema in codemasters-api-spec.yaml v3.
 * {@code totalContributionsCompleted} is the headline figure specifically
 * because it's the one number that can't be inflated by claim volume alone —
 * it only increments when API-03.4's review or GH-03.2's sync verification
 * actually resolves a claim.
 */
public record PlatformStats(
        long publishedProjects,
        long activeProjectsAcceptingContributions,
        long totalContributorsEngaged,
        long totalActiveClaims,
        long totalContributionsCompleted,
        OffsetDateTime generatedAt
) {
}
