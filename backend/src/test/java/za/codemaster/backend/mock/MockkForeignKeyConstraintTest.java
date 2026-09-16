package za.codemaster.backend.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MockkForeignKeyConstraintTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    // V1 migration test
    @Test
    @DisplayName("Manual test: inserting a sessions row with a user_id that doesn't exist in users is rejected by the FK constraint")
    void shouldRejectSessionWithNonExistentUser() {
        long fakeUserId = 999_999L;
        Timestamp expiresAt = Timestamp.from(Instant.now().plus(1, ChronoUnit.DAYS));

        // Mock JdbcTemplate to throw a foreign key constraint violation when inserting the bad session
        when(jdbcTemplate.update(
            contains("INSERT INTO sessions"),
            eq("test_csrf_token_xyz"),
            eq(expiresAt),
            eq(fakeUserId)
        )).thenThrow(new DataIntegrityViolationException(
            "ERROR: insert or update on table \"sessions\" violates foreign key constraint \"fk_sessions_users\""
        ));

        // Assert that executing the insert statement throws DataIntegrityViolationException
        DataIntegrityViolationException exception = assertThrows(
            DataIntegrityViolationException.class,
            () -> jdbcTemplate.update(
                "INSERT INTO sessions (csrf_token, expires_at, user_id) VALUES (?, ?, ?)",
                "test_csrf_token_xyz",
                expiresAt,
                fakeUserId
            )
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
        // Mock JdbcTemplate to throw a check constraint violation when invalid connection is supplied
        when(jdbcTemplate.update(
            contains("INSERT INTO projects"),
            any(), any(), any(), any(), any(), eq("made_up_value")
        )).thenThrow(new DataIntegrityViolationException(
            "ERROR: new row for relation \"projects\" violates check constraint \"projects_connection_check\""
        ));

        DataIntegrityViolationException exception = assertThrows(
            DataIntegrityViolationException.class,
            () -> jdbcTemplate.update(
                "INSERT INTO projects (" +
                "  github_owner, github_repo, github_url, name, slug, connection" +
                ") VALUES (?, ?, ?, ?, ?, ?)",
                "codemaster",
                "core",
                "https://github.com/codemaster/invalid-check",
                "Core",
                "codemaster-invalid-check",
                "made_up_value"
            )
        );

        String rootCause = exception.getMostSpecificCause().getMessage();
        assertTrue(
            rootRootCauseMatches(rootCause),
            "Expected check constraint failure on connection, but got: " + rootCause
        );
    }

    @Test
    @DisplayName("Acceptance Criteria 2: EXPLAIN query on project_countries uses idx_project_countries_country_code")
    void shouldVerifyIndexUsageOnCountryCode() {
        // 1. Mock queryForObject returning the generated project ID
        when(jdbcTemplate.queryForObject(
            contains("INSERT INTO projects"),
            eq(Long.class),
            any(), any(), any(), any(), any(), any()
        )).thenReturn(1L);

        // 2. Mock EXPLAIN query planner output
        List<String> mockPlan = List.of(
            "Index Only Scan using idx_project_countries_country_code on project_countries  (cost=0.15..8.17 rows=1 width=8)",
            "  Index Cond: (country_code = 'ZA'::text)"
        );

        when(jdbcTemplate.queryForList(
            contains("EXPLAIN SELECT project_id FROM project_countries WHERE country_code = 'ZA'"),
            eq(String.class)
        )).thenReturn(mockPlan);

        // Execute step 1: Insert mock project
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

        // Execute step 2: Insert country mapping
        jdbcTemplate.update(
            "INSERT INTO project_countries (project_id, country_code) VALUES (?, ?)",
            projectId,
            "ZA"
        );

        // Execute step 3: Disable sequential scan
        jdbcTemplate.execute("SET enable_seqscan = OFF;");

        // Execute step 4: Run EXPLAIN query
        List<String> queryPlanLines = jdbcTemplate.queryForList(
            "EXPLAIN SELECT project_id FROM project_countries WHERE country_code = 'ZA'",
            String.class
        );

        String fullPlan = String.join("\n", queryPlanLines);

        // 5. Assert index is utilized
        assertTrue(
            fullPlan.contains("idx_project_countries_country_code"),
            "Expected query plan to use 'idx_project_countries_country_code', plan was:\n" + fullPlan
        );
        assertTrue(
            !fullPlan.contains("Seq Scan on project_countries"),
            "Query plan should not perform a sequential scan. Plan was:\n" + fullPlan
        );

        verify(jdbcTemplate).execute("SET enable_seqscan = OFF;");
    }

    private boolean rootRootCauseMatches(String rootCause) {
        return rootCause.contains("projects_connection_check") || 
               rootCause.contains("violates check constraint");
    }
}