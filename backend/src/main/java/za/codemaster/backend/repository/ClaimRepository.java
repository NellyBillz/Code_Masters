package za.codemaster.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.codemaster.backend.domain.model.Claim;
import za.codemaster.backend.domain.model.ClaimStatus;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing {@link Claim} records.
 */
@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {

    /**
     * Looks up an existing claim by issue, user, and status.
     * Used by the service layer to catch duplicate active claims prior to persisting.
     */
    Optional<Claim> findByIssueIdAndUserIdAndStatus(Long issueId, Long userId, ClaimStatus status);

    /**
     * Verifies if a user already holds an active claim on the specified issue.
     */
    boolean existsByIssueIdAndUserIdAndStatus(Long issueId, Long userId, ClaimStatus status);

    /**
     * Finds all active claims for a given issue.
     */
    List<Claim> findByIssueIdAndStatus(Long issueId, ClaimStatus status);

    /**
     * Finds all claims associated with a given user.
     */
    List<Claim> findByUserId(Long userId);

    /**
     * Counts claims on an issue in a given status. Used to populate
     * {@code Issue.claimCount} (active claims) without loading full claim rows.
     */
    long countByIssueIdAndStatus(Long issueId, ClaimStatus status);
}