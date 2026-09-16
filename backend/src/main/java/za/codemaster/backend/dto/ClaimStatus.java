package za.codemaster.backend.dto;


import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Status of a claim. Values match the inline enum on {@code Claim.status}
 * in codemasters-api-spec.yaml v2.1 (spec doesn't name this schema; values
 * are not our choice, the class name is).
 */
public enum ClaimStatus {

    ACTIVE("active"),
    RELEASED("released"),
    COMPLETED("completed");

    private final String wireValue;

    ClaimStatus(String wireValue) {
        this.wireValue = wireValue;
    }

    @JsonValue
    public String getWireValue() {
        return wireValue;
    }
}
