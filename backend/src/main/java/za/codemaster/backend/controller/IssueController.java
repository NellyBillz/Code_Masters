package za.codemaster.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import za.codemaster.backend.dto.IssueDetail;
import za.codemaster.backend.service.ProjectQueryService;

/**
 * Public issue discovery endpoint(s).
 * <p>
 * Separate from {@link ProjectController} since these are top-level
 * {@code /api/v1/issues/...} routes, not nested under a project — even
 * though the underlying lookup currently lives in {@code ProjectQueryService}
 * (see that class's Javadoc note on its growing scope).
 */
@RestController
public class IssueController {

    private final ProjectQueryService projectQueryService;

    public IssueController(ProjectQueryService projectQueryService) {
        this.projectQueryService = projectQueryService;
    }

    /**
     * {@code GET /api/v1/issues/{issueId}}; a single issue's detail view.
     * <p>
     * Matches the {@code /issues/{issueId}} GET operation in
     * codemasters-api-spec.yaml v2.1. Throws {@code ISSUE_NOT_FOUND} (404)
     * via GlobalExceptionHandler if the id doesn't exist.
     */
    @GetMapping("/api/v1/issues/{issueId}")
    public IssueDetail getIssue(@PathVariable Long issueId) {
        return projectQueryService.getIssueDetail(issueId);
    }
}