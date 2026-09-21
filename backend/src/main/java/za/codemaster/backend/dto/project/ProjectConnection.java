package za.codemaster.backend.dto.project;


import com.fasterxml.jackson.annotation.JsonValue;

/**
 * How a project relates to the South African open-source ecosystem.
 * Matches the {@code ProjectConnection} schema in codemasters-api-spec.yaml.
 *
 * <p>{@code COMMUNITY_VERIFIED} means "verified as South African" — it isn't
 * a separate geography, it's the fallback for a project whose South African
 * connection can't be proven automatically from GitHub metadata alone but has
 * been confirmed by the platform's own community/curators.
 */
public enum ProjectConnection {

    SOUTH_AFRICAN("south_african"),
    COMMUNITY_VERIFIED("community_verified");

    private final String wireValue;

    ProjectConnection(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * The exact lowercase string this enum serializes to in JSON,
     * matching the spec's enum values (not Java's UPPER_CASE convention).
     */
    @JsonValue
    public String getWireValue() {
        return wireValue;
    }
}
