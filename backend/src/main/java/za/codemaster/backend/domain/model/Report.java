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
 * Entity representing an abuse report submitted by an authenticated user.
 * <p>
 * Backed by the {@code reports} table created in {@code V10__reports.sql}. Polymorphic target
 * resolution across comments and projects is handled at the application/service layer, matching
 * the XOR convention established for comments in DB-02.3.
 * </p>
 */
@Entity
@Table(
    name = "reports",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_reports_reporter_target",
            columnNames = {"reporter_user_id", "target_type", "target_id"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
public class Report {

    /**
     * Primary key identifier for the report.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The reporting user submitting the complaint.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_user_id", nullable = false)
    private User reporterUser;

    /**
     * Target polymorphic classification ('comment' or 'project').
     * Enforced by database check constraint {@code chk_reports_target_type}.
     */
    @Column(name = "target_type", nullable = false)
    private String targetType;

    /**
     * Identifier of the polymorphic target entity (comment ID or project ID).
     * Validated at the service layer rather than with a database FK.
     */
    @Column(name = "target_id", nullable = false)
    private Long targetId;

    /**
     * Detailed explanation or reason provided by the reporter.
     */
    @Column(name = "reason", nullable = false)
    private String reason;

    /**
     * Moderation lifecycle status ('open', 'resolved', 'dismissed').
     * Enforced by database check constraint {@code chk_reports_status}.
     */
    @Column(name = "status", nullable = false)
    private String status = "open";

    /**
     * Notes or rationale recorded by an administrator upon resolution or dismissal.
     */
    @Column(name = "resolution")
    private String resolution;

    /**
     * Timestamp when the report was submitted. Managed by database default.
     */
    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Timestamp when the report was resolved or dismissed by an administrator.
     */
    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;
}