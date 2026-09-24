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
 * A request from a contributor to join another user's claim as a
 * collaborator (not a merge of two claims — the claim keeps exactly one
 * owner, {@link Claim#getUser()}, which is who a pull request is expected
 * to be attached to on GitHub). Once accepted, the requester is credited as
 * a collaborator on the claim: if/when the claim completes, both the owner
 * and every accepted collaborator count it as their own contribution.
 * <p>
 * <b>Do NOT add a plain {@code @UniqueConstraint(columnNames = {"claim_id", "requester_user_id"})}.</b>
 * Same reasoning as {@link Claim}'s own javadoc: the database enforces a
 * <i>partial unique index</i> ({@code uq_claim_collaboration_requests_live_per_user},
 * V13__claim_collaboration_requests.sql) so a declined/cancelled request
 * doesn't block the same person from requesting again later.
 */
@Entity
@Table(name = "claim_collaboration_requests")
@Getter
@Setter
@NoArgsConstructor
public class ClaimCollaborationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "claim_id", nullable = false)
    private Claim claim;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_user_id", nullable = false)
    private User requester;

    @Column(name = "status", nullable = false)
    private CollaborationRequestStatus status = CollaborationRequestStatus.PENDING;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "responded_at")
    private OffsetDateTime respondedAt;
}
