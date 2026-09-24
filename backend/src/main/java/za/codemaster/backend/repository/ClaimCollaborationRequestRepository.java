package za.codemaster.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.codemaster.backend.domain.model.ClaimCollaborationRequest;
import za.codemaster.backend.domain.model.CollaborationRequestStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClaimCollaborationRequestRepository extends JpaRepository<ClaimCollaborationRequest, Long> {

    /**
     * Every request on a claim, regardless of status, oldest first — same
     * "full history, not just the live state" philosophy as
     * {@code ClaimRepository.findByIssueIdOrderByCreatedAtAsc}.
     */
    List<ClaimCollaborationRequest> findByClaimIdOrderByCreatedAtAsc(Long claimId);

    /** Accepted collaborators on a claim — backs {@code ClaimDto.collaborators}. */
    List<ClaimCollaborationRequest> findByClaimIdAndStatus(Long claimId, CollaborationRequestStatus status);

    /**
     * Looks up a specific requester's live (pending/accepted) request on a
     * claim — used to reject a duplicate request before it hits the partial
     * unique index.
     */
    Optional<ClaimCollaborationRequest> findByClaimIdAndRequesterIdAndStatusIn(
            Long claimId, Long requesterId, Collection<CollaborationRequestStatus> statuses);
}
