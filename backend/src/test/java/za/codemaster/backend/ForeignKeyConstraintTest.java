package za.codemaster.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = BackendApplication.class)
public class ForeignKeyConstraintTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // V1 migration test
    @Test
    @DisplayName("Manual test: inserting a sessions row with a user_id that doesn't exist in users is rejected by the FK constraint")
    void shouldRejectSessionWithNonExistentUser() {
        // Non-existent user_id for BIGINT/BIGSERIAL users.id
        long fakeUserId = 999_999L;
        Timestamp expiresAt = Timestamp.from(Instant.now().plus(1, ChronoUnit.DAYS));

        // Assert that executing the insert statement throws DataIntegrityViolationException
        DataIntegrityViolationException exception = assertThrows(
            DataIntegrityViolationException.class,
            () -> {
                jdbcTemplate.update(
                    "INSERT INTO sessions (csrf_token, expires_at, user_id) VALUES (?, ?, ?)",
                    "test_csrf_token_xyz",
                    expiresAt,
                    fakeUserId
                );
            }
        );

        // Verify that the underlying root cause mentions the foreign key violation
        String rootMessage = exception.getMostSpecificCause().getMessage();
        assertTrue(
            rootMessage.contains("violates foreign key constraint") || 
            rootMessage.contains("fk_sessions_users"),
            "Expected foreign key constraint violation, but got: " + rootMessage
        );
    }

    // V2 migration test
    @Test
    @DisplayName("Acceptance Criteria 1: Inserting a project with invalid connection is rejected by CHECK constraint")
    void shouldRejectProjectWithInvalidConnectionCheckConstraint() {
        DataIntegrityViolationException exception = assertThrows(
            DataIntegrityViolationException.class,
            () -> {
                jdbcTemplate.update(
                    "INSERT INTO projects (" +
                    "  github_owner, github_repo, github_url, name, slug, connection" +
                    ") VALUES (?, ?, ?, ?, ?, ?)",
                    "codemaster",
                    "core",
                    "https://github.com/codemaster/invalid-check",
                    "Core",
                    "codemaster-invalid-check",
                    "made_up_value"
                );
            }
        );

        String rootCause = exception.getMostSpecificCause().getMessage();
        assertTrue(
            rootCause.contains("projects_connection_check") || 
            rootCause.contains("violates check constraint"),
            "Expected check constraint failure on connection, but got: " + rootCause
        );
    }

    @Test
    @DisplayName("Acceptance Criteria 2: EXPLAIN query on project_countries uses idx_project_countries_country_code")
    void shouldVerifyIndexUsageOnCountryCode() {
        // 1. Insert a valid project parent
        Long projectId = jdbcTemplate.queryForObject(
            "INSERT INTO projects (" +
            "  github_owner, github_repo, github_url, name, slug, connection" +
            ") VALUES (?, ?, ?, ?, ?, ?) RETURNING id",
            Long.class,
            "codemaster",
            "search-indexer",
            "https://github.com/codemaster/search-indexer",
            "Search Indexer",
            "codemaster-search-indexer",
            "south_african"
        );

        // 2. Insert test country code mapping
        jdbcTemplate.update(
            "INSERT INTO project_countries (project_id, country_code) VALUES (?, ?)",
            projectId,
            "ZA"
        );

        // 3. Disable sequential scan within test session so Postgres favors the index
        jdbcTemplate.execute("SET enable_seqscan = OFF;");

        // 4. Run EXPLAIN on the WHERE country_code = 'ZA' query
        List<String> queryPlanLines = jdbcTemplate.queryForList(
            "EXPLAIN SELECT project_id FROM project_countries WHERE country_code = 'ZA'",
            String.class
        );

        String fullPlan = String.join("\n", queryPlanLines);

        // 5. Assert the index is utilized rather than a sequential scan
        assertTrue(
            fullPlan.contains("idx_project_countries_country_code"),
            "Expected query plan to use 'idx_project_countries_country_code', plan was:\n" + fullPlan
        );
        assertTrue(
            !fullPlan.contains("Seq Scan on project_countries"),
            "Query plan should not perform a sequential scan. Plan was:\n" + fullPlan
        );
    }
}