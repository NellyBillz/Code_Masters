package za.codemaster.backend.dto.project;

import com.fasterxml.jackson.annotation.JsonValue;

/** The two decisions a site admin can make on a pending project submission (API-03.1). */
public enum ModerationDecision {
    APPROVE("approve"),
    REJECT("reject");

    private final String wireValue;

    ModerationDecision(String wireValue) {
        this.wireValue = wireValue;
    }

    @JsonValue
    public String getWireValue() {
        return wireValue;
    }
}
