package za.codemaster.backend.dto.claim;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * State of a pull request attached to a claim. Matches the {@code PullRequestState}
 * schema in codemasters-api-spec.yaml v3 — system-maintained from the
 * contributor-provided {@code pullRequestUrl} (API-03.3) and later corrected by
 * GitHub sync (GH-03.2); never directly settable by a client.
 * <p>
 * Named {@code PullRequestStateDto} (not {@code PullRequestState}) to avoid
 * colliding with the JPA entity's own enum,
 * {@link za.codemaster.backend.domain.model.PullRequestState} — same reasoning
 * as {@link ClaimStatusDto}.
 */
public enum PullRequestStateDto {

    NONE("none"),
    OPEN("open"),
    MERGED("merged"),
    CLOSED_UNMERGED("closed_unmerged");

    private final String wireValue;

    PullRequestStateDto(String wireValue) {
        this.wireValue = wireValue;
    }

    @JsonValue
    public String getWireValue() {
        return wireValue;
    }
}
