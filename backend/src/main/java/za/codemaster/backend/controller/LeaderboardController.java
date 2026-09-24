package za.codemaster.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.dto.leaderboard.LeaderboardEntry;
import za.codemaster.backend.security.AuthenticatedUser;
import za.codemaster.backend.service.LeaderboardService;

import java.util.List;

/**
 * Recognition Leaderboard (wow-feature, 2026-09-24).
 */
@RestController
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    /** {@code GET /api/v1/leaderboard}: public, unauthenticated — the top 20 credited contributors, ranked. */
    @GetMapping("/api/v1/leaderboard")
    public List<LeaderboardEntry> getLeaderboard() {
        return leaderboardService.getLeaderboard();
    }

    /**
     * {@code GET /api/v1/users/me/leaderboard-rank}: session-authenticated —
     * the caller's own rank, even if outside the public top 20.
     */
    @GetMapping("/api/v1/users/me/leaderboard-rank")
    public LeaderboardEntry getMyLeaderboardRank(@AuthenticatedUser User currentUser) {
        return leaderboardService.getCallerRank(currentUser);
    }
}
