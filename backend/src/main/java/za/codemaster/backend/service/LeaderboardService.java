package za.codemaster.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.dto.leaderboard.LeaderboardEntry;
import za.codemaster.backend.repository.ClaimRepository;
import za.codemaster.backend.repository.LeaderboardRow;

import java.util.ArrayList;
import java.util.List;

/**
 * Recognition Leaderboard (wow-feature, 2026-09-24) — a public "Top SA Open
 * Source Contributors" ranking, plus a personalized "what's my rank" lookup
 * for the signed-in caller, matching the same public-list-plus-personalized-
 * endpoint split already used by the Skill-Matching Recommendation Engine.
 */
@Service
public class LeaderboardService {

    private static final int PUBLIC_LIST_LIMIT = 20;

    private final ClaimRepository claimRepository;

    public LeaderboardService(ClaimRepository claimRepository) {
        this.claimRepository = claimRepository;
    }

    /** @return the top 20 credited contributors, ranked, most contributions first. */
    @Transactional(readOnly = true)
    public List<LeaderboardEntry> getLeaderboard() {
        List<LeaderboardEntry> ranked = rank(claimRepository.findLeaderboardRows());
        return ranked.size() > PUBLIC_LIST_LIMIT ? ranked.subList(0, PUBLIC_LIST_LIMIT) : ranked;
    }

    /**
     * @param caller the authenticated caller, injected via {@code @AuthenticatedUser}
     * @return the caller's own rank and contribution count, even if they're
     *         well outside the public top 20 — {@code rank} is {@code null}
     *         if they have zero credited contributions (not yet ranked at all)
     */
    @Transactional(readOnly = true)
    public LeaderboardEntry getCallerRank(User caller) {
        return rank(claimRepository.findLeaderboardRows()).stream()
                .filter(entry -> entry.userId().equals(caller.getId()))
                .findFirst()
                .orElseGet(() -> new LeaderboardEntry(
                        null, caller.getId(), caller.getUsername(), caller.getDisplayName(),
                        caller.getAvatarUrl(), 0));
    }

    /** Assigns sequential rank numbers 1..N to already-sorted rows — see repository query's tie-break note. */
    private List<LeaderboardEntry> rank(List<LeaderboardRow> rows) {
        List<LeaderboardEntry> ranked = new ArrayList<>(rows.size());
        int rank = 1;
        for (LeaderboardRow row : rows) {
            ranked.add(new LeaderboardEntry(
                    rank, row.getUserId(), row.getUsername(), row.getDisplayName(),
                    row.getAvatarUrl(), row.getContributionCount()));
            rank++;
        }
        return ranked;
    }
}
