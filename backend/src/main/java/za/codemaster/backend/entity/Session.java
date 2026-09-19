package za.codemaster.backend.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "sessions")
public class Session {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "csrf_token", nullable = false)
    private String csrfToken;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "last_seen_at")
    private OffsetDateTime lastSeenAt;

    protected Session() {}

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public String getCsrfToken() { return csrfToken; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public OffsetDateTime getLastSeenAt() { return lastSeenAt; }
}
