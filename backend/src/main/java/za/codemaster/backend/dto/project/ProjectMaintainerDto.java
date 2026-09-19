package za.codemaster.backend.dto.project;

import za.codemaster.backend.dto.user.PublicUserProfile;

import java.time.OffsetDateTime;

/**
 * A Code Masters user's management relationship to a project.
 * <p>
 * Matches the {@code ProjectMaintainer} schema in codemasters-api-spec.yaml v2.1.
 * Named {@code ProjectMaintainerDto} (not {@code ProjectMaintainer}) to avoid
 * colliding with the JPA entity
 * {@link za.codemaster.backend.domain.model.ProjectMaintainer} of the same
 * spec name.
 */
public record ProjectMaintainerDto(
        PublicUserProfile user,
        MaintainerRole role,
        OffsetDateTime since
) {
}
