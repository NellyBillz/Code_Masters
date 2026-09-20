package za.codemaster.backend.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.dto.project.PagedProjects;
import za.codemaster.backend.dto.project.ProjectDto;
import za.codemaster.backend.dto.project.ProjectModerationRequest;
import za.codemaster.backend.security.AuthenticatedUser;
import za.codemaster.backend.service.ProjectModerationService;

/**
 * The site-admin project moderation queue (API-03.1). Every endpoint here is
 * site-admin only, enforced by {@link za.codemaster.backend.security.SiteAdminGuard}
 * inside {@link ProjectModerationService} — not by a separate security scheme
 * (design doc §4: one auth mechanism, a service-layer check on top of it).
 */
@RestController
public class AdminProjectController {

    private final ProjectModerationService moderationService;

    public AdminProjectController(ProjectModerationService moderationService) {
        this.moderationService = moderationService;
    }

    /**
     * {@code GET /api/v1/admin/projects/pending}: every project awaiting review.
     */
    @GetMapping("/api/v1/admin/projects/pending")
    public PagedProjects listPending(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @AuthenticatedUser User currentUser) {
        return moderationService.listPending(page, size, currentUser);
    }

    /**
     * {@code POST /api/v1/admin/projects/{projectId}/moderation}: approve or reject
     * a pending submission.
     */
    @PostMapping("/api/v1/admin/projects/{projectId}/moderation")
    public ProjectDto moderate(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectModerationRequest request,
            @AuthenticatedUser User currentUser) {
        return moderationService.decide(projectId, request, currentUser);
    }
}
