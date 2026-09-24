package za.codemaster.backend.dto.leaderboard;

/**
 * One row of the Recognition Leaderboard (wow-feature, 2026-09-24) — used
 * both for {@code GET /leaderboard}'s public ranked list and for
 * {@code GET /users/me/leaderboard-rank}'s personalized result.
 * <p>
 * {@code rank} is {@code null} specifically to mean "this person has zero
 * credited contributions, so they aren't ranked yet" — never a made-up
 * placement like "last." Only {@code GET /users/me/leaderboard-rank} can
 * ever return a null rank; every entry in the public list has a real one.
 */
public record LeaderboardEntry(
        Integer rank,
        Long userId,
        String username,
        String displayName,
        String avatarUrl,
        long contributionCount
) {
}
