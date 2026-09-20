package za.codemaster.backend.domain.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * State of a pull request attached to a claim.
 * Corresponds to {@code CHECK (pull_request_state IN ('none', 'open', 'merged', 'closed_unmerged'))}.
 */
public enum PullRequestState {
    NONE("none"),
    OPEN("open"),
    MERGED("merged"),
    CLOSED_UNMERGED("closed_unmerged");

    private final String value;

    PullRequestState(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static PullRequestState fromValue(String value) {
        for (PullRequestState state : values()) {
            if (state.value.equalsIgnoreCase(value)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown pull request state: " + value);
    }

    @Converter(autoApply = true)
    public static class JpaConverter implements AttributeConverter<PullRequestState, String> {
        @Override
        public String convertToDatabaseColumn(PullRequestState attribute) {
            return attribute != null ? attribute.getValue() : null;
        }

        @Override
        public PullRequestState convertToEntityAttribute(String dbData) {
            return dbData != null ? PullRequestState.fromValue(dbData) : null;
        }
    }
}