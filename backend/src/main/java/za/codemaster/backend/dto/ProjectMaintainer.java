package za.codemaster.backend.dto;


import java.time.OffsetDateTime;

/**
 * A Code Masters user's management relationship to a project.
 * <p>
 * Matches the {@code ProjectMaintainer} schema in codemasters-api-spec.yaml v2.1.
 */
public record ProjectMaintainer(
        PublicUserProfile user,
        MaintainerRole role,
        OffsetDateTime since
) {
}
