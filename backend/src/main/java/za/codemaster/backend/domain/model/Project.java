package za.codemaster.backend.domain.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Represents a curated open-source repository indexed by the Code Master platform[cite: 1].
 * <p>
 * Mapped to the {@code projects} table defined in database migration {@code DB-01.2} ({@code V2__projects.sql}).
 * Captures repository metadata, GitHub metrics (stars and open issues count), regional connectivity
 * classification, and beginner-friendly status[cite: 1].
 * </p>
 * <p>
 * Associated tags and ISO country codes are stored in separate normalization tables ({@code project_tags}
 * and {@code project_countries}) but exposed directly on this entity as {@link ElementCollection} lists,
 * aligning with the domain DTO contract[cite: 1].
 * </p>
 *
 * @author Code Master Team
 * @version 1.0
 * @since DB-01.2
 */

@Entity
@Table(name = "projects")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "github_owner", nullable = false)
    private String githubOwner;

    @Column(name = "github_repo", nullable = false)
    private String githubRepo;

    @Column(name = "github_url", nullable = false, unique = true)
    private String githubUrl;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    @Column(name = "description")
    private String description;

    @Column(name = "primary_language")
    private String primaryLanguage;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "languages", columnDefinition= "text[]")
    private String[] languages;

    @Column(name = "category", nullable = false)
    private String category;

    @Column(name = "connection", nullable = false)
    private String connection;

    @Column(name = "license")
    private String license;

    @Column(name = "stars")
    @Builder.Default
    private Integer stars = 0;

    @Column(name = "forks")
    @Builder.Default
    private Integer forks = 0;

    @Column(name = "open_issues")
    @Builder.Default
    private Integer openIssues = 0;

    @Column(name = "contributors")
    @Builder.Default
    private Integer contributors = 0;

    @Column(name = "has_beginner_friendly_issues")
    @Builder.Default
    private Boolean hasBeginnerFriendlyIssues = false;

    @Column(name = "last_activity_at", insertable = false, updatable = false)
    private OffsetDateTime lastActivityAt;

    @Column(name = "github_metadata_etag")
    private String githubMetadataEtag;

    @Column(name = "github_issues_etag")
    private String githubIssuesEtag;

    @Column(name = "has_contributing_guide", nullable = false)
    @Builder.Default
    private boolean hasContributingGuide = false;

    @Column(name = "has_code_of_conduct", nullable = false)
    @Builder.Default
    private boolean hasCodeOfConduct = false;

    @Column(name = "listing_status", nullable = false)
    @Builder.Default
    private ListingStatus listingStatus = ListingStatus.PENDING;

    @Column(name = "accepting_contributions", nullable = false)
    @Builder.Default
    private boolean acceptingContributions = true;

    @Column(name = "verified", nullable = false)
    @Builder.Default
    private Boolean verified = false;

    @Column(name = "verified_at", insertable = false, updatable = false)
    private OffsetDateTime verifiedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    /**
     * Normalized list of search and domain tags associated with this repository.
     * Mapped to the auxiliary {@code project_tags} table via {@link ElementCollection}[cite: 1].
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "project_tags", joinColumns = @JoinColumn(name = "project_id"))
    @Column(name = "tag", nullable = false)
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    /**
     * Normalized list of two-letter ISO country codes representing countries affiliated with this project.
     * Mapped to the auxiliary {@code project_countries} table via {@link ElementCollection}[cite: 1].
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "project_countries", joinColumns = @JoinColumn(name = "project_id"))
    @Column(name = "country_code", nullable = false)
    @Builder.Default
    private List<String> countryCodes = new ArrayList<>();
}
