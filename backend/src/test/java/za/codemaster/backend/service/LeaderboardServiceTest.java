package za.codemaster.backend.service;

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
import za.codemaster.backend.domain.model.ClaimCollaborationRequest;
import za.codemaster.backend.domain.model.ClaimStatus;
import za.codemaster.backend.domain.model.CollaborationRequestStatus;
import za.codemaster.backend.domain.model.CompletionSource;
import za.codemaster.backend.domain.model.Issue;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.dto.leaderboard.LeaderboardEntry;
import za.codemaster.backend.repository.ClaimCollaborationRequestRepository;
import za.codemaster.backend.repository.ClaimRepository;
import za.codemaster.backend.repository.IssueRepository;
import za.codemaster.backend.repository.ProjectRepository;
import za.codemaster.backend.repository.UserRepository;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the Recognition Leaderboard's ranking rules (wow-feature,
 * 2026-09-24): more credited contributions ranks higher, an accepted
 * collaborator is credited the same as the claim owner, a user with zero
 * credited contributions never appears in the public list but still gets a
 * (null-rank) personalized result, and ties break deterministically on
 * username.
 * <p>
 * Runs against a real (test) Postgres database, same pattern as
 * {@code RecommendationServiceTest}.
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
class LeaderboardServiceTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClaimCollaborationRequestRepository claimCollaborationRequestRepository;

    private LeaderboardService service() {
        return new LeaderboardService(claimRepository);
    }

    private User newUser(String usernamePrefix) {
        return userRepository.save(User.builder()
                .githubId(System.nanoTime())
                .username(usernamePrefix + "_" + System.nanoTime())
                .displayName("Display " + usernamePrefix)
                .build());
    }

    private Issue seedIssue() {
        ProjectQueryServiceFixtures fixtures = ProjectQueryServiceFixtures.seed(projectRepository, issueRepository);
        return issueRepository.findById(fixtures.issueId(0)).orElseThrow();
    }

    private Claim completedClaim(Issue issue, User owner) {
        Claim claim = new Claim();
        claim.setIssue(issue);
        claim.setUser(owner);
        claim.setStatus(ClaimStatus.COMPLETED);
        claim.setCompletionSource(CompletionSource.GITHUB_VERIFIED);
        claim.setCompletedAt(OffsetDateTime.now());
        return claimRepository.save(claim);
    }

    @Test
    void ranksUsersByContributionCountDescending() {
        User twoContributions = newUser("leadertwo");
        completedClaim(seedIssue(), twoContributions);
        completedClaim(seedIssue(), twoContributions);

        User oneContribution = newUser("leaderone");
        completedClaim(seedIssue(), oneContribution);

        List<LeaderboardEntry> leaderboard = service().getLeaderboard();

        int twoIndex = indexOfUser(leaderboard, twoContributions.getId());
        int oneIndex = indexOfUser(leaderboard, oneContribution.getId());
        assertTrue(twoIndex >= 0 && oneIndex >= 0, "both users must appear on the leaderboard");
        assertTrue(twoIndex < oneIndex, "the user with more credited contributions must rank higher");
        assertEquals(2, leaderboard.get(twoIndex).contributionCount());
        assertEquals(1, leaderboard.get(oneIndex).contributionCount());
    }

    @Test
    void anAcceptedCollaboratorIsCreditedTheSameAsTheClaimOwner() {
        User owner = newUser("owner");
        User collaborator = newUser("collaborator");
        Claim claim = completedClaim(seedIssue(), owner);

        ClaimCollaborationRequest accepted = new ClaimCollaborationRequest();
        accepted.setClaim(claim);
        accepted.setRequester(collaborator);
        accepted.setStatus(CollaborationRequestStatus.ACCEPTED);
        claimCollaborationRequestRepository.save(accepted);

        List<LeaderboardEntry> leaderboard = service().getLeaderboard();

        assertTrue(indexOfUser(leaderboard, owner.getId()) >= 0, "the claim owner must be credited");
        assertTrue(indexOfUser(leaderboard, collaborator.getId()) >= 0, "the accepted collaborator must be credited too");
    }

    @Test
    void aUserWithZeroCreditedContributionsIsAbsentFromThePublicListButStillGetsAPersonalizedResult() {
        User neverContributed = newUser("neverranked");

        List<LeaderboardEntry> leaderboard = service().getLeaderboard();
        assertTrue(indexOfUser(leaderboard, neverContributed.getId()) < 0,
                "a user with zero credited contributions must never appear on the public leaderboard");

        LeaderboardEntry myRank = service().getCallerRank(neverContributed);
        assertNull(myRank.rank(), "rank must be null (not yet ranked), never a made-up placement");
        assertEquals(0, myRank.contributionCount());
        assertEquals(neverContributed.getUsername(), myRank.username());
    }

    @Test
    void getCallerRankFindsTheRealRankEvenOutsideThePublicTop20() {
        User contributor = newUser("realrank");
        completedClaim(seedIssue(), contributor);

        LeaderboardEntry myRank = service().getCallerRank(contributor);

        assertEquals(1, myRank.contributionCount());
        assertTrue(myRank.rank() != null && myRank.rank() > 0, "a credited contributor must have a real, positive rank");
    }

    private int indexOfUser(List<LeaderboardEntry> entries, Long userId) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).userId().equals(userId)) {
                return i;
            }
        }
        return -1;
    }
}
