package za.codemaster.backend.dto.stats;

/**
 * One row of {@code PlatformStats.languageBreakdown} (API-03.12 extension,
 * 2026-09-24): how many published projects report {@code primaryLanguage} as
 * this value. Projects with no primary language recorded are excluded, not
 * folded into an "unknown" row — an absent signal isn't a language.
 */
public record LanguageBreakdownEntry(String language, long projectCount) {
}
