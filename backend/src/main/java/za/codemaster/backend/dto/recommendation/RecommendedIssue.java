package za.codemaster.backend.dto.recommendation;

import za.codemaster.backend.dto.issue.IssueDto;

import java.util.List;

/**
 * One entry of {@code GET /users/me/recommended-issues} (Skill-Matching
 * Recommendation Engine wow-feature, 2026-09-24). {@code reasons} is the
 * deliberate design choice this feature is built around: every recommended
 * issue carries at least one plain-language, traceable reason it was
 * suggested — no combined weighted score is ever returned, so there's no
 * black-box number to defend. See {@code RecommendationService} for the
 * scoring (used only to rank/select candidates server-side, never exposed).
 */
public record RecommendedIssue(IssueDto issue, RecommendationProjectSummary project, List<String> reasons) {
}
