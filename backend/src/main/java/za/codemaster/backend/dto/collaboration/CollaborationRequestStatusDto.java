package za.codemaster.backend.dto.collaboration;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Status of a claim collaboration request. Named {@code CollaborationRequestStatusDto}
 * (not {@code CollaborationRequestStatus}) to avoid colliding with the JPA
 * entity's own enum, {@link za.codemaster.backend.domain.model.CollaborationRequestStatus} —
 * same reasoning as {@code ClaimStatusDto}.
 */
public enum CollaborationRequestStatusDto {

    PENDING("pending"),
    ACCEPTED("accepted"),
    DECLINED("declined"),
    CANCELLED("cancelled");

    private final String wireValue;

    CollaborationRequestStatusDto(String wireValue) {
        this.wireValue = wireValue;
    }

    @JsonValue
    public String getWireValue() {
        return wireValue;
    }
}
