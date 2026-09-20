package za.codemaster.backend.dto.issue;


import com.fasterxml.jackson.annotation.JsonValue;
import za.codemaster.backend.dto.common.WireValued;

/**
 * Status of an issue. Values match the inline enum on {@code Issue.status}
 * in codemasters-api-spec.yaml v2.1.
 * Implements {@link WireValued} so {@code @RequestParam IssueStatus status}
 * binds the spec's lowercase wire value ({@code ?status=open}), not just
 * the Java constant name — see {@code WireValueMatcher}/{@code WebMvcConfig}.
 */
public enum IssueStatus implements WireValued {
    OPEN("open"),
    CLOSED("closed"),
    CLAIMED("claimed");

    private final String wireValue;

    IssueStatus(String wireValue) {
        this.wireValue = wireValue;
    }

    @Override
    @JsonValue
    public String getWireValue() {
        return wireValue;
    }
}
