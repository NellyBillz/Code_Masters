package za.codemaster.backend.dto;

import com.fasterxml.jackson.annotation.JsonValue;

/** Matches the {@code Difficulty} schema in codemasters-api-spec.yaml v2.1. */
public enum Difficulty {

    BEGINNER("beginner"),
    INTERMEDIATE("intermediate"),
    ADVANCED("advanced"),
    UNKNOWN("unknown");

    private final String wireValue;

    Difficulty(String wireValue) {
        this.wireValue = wireValue;
    }

    @JsonValue
    public String getWireValue() {
        return wireValue;
    }
}
