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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Entity representing a maintainer relationship between a {@link User} and a {@link Project}.
 * <p>
 * Enforces composite uniqueness on {@code (project_id, user_id)} at both the JPA metadata level
 * and the database constraint layer. Repositories and service layers should check for existing pairs
 * to reject duplicate invitations cleanly before relying on raw database constraint violations.
 */
@Entity
@Table(
    name = "project_maintainers",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_project_maintainers_project_user",
            columnNames = {"project_id", "user_id"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
public class ProjectMaintainer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "role", nullable = false)
    private String role;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}