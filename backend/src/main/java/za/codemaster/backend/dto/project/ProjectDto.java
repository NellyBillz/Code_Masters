package za.codemaster.backend.dto.project;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * A listed open-source project.
 * <p>
 * Matches the {@code Project} schema in codemasters-api-spec.yaml v3.
 * Named {@code ProjectDto} (not {@code Project}) to avoid colliding with the
 * JPA entity {@link za.codemaster.backend.domain.model.Project} of the same
 * spec name — see {@code ProjectQueryService} for the entity-to-DTO mapping.
 * Note: {@code owner} is the GitHub org/user login (a plain string), not a
 * reference to a Code Masters {@link za.codemaster.backend.dto.user.PublicUserProfile}
 * — a project's GitHub owner and its Code Masters maintainers are two
 * different concepts (see {@link ProjectMaintainerDto}).
 */
public record ProjectDto(
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
        boolean hasContributingGuide,
        boolean hasCodeOfConduct,
        OffsetDateTime lastActivityAt,
        ProjectListingStatus listingStatus,
        boolean acceptingContributions,
        boolean verified,
        OffsetDateTime verifiedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
