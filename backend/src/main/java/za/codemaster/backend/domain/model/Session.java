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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Represents an authenticated user session in the Code Master platform.
 * <p>
 * Mapped to the {@code sessions} table established in database migration {@code DB-01.1} ({@code V1__init.sql}).
 * Each session records CSRF validation tokens, expiration timestamps, and session activity for
 * stateful or token-backed security workflows, enforcing a cascading foreign key relationship
 * to the owning {@link User}.
 * </p>
 *
 * @author Code Master Team
 * @version 1.0
 * @since DB-01.1
 */

@Entity
@Table(name = "sessions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Session {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "csrf_token", nullable = false)
    private String csrToken;

    @Column(name = "created_at", insertable = false,updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "last_seen_at", insertable = false)
    private OffsetDateTime lastSeenAt;
}
