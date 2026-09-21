package za.codemaster.backend.service;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import za.codemaster.backend.domain.model.Claim;
import za.codemaster.backend.domain.model.ClaimStatus;
import za.codemaster.backend.domain.model.Issue;
import za.codemaster.backend.domain.model.Project;
import za.codemaster.backend.domain.model.ProjectMaintainer;
import za.codemaster.backend.domain.model.PullRequestState;
import za.codemaster.backend.domain.model.Session;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.repository.ClaimRepository;
import za.codemaster.backend.repository.IssueRepository;
import za.codemaster.backend.repository.ProjectMaintainerRepository;
import za.codemaster.backend.repository.ProjectRepository;
import za.codemaster.backend.repository.SessionRepository;
import za.codemaster.backend.repository.UserRepository;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack acceptance tests for API-03.7 (round3-tickets.md), run through the real
 * {@code SecurityFilterChain}. Covers the ticket's own acceptance criteria verbatim:
 * a user maintaining zero projects gets {@code { projects: [] }}, not an error, and a
 * claim with a PR attached appears in {@code claimsAwaitingReview} until reviewed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MaintainerActivityControllerIntegrationTest {

    private static final String SESSION_COOKIE = "CODEMASTERS_SESSION";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private ProjectMaintainerRepository projectMaintainerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionRepository sessionRepository;

    private Session createSession() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User user = userRepository.save(User.builder()
                .githubId(System.nanoTime())
                .username("activityuser_" + suffix)
                .displayName("Activity User")
                .build());

        return sessionRepository.save(Session.builder()
                .user(user)
                .csrToken("csrf-" + suffix)
                .expiresAt(OffsetDateTime.now().plusHours(1))
                .build());
    }

    private void addMaintainer(Project project, User user) {
        ProjectMaintainer relationship = new ProjectMaintainer();
        relationship.setProject(project);
        relationship.setUser(user);
        relationship.setRole("maintainer");
        projectMaintainerRepository.save(relationship);
    }

    @Test
    @DisplayName("GET /users/me/maintainer-activity while unauthenticated -> 401")
    void unauthenticatedIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/maintainer-activity"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("A user maintaining zero projects -> { projects: [] }, not an error")
    void userMaintainingZeroProjectsGetsEmptyList() throws Exception {
        Session session = createSession();

        mockMvc.perform(get("/api/v1/users/me/maintainer-activity")
                        .cookie(new Cookie(SESSION_COOKIE, session.getId().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projects.length()").value(0));
    }

    @Test
    @DisplayName("A claim with a PR attached appears in claimsAwaitingReview until reviewed")
    void claimAwaitingReviewDisappearsOnceReviewed() throws Exception {
        Session maintainerSession = createSession();
        ProjectQueryServiceFixtures fixtures = ProjectQueryServiceFixtures.seed(projectRepository, issueRepository);
        Issue issue = issueRepository.findById(fixtures.issueId(0)).orElseThrow();
        addMaintainer(issue.getProject(), maintainerSession.getUser());

        User contributor = userRepository.save(User.builder()
                .githubId(System.nanoTime())
                .username("contrib_" + UUID.randomUUID().toString().substring(0, 8))
                .displayName("Contributor")
                .build());
        Claim claim = new Claim();
        claim.setIssue(issue);
        claim.setUser(contributor);
        claim.setStatus(ClaimStatus.ACTIVE);
        claim.setPullRequestState(PullRequestState.OPEN);
        claimRepository.save(claim);

        mockMvc.perform(get("/api/v1/users/me/maintainer-activity")
                        .cookie(new Cookie(SESSION_COOKIE, maintainerSession.getId().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projects.length()").value(1))
                .andExpect(jsonPath("$.projects[0].claimsAwaitingReview.length()").value(1))
                .andExpect(jsonPath("$.projects[0].claimsAwaitingReview[0].id").value(claim.getId()));

        claim.setStatus(ClaimStatus.CHANGES_REQUESTED);
        claimRepository.save(claim);

        mockMvc.perform(get("/api/v1/users/me/maintainer-activity")
                        .cookie(new Cookie(SESSION_COOKIE, maintainerSession.getId().toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projects[0].claimsAwaitingReview.length()").value(0));
    }
}
