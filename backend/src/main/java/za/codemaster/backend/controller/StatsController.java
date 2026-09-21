package za.codemaster.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import za.codemaster.backend.dto.stats.PlatformStats;
import za.codemaster.backend.service.StatsService;

/**
 * Public, non-personal impact metrics (API-03.12). Matches the {@code Impact}
 * tag in codemasters-api-spec.yaml v3.
 */
@RestController
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    /**
     * {@code GET /api/v1/stats}: public, unauthenticated, aggregate platform-impact metrics.
     */
    @GetMapping("/api/v1/stats")
    public PlatformStats getStats() {
        return statsService.getStats();
    }
}
