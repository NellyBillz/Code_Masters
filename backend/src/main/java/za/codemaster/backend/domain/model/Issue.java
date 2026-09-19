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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Entity representing an issue imported or synced from a GitHub repository belonging to a {@link Project}.
 * <p>
 * Contains composite uniqueness on {@code (project_id, github_issue_number)} to facilitate synchronization
 * upsert workflows (GH-2.3) and includes algorithmic scoring metrics.
 */
@Entity
@Table(
    name = "issues",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_issues_project_issue_number",
            columnNames = {"project_id", "github_issue_number"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
public class Issue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "github_issue_id")
    private Long githubIssueId;

    @Column(name = "github_issue_number", nullable = false)
    private Integer githubIssueNumber;

    @Column(name = "github_url", nullable = false)
    private String githubUrl;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "body_excerpt")
    private String bodyExcerpt;

    @Column(name = "status", nullable = false)
    private String status;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "labels", columnDefinition = "text[]")
    private String[] labels;

    @Column(name = "difficulty")
    private String difficulty;

    @Column(name = "is_beginner_friendly")
    private Boolean isBeginnerFriendly = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "difficulty_overridden_by_user_id")
    private User difficultyOverriddenByUser;

    @Column(name = "maintainer_response_score")
    private BigDecimal maintainerResponseScore;

    @Column(name = "project_health_score")
    private BigDecimal projectHealthScore;

    @Column(name = "freshness_score")
    private BigDecimal freshnessScore;

    @Column(name = "contribution_score")
    private BigDecimal contributionScore;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;
}