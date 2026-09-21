package za.codemaster.backend.dto.report;

import com.fasterxml.jackson.annotation.JsonValue;

/** What a report is filed against. Matches the inline enum on {@code Report.targetType} in codemasters-api-spec.yaml v3. */
public enum ReportTargetType {
    COMMENT("comment"),
    PROJECT("project");

    private final String wireValue;

    ReportTargetType(String wireValue) {
        this.wireValue = wireValue;
    }

    @JsonValue
    public String getWireValue() {
        return wireValue;
    }
}
