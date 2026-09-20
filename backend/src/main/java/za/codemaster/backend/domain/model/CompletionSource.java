package za.codemaster.backend.domain.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Outcome verification source for completed claims.
 * Corresponds to {@code CHECK (completion_source IS NULL OR completion_source IN ('github_verified', 'maintainer_confirmed'))}.
 */
public enum CompletionSource {
    GITHUB_VERIFIED("github_verified"),
    MAINTAINER_CONFIRMED("maintainer_confirmed");

    private final String value;

    CompletionSource(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static CompletionSource fromValue(String value) {
        for (CompletionSource source : values()) {
            if (source.value.equalsIgnoreCase(value)) {
                return source;
            }
        }
        throw new IllegalArgumentException("Unknown completion source: " + value);
    }

    @Converter(autoApply = true)
    public static class JpaConverter implements AttributeConverter<CompletionSource, String> {
        @Override
        public String convertToDatabaseColumn(CompletionSource attribute) {
            return attribute != null ? attribute.getValue() : null;
        }

        @Override
        public CompletionSource convertToEntityAttribute(String dbData) {
            return dbData != null ? CompletionSource.fromValue(dbData) : null;
        }
    }
}