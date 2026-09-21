package za.codemaster.backend.dto.project;

import java.util.List;

/**
 * Request body for {@code PATCH /projects/{projectId}}.
 * <p>
 * Matches the {@code UpdateProjectRequest} schema in codemasters-api-spec.yaml v3.
 * All fields optional; only provided (non-null) fields are changed. Deliberately
 * excludes GitHub-derived fields like {@code stars} — those are sync's job
 * (GH-02.3), not this endpoint's; see design doc's note on {@code PATCH /projects/{projectId}}.
 * {@code listingStatus} is deliberately absent (API-03.1/03.8) — only a site
 * admin's moderation decision can change it, not even a project's own maintainer.
 */
public record UpdateProjectRequest(
        String category,
        List<String> tags,
        ProjectConnection connection,
        List<String> countryCodes,
        Boolean acceptingContributions
) {
}
