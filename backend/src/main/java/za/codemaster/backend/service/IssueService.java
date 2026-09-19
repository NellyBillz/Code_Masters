package za.codemaster.backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.codemaster.backend.domain.model.Issue;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.dto.UpdateIssueRequest;
import za.codemaster.backend.exception.ApiException;
import za.codemaster.backend.repository.IssueRepository;
import za.codemaster.backend.repository.ProjectMaintainerRepository;

/**
 * Service for maintainer-only issue classification overrides (API-02.6, design
 * doc §8's "bonus beat" of a maintainer correcting an issue's difficulty label).
 */
@Service
public class IssueService {

    private final IssueRepository issueRepository;
    private final ProjectMaintainerRepository projectMaintainerRepository;
    private final ProjectQueryService projectQueryService;

    public IssueService(IssueRepository issueRepository,
                         ProjectMaintainerRepository projectMaintainerRepository,
                         ProjectQueryService projectQueryService) {
        this.issueRepository = issueRepository;
        this.projectMaintainerRepository = projectMaintainerRepository;
        this.projectQueryService = projectQueryService;
    }

    /**
     * Applies a maintainer's correction to an issue's {@code difficulty} and/or
     * {@code isBeginnerFriendly}. Only fields present (non-null) on the request
     * are changed. If anything was actually changed, {@code difficultyOverriddenByUser}
     * is set to the caller — this is what tells a future GitHub sync (GH-02.3) not
     * to silently clobber the human correction (design doc §6).
     *
     * @param issueId the issue id from the path
     * @param request the fields to change; both optional
     * @param caller  the authenticated caller, injected via {@code @AuthenticatedUser}
     * @return the updated issue, in API shape
     * @throws ApiException with code {@code ISSUE_NOT_FOUND} (404) if the issue doesn't exist, or
     *                       {@code FORBIDDEN} (403) if the caller isn't a maintainer of the issue's
     *                       parent project
     */
    @Transactional
    public za.codemaster.backend.dto.Issue updateClassification(Long issueId, UpdateIssueRequest request, User caller) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ApiException(
                        "ISSUE_NOT_FOUND", "No issue exists with id " + issueId, HttpStatus.NOT_FOUND));

        Long projectId = issue.getProject().getId();
        if (!projectMaintainerRepository.existsByProjectIdAndUserId(projectId, caller.getId())) {
            throw new ApiException(
                    "FORBIDDEN",
                    "Only a maintainer of this issue's project may override its classification.",
                    HttpStatus.FORBIDDEN);
        }

        boolean changed = false;
        if (request.difficulty() != null) {
            issue.setDifficulty(request.difficulty().getWireValue());
            changed = true;
        }
        if (request.isBeginnerFriendly() != null) {
            issue.setIsBeginnerFriendly(request.isBeginnerFriendly());
            changed = true;
        }

        if (changed) {
            issue.setDifficultyOverriddenByUser(caller);
        }

        Issue saved = issueRepository.save(issue);
        return projectQueryService.toDto(saved);
    }
}
