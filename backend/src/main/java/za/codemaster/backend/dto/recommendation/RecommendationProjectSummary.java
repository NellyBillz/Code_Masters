package za.codemaster.backend.dto.recommendation;

/**
 * The project half of a {@link RecommendedIssue} — a lean, standalone shape
 * (not the full {@code ProjectDto}) since the "Recommended for you" card
 * only ever needs enough to render a linked project name (wow-feature,
 * 2026-09-24).
 */
public record RecommendationProjectSummary(Long id, String name, String slug, String primaryLanguage) {
}
