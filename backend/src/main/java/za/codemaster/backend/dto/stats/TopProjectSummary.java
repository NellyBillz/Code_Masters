package za.codemaster.backend.dto.stats;

/**
 * One row of {@code PlatformStats.topProjects} (API-03.12 extension,
 * 2026-09-24): the platform's most-starred published projects. Deliberately
 * a lean, standalone shape rather than the full {@code ProjectDto} — this
 * endpoint's whole design point is staying cheap regardless of table size,
 * and the impact page only ever needs enough to render a linked row.
 */
public record TopProjectSummary(Long id, String name, String slug, String primaryLanguage, int stars) {
}
