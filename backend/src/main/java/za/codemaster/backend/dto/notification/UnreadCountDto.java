package za.codemaster.backend.dto.notification;

/** Response shape for {@code GET /users/me/notifications/unread-count}. */
public record UnreadCountDto(long count) {
}
