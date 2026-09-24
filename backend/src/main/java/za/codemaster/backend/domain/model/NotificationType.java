package za.codemaster.backend.domain.model;

/**
 * The fixed set of events a notification can represent, matching the DB
 * check constraint: {@code CHECK (type IN ('claim_reviewed',
 * 'collaboration_requested', 'collaboration_responded', 'project_moderated'))}
 * (V14__notifications.sql). Deliberately small — see that migration's
 * comment for why this isn't "notify on everything."
 */
public enum NotificationType {
    CLAIM_REVIEWED("claim_reviewed"),
    COLLABORATION_REQUESTED("collaboration_requested"),
    COLLABORATION_RESPONDED("collaboration_responded"),
    PROJECT_MODERATED("project_moderated");

    private final String value;

    NotificationType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static NotificationType fromValue(String value) {
        for (NotificationType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown notification type: " + value);
    }
}
