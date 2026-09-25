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
import za.codemaster.backend.domain.model.ListingStatus;
import za.codemaster.backend.domain.model.Project;
import za.codemaster.backend.domain.model.ProjectMaintainer;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.repository.IssueRepository;
import za.codemaster.backend.repository.ProjectMaintainerRepository;
import za.codemaster.backend.repository.ProjectRepository;
import za.codemaster.backend.repository.UserRepository;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the Embeddable README Badge wow-feature's {@link BadgeService}
 * against a real (test) database, same pattern as {@code MaintainerServiceTest}.
 * Covers the three cases {@code BadgeService}'s Javadoc calls out: a
 * published project with maintainers, a project GitHub sync has never given
 * a maintainer (0, singular-vs-plural wording), and a slug that resolves to
 * nothing renderable (unknown, or real but not yet published) — all of
 * which must render a valid SVG, never throw, since a thrown exception here
 * would break a maintainer's live GitHub README rather than just an API call.
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
class BadgeServiceTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private ProjectMaintainerRepository projectMaintainerRepository;

    @Autowired
    private UserRepository userRepository;

    private BadgeService service;
    private ProjectQueryServiceFixtures fixtures;

    @BeforeEach
    void setUp() {
        service = new BadgeService(projectRepository, projectMaintainerRepository);
        fixtures = ProjectQueryServiceFixtures.seed(projectRepository, issueRepository);
    }

    @Test
    void rendersTheRealMaintainerCountForAPublishedProjectWithOneMaintainer() {
        Long projectId = fixtures.projectId(0);
        addMaintainer(projectId, newUser());

        String svg = service.renderMaintainerCountBadge(slugOf(projectId));

        assertTrue(svg.contains("1 maintainer"), "singular wording expected for a count of exactly 1: " + svg);
        assertFalse(svg.contains("1 maintainers"));
    }

    @Test
    void usesPluralWordingWhenAProjectHasMoreThanOneMaintainer() {
        Long projectId = fixtures.projectId(0);
        addMaintainer(projectId, newUser());
        addMaintainer(projectId, newUser());
        addMaintainer(projectId, newUser());

        String svg = service.renderMaintainerCountBadge(slugOf(projectId));

        assertTrue(svg.contains("3 maintainers"), svg);
    }

    @Test
    void rendersZeroMaintainersRatherThanErroringWhenGithubSyncHasNeverPopulatedAny() {
        Long projectId = fixtures.projectId(0);
        // Deliberately no addMaintainer call — mirrors a real, pre-existing seed case.

        String svg = service.renderMaintainerCountBadge(slugOf(projectId));

        assertTrue(svg.contains("0 maintainers"), svg);
    }

    @Test
    void rendersANotFoundBadgeForASlugThatDoesNotExistAtAllInsteadOfThrowing() {
        String svg = service.renderMaintainerCountBadge("no-such-project-" + UUID.randomUUID());

        assertTrue(svg.contains("not found"), svg);
    }

    @Test
    void rendersANotFoundBadgeForARealProjectThatIsNotYetPublished() {
        Project pending = new Project();
        pending.setGithubOwner("owner-" + UUID.randomUUID());
        pending.setGithubRepo("repo-" + UUID.randomUUID());
        pending.setGithubUrl("https://github.com/owner/repo-" + UUID.randomUUID());
        pending.setName("Pending Project");
        pending.setSlug("pending-project-" + UUID.randomUUID());
        pending.setCategory("Other");
        pending.setConnection("south_african");
        pending.setListingStatus(ListingStatus.PENDING);
        pending = projectRepository.save(pending);
        addMaintainer(pending.getId(), newUser());

        String svg = service.renderMaintainerCountBadge(pending.getSlug());

        assertTrue(svg.contains("not found"),
                "a pending project's slug must not leak its maintainer count before moderation approves it: " + svg);
    }

    private void addMaintainer(Long projectId, User user) {
        Project project = projectRepository.findById(projectId).orElseThrow();
        ProjectMaintainer relationship = new ProjectMaintainer();
        relationship.setProject(project);
        relationship.setUser(user);
        relationship.setRole("maintainer");
        projectMaintainerRepository.save(relationship);
    }

    private User newUser() {
        return userRepository.save(User.builder()
                .githubId(System.nanoTime())
                .username("user_" + System.nanoTime())
                .displayName("User")
                .build());
    }

    private String slugOf(Long projectId) {
        return projectRepository.findById(projectId).orElseThrow().getSlug();
    }
}
