package za.codemaster.backend.domain.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts {@link NotificationType} to/from the lowercase strings the
 * {@code notifications.type} database check constraint requires. Same
 * reasoning as {@link ClaimStatusConverter}.
 */
@Converter(autoApply = true)
public class NotificationTypeConverter implements AttributeConverter<NotificationType, String> {

    @Override
    public String convertToDatabaseColumn(NotificationType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public NotificationType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : NotificationType.fromValue(dbData);
    }
}
