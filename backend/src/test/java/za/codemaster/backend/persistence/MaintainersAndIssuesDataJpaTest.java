package za.codemaster.backend.persistence;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import za.codemaster.backend.BackendApplication;
import za.codemaster.backend.domain.model.Issue;
import za.codemaster.backend.domain.model.Project;
import za.codemaster.backend.domain.model.ProjectMaintainer;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.repository.IssueRepository;
import za.codemaster.backend.repository.ProjectMaintainerRepository;
import za.codemaster.backend.repository.ProjectRepository;
import za.codemaster.backend.repository.UserRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
public class MaintainersAndIssuesDataJpaTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMaintainerRepository projectMaintainerRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Contract GH-2.3: Prove issue can be located by (project_id, github_issue_number)")
    void shouldFindIssueByProjectIdAndGithubIssueNumber() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        Project project = createAndSaveProject("repo-" + suffix);
        Integer githubIssueNumber = 42;

        Issue issue = new Issue();
        issue.setProject(project);
        issue.setGithubIssueId(11223344L);
        issue.setGithubIssueNumber(githubIssueNumber);
        issue.setGithubUrl("https://github.com/codemaster-org/repo-" + suffix + "/issues/42");
        issue.setTitle("Implement API Rate Limiting");
        issue.setBodyExcerpt("Rate limiting should apply to all public endpoints...");
        issue.setStatus("open");
        issue.setLabels(new String[]{"security", "enhancement"});
        issue.setDifficulty("intermediate");
        issue.setIsBeginnerFriendly(false);
        issue.setMaintainerResponseScore(new BigDecimal("91.50"));
        issue.setProjectHealthScore(new BigDecimal("84.00"));
        issue.setFreshnessScore(new BigDecimal("97.20"));
        issue.setContributionScore(new BigDecimal("89.10"));

        issueRepository.save(issue);
        entityManager.flush();
        entityManager.clear();

        // Contract Verification: GH-2.3 lookup
        Optional<Issue> issueOpt = issueRepository.findByProjectIdAndGithubIssueNumber(project.getId(), githubIssueNumber);
        assertTrue(issueOpt.isPresent(), "Issue must be found by project ID and GitHub issue number");

        Issue retrieved = issueOpt.get();
        assertEquals("Implement API Rate Limiting", retrieved.getTitle());
        assertEquals(githubIssueNumber, retrieved.getGithubIssueNumber());
        assertEquals("intermediate", retrieved.getDifficulty());
        assertNotNull(retrieved.getLabels());
        assertArrayEquals(
            new String[]{"enhancement", "security"},
            java.util.Arrays.stream(retrieved.getLabels()).sorted().toArray(String[]::new)
        );

        // API-01.5 Filter Verification
        Page<Issue> page = issueRepository.findWithFilters(
            project.getId(),
            "open",
            "intermediate",
            false,
            "security",
            PageRequest.of(0, 10)
        );
        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
    }

    @Test
    @DisplayName("Acceptance Criteria: Prove (project_id, user_id) unique constraint is enforced on ProjectMaintainer")
    void shouldEnforceUniqueConstraintOnProjectMaintainer() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        User user = createAndSaveUser("maintainer_" + suffix);
        Project project = createAndSaveProject("unique-repo-" + suffix);

        // 1. First maintainer assignment
        ProjectMaintainer maintainer1 = new ProjectMaintainer();
        maintainer1.setProject(project);
        maintainer1.setUser(user);
        maintainer1.setRole("maintainer");
        projectMaintainerRepository.save(maintainer1);
        entityManager.flush();

        // 2. Pre-check assertion (Service-layer 409 check verification)
        boolean exists = projectMaintainerRepository.existsByProjectIdAndUserId(project.getId(), user.getId());
        assertTrue(exists, "Repository pre-check must return true to prevent duplicate SQL execution and return 409");

        // 3. Database constraint fallback assertion
        ProjectMaintainer maintainer2 = new ProjectMaintainer();
        maintainer2.setProject(project);
        maintainer2.setUser(user);
        maintainer2.setRole("owner");

        assertThrows(DataIntegrityViolationException.class, () -> {
            projectMaintainerRepository.save(maintainer2);
            entityManager.flush();
        }, "Persisting a duplicate (project_id, user_id) pair must violate uniqueness constraint");
    }

    private User createAndSaveUser(String username) {
        User user = new User();
        user.setGithubId(System.nanoTime());
        user.setUsername(username);
        user.setDisplayName("Test Maintainer");
        user.setEmail(username + "@codemaster.za");
        return userRepository.save(user);
    }

    private Project createAndSaveProject(String repoName) {
        Project project = new Project();
        project.setGithubOwner("codemaster-org");
        project.setGithubRepo(repoName);
        project.setGithubUrl("https://github.com/codemaster-org/" + repoName);
        project.setName("Project " + repoName);
        project.setSlug(repoName);
        project.setPrimaryLanguage("Java");
        project.setCategory("Developer Tools");
        project.setConnection("south_african");
        project.setLicense("MIT");
        return projectRepository.save(project);
    }
}