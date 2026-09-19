package za.codemaster.backend.dto;

import java.util.List;

/**
 * A user's public-facing profile, as embedded in {@link ProjectMaintainer},
 * {@link Comment}, and {@link Claim}.
 * <p>
 * Matches the {@code PublicUserProfile} schema in codemasters-api-spec.yaml v2.1.
 */
public record PublicUserProfile(
        Long id,
        String username,
        String displayName,
        String avatarUrl,
        String bio,
        String location,
        List<String> skills,
        Integer projectsCount,
        Integer contributionsCount,
        Integer reputation
) {
}
