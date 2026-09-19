package za.codemaster.backend.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import za.codemaster.backend.BackendApplication;
import za.codemaster.backend.domain.model.Project;
import za.codemaster.backend.domain.model.SyncJob;
import za.codemaster.backend.repository.ProjectRepository;
import za.codemaster.backend.repository.SyncJobRepository;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
public class SyncJobDataJpaTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private SyncJobRepository syncJobRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("SyncJob Acceptance: Verify a SyncJob row round-trips with its UUID intact")
    void shouldPersistAndRetrieveSyncJobWithUuidIntact() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        // 1. Create and persist parent Project so projectId satisfies foreign key fk_sync_jobs_projects
        Project project = new Project();
        project.setGithubOwner("codemaster");
        project.setGithubRepo("sync-target-" + suffix);
        project.setGithubUrl("https://github.com/codemaster/sync-target-" + suffix);
        project.setName("Sync Target " + suffix);
        project.setSlug("sync-target-" + suffix);
        project.setPrimaryLanguage("Java");
        project.setCategory("Developer Tools");
        project.setConnection("south_african");
        project.setLicense("MIT");
        Project savedProject = projectRepository.save(project);
        assertNotNull(savedProject.getId());

        // 2. Instantiate SyncJob using constructor with projectId
        SyncJob job = new SyncJob(savedProject.getId());
        job.running();

        SyncJob savedJob = syncJobRepository.save(job);
        UUID generatedJobId = savedJob.getId();
        assertNotNull(generatedJobId, "UUID primary key must be assigned upon persistence");

        // 3. Flush and clear persistence context to force a clean DB read
        entityManager.flush();
        entityManager.clear();

        // 4. Retrieve back using findById and assert state
        Optional<SyncJob> fetchedJobOpt = syncJobRepository.findById(generatedJobId);
        assertTrue(fetchedJobOpt.isPresent(), "Job must be retrievable by its generated UUID");

        SyncJob retrievedJob = fetchedJobOpt.get();
        assertEquals(generatedJobId, retrievedJob.getId(), "UUID must remain intact across roundtrip");
        assertEquals("running", retrievedJob.getStatus());
        assertEquals(savedProject.getId(), retrievedJob.getProjectId());
        assertNotNull(retrievedJob.getStartedAt());
        assertNull(retrievedJob.getCompletedAt());
        assertEquals(0, retrievedJob.getIssuesCreatedCount());
        assertEquals(0, retrievedJob.getIssuesUpdatedCount());

        // 5. Verify state transition and completed status persistence
        retrievedJob.completed(5, 2);
        syncJobRepository.save(retrievedJob);
        entityManager.flush();
        entityManager.clear();

        SyncJob completedJob = syncJobRepository.findById(generatedJobId).orElseThrow();
        assertEquals("completed", completedJob.getStatus());
        assertNotNull(completedJob.getCompletedAt());
        assertEquals(5, completedJob.getIssuesCreatedCount());
        assertEquals(2, completedJob.getIssuesUpdatedCount());
    }
}