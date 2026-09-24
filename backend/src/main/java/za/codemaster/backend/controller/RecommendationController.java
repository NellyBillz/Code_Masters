package za.codemaster.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.dto.recommendation.RecommendedIssue;
import za.codemaster.backend.security.AuthenticatedUser;
import za.codemaster.backend.service.RecommendationService;

import java.util.List;

/**
 * Skill-Matching Recommendation Engine (wow-feature, 2026-09-24).
 */
@RestController
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    /**
     * {@code GET /api/v1/users/me/recommended-issues}: up to 10 open issues
     * ranked by fit against the caller's own skills, each with the
     * plain-language reasons it matched. Session-authenticated — this is
     * personalized to the caller, there's no public/anonymous version.
     */
    @GetMapping("/api/v1/users/me/recommended-issues")
    public List<RecommendedIssue> getRecommendedIssues(@AuthenticatedUser User currentUser) {
        return recommendationService.getRecommendedIssues(currentUser);
    }
}
