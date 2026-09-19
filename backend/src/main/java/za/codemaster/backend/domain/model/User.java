package za.codemaster.backend.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * Represents a registered developer on the Code Master platform.
 * <p>
 * Mapped to the {@code users} table established in database migration {@code DB-01.1} ({@code V1__init.sql}).
 * Each user is uniquely identified by their GitHub account ID and username, acting as the primary identity
 * referenced across the platform's social and contribution ecosystems (e.g., project maintainers,
 * issue claims, and comments).
 * </p>
 *
 * @author Code Master Team
 * @version 1.0
 * @since DB-01.1
 */

@Entity 
@Table(name="users")
@Getter
@Setter
@Builder
@NoArgsConstructor 
@AllArgsConstructor
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "github_id", nullable = false, unique = true)
    private Long githubId;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "email")
    private String email;

    @Column(name="bio")
    private String bio;

    @Column(name = "location")
    private String location;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "skills", columnDefinition= "text[]")
    private String[] skills;

    @Column(name = "reputation")
    @Builder.Default
    private Integer reputation = 0;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updateAt;
}
