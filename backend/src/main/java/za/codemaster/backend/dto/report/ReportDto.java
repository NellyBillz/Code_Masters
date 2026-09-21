package za.codemaster.backend.dto.report;

import za.codemaster.backend.dto.user.PublicUserProfile;

import java.time.OffsetDateTime;

/**
 * A flag raised by any authenticated user against a comment or a project
 * listing, for a site admin to review (API-03.9, design doc §6.3/§15.4).
 * <p>
 * Matches the {@code Report} schema in codemasters-api-spec.yaml v3. Named
 * {@code ReportDto} (not {@code Report}) to avoid colliding with the JPA
 * entity {@link za.codemaster.backend.domain.model.Report} of the same spec name.
 */
public record ReportDto(
        Long id,
        ReportTargetType targetType,
        Long targetId,
        PublicUserProfile reporter,
        String reason,
        ReportStatus status,
        String resolution,
        OffsetDateTime createdAt,
        OffsetDateTime resolvedAt
) {
}
