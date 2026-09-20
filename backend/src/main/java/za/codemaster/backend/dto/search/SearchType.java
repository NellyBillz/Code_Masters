package za.codemaster.backend.dto.search;

import za.codemaster.backend.dto.common.WireValued;

/**
 * The {@code type} query parameter on {@code GET /search} — narrows results to
 * one resource type, or {@link #ALL} (the spec's default). Request-binding only;
 * never serialized in a response (unlike {@link SearchResultType}, which is).
 * Implements {@link WireValued} so {@code @RequestParam SearchType type} binds
 * the spec's lowercase values ({@code ?type=projects}) — see
 * {@code WireValueMatcher}/{@code WebMvcConfig}.
 */
public enum SearchType implements WireValued {
    ALL("all"),
    PROJECTS("projects"),
    ISSUES("issues");

    private final String wireValue;

    SearchType(String wireValue) {
        this.wireValue = wireValue;
    }

    @Override
    public String getWireValue() {
        return wireValue;
    }
}
