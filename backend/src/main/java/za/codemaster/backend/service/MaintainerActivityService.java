package za.codemaster.backend.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.codemaster.backend.domain.model.ClaimStatus;
import za.codemaster.backend.domain.model.Project;
import za.codemaster.backend.domain.model.ProjectMaintainer;
import za.codemaster.backend.domain.model.PullRequestState;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.dto.claim.ClaimDto;
import za.codemaster.backend.dto.comment.CommentDto;
import za.codemaster.backend.dto.user.MaintainerActivitySummary;
import za.codemaster.backend.dto.user.MaintainerProjectActivity;
import za.codemaster.backend.repository.ClaimRepository;
import za.codemaster.backend.repository.CommentRepository;
import za.codemaster.backend.repository.ProjectMaintainerRepository;

import java.util.List;

/**
 * The maintainer-sanity activity rollup (API-03.7, design doc §18): one call
 * instead of a maintainer checking every project they maintain individually.
 */
@Service
public class MaintainerActivityService {

    private static final int RECENT_COMMENTS_LIMIT = 10;

    private final ProjectMaintainerRepository projectMaintainerRepository;
    private final ClaimRepository claimRepository;
    private final CommentRepository commentRepository;
    private final ProjectQueryService projectQueryService;
    private final ClaimService claimService;
    private final CommentService commentService;

    public MaintainerActivityService(ProjectMaintainerRepository projectMaintainerRepository,
                                      ClaimRepository claimRepository,
                                      CommentRepository commentRepository,
                                      ProjectQueryService projectQueryService,
                                      ClaimService claimService,
                                      CommentService commentService) {
        this.projectMaintainerRepository = projectMaintainerRepository;
        this.claimRepository = claimRepository;
        this.commentRepository = commentRepository;
        this.projectQueryService = projectQueryService;
        this.claimService = claimService;
        this.commentService = commentService;
    }

    /**
     * {@code GET /users/me/maintainer-activity}: per project the caller maintains,
     * the projects' active claims, claims awaiting review, and recent comments.
     * A user maintaining zero projects gets {@code { projects: [] }}, not an error.
     *
     * @param caller the authenticated caller, injected via {@code @AuthenticatedUser}
     * @return the caller's maintainer activity rollup
     */
    @Transactional(readOnly = true)
    public MaintainerActivitySummary getActivity(User caller) {
        List<MaintainerProjectActivity> projects = projectMaintainerRepository.findByUserId(caller.getId()).stream()
                .map(ProjectMaintainer::getProject)
                .map(this::toActivity)
                .toList();

        return new MaintainerActivitySummary(projects);
    }

    private MaintainerProjectActivity toActivity(Project project) {
        List<ClaimDto> activeClaims = claimRepository
                .findByIssueProjectIdAndStatus(project.getId(), ClaimStatus.ACTIVE).stream()
                .map(claimService::toDto)
                .toList();

        List<ClaimDto> claimsAwaitingReview = claimRepository
                .findByIssueProjectIdAndStatusAndPullRequestState(
                        project.getId(), ClaimStatus.ACTIVE, PullRequestState.OPEN).stream()
                .map(claimService::toDto)
                .toList();

        List<CommentDto> recentComments = commentRepository
                .findRecentByProjectOrItsIssues(project.getId(), PageRequest.of(0, RECENT_COMMENTS_LIMIT)).stream()
                .map(commentService::toDto)
                .toList();

        List<CommentDto> unansweredQuestions = commentRepository
                .findByIssueProjectIdAndIsQuestionTrueAndResolvedAtIsNullAndDeletedAtIsNullOrderByCreatedAtAsc(project.getId())
                .stream()
                .map(commentService::toDto)
                .toList();

        return new MaintainerProjectActivity(
                projectQueryService.toDto(project), activeClaims, claimsAwaitingReview, recentComments, unansweredQuestions);
    }
}
