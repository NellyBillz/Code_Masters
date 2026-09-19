package za.codemaster.backend.dto.issue;


import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Status of an issue. Values match the inline enum on {@code Issue.status}
 * in codemasters-api-spec.yaml v2.1.
 */
public enum IssueStatus {
    OPEN("open"),
    CLOSED("closed"),
    CLAIMED("claimed");

    private final String wireValue;

    IssueStatus(String wireValue) {
        this.wireValue = wireValue;
    }

    @JsonValue
    public String getWireValue() {
        return wireValue;
    }
}
