package za.codemaster.backend.dto.claim;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Status of a claim. Values match the {@code ClaimStatus} schema in
 * codemasters-api-spec.yaml v3 ({@code CHANGES_REQUESTED} added there —
 * API-03.4, a maintainer's "request changes" review decision).
 * <p>
 * Named {@code ClaimStatusDto} (not {@code ClaimStatus}) to avoid colliding
 * with the JPA entity's own status enum,
 * {@link za.codemaster.backend.domain.model.ClaimStatus}.
 */
public enum ClaimStatusDto {

    ACTIVE("active"),
    RELEASED("released"),
    COMPLETED("completed"),
    CHANGES_REQUESTED("changes_requested");

    private final String wireValue;

    ClaimStatusDto(String wireValue) {
        this.wireValue = wireValue;
    }

    @JsonValue
    public String getWireValue() {
        return wireValue;
    }
}
