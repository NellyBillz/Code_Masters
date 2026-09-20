package za.codemaster.backend.dto.search;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The {@code resultType} discriminator on each item in {@code GET /search}'s results.
 * <p>
 * Matches the fixed {@code resultType} values on {@code ProjectSearchResult}/
 * {@code IssueSearchResult} in codemasters-api-spec.yaml v2.1 — the frontend
 * branches its render on this field rather than guessing shape from content.
 */
public enum SearchResultType {

    PROJECT("project"),
    ISSUE("issue");

    private final String wireValue;

    SearchResultType(String wireValue) {
        this.wireValue = wireValue;
    }

    @JsonValue
    public String getWireValue() {
        return wireValue;
    }
}
