package za.codemaster.backend.dto.notification;

import za.codemaster.backend.dto.common.PageMeta;

import java.util.List;

/** Response shape for {@code GET /users/me/notifications}. */
public record PagedNotifications(
        List<NotificationDto> items,
        PageMeta meta
) {
}
