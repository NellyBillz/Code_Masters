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
import za.codemaster.backend.domain.model.Issue;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.dto.issue.IssueDto;
import za.codemaster.backend.repository.ClaimRepository;
import za.codemaster.backend.repository.IssueRepository;
import za.codemaster.backend.repository.ProjectMaintainerRepository;
import za.codemaster.backend.repository.ProjectRepository;
import za.codemaster.backend.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies API-03.5's {@code Issue.claimCount} acceptance criterion (round3-tickets.md):
 * a claim moved to {@code changes_requested} still counts toward {@code claimCount};
 * one moved to {@code completed} or {@code released} does not.
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
class ClaimCountSemanticsTest {

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

    private ProjectQueryService service;
    private Issue issue;

    @BeforeEach
    void setUp() {
        service = new ProjectQueryService(projectRepository, issueRepository, claimRepository, projectMaintainerRepository);
        ProjectQueryServiceFixtures fixtures = ProjectQueryServiceFixtures.seed(projectRepository, issueRepository);
        issue = issueRepository.findById(fixtures.issueId(0)).orElseThrow();
    }

    private User newUser() {
        return userRepository.save(User.builder()
                .githubId(System.nanoTime())
                .username("claimcountuser_" + System.nanoTime())
                .displayName("Claim Count User")
                .build());
    }

    private void claim(ClaimStatus status) {
        Claim claim = new Claim();
        claim.setIssue(issue);
        claim.setUser(newUser());
        claim.setStatus(status);
        claimRepository.save(claim);
    }

    private int claimCount() {
        IssueDto dto = service.toDto(issue);
        return dto.claimCount();
    }

    @Test
    void freshIssueHasZeroClaimCount() {
        assertEquals(0, claimCount());
    }

    @Test
    void activeClaimCounts() {
        claim(ClaimStatus.ACTIVE);
        assertEquals(1, claimCount());
    }

    @Test
    void changesRequestedClaimStillCounts() {
        claim(ClaimStatus.CHANGES_REQUESTED);
        assertEquals(1, claimCount());
    }

    @Test
    void completedClaimDoesNotCount() {
        claim(ClaimStatus.COMPLETED);
        assertEquals(0, claimCount());
    }

    @Test
    void releasedClaimDoesNotCount() {
        claim(ClaimStatus.RELEASED);
        assertEquals(0, claimCount());
    }

    @Test
    void onlyInFlightClaimsAreCountedAmongAMixOfStatuses() {
        claim(ClaimStatus.ACTIVE);
        claim(ClaimStatus.CHANGES_REQUESTED);
        claim(ClaimStatus.COMPLETED);
        claim(ClaimStatus.RELEASED);

        assertEquals(2, claimCount(), "only the active and changes_requested claims should count");
    }
}
