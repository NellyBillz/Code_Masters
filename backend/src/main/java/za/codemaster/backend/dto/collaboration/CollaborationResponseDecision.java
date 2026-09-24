package za.codemaster.backend.dto.collaboration;

import com.fasterxml.jackson.annotation.JsonValue;

/** The two decisions a claim's owner can make on a collaboration request. */
public enum CollaborationResponseDecision {

    ACCEPT("accept"),
    DECLINE("decline");

    private final String wireValue;

    CollaborationResponseDecision(String wireValue) {
        this.wireValue = wireValue;
    }

    @JsonValue
    public String getWireValue() {
        return wireValue;
    }
}
