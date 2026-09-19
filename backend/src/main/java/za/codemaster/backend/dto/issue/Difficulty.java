package za.codemaster.backend.dto.issue;

import com.fasterxml.jackson.annotation.JsonValue;
import za.codemaster.backend.dto.common.WireValued;

/**
 * Matches the {@code Difficulty} schema in codemasters-api-spec.yaml v2.1.
 * Implements {@link WireValued} so {@code @RequestParam Difficulty difficulty}
 * binds the spec's lowercase wire value ({@code ?difficulty=beginner}), not just
 * the Java constant name — see {@code WireValueMatcher}/{@code WebMvcConfig}.
 */
public enum Difficulty implements WireValued {

    BEGINNER("beginner"),
    INTERMEDIATE("intermediate"),
    ADVANCED("advanced"),
    UNKNOWN("unknown");

    private final String wireValue;

    Difficulty(String wireValue) {
        this.wireValue = wireValue;
    }

    @Override
    @JsonValue
    public String getWireValue() {
        return wireValue;
    }
}
