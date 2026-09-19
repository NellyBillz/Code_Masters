package za.codemaster.backend.config;

import za.codemaster.backend.dto.common.WireValued;

/**
 * Matches a raw query-param string against a {@link WireValued} enum's wire
 * values, case-insensitively — the fix for the gap described on {@link WireValued}.
 * Used by {@code WebMvcConfig} to register one {@code Converter<String, T>} per
 * affected enum, rather than relying on Spring's default (constant-name-only)
 * enum conversion.
 */
final class WireValueMatcher {

    private WireValueMatcher() {
    }

    static <T extends Enum<T> & WireValued> T match(Class<T> type, String source) {
        String trimmed = source.trim();
        for (T constant : type.getEnumConstants()) {
            if (constant.getWireValue().equalsIgnoreCase(trimmed)) {
                return constant;
            }
        }
        throw new IllegalArgumentException(
                "No " + type.getSimpleName() + " matches wire value '" + source + "'");
    }
}
