package za.codemaster.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.dto.notification.NotificationDto;
import za.codemaster.backend.dto.notification.PagedNotifications;
import za.codemaster.backend.dto.notification.UnreadCountDto;
import za.codemaster.backend.security.AuthenticatedUser;
import za.codemaster.backend.service.NotificationService;

/** The caller's own in-app notifications — see {@link NotificationService}'s javadoc. */
@RestController
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/api/v1/users/me/notifications")
    public PagedNotifications list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @AuthenticatedUser User currentUser) {
        return notificationService.list(currentUser, page, size);
    }

    @GetMapping("/api/v1/users/me/notifications/unread-count")
    public UnreadCountDto unreadCount(@AuthenticatedUser User currentUser) {
        return new UnreadCountDto(notificationService.unreadCount(currentUser));
    }

    @PostMapping("/api/v1/users/me/notifications/{notificationId}/read")
    public NotificationDto markRead(
            @PathVariable Long notificationId,
            @AuthenticatedUser User currentUser) {
        return notificationService.markRead(notificationId, currentUser);
    }

    @PostMapping("/api/v1/users/me/notifications/read-all")
    public void markAllRead(@AuthenticatedUser User currentUser) {
        notificationService.markAllRead(currentUser);
    }
}
