package za.codemaster.backend.repository;

/**
 * Spring Data projection for {@link ClaimRepository#findLeaderboardRows()}
 * (Recognition Leaderboard wow-feature, 2026-09-24) — one ranked row before
 * rank numbers are assigned in {@code LeaderboardService}. Property names
 * are matched to the native query's column aliases automatically.
 */
public interface LeaderboardRow {
    Long getUserId();

    String getUsername();

    String getDisplayName();

    String getAvatarUrl();

    long getContributionCount();
}
