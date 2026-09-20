package za.codemaster.backend.dto.claim;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * How a completed claim was verified. Matches the {@code ClaimCompletionSource}
 * schema in codemasters-api-spec.yaml v3.
 * <p>
 * {@code MAINTAINER_CONFIRMED} is set by API-03.4's review endpoint;
 * {@code GITHUB_VERIFIED} is set by GH-03.2's sync verification. Deliberately
 * two distinct values (design doc §13.3) — not to distrust a maintainer's
 * confirmation, but so impact reporting can tell "the platform independently
 * confirmed this against GitHub" apart from "a human vouched for it."
 * <p>
 * Named {@code ClaimCompletionSourceDto} (not {@code CompletionSource}) to
 * avoid colliding with the JPA entity's own enum,
 * {@link za.codemaster.backend.domain.model.CompletionSource}.
 */
public enum ClaimCompletionSourceDto {

    GITHUB_VERIFIED("github_verified"),
    MAINTAINER_CONFIRMED("maintainer_confirmed");

    private final String wireValue;

    ClaimCompletionSourceDto(String wireValue) {
        this.wireValue = wireValue;
    }

    @JsonValue
    public String getWireValue() {
        return wireValue;
    }
}
