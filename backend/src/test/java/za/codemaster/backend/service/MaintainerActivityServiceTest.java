package za.codemaster.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import za.codemaster.backend.BackendApplication;
import za.codemaster.backend.domain.model.Claim;
import za.codemaster.backend.domain.model.ClaimStatus;
import za.codemaster.backend.domain.model.Comment;
import za.codemaster.backend.domain.model.Issue;
import za.codemaster.backend.domain.model.Project;
import za.codemaster.backend.domain.model.ProjectMaintainer;
import za.codemaster.backend.domain.model.PullRequestState;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.dto.user.MaintainerActivitySummary;
import za.codemaster.backend.dto.user.MaintainerProjectActivity;
import za.codemaster.backend.repository.ClaimCollaborationRequestRepository;
import za.codemaster.backend.repository.ClaimRepository;
import za.codemaster.backend.repository.CommentRepository;
import za.codemaster.backend.repository.IssueRepository;
import za.codemaster.backend.repository.ProjectMaintainerRepository;
import za.codemaster.backend.repository.ProjectRepository;
import za.codemaster.backend.repository.UserRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies API-03.7's acceptance criteria (round3-tickets.md): a user maintaining
 * zero projects gets {@code { projects: [] }}, not an error; a claim with a PR
 * attached appears in {@code claimsAwaitingReview} until reviewed, then disappears
 * from that specific list while still appearing elsewhere per its new status.
 * <p>
 * Runs against a real (test) Postgres database, same pattern as every other
 * service test in this package.
 */
@SpringBootTest(
    classes = BackendApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@EnableAutoConfiguration(exclude = {
    SecurityAutoConfiguration.class,
    OAuth2ResourceServerAutoConfiguration.class,
    OAuth2ClientAutoConfiguration.class
})
@Transactional
class MaintainerActivityServiceTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ProjectMaintainerRepository projectMaintainerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClaimCollaborationRequestRepository claimCollaborationRequestRepository;

    private MaintainerActivityService service;
    private ClaimService claimService;
    private CommentService commentService;
    private ProjectQueryService projectQueryService;

    @BeforeEach
    void setUp() {
        projectQueryService = new ProjectQueryService(projectRepository, issueRepository, claimRepository, projectMaintainerRepository);
        // A generous limit — this class isn't testing API-03.10's rate limiting.
        RateLimitService unlimitedRateLimitService = new RateLimitService(1_000_000, 1_000_000, 1_000_000, 1_000_000);
        claimService = new ClaimService(claimRepository, issueRepository, projectMaintainerRepository, unlimitedRateLimitService,
                claimCollaborationRequestRepository);
        commentService = new CommentService(commentRepository, projectRepository, issueRepository, projectMaintainerRepository,
                claimRepository, unlimitedRateLimitService);
        service = new MaintainerActivityService(
                projectMaintainerRepository, claimRepository, commentRepository, projectQueryService, claimService, commentService);
    }

    private User newUser(String prefix) {
        return userRepository.save(User.builder()
                .githubId(System.nanoTime())
                .username(prefix + "_" + System.nanoTime())
                .displayName(prefix)
                .build());
    }

    private void addMaintainer(Project project, User user) {
        ProjectMaintainer relationship = new ProjectMaintainer();
        relationship.setProject(project);
        relationship.setUser(user);
        relationship.setRole("maintainer");
        projectMaintainerRepository.save(relationship);
    }

    private Claim claim(Issue issue, User user, ClaimStatus status, PullRequestState pullRequestState) {
        Claim claim = new Claim();
        claim.setIssue(issue);
        claim.setUser(user);
        claim.setStatus(status);
        claim.setPullRequestState(pullRequestState);
        return claimRepository.save(claim);
    }

    private void projectComment(Project project, User author, String body) {
        Comment comment = new Comment();
        comment.setProject(project);
        comment.setUser(author);
        comment.setBody(body);
        commentRepository.save(comment);
    }

    @Test
    void userMaintainingZeroProjectsGetsEmptyProjectsListNotAnError() {
        User user = newUser("nomaintainer");

        MaintainerActivitySummary summary = service.getActivity(user);

        assertTrue(summary.projects().isEmpty());
    }

    @Test
    void claimWithAttachedPrAppearsInClaimsAwaitingReviewUntilReviewed() {
        ProjectQueryServiceFixtures fixtures = ProjectQueryServiceFixtures.seed(projectRepository, issueRepository);
        Issue issue = issueRepository.findById(fixtures.issueId(0)).orElseThrow();
        Project project = issue.getProject();
        User maintainer = newUser("maintainer");
        addMaintainer(project, maintainer);
        User contributor = newUser("contributor");

        Claim awaitingReview = claim(issue, contributor, ClaimStatus.ACTIVE, PullRequestState.OPEN);

        MaintainerProjectActivity before = service.getActivity(maintainer).projects().get(0);
        assertEquals(1, before.claimsAwaitingReview().size());
        assertEquals(awaitingReview.getId(), before.claimsAwaitingReview().get(0).id());

        // Reviewed: maintainer requests changes -> no longer awaiting review,
        // but the claim itself still exists with its new status.
        awaitingReview.setStatus(ClaimStatus.CHANGES_REQUESTED);
        claimRepository.save(awaitingReview);

        MaintainerProjectActivity after = service.getActivity(maintainer).projects().get(0);
        assertTrue(after.claimsAwaitingReview().isEmpty(),
                "a reviewed claim must disappear from claimsAwaitingReview");
    }

    @Test
    void activeClaimsListsOnlyActiveStatusClaims() {
        ProjectQueryServiceFixtures fixtures = ProjectQueryServiceFixtures.seed(projectRepository, issueRepository);
        Issue issue = issueRepository.findById(fixtures.issueId(0)).orElseThrow();
        Project project = issue.getProject();
        User maintainer = newUser("maintainer");
        addMaintainer(project, maintainer);
        User contributorA = newUser("contribA");
        User contributorB = newUser("contribB");

        claim(issue, contributorA, ClaimStatus.ACTIVE, PullRequestState.NONE);
        claim(issue, contributorB, ClaimStatus.RELEASED, PullRequestState.NONE);

        MaintainerProjectActivity activity = service.getActivity(maintainer).projects().get(0);

        assertEquals(1, activity.activeClaims().size());
        assertEquals(contributorA.getUsername(), activity.activeClaims().get(0).user().username());
    }

    @Test
    void claimsAwaitingReviewRequiresBothActiveStatusAndOpenPullRequestState() {
        ProjectQueryServiceFixtures fixtures = ProjectQueryServiceFixtures.seed(projectRepository, issueRepository);
        Issue issue = issueRepository.findById(fixtures.issueId(0)).orElseThrow();
        Project project = issue.getProject();
        User maintainer = newUser("maintainer");
        addMaintainer(project, maintainer);

        // active but no PR attached -> not awaiting review
        claim(issue, newUser("noPr"), ClaimStatus.ACTIVE, PullRequestState.NONE);
        // PR attached but changes_requested (already reviewed) -> not awaiting review
        claim(issue, newUser("alreadyReviewed"), ClaimStatus.CHANGES_REQUESTED, PullRequestState.OPEN);
        // active with PR attached -> awaiting review
        Claim genuine = claim(issue, newUser("genuine"), ClaimStatus.ACTIVE, PullRequestState.OPEN);

        MaintainerProjectActivity activity = service.getActivity(maintainer).projects().get(0);

        assertEquals(1, activity.claimsAwaitingReview().size());
        assertEquals(genuine.getId(), activity.claimsAwaitingReview().get(0).id());
    }

    @Test
    void recentCommentsIncludesBothProjectAndIssueAttachedComments() {
        ProjectQueryServiceFixtures fixtures = ProjectQueryServiceFixtures.seed(projectRepository, issueRepository);
        Issue issue = issueRepository.findById(fixtures.issueId(0)).orElseThrow();
        Project project = issue.getProject();
        User maintainer = newUser("maintainer");
        addMaintainer(project, maintainer);
        User commenter = newUser("commenter");

        projectComment(project, commenter, "A project-level comment");
        Comment issueComment = new Comment();
        issueComment.setIssue(issue);
        issueComment.setUser(commenter);
        issueComment.setBody("An issue-level comment");
        commentRepository.save(issueComment);

        MaintainerProjectActivity activity = service.getActivity(maintainer).projects().get(0);

        assertEquals(2, activity.recentComments().size());
    }

    @Test
    void rollupCoversEveryProjectTheUserMaintains() {
        ProjectQueryServiceFixtures fixtures = ProjectQueryServiceFixtures.seed(projectRepository, issueRepository);
        Project projectA = issueRepository.findById(fixtures.issueId(0)).orElseThrow().getProject();
        Project projectB = issueRepository.findById(fixtures.issueId(2)).orElseThrow().getProject();
        User maintainer = newUser("multimaintainer");
        addMaintainer(projectA, maintainer);
        addMaintainer(projectB, maintainer);

        MaintainerActivitySummary summary = service.getActivity(maintainer);

        assertEquals(2, summary.projects().size());
        List<Long> maintainedProjectIds = summary.projects().stream().map(p -> p.project().id()).toList();
        assertTrue(maintainedProjectIds.contains(projectA.getId()));
        assertTrue(maintainedProjectIds.contains(projectB.getId()));
    }
}
