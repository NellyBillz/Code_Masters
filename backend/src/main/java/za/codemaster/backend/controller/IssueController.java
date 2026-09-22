package za.codemaster.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.dto.issue.IssueContributionContext;
import za.codemaster.backend.dto.issue.IssueDetail;
import za.codemaster.backend.dto.issue.IssueDto;
import za.codemaster.backend.dto.issue.UpdateIssueRequest;
import za.codemaster.backend.security.AuthenticatedUser;
import za.codemaster.backend.service.IssueService;
import za.codemaster.backend.service.ProjectQueryService;

/**
 * Issue discovery and maintainer-only classification override endpoint(s).
 * <p>
 * Separate from {@link ProjectController} since these are top-level
 * {@code /api/v1/issues/...} routes, not nested under a project — even
 * though the read-side lookup currently lives in {@code ProjectQueryService}.
 */
@RestController
public class IssueController {

    private final ProjectQueryService projectQueryService;
    private final IssueService issueService;

    public IssueController(ProjectQueryService projectQueryService, IssueService issueService) {
        this.projectQueryService = projectQueryService;
        this.issueService = issueService;
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

    /**
     * {@code GET /api/v1/issues/{issueId}/contribution-context}; evidence-based
     * contribution context (API-04.1) — public, no auth required, same
     * visibility as {@link #getIssue}. Plain facts and counts only, no
     * combined score; rendered by {@code FE-04.6}. Throws {@code ISSUE_NOT_FOUND}
     * (404) via GlobalExceptionHandler if the id doesn't exist.
     */
    @GetMapping("/api/v1/issues/{issueId}/contribution-context")
    public IssueContributionContext getContributionContext(@PathVariable Long issueId) {
        return projectQueryService.getIssueContributionContext(issueId);
    }

    /**
     * {@code PATCH /api/v1/issues/{issueId}}: maintainer-only correction of local
     * classification (API-02.6). See {@link IssueService#updateClassification} for
     * the override-flagging behavior that protects this from being clobbered by sync.
     */
    @PatchMapping("/api/v1/issues/{issueId}")
    public IssueDto updateIssue(
            @PathVariable Long issueId,
            @RequestBody UpdateIssueRequest request,
            @AuthenticatedUser User currentUser) {
        return issueService.updateClassification(issueId, request, currentUser);
    }
}