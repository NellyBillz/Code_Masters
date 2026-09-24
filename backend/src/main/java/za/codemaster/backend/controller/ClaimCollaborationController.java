package za.codemaster.backend.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.dto.collaboration.ClaimCollaborationRequestDto;
import za.codemaster.backend.dto.collaboration.RespondCollaborationRequest;
import za.codemaster.backend.security.AuthenticatedUser;
import za.codemaster.backend.service.ClaimCollaborationService;

import java.util.List;

/**
 * Requests to join another contributor's claim as a collaborator — not a
 * merge of two claims, see {@link ClaimCollaborationService}'s javadoc.
 * Kept separate from {@link ClaimController}, same reasoning as
 * {@code CommentController}/{@code MaintainerController}: a distinct
 * concern nested under the same path.
 */
@RestController
public class ClaimCollaborationController {

    private final ClaimCollaborationService collaborationService;

    public ClaimCollaborationController(ClaimCollaborationService collaborationService) {
        this.collaborationService = collaborationService;
    }

    /**
     * {@code POST /api/v1/issues/{issueId}/claims/{claimId}/collaboration-requests}:
     * request to join a claim the caller doesn't own.
     */
    @PostMapping("/api/v1/issues/{issueId}/claims/{claimId}/collaboration-requests")
    public ResponseEntity<ClaimCollaborationRequestDto> requestCollaboration(
            @PathVariable Long issueId,
            @PathVariable Long claimId,
            @AuthenticatedUser User currentUser) {
        ClaimCollaborationRequestDto created = collaborationService.requestCollaboration(issueId, claimId, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * {@code GET /api/v1/issues/{issueId}/claims/{claimId}/collaboration-requests}:
     * every request on a claim, regardless of status. Public, no auth required.
     */
    @GetMapping("/api/v1/issues/{issueId}/claims/{claimId}/collaboration-requests")
    public List<ClaimCollaborationRequestDto> listRequests(
            @PathVariable Long issueId,
            @PathVariable Long claimId) {
        return collaborationService.listRequests(issueId, claimId);
    }

    /**
     * {@code POST /api/v1/issues/{issueId}/claims/{claimId}/collaboration-requests/{requestId}/response}:
     * the claim owner's accept/decline decision on a pending request.
     */
    @PostMapping("/api/v1/issues/{issueId}/claims/{claimId}/collaboration-requests/{requestId}/response")
    public ClaimCollaborationRequestDto respondToRequest(
            @PathVariable Long issueId,
            @PathVariable Long claimId,
            @PathVariable Long requestId,
            @Valid @RequestBody RespondCollaborationRequest request,
            @AuthenticatedUser User currentUser) {
        return collaborationService.respondToRequest(issueId, claimId, requestId, request.decision(), currentUser);
    }

    /**
     * {@code DELETE /api/v1/issues/{issueId}/claims/{claimId}/collaboration-requests/{requestId}}:
     * the requester withdraws their own still-pending request.
     */
    @DeleteMapping("/api/v1/issues/{issueId}/claims/{claimId}/collaboration-requests/{requestId}")
    public ResponseEntity<Void> cancelRequest(
            @PathVariable Long issueId,
            @PathVariable Long claimId,
            @PathVariable Long requestId,
            @AuthenticatedUser User currentUser) {
        collaborationService.cancelRequest(issueId, claimId, requestId, currentUser);
        return ResponseEntity.noContent().build();
    }
}
