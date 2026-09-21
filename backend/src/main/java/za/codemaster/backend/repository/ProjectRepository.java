package za.codemaster.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import za.codemaster.backend.domain.model.ListingStatus;
import za.codemaster.backend.domain.model.Project;

import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findBySlug(String slug);

    Optional<Project> findByGithubUrl(String githubUrl);

    /**
     * Public visibility lookup: finds a project by ID only if it is in the specified status (e.g., PUBLISHED).
     */
    Optional<Project> findByIdAndListingStatus(Long id, ListingStatus listingStatus);

    /**
     * Public visibility lookup by slug: finds a project by slug only if it is in the specified status.
     */
    Optional<Project> findBySlugAndListingStatus(String slug, ListingStatus listingStatus);

    /**
     * Moderation queue query: returns projects matching a given listing status (e.g., PENDING).
     */
    Page<Project> findByListingStatus(ListingStatus listingStatus, Pageable pageable);

    /**
     * Counts projects in a given listing status. Backs {@code PlatformStats.publishedProjects}
     * (API-03.12) — a plain {@code COUNT()}, not a full row load.
     */
    long countByListingStatus(ListingStatus listingStatus);

    /**
     * Counts projects in a given listing status with a given {@code acceptingContributions}
     * value. Backs {@code PlatformStats.activeProjectsAcceptingContributions} (API-03.12).
     */
    long countByListingStatusAndAcceptingContributions(ListingStatus listingStatus, boolean acceptingContributions);

    /**
     * Counts distinct country codes across every {@code published} project's
     * {@code countryCodes}. Backs {@code PlatformStats.countriesRepresented}
     * (API-03.12) — a single aggregate query, not a per-project in-memory scan.
     */
    @Query("SELECT COUNT(DISTINCT cc) FROM Project p JOIN p.countryCodes cc WHERE p.listingStatus = :status")
    long countDistinctCountryCodesByListingStatus(@Param("status") ListingStatus status);

    /**
     * Preserved legacy JPQL filter method to maintain backwards compatibility with existing tests.
     */
    @Query("""
        SELECT p FROM Project p
        WHERE (:primaryLanguage IS NULL OR LOWER(p.primaryLanguage) = LOWER(:primaryLanguage))
          AND (:category IS NULL OR LOWER(p.category) = LOWER(:category))
          AND (:connection IS NULL OR LOWER(p.connection) = LOWER(:connection))
          AND (:hasBeginnerFriendlyIssues IS NULL OR p.hasBeginnerFriendlyIssues = :hasBeginnerFriendlyIssues)
    """)
    Page<Project> findWithFilters(
        @Param("primaryLanguage") String primaryLanguage,
        @Param("category") String category,
        @Param("connection") String connection,
        @Param("hasBeginnerFriendlyIssues") Boolean hasBeginnerFriendlyIssues,
        Pageable pageable
    );

    /**
     * Native PostgreSQL relevance-ranked search query.
     * Integrates ts_rank over weighted tsvector, trigram similarity fallback,
     * and in-query tag and country filtering without in-memory post-filtering.
     * {@code q} also matches a substring of any of the project's tags — carried
     * over from the pre-API-03.13 in-memory matcher, since {@code tsv} (name/
     * description/owner) doesn't cover tags and a freshly submitted project
     * (no description yet) otherwise has no field {@code q} could ever hit.
     */
    @Query(
        value = """
            SELECT p.*
            FROM projects p
            WHERE (:listingStatus IS NULL OR :listingStatus = '' OR p.listing_status = :listingStatus)
              AND (:primaryLanguage IS NULL OR :primaryLanguage = '' OR LOWER(p.primary_language) = LOWER(:primaryLanguage))
              AND (:category IS NULL OR :category = '' OR LOWER(p.category) = LOWER(:category))
              AND (:connection IS NULL OR :connection = '' OR LOWER(p.connection) = LOWER(:connection))
              AND (:hasBeginnerFriendlyIssues IS NULL OR p.has_beginner_friendly_issues = :hasBeginnerFriendlyIssues)
              AND (:tag IS NULL OR :tag = '' OR EXISTS (
                    SELECT 1 FROM project_tags pt WHERE pt.project_id = p.id AND LOWER(pt.tag) = LOWER(:tag)
                  ))
              AND (:country IS NULL OR :country = '' OR EXISTS (
                    SELECT 1 FROM project_countries pc WHERE pc.project_id = p.id AND UPPER(pc.country_code) = UPPER(:country)
                  ))
              AND (
                    :q IS NULL OR :q = ''
                    OR p.tsv @@ plainto_tsquery('english', :q)
                    OR similarity(p.name, :q) >= 0.2
                    OR word_similarity(:q, p.name) >= 0.25
                    OR p.name % :q
                    OR EXISTS (SELECT 1 FROM project_tags qt WHERE qt.project_id = p.id AND qt.tag ILIKE '%' || :q || '%')
                  )
            ORDER BY
              CASE WHEN :sortByRelevance = true THEN (
                ts_rank(p.tsv, plainto_tsquery('english', coalesce(:q, ''))) + similarity(p.name, coalesce(:q, ''))
              ) END DESC NULLS LAST,
              p.id DESC
        """,
        countQuery = """
            SELECT count(*)
            FROM projects p
            WHERE (:listingStatus IS NULL OR :listingStatus = '' OR p.listing_status = :listingStatus)
              AND (:primaryLanguage IS NULL OR :primaryLanguage = '' OR LOWER(p.primary_language) = LOWER(:primaryLanguage))
              AND (:category IS NULL OR :category = '' OR LOWER(p.category) = LOWER(:category))
              AND (:connection IS NULL OR :connection = '' OR LOWER(p.connection) = LOWER(:connection))
              AND (:hasBeginnerFriendlyIssues IS NULL OR p.has_beginner_friendly_issues = :hasBeginnerFriendlyIssues)
              AND (:tag IS NULL OR :tag = '' OR EXISTS (
                    SELECT 1 FROM project_tags pt WHERE pt.project_id = p.id AND LOWER(pt.tag) = LOWER(:tag)
                  ))
              AND (:country IS NULL OR :country = '' OR EXISTS (
                    SELECT 1 FROM project_countries pc WHERE pc.project_id = p.id AND UPPER(pc.country_code) = UPPER(:country)
                  ))
              AND (
                    :q IS NULL OR :q = ''
                    OR p.tsv @@ plainto_tsquery('english', :q)
                    OR similarity(p.name, :q) >= 0.2
                    OR word_similarity(:q, p.name) >= 0.25
                    OR p.name % :q
                    OR EXISTS (SELECT 1 FROM project_tags qt WHERE qt.project_id = p.id AND qt.tag ILIKE '%' || :q || '%')
                  )
        """,
        nativeQuery = true
    )
    Page<Project> searchProjects(
        @Param("q") String q,
        @Param("listingStatus") String listingStatus,
        @Param("primaryLanguage") String primaryLanguage,
        @Param("category") String category,
        @Param("connection") String connection,
        @Param("hasBeginnerFriendlyIssues") Boolean hasBeginnerFriendlyIssues,
        @Param("tag") String tag,
        @Param("country") String country,
        @Param("sortByRelevance") boolean sortByRelevance,
        Pageable pageable
    );
}