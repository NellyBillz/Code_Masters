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
 * Represents a developer claim on a given {@link Issue}.
 * <p>
 * <b>CRITICAL CONSTRAINT NOTE:</b>
 * Do NOT add {@code @UniqueConstraint(columnNames = {"issue_id", "user_id"})} to this entity!
 * The database enforces a <i>partial unique index</i>:
 * {@code CREATE UNIQUE INDEX uq_claims_active_per_user_issue ON claims (issue_id, user_id) WHERE status = 'active';}
 * <p>
 * Hibernate and standard JPA annotations have no native concept of partial indexes. A plain
 * {@code @UniqueConstraint} would break multi-claimant support and history tracking by preventing
 * a user from having a past 'released' or 'completed' claim alongside a new active claim.
 * Uniqueness of active claims is enforced directly by PostgreSQL and translated to a 409 Conflict
 * by the application/service layer.
 */
@Entity
@Table(name = "claims")
@Getter
@Setter
@NoArgsConstructor
public class Claim {

    /**
     * Primary key identifier for the claim.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The issue claimed by the user.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;

    /**
     * The user claiming the issue.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Lifecycle status of the claim ('active', 'released', 'completed').
     * Stored as the lower-case DB value via {@link ClaimStatusConverter}, not
     * {@code @Enumerated(EnumType.STRING)} — that would store the Java constant
     * name ({@code "ACTIVE"}) instead, which the database check constraint rejects.
     */
    @Column(name = "status", nullable = false)
    private ClaimStatus status = ClaimStatus.ACTIVE;

    /**
     * Optional message or note attached to the claim.
     * Length limit (280 characters) is validated in the service layer per ticket requirements.
     */
    @Column(name = "note")
    private String note;

    @Column(name = "pull_request_url")
    private String pullRequestUrl;

    @Column(name = "pull_request_state", nullable = false)
    private String pullRequestState = "none";

    @Column(name = "maintainer_feedback")
    private String maintainerFeedback;

    @Column(name = "completion_source")
    private String completionSource;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    /**
     * Creation timestamp generated automatically by the database default.
     */
    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Last update timestamp generated automatically by the database default.
     */
    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;
}