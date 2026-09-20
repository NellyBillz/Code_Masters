package za.codemaster.backend.persistence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import za.codemaster.backend.BackendApplication;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
public class ConstraintsIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long user1Id;
    private Long user2Id;
    private Long projectId;
    private Long issue1Id;
    private Long issue2Id;
    private String suffix;

    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString().substring(0, 8);

        user1Id = jdbcTemplate.queryForObject(
            "INSERT INTO users (github_id, username, display_name) VALUES (?, ?, ?) RETURNING id",
            Long.class,
            System.nanoTime(),
            "u1_" + suffix,
            "Test User One"
        );

        user2Id = jdbcTemplate.queryForObject(
            "INSERT INTO users (github_id, username, display_name) VALUES (?, ?, ?) RETURNING id",
            Long.class,
            System.nanoTime() + 1,
            "u2_" + suffix,
            "Test User Two"
        );

        projectId = jdbcTemplate.queryForObject(
            "INSERT INTO projects (github_owner, github_repo, github_url, name, slug, connection, listing_status) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id",
            Long.class,
            "codemaster",
            "p-" + suffix,
            "https://github.com/codemaster/p-" + suffix,
            "Constraint Project",
            "p-" + suffix,
            "south_african",
            "pending"
        );

        issue1Id = jdbcTemplate.queryForObject(
            "INSERT INTO issues (project_id, github_issue_number, github_url, title, status) " +
            "VALUES (?, ?, ?, ?, ?) RETURNING id",
            Long.class,
            projectId,
            1,
            "https://github.com/codemaster/p-" + suffix + "/issues/1",
            "Constraint Issue 1",
            "open"
        );

        issue2Id = jdbcTemplate.queryForObject(
            "INSERT INTO issues (project_id, github_issue_number, github_url, title, status) " +
            "VALUES (?, ?, ?, ?, ?) RETURNING id",
            Long.class,
            projectId,
            2,
            "https://github.com/codemaster/p-" + suffix + "/issues/2",
            "Constraint Issue 2",
            "open"
        );
    }

    @AfterEach
    void tearDown() {
        if (projectId != null) {
            jdbcTemplate.update("DELETE FROM reports WHERE target_id = ?", projectId);
            jdbcTemplate.update("DELETE FROM claims WHERE issue_id IN (?, ?)", issue1Id, issue2Id);
            jdbcTemplate.update("DELETE FROM issues WHERE project_id = ?", projectId);
            jdbcTemplate.update("DELETE FROM projects WHERE id = ?", projectId);
        }
        if (user1Id != null) {
            jdbcTemplate.update("DELETE FROM reports WHERE reporter_user_id = ?", user1Id);
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", user1Id);
        }
        if (user2Id != null) {
            jdbcTemplate.update("DELETE FROM reports WHERE reporter_user_id = ?", user2Id);
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", user2Id);
        }
    }

    // =========================================================================
    // 1. projects.listing_status CHECK CONSTRAINT (V7)
    // =========================================================================
    @Test
    @DisplayName("DB-03.1: Reject invalid listing_status check constraint value on projects")
    void shouldRejectInvalidListingStatusCheckConstraint() {
        DataIntegrityViolationException ex = assertThrows(
            DataIntegrityViolationException.class,
            () -> jdbcTemplate.update(
                "INSERT INTO projects (github_owner, github_repo, github_url, name, slug, connection, listing_status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)",
                "codemaster",
                "bad-listing-" + suffix,
                "https://github.com/codemaster/bad-listing-" + suffix,
                "Bad Listing Project",
                "bad-listing-" + suffix,
                "south_african",
                "under_review" // Invalid; allowed: pending, published, rejected
            )
        );

        String rootCause = ex.getMostSpecificCause().getMessage();
        assertTrue(
            rootCause.contains("chk_projects_listing_status") || rootCause.contains("violates check constraint"),
            "Expected chk_projects_listing_status check constraint violation, got: " + rootCause
        );
    }

    // =========================================================================
    // 2. claims.pull_request_state CHECK CONSTRAINT (V8)
    // =========================================================================
    @Test
    @DisplayName("DB-03.3: Reject invalid pull_request_state check constraint value on claims")
    void shouldRejectInvalidPullRequestStateCheckConstraint() {
        DataIntegrityViolationException ex = assertThrows(
            DataIntegrityViolationException.class,
            () -> jdbcTemplate.update(
                "INSERT INTO claims (issue_id, user_id, status, pull_request_state) VALUES (?, ?, ?, ?)",
                issue1Id,
                user1Id,
                "active",
                "in_review" // Invalid; allowed: none, open, merged, closed_unmerged
            )
        );

        String rootCause = ex.getMostSpecificCause().getMessage();
        assertTrue(
            rootCause.contains("chk_claims_pull_request_state") || rootCause.contains("violates check constraint"),
            "Expected chk_claims_pull_request_state check constraint violation, got: " + rootCause
        );
    }

    // =========================================================================
    // 3. claims.status CHECK CONSTRAINT (V8 update)
    // =========================================================================
    @Test
    @DisplayName("DB-03.3: Accept updated status = 'changes_requested' and reject invalid status on claims")
    void shouldEnforceUpdatedClaimsStatusCheckConstraint() {
        // Valid insert: changes_requested is now permitted
        assertDoesNotThrow(() -> {
            jdbcTemplate.update(
                "INSERT INTO claims (issue_id, user_id, status, pull_request_state) VALUES (?, ?, ?, ?)",
                issue1Id,
                user1Id,
                "changes_requested",
                "open"
            );
        });

        // Invalid insert: arbitrary status still rejected
        DataIntegrityViolationException ex = assertThrows(
            DataIntegrityViolationException.class,
            () -> jdbcTemplate.update(
                "INSERT INTO claims (issue_id, user_id, status, pull_request_state) VALUES (?, ?, ?, ?)",
                issue2Id,
                user2Id,
                "abandoned", // Invalid; allowed: active, released, completed, changes_requested
                "none"
            )
        );

        String rootCause = ex.getMostSpecificCause().getMessage();
        assertTrue(
            rootCause.contains("chk_claims_status") || rootCause.contains("claims_status_check") || rootCause.contains("violates check constraint"),
            "Expected claims status check constraint violation, got: " + rootCause
        );
    }

    // =========================================================================
    // 4. claims PARTIAL UNIQUE INDEX (uq_claims_active_per_user_issue)
    // =========================================================================
    @Test
    @DisplayName("DB-03.3: Pre-existing partial unique index still fires ONLY for 'active' rows alongside 'changes_requested'")
    void shouldEnforcePartialUniqueIndexOnlyForActiveRows() {
        // 1. User 1 acquires ACTIVE claim on Issue 1 -> SUCCEEDS
        jdbcTemplate.update(
            "INSERT INTO claims (issue_id, user_id, status) VALUES (?, ?, ?)",
            issue1Id,
            user1Id,
            "active"
        );

        // 2. User 1 attempts second ACTIVE claim on Issue 1 -> REJECTED by partial index
        DuplicateKeyException dupEx = assertThrows(
            DuplicateKeyException.class,
            () -> jdbcTemplate.update(
                "INSERT INTO claims (issue_id, user_id, status) VALUES (?, ?, ?)",
                issue1Id,
                user1Id,
                "active"
            )
        );
        assertTrue(dupEx.getMostSpecificCause().getMessage().contains("uq_claims_active_per_user_issue"));

        // 3. User 1 receives a CHANGES_REQUESTED claim on Issue 1 -> SUCCEEDS (partial index ignores non-active)
        assertDoesNotThrow(() -> {
            jdbcTemplate.update(
                "INSERT INTO claims (issue_id, user_id, status) VALUES (?, ?, ?)",
                issue1Id,
                user1Id,
                "changes_requested"
            );
        });

        // 4. User 1 holds an ACTIVE claim on Issue 1 and a CHANGES_REQUESTED claim on Issue 2 -> SUCCEEDS
        assertDoesNotThrow(() -> {
            jdbcTemplate.update(
                "INSERT INTO claims (issue_id, user_id, status) VALUES (?, ?, ?)",
                issue2Id,
                user1Id,
                "changes_requested"
            );
        });

        // 5. User 2 claims Issue 1 as ACTIVE (multi-claimant support) -> SUCCEEDS
        assertDoesNotThrow(() -> {
            jdbcTemplate.update(
                "INSERT INTO claims (issue_id, user_id, status) VALUES (?, ?, ?)",
                issue1Id,
                user2Id,
                "active"
            );
        });
    }

    // =========================================================================
    // 5. reports UNIQUE CONSTRAINT (V10)
    // =========================================================================
    @Test
    @DisplayName("DB-03.4: uq_reports_reporter_target rejects duplicate user report on same target, allows distinct users")
    void shouldEnforceReportsUniqueConstraint() {
        // User 1 reports the project -> SUCCEEDS
        assertDoesNotThrow(() -> {
            jdbcTemplate.update(
                "INSERT INTO reports (reporter_user_id, target_type, target_id, reason) VALUES (?, ?, ?, ?)",
                user1Id,
                "project",
                projectId,
                "First report on project"
            );
        });

        // User 2 reports the same project -> SUCCEEDS
        assertDoesNotThrow(() -> {
            jdbcTemplate.update(
                "INSERT INTO reports (reporter_user_id, target_type, target_id, reason) VALUES (?, ?, ?, ?)",
                user2Id,
                "project",
                projectId,
                "Independent report on same project"
            );
        });

        // User 1 submits a duplicate report on the same project -> REJECTED by unique constraint
        DuplicateKeyException dupEx = assertThrows(
            DuplicateKeyException.class,
            () -> jdbcTemplate.update(
                "INSERT INTO reports (reporter_user_id, target_type, target_id, reason) VALUES (?, ?, ?, ?)",
                user1Id,
                "project",
                projectId,
                "Duplicate report attempt"
            )
        );

        String rootCause = dupEx.getMostSpecificCause().getMessage();
        assertTrue(
            rootCause.contains("uq_reports_reporter_target") || rootCause.contains("violates unique constraint"),
            "Expected uq_reports_reporter_target violation, got: " + rootCause
        );
    }
}