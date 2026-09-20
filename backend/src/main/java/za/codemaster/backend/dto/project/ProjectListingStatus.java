package za.codemaster.backend.dto.project;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * A project's moderation lifecycle, independent of {@code acceptingContributions}
 * (API-03.8) and {@code verified} (Layer 2). Matches the {@code ProjectListingStatus}
 * schema in codemasters-api-spec.yaml v3.
 * <p>
 * Only a site admin's decision via {@code POST /admin/projects/{projectId}/moderation}
 * (API-03.1) moves a project out of {@code PENDING} — see {@code ProjectModerationService}.
 */
public enum ProjectListingStatus {
    PENDING("pending"),
    PUBLISHED("published"),
    REJECTED("rejected");

    private final String wireValue;

    ProjectListingStatus(String wireValue) {
        this.wireValue = wireValue;
    }

    @JsonValue
    public String getWireValue() {
        return wireValue;
    }
}
