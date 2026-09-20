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
     * Public visibility lookup: retrieves a project by ID only if it is in the target listing status.
     */
    Optional<Project> findByIdAndListingStatus(Long id, ListingStatus listingStatus);

    /**
     * Public visibility lookup by slug: retrieves a project by slug only if it is in the target listing status.
     */
    Optional<Project> findBySlugAndListingStatus(String slug, ListingStatus listingStatus);

    /**
     * Moderation queue query: pages projects by listing status.
     */
    
    Page<Project> findByListingStatus(ListingStatus listingStatus, Pageable pageable);

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
}