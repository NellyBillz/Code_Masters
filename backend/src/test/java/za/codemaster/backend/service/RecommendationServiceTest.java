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
import za.codemaster.backend.domain.model.ClaimStatus;
import za.codemaster.backend.domain.model.CompletionSource;
import za.codemaster.backend.domain.model.Issue;
import za.codemaster.backend.domain.model.ListingStatus;
import za.codemaster.backend.domain.model.Project;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.dto.recommendation.RecommendedIssue;
import za.codemaster.backend.repository.ClaimRepository;
import za.codemaster.backend.repository.IssueRepository;
import za.codemaster.backend.repository.ProjectRepository;
import za.codemaster.backend.repository.UserRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the Skill-Matching Recommendation Engine's scoring rules
 * (wow-feature, 2026-09-24): every recommendation carries a concrete reason,
 * recency alone never qualifies an issue, already-claimed issues never
 * appear, and closed/unpublished/non-accepting issues are never candidates.
 * <p>
 * Runs against a real (test) Postgres database, same pattern as
 * {@code StatsServiceTest}.
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
class RecommendationServiceTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private UserRepository userRepository;

    private User newUser(String... skills) {
        return userRepository.save(User.builder()
                .githubId(System.nanoTime())
                .username("recuser_" + System.nanoTime())
                .displayName("Rec User")
                .skills(skills)
                .build());
    }

    private Project newProject(String primaryLanguage, List<String> tags, boolean accepting, ListingStatus listingStatus) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Project project = new Project();
        project.setGithubOwner("rec-org");
        project.setGithubRepo("repo-" + suffix);
        project.setGithubUrl("https://github.com/rec-org/repo-" + suffix);
        project.setName("Rec Project " + suffix);
        project.setSlug("rec-project-" + suffix);
        project.setCategory("Developer Tools");
        project.setConnection("south_african");
        project.setPrimaryLanguage(primaryLanguage);
        project.setTags(tags);
        project.setListingStatus(listingStatus);
        project.setAcceptingContributions(accepting);
        return projectRepository.save(project);
    }

    private Issue newIssue(Project project, String status, boolean beginnerFriendly) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        int issueNumber = (int) (System.nanoTime() % 1_000_000);
        Issue issue = new Issue();
        issue.setProject(project);
        issue.setGithubIssueNumber(issueNumber);
        issue.setGithubUrl(project.getGithubUrl() + "/issues/" + issueNumber + "-" + suffix);
        issue.setTitle("Rec issue " + suffix);
        issue.setStatus(status);
        issue.setIsBeginnerFriendly(beginnerFriendly);
        return issueRepository.save(issue);
    }

    @Test
    void recommendsAnIssueOnAPrimaryLanguageMatchWithAReason() {
        RecommendationService recService = buildService();
        User user = newUser("Java");
        Project project = newProject("Java", List.of(), true, ListingStatus.PUBLISHED);
        Issue issue = newIssue(project, "open", false);

        List<RecommendedIssue> results = recService.getRecommendedIssues(user);

        RecommendedIssue match = results.stream().filter(r -> r.issue().id().equals(issue.getId())).findFirst()
                .orElseThrow(() -> new AssertionError("Expected the Java-matching issue to be recommended"));
        assertTrue(match.reasons().stream().anyMatch(reason -> reason.contains("Java")),
                "must carry a plain-language reason naming the matched skill");
    }

    @Test
    void anIssueWithNoMatchingSignalAtAllIsNeverRecommended() {
        RecommendationService recService = buildService();
        User user = newUser("Python");
        Project project = newProject("Java", List.of(), true, ListingStatus.PUBLISHED);
        Issue issue = newIssue(project, "open", false); // not beginner-friendly either, so nothing can match

        List<RecommendedIssue> results = recService.getRecommendedIssues(user);

        assertFalse(results.stream().anyMatch(r -> r.issue().id().equals(issue.getId())),
                "recency/existence alone must never qualify an issue with zero real match reasons");
    }

    @Test
    void beginnerBoostOnlyAppliesForACallerWithZeroPriorCreditedContributions() {
        RecommendationService recService = buildService();
        User newContributor = newUser("Python"); // matches nothing on language, relies purely on beginner boost
        Project project = newProject("Java", List.of(), true, ListingStatus.PUBLISHED);
        Issue beginnerIssue = newIssue(project, "open", true);

        List<RecommendedIssue> results = recService.getRecommendedIssues(newContributor);
        assertTrue(results.stream().anyMatch(r -> r.issue().id().equals(beginnerIssue.getId())),
                "a caller with zero prior contributions should get the beginner boost");

        // Give the same user a completed claim elsewhere, then re-check: the boost must no longer apply.
        Project otherProject = newProject("Go", List.of(), true, ListingStatus.PUBLISHED);
        Issue otherIssue = newIssue(otherProject, "open", false);
        Claim completed = new Claim();
        completed.setIssue(otherIssue);
        completed.setUser(newContributor);
        completed.setStatus(ClaimStatus.COMPLETED);
        completed.setCompletionSource(CompletionSource.GITHUB_VERIFIED);
        completed.setCompletedAt(OffsetDateTime.now());
        claimRepository.save(completed);

        List<RecommendedIssue> afterFirstContribution = recService.getRecommendedIssues(newContributor);
        assertFalse(afterFirstContribution.stream().anyMatch(r -> r.issue().id().equals(beginnerIssue.getId())),
                "the beginner boost must not apply once the caller has a credited contribution");
    }

    @Test
    void neverRecommendsAnIssueTheCallerAlreadyHoldsOrHeldAnyClaimOn() {
        RecommendationService recService = buildService();
        User user = newUser("Java");
        Project project = newProject("Java", List.of(), true, ListingStatus.PUBLISHED);
        Issue issue = newIssue(project, "open", false);

        Claim released = new Claim();
        released.setIssue(issue);
        released.setUser(user);
        released.setStatus(ClaimStatus.RELEASED);
        claimRepository.save(released);

        List<RecommendedIssue> results = recService.getRecommendedIssues(user);

        assertFalse(results.stream().anyMatch(r -> r.issue().id().equals(issue.getId())),
                "an issue the caller already dealt with (any claim status) must never be recommended again");
    }

    @Test
    void closedIssuesAndNonAcceptingOrUnpublishedProjectsAreNeverCandidates() {
        RecommendationService recService = buildService();
        User user = newUser("Java");

        Project publishedAccepting = newProject("Java", List.of(), true, ListingStatus.PUBLISHED);
        Issue closedIssue = newIssue(publishedAccepting, "closed", false);

        Project notAccepting = newProject("Java", List.of(), false, ListingStatus.PUBLISHED);
        Issue issueOnNonAccepting = newIssue(notAccepting, "open", false);

        Project unpublished = newProject("Java", List.of(), true, ListingStatus.PENDING);
        Issue issueOnUnpublished = newIssue(unpublished, "open", false);

        List<RecommendedIssue> results = recService.getRecommendedIssues(user);
        List<Long> resultIds = results.stream().map(r -> r.issue().id()).toList();

        assertFalse(resultIds.contains(closedIssue.getId()));
        assertFalse(resultIds.contains(issueOnNonAccepting.getId()));
        assertFalse(resultIds.contains(issueOnUnpublished.getId()));
    }

    @Test
    void aCallerWithNoMatchingSkillsAndPriorContributionsGetsAnEmptyListRatherThanInventedPicks() {
        RecommendationService recService = buildService();
        User user = newUser(); // no skills at all
        Project unrelatedProject = newProject("Java", List.of(), true, ListingStatus.PUBLISHED);
        newIssue(unrelatedProject, "open", false); // not beginner-friendly, no skill overlap possible

        // Give the user a completed claim so the beginner boost can never apply either.
        Project priorProject = newProject("Go", List.of(), true, ListingStatus.PUBLISHED);
        Issue priorIssue = newIssue(priorProject, "open", false);
        Claim completed = new Claim();
        completed.setIssue(priorIssue);
        completed.setUser(user);
        completed.setStatus(ClaimStatus.COMPLETED);
        completed.setCompletionSource(CompletionSource.GITHUB_VERIFIED);
        completed.setCompletedAt(OffsetDateTime.now());
        claimRepository.save(completed);

        List<RecommendedIssue> results = recService.getRecommendedIssues(user);

        assertTrue(results.isEmpty(),
                "no skills, no beginner boost eligibility — nothing has a real reason to recommend, so the list must be empty, not padded with guesses");
    }

    private RecommendationService buildService() {
        ProjectQueryService projectQueryService =
                new ProjectQueryService(projectRepository, issueRepository, claimRepository, null);
        return new RecommendationService(issueRepository, claimRepository, projectQueryService);
    }
}
