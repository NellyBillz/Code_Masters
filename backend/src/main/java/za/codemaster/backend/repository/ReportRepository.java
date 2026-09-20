package za.codemaster.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.codemaster.backend.domain.model.Report;

import java.util.Optional;

/**
 * Repository interface for managing {@link Report} instances.
 * <p>
 * Serves API-03.9 abuse report endpoints and the admin review queue.
 * </p>
 */
@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    /**
     * Pre-check query to prevent duplicate reports before database insertion.
     * Follows the duplicate-check pattern to permit a clean 409 Conflict response.
     *
     * @param reporterUserId the reporting user's identifier
     * @param targetType the target type ('comment' or 'project')
     * @param targetId the target entity identifier
     * @return an Optional containing the report if one already exists
     */
    Optional<Report> findByReporterUserIdAndTargetTypeAndTargetId(Long reporterUserId, String targetType, Long targetId);

    /**
     * Paginated retrieval of reports filtered by lifecycle status for the administrator queue.
     *
     * @param status the report status ('open', 'resolved', 'dismissed')
     * @param pageable pagination and sorting parameters
     * @return a paged list of matching reports
     */
    Page<Report> findByStatus(String status, Pageable pageable);
}