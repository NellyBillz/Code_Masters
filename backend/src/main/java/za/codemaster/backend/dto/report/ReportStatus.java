package za.codemaster.backend.dto.report;

import com.fasterxml.jackson.annotation.JsonValue;
import za.codemaster.backend.dto.common.WireValued;

/**
 * A report's moderation lifecycle. Matches the inline enum on {@code Report.status}
 * in codemasters-api-spec.yaml v3. Implements {@link WireValued} so
 * {@code @RequestParam ReportStatus status} on {@code GET /admin/reports} binds
 * the spec's lowercase wire value ({@code ?status=open}), not just the Java
 * constant name.
 */
public enum ReportStatus implements WireValued {
    OPEN("open"),
    RESOLVED("resolved"),
    DISMISSED("dismissed");

    private final String wireValue;

    ReportStatus(String wireValue) {
        this.wireValue = wireValue;
    }

    @Override
    @JsonValue
    public String getWireValue() {
        return wireValue;
    }
}
