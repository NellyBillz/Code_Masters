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
import za.codemaster.backend.dto.claim.ClaimDto;
import za.codemaster.backend.dto.claim.CreateClaimRequest;
import za.codemaster.backend.security.AuthenticatedUser;
import za.codemaster.backend.service.ClaimService;

import java.util.List;

/**
 * The claim half of the demo flow (design doc §8, step 6): {@code POST}/{@code DELETE}/{@code GET}
 * on {@code /issues/{issueId}/claim(s)} (API-02.5, round2-tickets.md).
 */
@RestController
public class ClaimController {

    private final ClaimService claimService;

    public ClaimController(ClaimService claimService) {
        this.claimService = claimService;
    }

    /**
     * {@code POST /api/v1/issues/{issueId}/claim}: signal intent to work on an issue.
     * The request body is entirely optional (a bare claim needs no note).
     */
    @PostMapping("/api/v1/issues/{issueId}/claim")
    public ResponseEntity<ClaimDto> createClaim(
            @PathVariable Long issueId,
            @Valid @RequestBody(required = false) CreateClaimRequest request,
            @AuthenticatedUser User currentUser) {
        String note = request == null ? null : request.note();
        ClaimDto created = claimService.createClaim(issueId, note, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * {@code DELETE /api/v1/issues/{issueId}/claim}: release the caller's own active claim.
     */
    @DeleteMapping("/api/v1/issues/{issueId}/claim")
    public ResponseEntity<Void> releaseClaim(
            @PathVariable Long issueId,
            @AuthenticatedUser User currentUser) {
        claimService.releaseClaim(issueId, currentUser);
        return ResponseEntity.noContent().build();
    }

    /**
     * {@code GET /api/v1/issues/{issueId}/claims}: list active claims, oldest first.
     * Returns a bare array, not a paginated envelope — matches the spec's schema
     * for this endpoint exactly (unlike comments/projects/issues, this list isn't paged).
     */
    @GetMapping("/api/v1/issues/{issueId}/claims")
    public List<ClaimDto> getActiveClaims(@PathVariable Long issueId) {
        return claimService.getActiveClaims(issueId);
    }
}
