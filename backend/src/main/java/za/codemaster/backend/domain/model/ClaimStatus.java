package za.codemaster.backend.domain.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Valid states for an issue claim matching the DB check constraint:
 * {@code CHECK (status IN ('active', 'released', 'completed', 'changes_requested'))}.
 */
public enum ClaimStatus {
    ACTIVE("active"),
    RELEASED("released"),
    COMPLETED("completed"),
    CHANGES_REQUESTED("changes_requested");

    private final String value;

    ClaimStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ClaimStatus fromValue(String value) {
        for (ClaimStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown claim status: " + value);
    }
}