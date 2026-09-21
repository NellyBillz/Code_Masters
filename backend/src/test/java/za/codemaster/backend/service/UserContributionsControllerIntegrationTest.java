package za.codemaster.backend.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import za.codemaster.backend.domain.model.Claim;
import za.codemaster.backend.domain.model.ClaimStatus;
import za.codemaster.backend.domain.model.CompletionSource;
import za.codemaster.backend.domain.model.Issue;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.repository.ClaimRepository;
import za.codemaster.backend.repository.IssueRepository;
import za.codemaster.backend.repository.ProjectRepository;
import za.codemaster.backend.repository.UserRepository;

import java.time.OffsetDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack acceptance tests for API-03.6 (round3-tickets.md), run through the real
 * {@code SecurityFilterChain} (this endpoint is public, no session needed). Covers
 * the ticket's own acceptance criteria verbatim: a user with 2 completed, 1 active,
 * 1 released claim shows exactly the 2 completed ones; an unknown username -> 404.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserContributionsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private ClaimRepository claimRepository;

    private User createUser() {
        String suffix = String.valueOf(System.nanoTime());
        return userRepository.save(User.builder()
                .githubId(System.nanoTime())
                .username("contribuser_" + suffix)
                .displayName("Contributor")
                .build());
    }

    private Issue seedIssue() {
        ProjectQueryServiceFixtures fixtures = ProjectQueryServiceFixtures.seed(projectRepository, issueRepository);
        return issueRepository.findById(fixtures.issueId(0)).orElseThrow();
    }

    private void claim(Issue issue, User user, ClaimStatus status) {
        Claim claim = new Claim();
        claim.setIssue(issue);
        claim.setUser(user);
        claim.setStatus(status);
        if (status == ClaimStatus.COMPLETED) {
            claim.setCompletionSource(CompletionSource.GITHUB_VERIFIED);
            claim.setCompletedAt(OffsetDateTime.now());
        }
        claimRepository.save(claim);
    }

    @Test
    @DisplayName("A user with 2 completed, 1 active, 1 released claim -> the list shows exactly the 2 completed ones")
    void showsExactlyTheCompletedClaims() throws Exception {
        User user = createUser();
        claim(seedIssue(), user, ClaimStatus.COMPLETED);
        claim(seedIssue(), user, ClaimStatus.COMPLETED);
        claim(seedIssue(), user, ClaimStatus.ACTIVE);
        claim(seedIssue(), user, ClaimStatus.RELEASED);

        mockMvc.perform(get("/api/v1/users/{username}/contributions", user.getUsername()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.meta.total").value(2))
                .andExpect(jsonPath("$.items[0].status").value("completed"))
                .andExpect(jsonPath("$.items[1].status").value("completed"));
    }

    @Test
    @DisplayName("Unknown username -> 404 USER_NOT_FOUND")
    void unknownUsernameIs404() throws Exception {
        mockMvc.perform(get("/api/v1/users/{username}/contributions", "no-such-user-xyz"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    @DisplayName("A contribution carries its issue, project, pull request url, and completion source")
    void contributionCarriesFullShape() throws Exception {
        User user = createUser();
        Issue issue = seedIssue();
        Claim claim = new Claim();
        claim.setIssue(issue);
        claim.setUser(user);
        claim.setStatus(ClaimStatus.COMPLETED);
        claim.setCompletionSource(CompletionSource.MAINTAINER_CONFIRMED);
        claim.setCompletedAt(OffsetDateTime.now());
        claim.setPullRequestUrl("https://github.com/example-org/repo/pull/9");
        claimRepository.save(claim);

        mockMvc.perform(get("/api/v1/users/{username}/contributions", user.getUsername()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].issue.id").value(issue.getId()))
                .andExpect(jsonPath("$.items[0].project.id").value(issue.getProject().getId()))
                .andExpect(jsonPath("$.items[0].pullRequestUrl").value("https://github.com/example-org/repo/pull/9"))
                .andExpect(jsonPath("$.items[0].completionSource").value("maintainer_confirmed"))
                .andExpect(jsonPath("$.items[0].completedAt").exists());
    }

    @Test
    @DisplayName("A user with no completed claims -> an empty list, not an error")
    void noCompletedClaimsIsAnEmptyList() throws Exception {
        User user = createUser();

        mockMvc.perform(get("/api/v1/users/{username}/contributions", user.getUsername()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.meta.total").value(0));
    }
}
