package za.codemaster.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.codemaster.backend.domain.model.Notification;
import za.codemaster.backend.domain.model.NotificationType;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.dto.common.PageMeta;
import za.codemaster.backend.dto.notification.NotificationDto;
import za.codemaster.backend.dto.notification.PagedNotifications;
import za.codemaster.backend.exception.ApiException;
import za.codemaster.backend.repository.NotificationRepository;

import java.time.OffsetDateTime;

/**
 * In-app notifications for a small, fixed set of state-change events — see
 * {@code V14__notifications.sql}'s comment for why this isn't a general
 * activity feed. {@link #notify} is called directly by the handful of
 * services that produce these events ({@code ClaimService},
 * {@code ClaimCollaborationService}, {@code ProjectModerationService}), not
 * exposed as its own write endpoint — a notification is always a side
 * effect of a real action, never created directly by a client.
 */
@Service
public class NotificationService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Creates a notification for {@code recipient}. Called directly by
     * other services as a side effect of their own action — never rolled
     * into its own public write endpoint.
     */
    @Transactional
    public void notify(User recipient, NotificationType type, String message, String link) {
        Notification notification = new Notification();
        notification.setUser(recipient);
        notification.setType(type);
        notification.setMessage(message);
        notification.setLink(link);
        notificationRepository.save(notification);
    }

    /** {@code GET /users/me/notifications}: the caller's own notifications, most recent first. */
    @Transactional(readOnly = true)
    public PagedNotifications list(User caller, Integer page, Integer size) {
        int resolvedPage = clampPage(page);
        int resolvedSize = clampSize(size);

        Page<Notification> result = notificationRepository.findByUserIdOrderByCreatedAtDesc(
                caller.getId(), PageRequest.of(resolvedPage, resolvedSize));

        var items = result.getContent().stream().map(this::toDto).toList();
        return new PagedNotifications(items, new PageMeta(resolvedPage, resolvedSize, (int) result.getTotalElements()));
    }

    /** {@code GET /users/me/notifications/unread-count}: backs the bell icon's badge. */
    @Transactional(readOnly = true)
    public long unreadCount(User caller) {
        return notificationRepository.countByUserIdAndReadAtIsNull(caller.getId());
    }

    /**
     * {@code POST /users/me/notifications/{id}/read}: marks one of the
     * caller's own notifications read. Idempotent — marking an
     * already-read notification read again is a no-op, not an error.
     *
     * @throws ApiException with code {@code NOTIFICATION_NOT_FOUND} (404) if no notification
     *                       with that id exists for the caller
     */
    @Transactional
    public NotificationDto markRead(Long notificationId, User caller) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, caller.getId())
                .orElseThrow(() -> new ApiException(
                        "NOTIFICATION_NOT_FOUND", "No notification exists with id " + notificationId,
                        HttpStatus.NOT_FOUND));

        if (notification.getReadAt() == null) {
            notification.setReadAt(OffsetDateTime.now());
            notification = notificationRepository.save(notification);
        }
        return toDto(notification);
    }

    /** {@code POST /users/me/notifications/read-all}: marks every one of the caller's unread notifications read. */
    @Transactional
    public void markAllRead(User caller) {
        notificationRepository.markAllRead(caller.getId(), OffsetDateTime.now());
    }

    private NotificationDto toDto(Notification entity) {
        return new NotificationDto(
                entity.getId(),
                entity.getType().getValue(),
                entity.getMessage(),
                entity.getLink(),
                entity.getReadAt() != null,
                entity.getCreatedAt()
        );
    }

    private int clampSize(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        return Math.max(1, Math.min(size, MAX_SIZE));
    }

    private int clampPage(Integer page) {
        if (page == null || page < 0) {
            return DEFAULT_PAGE;
        }
        return page;
    }
}
