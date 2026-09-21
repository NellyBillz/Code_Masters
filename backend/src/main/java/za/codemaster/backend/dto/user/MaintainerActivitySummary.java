package za.codemaster.backend.dto.user;

import java.util.List;

/**
 * Response shape for {@code GET /users/me/maintainer-activity} (API-03.7).
 * <p>
 * Matches the {@code MaintainerActivitySummary} schema in codemasters-api-spec.yaml v3.
 * One rollup instead of checking every maintained project individually — a
 * maintainer-sanity feature (design doc §18). A user maintaining zero projects
 * gets {@code { projects: [] }}, not an error.
 */
public record MaintainerActivitySummary(
        List<MaintainerProjectActivity> projects
) {
}
