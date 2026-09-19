package za.codemaster.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.codemaster.backend.domain.model.ProjectMaintainer;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing {@link ProjectMaintainer} relationships.
 */
@Repository
public interface ProjectMaintainerRepository extends JpaRepository<ProjectMaintainer, Long> {

    /**
     * Checks if a maintainer record already exists for the given project and user.
     * Use this pre-check to return clean 409 Conflict responses instead of relying on database exceptions.
     *
     * @param projectId the ID of the project
     * @param userId the ID of the user
     * @return true if the relationship exists, false otherwise
     */
    boolean existsByProjectIdAndUserId(Long projectId, Long userId);

    /**
     * Finds a maintainer record by composite project and user IDs.
     *
     * @param projectId the ID of the project
     * @param userId the ID of the user
     * @return an Optional containing the maintainer relationship if found
     */
    Optional<ProjectMaintainer> findByProjectIdAndUserId(Long projectId, Long userId);

    /**
     * Retrieves all maintainers assigned to a specific project.
     *
     * @param projectId the project identifier
     * @return list of project maintainers
     */
    List<ProjectMaintainer> findByProjectId(Long projectId);

    /**
     * Retrieves all maintainer memberships held by a specific user.
     *
     * @param userId the user identifier
     * @return list of project maintainers
     */
    List<ProjectMaintainer> findByUserId(Long userId);
}