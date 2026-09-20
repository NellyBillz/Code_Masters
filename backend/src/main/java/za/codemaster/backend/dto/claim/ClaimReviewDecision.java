package za.codemaster.backend.dto.claim;

import com.fasterxml.jackson.annotation.JsonValue;

/** The two decisions a maintainer can make on a claim's pull request (API-03.4). */
public enum ClaimReviewDecision {

    REQUEST_CHANGES("request_changes"),
    CONFIRM_COMPLETED("confirm_completed");

    private final String wireValue;

    ClaimReviewDecision(String wireValue) {
        this.wireValue = wireValue;
    }

    @JsonValue
    public String getWireValue() {
        return wireValue;
    }
}
