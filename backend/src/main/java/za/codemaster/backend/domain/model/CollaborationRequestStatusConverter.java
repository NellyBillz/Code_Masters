package za.codemaster.backend.domain.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts {@link CollaborationRequestStatus} to/from the lowercase strings
 * the {@code claim_collaboration_requests.status} database check constraint
 * requires. Same reasoning as {@link ClaimStatusConverter}: plain
 * {@code @Enumerated(EnumType.STRING)} would store the Java constant name
 * instead of the lowercase wire/DB value.
 */
@Converter(autoApply = true)
public class CollaborationRequestStatusConverter implements AttributeConverter<CollaborationRequestStatus, String> {

    @Override
    public String convertToDatabaseColumn(CollaborationRequestStatus attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public CollaborationRequestStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : CollaborationRequestStatus.fromValue(dbData);
    }
}
