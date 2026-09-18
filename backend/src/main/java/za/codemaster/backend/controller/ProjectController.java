package za.codemaster.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import za.codemaster.backend.dto.*;
import za.codemaster.backend.service.ProjectIssuesSearchParams;
import za.codemaster.backend.service.ProjectQueryService;
import za.codemaster.backend.service.ProjectSearchParams;

/**
 * Public project discovery endpoint(s).
 * <p>
 * Deliberately thin: reads query parameters off the request and delegates
 * all filtering/sorting/pagination logic to {@link ProjectQueryService}.
 */
@RestController
public class ProjectController {

    private final ProjectQueryService projectQueryService;

    public ProjectController(ProjectQueryService projectQueryService) {
        this.projectQueryService = projectQueryService;
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
     * codemasters-api-spec.yaml v2.1. Throws {@code PROJECT_NOT_FOUND} (404)
     * via GlobalExceptionHandler if the id doesn't exist - see
     * {@link ProjectQueryService#getProjectDetail}.
     */
    @GetMapping("/api/v1/projects/{projectId}")
    public ProjectDetail getProject(@PathVariable Long projectId) {
        return projectQueryService.getProjectDetail(projectId);
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
}
