package za.codemaster.backend.dto.project;


import com.fasterxml.jackson.annotation.JsonValue;

/**
 * How a project relates to the African open-source ecosystem.
 * Matches the {@code ProjectConnection} schema in codemasters-api-spec.yaml v2.1.
 */
public enum ProjectConnection {

    SOUTH_AFRICAN("south_african"),
    AFRICA_FOCUSED("africa_focused"),
    AFRICA_LED("africa_led"),
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
