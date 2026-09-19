package za.codemaster.backend.persistence;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import za.codemaster.backend.BackendApplication;
import za.codemaster.backend.domain.model.Project;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.repository.ProjectRepository;
import za.codemaster.backend.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
public class EntityMappingDataJpaTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("JPA Mapping: Save User and Project with array collections and read both back intact")
    void shouldPersistAndRetrieveUserAndProjectWithCollections() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        // -----------------------------------------------------------------
        // 1. Persist User Entity
        // -----------------------------------------------------------------
        User user = new User();
        user.setGithubId(System.nanoTime());
        user.setUsername("dev_" + suffix);
        user.setDisplayName("Mzansi Coder");
        user.setEmail("dev_" + suffix + "@codemaster.za");
        user.setBio("Full-stack engineer based in Johannesburg");
        user.setLocation("Johannesburg, South Africa");
        user.setSkills(new String[]{"Java", "Spring Boot", "PostgreSQL"});
        user.setReputation(10);

        User savedUser = userRepository.save(user);
        assertNotNull(savedUser.getId(), "User ID must be generated upon persistence");

        // -----------------------------------------------------------------
        // 2. Persist Project Entity Matching Target Schema
        // -----------------------------------------------------------------
        Project project = new Project();
        project.setGithubOwner("codemaster");
        project.setGithubRepo("core-api-" + suffix);
        project.setGithubUrl("https://github.com/codemaster/core-api-" + suffix);
        project.setName("Core API " + suffix);
        project.setSlug("core-api-" + suffix);
        project.setDescription("Core registry services and API platform");
        project.setPrimaryLanguage("Java");
        project.setLanguages(new String[]{"Java", "SQL", "Shell"});
        project.setCategory("Developer Tools");
        project.setConnection("south_african");
        project.setLicense("MIT");
        project.setStars(45);
        project.setForks(12);
        project.setOpenIssues(3);
        project.setContributors(5);
        project.setHasBeginnerFriendlyIssues(true);
        project.setVerified(true);

        Project savedProject = projectRepository.save(project);
        assertNotNull(savedProject.getId(), "Project ID must be generated upon persistence");

        // -----------------------------------------------------------------
        // 3. Flush & Clear Hibernate Persistence Context (Force DB Roundtrip)
        // -----------------------------------------------------------------
        entityManager.flush();
        entityManager.clear();

        // -----------------------------------------------------------------
        // 4. Retrieve & Assert User
        // -----------------------------------------------------------------
        Optional<User> fetchedUserOpt = userRepository.findByGithubId(savedUser.getGithubId());
        assertTrue(fetchedUserOpt.isPresent(), "Persisted User must be retrievable");
        User retrievedUser = fetchedUserOpt.get();

        assertEquals("Mzansi Coder", retrievedUser.getDisplayName());
        assertEquals("dev_" + suffix, retrievedUser.getUsername());
        assertNotNull(retrievedUser.getSkills());
        assertArrayEquals(new String[]{"Java", "Spring Boot", "PostgreSQL"}, retrievedUser.getSkills());

        // -----------------------------------------------------------------
        // 5. Retrieve & Assert Project with Arrays and Metadata Intact
        // -----------------------------------------------------------------
        Optional<Project> fetchedProjectOpt = projectRepository.findBySlug("core-api-" + suffix);
        assertTrue(fetchedProjectOpt.isPresent(), "Persisted Project must be retrievable by slug");
        Project retrievedProject = fetchedProjectOpt.get();

        assertEquals("Core API " + suffix, retrievedProject.getName());
        assertEquals("Java", retrievedProject.getPrimaryLanguage());
        assertEquals("Developer Tools", retrievedProject.getCategory());
        assertEquals("south_african", retrievedProject.getConnection());
        assertEquals("MIT", retrievedProject.getLicense());
        assertEquals(45, retrievedProject.getStars());
        assertEquals(12, retrievedProject.getForks());
        assertEquals(3, retrievedProject.getOpenIssues());
        assertEquals(5, retrievedProject.getContributors());
        assertTrue(retrievedProject.getHasBeginnerFriendlyIssues());
        assertTrue(retrievedProject.getVerified());

        // Assert Postgres text[] array mapping integrity
        assertNotNull(retrievedProject.getLanguages());
        assertArrayEquals(new String[]{"Java", "SQL", "Shell"},
            java.util.Arrays.stream(retrievedProject.getLanguages()).sorted().toArray(String[]::new));

        // -----------------------------------------------------------------
        // 6. Contract Verification: ProjectRepository.findWithFilters
        // -----------------------------------------------------------------
        Page<Project> page = projectRepository.findWithFilters(
            "Java",
            "Developer Tools",
            "south_african",
            true,
            PageRequest.of(0, 10)
        );

        assertNotNull(page, "Repository page must not be null");
        assertTrue(page.getTotalElements() >= 1, "Filter query must match the persisted project");
        assertTrue(page.getContent().stream().anyMatch(p -> p.getSlug().equals("core-api-" + suffix)));
    }
}