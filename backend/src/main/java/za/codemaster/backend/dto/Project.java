package za.codemaster.backend.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * A listed open-source project.
 * <p>
 * Matches the {@code Project} schema in codemasters-api-spec.yaml v2.1.
 * Note: {@code owner} is the GitHub org/user login (a plain string), not a
 * reference to a Code Masters {@link PublicUserProfile} — a project's GitHub
 * owner and its Code Masters maintainers are two different concepts (see
 * {@link ProjectMaintainer}).
 */
public record Project(
        Long id,
        String name,
        String slug,
        String description,
        String githubUrl,
        String owner,
        String primaryLanguage,
        List<String> languages,
        String category,
        List<String> tags,
        List<String> countryCodes,
        ProjectConnection connection,
        String license,
        Integer stars,
        Integer forks,
        Integer openIssues,
        Integer contributors,
        boolean hasBeginnerFriendlyIssues,
        OffsetDateTime lastActivityAt,
        boolean verified,
        OffsetDateTime verifiedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
