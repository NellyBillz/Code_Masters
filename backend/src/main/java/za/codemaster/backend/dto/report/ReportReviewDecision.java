package za.codemaster.backend.dto.report;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The two decisions a site admin can make on a report (API-03.9) — narrower
 * than {@link ReportStatus}: an admin can move a report to {@code resolved}
 * or {@code dismissed}, never back to {@code open}.
 */
public enum ReportReviewDecision {
    RESOLVED("resolved"),
    DISMISSED("dismissed");

    private final String wireValue;

    ReportReviewDecision(String wireValue) {
        this.wireValue = wireValue;
    }

    @JsonValue
    public String getWireValue() {
        return wireValue;
    }
}
