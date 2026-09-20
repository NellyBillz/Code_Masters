package za.codemaster.backend.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum ListingStatus {
    PENDING("pending"),
    PUBLISHED("published"),
    REJECTED("rejected");

    private final String value;

    ListingStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ListingStatus fromValue(String value) {
        if (value == null) return null;
        for (ListingStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown listing status: " + value);
    }

    /**
     * Translates Java enum constants to lowercase DB strings automatically.
     */
    @Converter(autoApply = true)
    public static class JpaConverter implements AttributeConverter<ListingStatus, String> {
        @Override
        public String convertToDatabaseColumn(ListingStatus attribute) {
            return attribute != null ? attribute.getValue() : null;
        }

        @Override
        public ListingStatus convertToEntityAttribute(String dbData) {
            return dbData != null ? ListingStatus.fromValue(dbData) : null;
        }
        
    }
}