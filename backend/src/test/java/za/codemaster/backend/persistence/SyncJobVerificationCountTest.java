package za.codemaster.backend.persistence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import za.codemaster.backend.BackendApplication;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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
public class SyncJobVerificationCountTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long testProjectId;
    private UUID testJobIdWithCount;
    private UUID testJobIdWithNull;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        testProjectId = jdbcTemplate.queryForObject(
            "INSERT INTO projects (github_owner, github_repo, github_url, name, slug, connection) " +
            "VALUES (?, ?, ?, ?, ?, ?) RETURNING id",
            Long.class,
            "codemaster",
            "v9-repo-" + suffix,
            "https://github.com/codemaster/v9-repo-" + suffix,
            "V9 Project",
            "slug-v9-" + suffix,
            "south_african"
        );
    }

    @AfterEach
    void tearDown() {
        if (testProjectId != null) {
            jdbcTemplate.update("DELETE FROM sync_jobs WHERE project_id = ?", testProjectId);
            jdbcTemplate.update("DELETE FROM projects WHERE id = ?", testProjectId);
        }
    }

    @Test
    @DisplayName("Acceptance Criteria: V9 migration allows nullable contributions_verified_count on sync_jobs")
    void shouldPersistAndRetrieveContributionsVerifiedCount() {
        // 1. Insert row with explicit contributions_verified_count
        testJobIdWithCount = jdbcTemplate.queryForObject(
            "INSERT INTO sync_jobs (project_id, status, contributions_verified_count) " +
            "VALUES (?, ?, ?) RETURNING id",
            UUID.class,
            testProjectId,
            "completed",
            5
        );

        // 2. Insert row with null contributions_verified_count
        testJobIdWithNull = jdbcTemplate.queryForObject(
            "INSERT INTO sync_jobs (project_id, status, contributions_verified_count) " +
            "VALUES (?, ?, ?) RETURNING id",
            UUID.class,
            testProjectId,
            "accepted",
            null
        );

        assertNotNull(testJobIdWithCount);
        assertNotNull(testJobIdWithNull);

        // Verify explicit value
        Map<String, Object> jobWithCount = jdbcTemplate.queryForMap(
            "SELECT status, contributions_verified_count FROM sync_jobs WHERE id = ?",
            testJobIdWithCount
        );
        assertEquals("completed", jobWithCount.get("status"));
        assertEquals(5, ((Number) jobWithCount.get("contributions_verified_count")).intValue());

        // Verify null value
        Map<String, Object> jobWithNull = jdbcTemplate.queryForMap(
            "SELECT status, contributions_verified_count FROM sync_jobs WHERE id = ?",
            testJobIdWithNull
        );
        assertEquals("accepted", jobWithNull.get("status"));
        assertNull(jobWithNull.get("contributions_verified_count"));
    }
}