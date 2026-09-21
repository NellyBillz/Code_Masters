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
import za.codemaster.backend.domain.model.CompletionSource;
import za.codemaster.backend.domain.model.Issue;
import za.codemaster.backend.domain.model.ListingStatus;
import za.codemaster.backend.domain.model.Project;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.dto.stats.PlatformStats;
import za.codemaster.backend.repository.ClaimRepository;
import za.codemaster.backend.repository.IssueRepository;
import za.codemaster.backend.repository.ProjectRepository;
import za.codemaster.backend.repository.UserRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies API-03.12's acceptance criteria (round3-tickets.md): approving a
 * pending project increments {@code publishedProjects}; a claim reaching
 * {@code completed} increments {@code totalContributionsCompleted} on the next
 * call; a pending project counts toward nothing.
 * <p>
 * Every assertion here compares a <em>before</em> and <em>after</em> snapshot
 * rather than an absolute count — the dev database already has committed seed
 * data (and other tests' committed side effects, if any), so an absolute
 * count would be coupled to whatever else happens to exist, not to the actual
 * behavior under test.
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
class StatsServiceTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private UserRepository userRepository;

    private StatsService service;

    @BeforeEach
    void setUp() {
        service = new StatsService(projectRepository, claimRepository);
    }

    private Project newProject(ListingStatus listingStatus, boolean acceptingContributions, List<String> countryCodes) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Project project = new Project();
        project.setGithubOwner("stats-org");
        project.setGithubRepo("repo-" + suffix);
        project.setGithubUrl("https://github.com/stats-org/repo-" + suffix);
        project.setName("Stats Project " + suffix);
        project.setSlug("stats-project-" + suffix);
        project.setCategory("Developer Tools");
        project.setConnection("south_african");
        project.setListingStatus(listingStatus);
        project.setAcceptingContributions(acceptingContributions);
        project.setCountryCodes(countryCodes);
        return projectRepository.save(project);
    }

    private User newUser() {
        return userRepository.save(User.builder()
                .githubId(System.nanoTime())
                .username("statsuser_" + System.nanoTime())
                .displayName("Stats User")
                .build());
    }

    private Issue newIssue(Project project) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        int issueNumber = (int) (System.nanoTime() % 1_000_000);
        Issue issue = new Issue();
        issue.setProject(project);
        issue.setGithubIssueNumber(issueNumber);
        issue.setGithubUrl(project.getGithubUrl() + "/issues/" + issueNumber + "-" + suffix);
        issue.setTitle("Stats issue " + suffix);
        issue.setStatus("open");
        return issueRepository.save(issue);
    }

    private Claim newClaim(Issue issue, User user, ClaimStatus status) {
        Claim claim = new Claim();
        claim.setIssue(issue);
        claim.setUser(user);
        claim.setStatus(status);
        if (status == ClaimStatus.COMPLETED) {
            claim.setCompletionSource(CompletionSource.GITHUB_VERIFIED);
            claim.setCompletedAt(OffsetDateTime.now());
        }
        return claimRepository.save(claim);
    }

    @Test
    void approvingAPendingProjectIncrementsPublishedProjects() {
        PlatformStats before = service.getStats();

        newProject(ListingStatus.PENDING, true, List.of("ZA"));
        assertEquals(before.publishedProjects(), service.getStats().publishedProjects(),
                "a pending project must not count toward publishedProjects");

        newProject(ListingStatus.PUBLISHED, true, List.of("ZA"));
        assertEquals(before.publishedProjects() + 1, service.getStats().publishedProjects());
    }

    @Test
    void aPendingProjectCountsTowardNothing() {
        PlatformStats before = service.getStats();

        // A pending project with a country code no other published project has,
        // and acceptingContributions true — none of it should move the needle.
        newProject(ListingStatus.PENDING, true, List.of("ZZ"));

        PlatformStats after = service.getStats();
        assertEquals(before.publishedProjects(), after.publishedProjects());
        assertEquals(before.activeProjectsAcceptingContributions(), after.activeProjectsAcceptingContributions());
        assertEquals(before.countriesRepresented(), after.countriesRepresented());
    }

    @Test
    void activeProjectsAcceptingContributionsOnlyCountsPublishedAndAccepting() {
        PlatformStats before = service.getStats();

        newProject(ListingStatus.PUBLISHED, false, List.of("ZA"));
        assertEquals(before.activeProjectsAcceptingContributions(), service.getStats().activeProjectsAcceptingContributions(),
                "published but not accepting contributions must not count");

        newProject(ListingStatus.PUBLISHED, true, List.of("ZA"));
        assertEquals(before.activeProjectsAcceptingContributions() + 1,
                service.getStats().activeProjectsAcceptingContributions());
    }

    @Test
    void countriesRepresentedOnlyCountsPublishedProjectsCountryCodes() {
        PlatformStats before = service.getStats();

        newProject(ListingStatus.PENDING, true, List.of("QQ"));
        assertEquals(before.countriesRepresented(), service.getStats().countriesRepresented(),
                "a country code that only exists on a pending project must not count");

        newProject(ListingStatus.PUBLISHED, true, List.of("QQ"));
        assertEquals(before.countriesRepresented() + 1, service.getStats().countriesRepresented());
    }

    @Test
    void totalActiveClaimsOnlyCountsActiveStatus() {
        PlatformStats before = service.getStats();
        Project project = newProject(ListingStatus.PUBLISHED, true, List.of("ZA"));
        Issue issue = newIssue(project);

        newClaim(issue, newUser(), ClaimStatus.RELEASED);
        assertEquals(before.totalActiveClaims(), service.getStats().totalActiveClaims(),
                "a released claim must not count toward totalActiveClaims");

        newClaim(issue, newUser(), ClaimStatus.ACTIVE);
        assertEquals(before.totalActiveClaims() + 1, service.getStats().totalActiveClaims());
    }

    @Test
    void aClaimReachingCompletedIncrementsTotalContributionsCompleted() {
        Project project = newProject(ListingStatus.PUBLISHED, true, List.of("ZA"));
        Issue issue = newIssue(project);
        User user = newUser();
        Claim claim = newClaim(issue, user, ClaimStatus.ACTIVE);

        PlatformStats before = service.getStats();

        claim.setStatus(ClaimStatus.COMPLETED);
        claim.setCompletionSource(CompletionSource.MAINTAINER_CONFIRMED);
        claim.setCompletedAt(OffsetDateTime.now());
        claimRepository.save(claim);

        PlatformStats after = service.getStats();
        assertEquals(before.totalContributionsCompleted() + 1, after.totalContributionsCompleted());
        // The claim is no longer active, so this should have moved too.
        assertEquals(before.totalActiveClaims() - 1, after.totalActiveClaims());
    }

    @Test
    void totalContributorsEngagedCountsDistinctUsersAcrossAnyClaimStatus() {
        Project project = newProject(ListingStatus.PUBLISHED, true, List.of("ZA"));
        Issue issue = newIssue(project);
        User userA = newUser();
        User userB = newUser();

        PlatformStats before = service.getStats();

        newClaim(issue, userA, ClaimStatus.ACTIVE);
        newClaim(issue, userA, ClaimStatus.RELEASED);
        assertEquals(before.totalContributorsEngaged() + 1, service.getStats().totalContributorsEngaged(),
                "the same user claiming twice must only count once");

        newClaim(newIssue(project), userB, ClaimStatus.COMPLETED);
        assertEquals(before.totalContributorsEngaged() + 2, service.getStats().totalContributorsEngaged());
    }

    @Test
    void generatedAtIsPresentAndRecent() {
        OffsetDateTime beforeCall = OffsetDateTime.now().minusSeconds(5);

        PlatformStats stats = service.getStats();

        assertNotNull(stats.generatedAt());
        assertTrue(stats.generatedAt().isAfter(beforeCall));
    }
}
