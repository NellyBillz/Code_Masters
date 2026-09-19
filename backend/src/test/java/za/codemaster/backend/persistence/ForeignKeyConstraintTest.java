package za.codemaster.backend.persistence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import za.codemaster.backend.BackendApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = BackendApplication.class)
public class ForeignKeyConstraintTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String TEST_PREFIX = "test_fk_";

    @AfterEach
    void cleanUp() {
        // Explicitly clean up test records across all affected tables
        jdbcTemplate.update("DELETE FROM sessions WHERE csrf_token LIKE ?", TEST_PREFIX + "%");
        jdbcTemplate.update("DELETE FROM issues WHERE title LIKE ?", TEST_PREFIX + "%");
        jdbcTemplate.update("DELETE FROM project_maintainers WHERE user_id IN (SELECT id FROM users WHERE username LIKE ?)", TEST_PREFIX + "%");
        jdbcTemplate.update("DELETE FROM project_countries WHERE country_code = 'ZA'");
        jdbcTemplate.update("DELETE FROM projects WHERE name LIKE ?", TEST_PREFIX + "%");
        jdbcTemplate.update("DELETE FROM users WHERE username LIKE ?", TEST_PREFIX + "%");

        // Reset query planner default in case sequential scans were disabled
        jdbcTemplate.execute("SET enable_seqscan = ON;");
    }

    // V1 Migration Test
    @Test
    @DisplayName("Manual test: inserting a sessions row with a user_id that doesn't exist in users is rejected by the FK constraint")
    void shouldRejectSessionWithNonExistentUser() {
        long fakeUserId = 999_999L;
        Timestamp expiresAt = Timestamp.from(Instant.now().plus(1, ChronoUnit.DAYS));
        String testCsrfToken = TEST_PREFIX + "csrf_" + UUID.randomUUID().toString().substring(0, 8);

        DataIntegrityViolationException exception = assertThrows(
            DataIntegrityViolationException.class,
            () -> jdbcTemplate.update(
                "INSERT INTO sessions (csrf_token, expires_at, user_id) VALUES (?, ?, ?)",
                testCsrfToken,
                expiresAt,
                fakeUserId
            )
        );

        String rootMessage = exception.getMostSpecificCause().getMessage();
        assertTrue(
            rootMessage.contains("violates foreign key constraint") || 
            rootMessage.contains("fk_sessions_users"),
            "Expected foreign key constraint violation, but got: " + rootMessage
        );
    }

    // V2 Migration Tests
    @Test
    @DisplayName("Acceptance Criteria 1: Inserting a project with invalid connection is rejected by CHECK constraint")
    void shouldRejectProjectWithInvalidConnectionCheckConstraint() {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);

        DataIntegrityViolationException exception = assertThrows(
            DataIntegrityViolationException.class,
            () -> jdbcTemplate.update(
                "INSERT INTO projects (" +
                "  github_owner, github_repo, github_url, name, slug, connection" +
                ") VALUES (?, ?, ?, ?, ?, ?)",
                "codemaster",
                "core-" + uniqueSuffix,
                "https://github.com/codemaster/invalid-check-" + uniqueSuffix,
                TEST_PREFIX + "Invalid Project",
                "slug-" + uniqueSuffix,
                "made_up_value"
            )
        );

        String rootCause = exception.getMostSpecificCause().getMessage();
        assertTrue(
            rootCause.contains("projects_connection_check") || rootCause.contains("violates check constraint"),
            "Expected check constraint failure on connection, but got: " + rootCause
        );
    }

    @Test
    @DisplayName("Acceptance Criteria 2: EXPLAIN query on project_countries uses idx_project_countries_country_code")
    void shouldVerifyIndexUsageOnCountryCode() {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);

        Long projectId = jdbcTemplate.queryForObject(
            "INSERT INTO projects (" +
            "  github_owner, github_repo, github_url, name, slug, connection" +
            ") VALUES (?, ?, ?, ?, ?, ?) RETURNING id",
            Long.class,
            "codemaster",
            "search-" + uniqueSuffix,
            "https://github.com/codemaster/search-" + uniqueSuffix,
            TEST_PREFIX + "Search Indexer",
            "slug-search-" + uniqueSuffix,
            "south_african"
        );

        jdbcTemplate.update(
            "INSERT INTO project_countries (project_id, country_code) VALUES (?, ?)",
            projectId,
            "ZA"
        );

        jdbcTemplate.execute("SET enable_seqscan = OFF;");

        List<String> queryPlanLines = jdbcTemplate.queryForList(
            "EXPLAIN SELECT project_id FROM project_countries WHERE country_code = 'ZA'",
            String.class
        );

        String fullPlan = String.join("\n", queryPlanLines);

        assertTrue(
            fullPlan.contains("idx_project_countries_country_code"),
            "Expected query plan to use 'idx_project_countries_country_code', plan was:\n" + fullPlan
        );
        assertTrue(
            !fullPlan.contains("Seq Scan on project_countries"),
            "Query plan should not perform a sequential scan. Plan was:\n" + fullPlan
        );
    }

    // V3 Migration Tests
    @Test
    @DisplayName("V3 Acceptance Criteria 1: Reject duplicate (project_id, user_id) in project_maintainers")
    void shouldRejectDuplicateProjectMaintainerPair() {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);

        Long userId = jdbcTemplate.queryForObject(
            "INSERT INTO users (github_id, username, display_name) VALUES (?, ?, ?) RETURNING id",
            Long.class,
            System.nanoTime(),
            TEST_PREFIX + "user_" + uniqueSuffix,
            "Maintainer User"
        );

        Long projectId = jdbcTemplate.queryForObject(
            "INSERT INTO projects (github_owner, github_repo, github_url, name, slug, connection) " +
            "VALUES (?, ?, ?, ?, ?, ?) RETURNING id",
            Long.class,
            "codemaster",
            "maint-repo-" + uniqueSuffix,
            "https://github.com/codemaster/maint-" + uniqueSuffix,
            TEST_PREFIX + "Maint Project",
            "slug-maint-" + uniqueSuffix,
            "south_african"
        );

        // First insert succeeds with valid role 'maintainer'
        jdbcTemplate.update(
            "INSERT INTO project_maintainers (project_id, user_id, role) VALUES (?, ?, ?)",
            projectId, userId, "maintainer"
        );

        // Second insert with identical (project_id, user_id) must be rejected by unique constraint
        DataIntegrityViolationException exception = assertThrows(
            DataIntegrityViolationException.class,
            () -> jdbcTemplate.update(
                "INSERT INTO project_maintainers (project_id, user_id, role) VALUES (?, ?, ?)",
                projectId, userId, "maintainer"
            )
        );

        String rootCause = exception.getMostSpecificCause().getMessage();
        assertTrue(
            rootCause.contains("uq_project_maintainers_project_user") || rootCause.contains("violates unique constraint"),
            "Expected unique constraint violation on maintainer pair, but got: " + rootCause
        );
    }

    @Test
    @DisplayName("V3 Acceptance Criteria 2: Reject duplicate (project_id, github_issue_number) in issues")
    void shouldRejectDuplicateProjectIssueNumberPair() {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);

        Long projectId = jdbcTemplate.queryForObject(
            "INSERT INTO projects (github_owner, github_repo, github_url, name, slug, connection) " +
            "VALUES (?, ?, ?, ?, ?, ?) RETURNING id",
            Long.class,
            "codemaster",
            "issue-repo-" + uniqueSuffix,
            "https://github.com/codemaster/issue-" + uniqueSuffix,
            TEST_PREFIX + "Issue Project",
            "slug-issue-" + uniqueSuffix,
            "south_african"
        );

        jdbcTemplate.update(
            "INSERT INTO issues (project_id, github_issue_number, github_url, title, status) VALUES (?, ?, ?, ?, ?)",
            projectId, 42, "https://github.com/codemaster/issues-repo/issues/42", TEST_PREFIX + "Issue 42", "open"
        );

        DataIntegrityViolationException exception = assertThrows(
            DataIntegrityViolationException.class,
            () -> jdbcTemplate.update(
                "INSERT INTO issues (project_id, github_issue_number, github_url, title, status) VALUES (?, ?, ?, ?, ?)",
                projectId, 42, "https://github.com/codemaster/issues-repo/issues/42_dup", TEST_PREFIX + "Duplicate Issue", "open"
            )
        );

        String rootCause = exception.getMostSpecificCause().getMessage();
        assertTrue(
            rootCause.contains("uq_issues_project_issue_number") || rootCause.contains("violates unique constraint"),
            "Expected unique constraint violation on project and issue number, but got: " + rootCause
        );
    }
}