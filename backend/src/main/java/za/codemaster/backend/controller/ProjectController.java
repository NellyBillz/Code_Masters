package za.codemaster.backend.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.dto.issue.Difficulty;
import za.codemaster.backend.dto.issue.IssueStatus;
import za.codemaster.backend.dto.issue.PagedIssues;
import za.codemaster.backend.dto.project.CreateProjectRequest;
import za.codemaster.backend.dto.project.PagedProjects;
import za.codemaster.backend.dto.project.ProjectDetail;
import za.codemaster.backend.dto.project.ProjectDto;
import za.codemaster.backend.dto.project.UpdateProjectRequest;
import za.codemaster.backend.security.AuthenticatedUser;
import za.codemaster.backend.security.OptionalAuthenticatedUser;
import za.codemaster.backend.service.ProjectIssuesSearchParams;
import za.codemaster.backend.service.ProjectQueryService;
import za.codemaster.backend.service.ProjectSearchParams;
import za.codemaster.backend.service.ProjectService;

/**
 * Project discovery and submission/maintenance endpoint(s).
 * <p>
 * Reads are deliberately thin: they read query parameters off the request and
 * delegate all filtering/sorting/pagination logic to {@link ProjectQueryService}.
 * Writes (API-02.7) delegate to {@link ProjectService}.
 */
@RestController
public class ProjectController {

    private final ProjectQueryService projectQueryService;
    private final ProjectService projectService;

    public ProjectController(ProjectQueryService projectQueryService, ProjectService projectService) {
        this.projectQueryService = projectQueryService;
        this.projectService = projectService;
    }

    /**
     * {@code GET /api/v1/projects}: list and filter projects.
     * <p>
     * Matches the {@code /projects} GET operation in codemasters-api-spec.yaml
     * v2.1. All parameters are optional; see {@link ProjectQueryService#search}
     * for how each one is applied.
     */
    @GetMapping("/api/v1/projects")
    public PagedProjects listProjects(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Boolean hasBeginnerIssues
    ) {
        ProjectSearchParams params = new ProjectSearchParams(
                page, size, q, language, category, tag, country, sort, hasBeginnerIssues
        );
        return projectQueryService.search(params);
    }

    /**
     * {@code GET /api/v1/projects/{projectId}} - a single project's detail view.
     * <p>
     * Matches the {@code /projects/{projectId}} GET operation in
     * codemasters-api-spec.yaml v3. Throws {@code PROJECT_NOT_FOUND} (404)
     * via GlobalExceptionHandler if the id doesn't exist, or if it exists but
     * isn't {@code published} and the caller isn't its submitter/a maintainer
     * (API-03.1) - see {@link ProjectQueryService#getProjectDetail}.
     */
    @GetMapping("/api/v1/projects/{projectId}")
    public ProjectDetail getProject(@PathVariable Long projectId, @OptionalAuthenticatedUser User caller) {
        return projectQueryService.getProjectDetail(projectId, caller);
    }

    /**
     * {@code GET /api/v1/projects/{projectId}/issues}; a project's issues, filtered and paginated.
     * <p>
     * Matches the {@code /projects/{projectId}/issues} GET operation in
     * codemasters-api-spec.yaml v2.1. Throws {@code PROJECT_NOT_FOUND} (404)
     * via GlobalExceptionHandler if the project itself doesn't exist.
     */
    @GetMapping("/api/v1/projects/{projectId}/issues")
    public PagedIssues getProjectIssues(
            @PathVariable Long projectId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(required = false) String label,
            @RequestParam(required = false) IssueStatus status
    ) {
        ProjectIssuesSearchParams params = new ProjectIssuesSearchParams(page, size, difficulty, label, status);
        return projectQueryService.getProjectIssues(projectId, params);
    }

    /**
     * {@code POST /api/v1/projects}: submit a project (API-02.7). Creates the project and
     * adds the caller as its {@code owner} maintainer in one transaction — see
     * {@link ProjectService#createProject}.
     */
    @PostMapping("/api/v1/projects")
    public ResponseEntity<ProjectDto> submitProject(
            @Valid @RequestBody CreateProjectRequest request,
            @AuthenticatedUser User currentUser) {
        ProjectDto created = projectService.createProject(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * {@code PATCH /api/v1/projects/{projectId}}: update a project's Code-Masters-specific
     * metadata (API-02.7). Maintainer-only (any role).
     */
    @PatchMapping("/api/v1/projects/{projectId}")
    public ProjectDto updateProject(
            @PathVariable Long projectId,
            @Valid @RequestBody UpdateProjectRequest request,
            @AuthenticatedUser User currentUser) {
        return projectService.updateProject(projectId, request, currentUser);
    }
}
