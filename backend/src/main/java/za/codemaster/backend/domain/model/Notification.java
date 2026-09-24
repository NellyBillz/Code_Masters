package za.codemaster.backend.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * A single in-app notification for one recipient — a small, fixed set of
 * state-change events (see {@link NotificationType}), not a general
 * activity feed. {@code message} is pre-rendered server-side at creation
 * time, not a structured payload the frontend has to interpret per type.
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "type", nullable = false)
    private NotificationType type;

    @Column(name = "message", nullable = false)
    private String message;

    /** Relative frontend path the notification links to, e.g. {@code /issues/123}. */
    @Column(name = "link")
    private String link;

    @Column(name = "read_at")
    private OffsetDateTime readAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}
