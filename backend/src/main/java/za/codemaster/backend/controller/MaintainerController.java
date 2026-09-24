package za.codemaster.backend.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.dto.project.AddMaintainerRequest;
import za.codemaster.backend.dto.project.ProjectMaintainerDto;
import za.codemaster.backend.security.AuthenticatedUser;
import za.codemaster.backend.service.MaintainerService;

/**
 * Owner-only maintainer invite/removal (API-02.8, round2-tickets.md). Kept separate
 * from {@link ProjectController} — same reasoning as {@link CommentController}/
 * {@link ClaimController} — since it's the spec's own {@code Maintainers} tag,
 * a distinct concern from project CRUD even though it's nested under the same path.
 */
@RestController
public class MaintainerController {

    private final MaintainerService maintainerService;

    public MaintainerController(MaintainerService maintainerService) {
        this.maintainerService = maintainerService;
    }

    /**
     * {@code POST /api/v1/projects/{projectId}/maintainers}: invite a maintainer by username.
     * Owner-only; {@code role} defaults to {@code maintainer}.
     */
    @PostMapping("/api/v1/projects/{projectId}/maintainers")
    public ResponseEntity<ProjectMaintainerDto> inviteMaintainer(
            @PathVariable Long projectId,
            @Valid @RequestBody AddMaintainerRequest request,
            @AuthenticatedUser User currentUser) {
        ProjectMaintainerDto created = maintainerService.inviteMaintainer(projectId, request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * {@code POST /api/v1/projects/{projectId}/maintainers/assign-owner}: site-admin-only,
     * assigns the first owner to a project that currently has zero maintainers (see
     * {@link MaintainerService#assignOwner}'s Javadoc for why this exists).
     */
    @PostMapping("/api/v1/projects/{projectId}/maintainers/assign-owner")
    public ResponseEntity<ProjectMaintainerDto> assignOwner(
            @PathVariable Long projectId,
            @Valid @RequestBody AddMaintainerRequest request,
            @AuthenticatedUser User currentUser) {
        ProjectMaintainerDto created = maintainerService.assignOwner(projectId, request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * {@code DELETE /api/v1/projects/{projectId}/maintainers/{userId}}: remove a maintainer.
     * Owner-only; refuses to remove the last remaining owner.
     */
    @DeleteMapping("/api/v1/projects/{projectId}/maintainers/{userId}")
    public ResponseEntity<Void> removeMaintainer(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            @AuthenticatedUser User currentUser) {
        maintainerService.removeMaintainer(projectId, userId, currentUser);
        return ResponseEntity.noContent().build();
    }
}
