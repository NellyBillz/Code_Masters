package za.codemaster.backend.dto.claim;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Status of a claim. Values match the inline enum on {@code Claim.status}
 * in codemasters-api-spec.yaml v2.1 (spec doesn't name this schema; values
 * are not our choice, the class name is).
 * <p>
 * Named {@code ClaimStatusDto} (not {@code ClaimStatus}) to avoid colliding
 * with the JPA entity's own status enum,
 * {@link za.codemaster.backend.domain.model.ClaimStatus}.
 */
public enum ClaimStatusDto {

    ACTIVE("active"),
    RELEASED("released"),
    COMPLETED("completed");

    private final String wireValue;

    ClaimStatusDto(String wireValue) {
        this.wireValue = wireValue;
    }

    @JsonValue
    public String getWireValue() {
        return wireValue;
    }
}
