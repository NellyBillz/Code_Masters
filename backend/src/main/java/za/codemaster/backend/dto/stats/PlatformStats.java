package za.codemaster.backend.dto.stats;

import java.time.OffsetDateTime;
import java.util.List;

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
 * <p>
 * {@code totalStars}, {@code languageBreakdown}, and {@code topProjects}
 * (2026-09-24, Impact Dashboard wow-feature) exist to answer the open-source
 * agenda's own problem statement — "local solutions struggle to gain
 * visibility" — directly on this page, not just via the four headline
 * counts. Each is still a single-table query against {@code projects},
 * keeping this endpoint's original "stays cheap regardless of table size"
 * design intact.
 */
public record PlatformStats(
        long publishedProjects,
        long activeProjectsAcceptingContributions,
        long totalContributorsEngaged,
        long totalActiveClaims,
        long totalContributionsCompleted,
        long totalStars,
        List<LanguageBreakdownEntry> languageBreakdown,
        List<TopProjectSummary> topProjects,
        OffsetDateTime generatedAt
) {
}
