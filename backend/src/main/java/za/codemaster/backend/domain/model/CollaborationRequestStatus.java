package za.codemaster.backend.domain.model;

/**
 * Valid states for a claim collaboration request, matching the DB check
 * constraint: {@code CHECK (status IN ('pending', 'accepted', 'declined', 'cancelled'))}
 * (V13__claim_collaboration_requests.sql).
 */
public enum CollaborationRequestStatus {
    PENDING("pending"),
    ACCEPTED("accepted"),
    DECLINED("declined"),
    CANCELLED("cancelled");

    private final String value;

    CollaborationRequestStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static CollaborationRequestStatus fromValue(String value) {
        for (CollaborationRequestStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown collaboration request status: " + value);
    }
}
