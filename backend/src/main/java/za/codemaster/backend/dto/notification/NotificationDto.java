package za.codemaster.backend.dto.notification;

import java.time.OffsetDateTime;

public record NotificationDto(
        Long id,
        String type,
        String message,
        String link,
        boolean read,
        OffsetDateTime createdAt
) {
}
