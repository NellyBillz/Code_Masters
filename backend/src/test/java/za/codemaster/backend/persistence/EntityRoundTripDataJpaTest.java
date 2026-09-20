package za.codemaster.backend.persistence;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import za.codemaster.backend.BackendApplication;
import za.codemaster.backend.domain.model.Claim;
import za.codemaster.backend.domain.model.ClaimStatus;
import za.codemaster.backend.domain.model.CompletionSource;
import za.codemaster.backend.domain.model.Issue;
import za.codemaster.backend.domain.model.ListingStatus;
import za.codemaster.backend.domain.model.Project;
import za.codemaster.backend.domain.model.PullRequestState;
import za.codemaster.backend.domain.model.SyncJob;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.repository.ClaimRepository;
import za.codemaster.backend.repository.IssueRepository;
import za.codemaster.backend.repository.ProjectMaintainerRepository;
import za.codemaster.backend.repository.ProjectRepository;
import za.codemaster.backend.repository.SyncJobRepository;
import za.codemaster.backend.repository.UserRepository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
    classes = BackendApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "spring.autoconfigure.exclude=" +
            "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration," +
            "org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration," +
            "org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration"
    }
)
@Transactional
public class EntityRoundTripDataJpaTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private SyncJobRepository syncJobRepository;

    @Autowired
    private ProjectMaintainerRepository projectMaintainerRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Round-trip test for new fields on User, Project, Claim, and SyncJob")
    void shouldRoundTripAllNewEntityFields() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        OffsetDateTime now = OffsetDateTime.now();

        // 1. User: verify isSiteAdmin
        User user = new User();
        user.setGithubId(System.nanoTime());
        user.setUsername("user_" + suffix);
        user.setDisplayName("Test User");
        // Lombok generates setIsSiteAdmin(Boolean) or setSiteAdmin(boolean)
        user.setSiteAdmin(true);
        User savedUser = userRepository.save(user);

        // 2. Project: verify hasContributingGuide, hasCodeOfConduct, listingStatus, acceptingContributions
        Project project = new Project();
        project.setGithubOwner("codemaster");
        project.setGithubRepo("roundtrip-" + suffix);
        project.setGithubUrl("https://github.com/codemaster/roundtrip-" + suffix);
        project.setName("Roundtrip Project");
        project.setSlug("roundtrip-" + suffix);
        project.setCategory("Developer Tools");
        project.setConnection("south_african");
        project.setLicense("MIT");
        project.setHasContributingGuide(true);
        project.setHasCodeOfConduct(true);
        project.setListingStatus(ListingStatus.PUBLISHED);
        project.setAcceptingContributions(false);
        Project savedProject = projectRepository.save(project);

        // 3. Issue (parent for Claim)
        Issue issue = new Issue();
        issue.setProject(savedProject);
        issue.setGithubIssueNumber(1);
        issue.setGithubUrl("https://github.com/codemaster/roundtrip-" + suffix + "/issues/1");
        issue.setTitle("Verify round-trip");
        issue.setStatus("open");
        Issue savedIssue = issueRepository.save(issue);

        // 4. Claim: verify pullRequestUrl, pullRequestState, maintainerFeedback, completionSource, completedAt
        Claim claim = new Claim();
        claim.setUser(savedUser);
        claim.setIssue(savedIssue);
        claim.setStatus(ClaimStatus.COMPLETED);
        claim.setPullRequestUrl("https://github.com/codemaster/roundtrip-" + suffix + "/pull/10");
        claim.setPullRequestState(PullRequestState.MERGED);
        claim.setMaintainerFeedback("LGTM! Verified by CI");
        claim.setCompletionSource(CompletionSource.GITHUB_VERIFIED);
        claim.setCompletedAt(now);
        Claim savedClaim = claimRepository.save(claim);

        // 5. SyncJob: verify contributionsVerifiedCount
        SyncJob syncJob = new SyncJob();
        syncJob.setProject(savedProject);
        syncJob.setStatus("completed");
        syncJob.setContributionsVerifiedCount(14);
        SyncJob savedSyncJob = syncJobRepository.save(syncJob);

        // Flush and clear cache to force database round-trip
        entityManager.flush();
        entityManager.clear();

        // ----------------- Assert User -----------------
        User fetchedUser = userRepository.findById(savedUser.getId()).orElseThrow();
        // Boolean accessor matches Lombok's getter
        assertTrue(fetchedUser.isSiteAdmin(), "isSiteAdmin must round-trip as true");

        // ----------------- Assert Project -----------------
        Project fetchedProject = projectRepository.findById(savedProject.getId()).orElseThrow();
        // boolean accessors matching Lombok generated isHas... / isAccepting... methods
        assertTrue(fetchedProject.isHasContributingGuide(), "hasContributingGuide must round-trip as true");
        assertTrue(fetchedProject.isHasCodeOfConduct(), "hasCodeOfConduct must round-trip as true");
        assertEquals(ListingStatus.PUBLISHED, fetchedProject.getListingStatus(), "listingStatus must round-trip as PUBLISHED");
        assertFalse(fetchedProject.isAcceptingContributions(), "acceptingContributions must round-trip as false");

        // Assert ProjectRepository findByIdAndListingStatus finder
        Optional<Project> publicLookup = projectRepository.findByIdAndListingStatus(savedProject.getId(), ListingStatus.PUBLISHED);
        assertTrue(publicLookup.isPresent(), "Published project must be retrievable by findByIdAndListingStatus");

        Optional<Project> pendingLookup = projectRepository.findByIdAndListingStatus(savedProject.getId(), ListingStatus.PENDING);
        assertFalse(pendingLookup.isPresent(), "Published project must not match PENDING search");

        // ----------------- Assert Claim -----------------
        Claim fetchedClaim = claimRepository.findById(savedClaim.getId()).orElseThrow();
        assertEquals("https://github.com/codemaster/roundtrip-" + suffix + "/pull/10", fetchedClaim.getPullRequestUrl());
        assertEquals(PullRequestState.MERGED, fetchedClaim.getPullRequestState());
        assertEquals("LGTM! Verified by CI", fetchedClaim.getMaintainerFeedback());
        assertEquals(CompletionSource.GITHUB_VERIFIED, fetchedClaim.getCompletionSource());
        assertNotNull(fetchedClaim.getCompletedAt());

        // ----------------- Assert SyncJob -----------------
        SyncJob fetchedSyncJob = syncJobRepository.findById(savedSyncJob.getId()).orElseThrow();
        assertEquals(14, fetchedSyncJob.getContributionsVerifiedCount());
    }
}