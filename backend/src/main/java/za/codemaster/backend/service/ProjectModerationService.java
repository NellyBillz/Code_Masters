package za.codemaster.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.codemaster.backend.domain.model.ListingStatus;
import za.codemaster.backend.domain.model.NotificationType;
import za.codemaster.backend.domain.model.Project;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.dto.common.PageMeta;
import za.codemaster.backend.dto.project.ModerationDecision;
import za.codemaster.backend.dto.project.PagedProjects;
import za.codemaster.backend.dto.project.ProjectDto;
import za.codemaster.backend.dto.project.ProjectModerationRequest;
import za.codemaster.backend.exception.ApiException;
import za.codemaster.backend.repository.ProjectMaintainerRepository;
import za.codemaster.backend.repository.ProjectRepository;
import za.codemaster.backend.security.SiteAdminGuard;

import java.util.List;

/**
 * The submission moderation gate (API-03.1, design doc §15.2): the site-admin-only
 * queue of {@code pending} projects, and the decision that moves one to
 * {@code published} or {@code rejected}. This is explicitly not the Layer 2
 * verification feature — see {@link ProjectModerationRequest}'s Javadoc.
 */
@Service
public class ProjectModerationService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final ProjectRepository projectRepository;
    private final ProjectQueryService projectQueryService;
    private final SiteAdminGuard siteAdminGuard;
    private final ProjectMaintainerRepository projectMaintainerRepository;
    private final NotificationService notificationService;

    public ProjectModerationService(ProjectRepository projectRepository,
                                     ProjectQueryService projectQueryService,
                                     SiteAdminGuard siteAdminGuard,
                                     ProjectMaintainerRepository projectMaintainerRepository,
                                     NotificationService notificationService) {
        this.projectRepository = projectRepository;
        this.projectQueryService = projectQueryService;
        this.siteAdminGuard = siteAdminGuard;
        this.projectMaintainerRepository = projectMaintainerRepository;
        this.notificationService = notificationService;
    }

    /**
     * {@code GET /admin/projects/pending}: every project awaiting review, paginated.
     *
     * @throws ApiException with code {@code FORBIDDEN} (403) if {@code caller} isn't a site admin
     */
    @Transactional(readOnly = true)
    public PagedProjects listPending(Integer page, Integer size, User caller) {
        siteAdminGuard.requireSiteAdmin(caller);

        int resolvedPage = clampPage(page);
        int resolvedSize = clampSize(size);

        Page<Project> result = projectRepository.findByListingStatus(
                ListingStatus.PENDING, PageRequest.of(resolvedPage, resolvedSize));

        List<ProjectDto> items = result.getContent().stream().map(projectQueryService::toDto).toList();
        return new PagedProjects(items, new PageMeta(resolvedPage, resolvedSize, (int) result.getTotalElements()));
    }

    /**
     * {@code POST /admin/projects/{projectId}/moderation}: approve or reject a
     * pending submission. Only ever changes {@code listingStatus} — never
     * touches {@code verified}/{@code verifiedAt} (design doc §15.3).
     *
     * @throws ApiException with code {@code FORBIDDEN} (403) if {@code caller} isn't a site admin, or
     *                       {@code PROJECT_NOT_FOUND} (404) if the project doesn't exist
     */
    @Transactional
    public ProjectDto decide(Long projectId, ProjectModerationRequest request, User caller) {
        siteAdminGuard.requireSiteAdmin(caller);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException(
                        "PROJECT_NOT_FOUND", "No project exists with id " + projectId, HttpStatus.NOT_FOUND));

        boolean approved = request.decision() == ModerationDecision.APPROVE;
        ListingStatus newStatus = approved ? ListingStatus.PUBLISHED : ListingStatus.REJECTED;
        project.setListingStatus(newStatus);

        Project saved = projectRepository.save(project);
        ProjectDto dto = projectQueryService.toDto(saved);

        String message = approved
                ? "Your project \"" + saved.getName() + "\" was approved and is now live!"
                : "Your project \"" + saved.getName() + "\" was not approved.";
        projectMaintainerRepository.findByProjectId(saved.getId()).stream()
                .filter(maintainer -> "owner".equals(maintainer.getRole()))
                .forEach(maintainer -> notificationService.notify(
                        maintainer.getUser(), NotificationType.PROJECT_MODERATED, message, "/projects/" + saved.getId()));

        return dto;
    }

    private int clampSize(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        return Math.max(1, Math.min(size, MAX_SIZE));
    }

    private int clampPage(Integer page) {
        if (page == null || page < 0) {
            return DEFAULT_PAGE;
        }
        return page;
    }
}
