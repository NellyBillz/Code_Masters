package za.codemaster.backend.dto;

import com.fasterxml.jackson.annotation.JsonValue;

/** Matches the {@code MaintainerRole} schema in codemasters-api-spec.yaml v2.1. */
public enum MaintainerRole {
    OWNER("owner"),
    MAINTAINER("maintainer");

    private final String wireValue;

    MaintainerRole(String wireValue) {
        this.wireValue = wireValue;
    }

    @JsonValue
    public String getWireValue() {
        return wireValue;
    }
}
