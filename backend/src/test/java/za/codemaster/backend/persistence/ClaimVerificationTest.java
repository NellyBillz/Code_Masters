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
import org.springframework.transaction.annotation.Transactional;
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
// @Transactional
public class ClaimVerificationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long testUserId;
    private Long testProjectId;
    private Long testIssueId1;
    private Long testIssueId2;
    private String uniqueSuffix;

    @BeforeEach
    void setUp() {
        uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);

        testUserId = jdbcTemplate.queryForObject(
            "INSERT INTO users (github_id, username, display_name) VALUES (?, ?, ?) RETURNING id",
            Long.class,
            System.nanoTime(),
            "claim_user_" + uniqueSuffix,
            "Claim Test User"
        );

        testProjectId = jdbcTemplate.queryForObject(
            "INSERT INTO projects (github_owner, github_repo, github_url, name, slug, connection) " +
            "VALUES (?, ?, ?, ?, ?, ?) RETURNING id",
            Long.class,
            "codemaster",
            "v8-repo-" + uniqueSuffix,
            "https://github.com/codemaster/v8-repo-" + uniqueSuffix,
            "V8 Project",
            "slug-v8-" + uniqueSuffix,
            "south_african"
        );

        testIssueId1 = jdbcTemplate.queryForObject(
            "INSERT INTO issues (project_id, github_issue_number, github_url, title, status) " +
            "VALUES (?, ?, ?, ?, ?) RETURNING id",
            Long.class,
            testProjectId,
            1,
            "https://github.com/codemaster/v8-repo-" + uniqueSuffix + "/issues/1",
            "V8 Issue 1",
            "open"
        );

        testIssueId2 = jdbcTemplate.queryForObject(
            "INSERT INTO issues (project_id, github_issue_number, github_url, title, status) " +
            "VALUES (?, ?, ?, ?, ?) RETURNING id",
            Long.class,
            testProjectId,
            2,
            "https://github.com/codemaster/v8-repo-" + uniqueSuffix + "/issues/2",
            "V8 Issue 2",
            "open"
        );
    }

    @AfterEach
    void tearDown() {
        // Explicitly clean up all test rows in reverse foreign key order
        if (testUserId != null) {
            jdbcTemplate.update("DELETE FROM claims WHERE user_id = ?", testUserId);
        }
        if (testProjectId != null) {
            jdbcTemplate.update("DELETE FROM issues WHERE project_id = ?", testProjectId);
            jdbcTemplate.update("DELETE FROM projects WHERE id = ?", testProjectId);
        }
        if (testUserId != null) {
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", testUserId);
        }

        // Align sequence generators so subsequent test IDs remain consistent
        jdbcTemplate.execute("SELECT setval('users_id_seq', (SELECT COALESCE(MAX(id), 1) FROM users))");
        jdbcTemplate.execute("SELECT setval('projects_id_seq', (SELECT COALESCE(MAX(id), 1) FROM projects))");
        jdbcTemplate.execute("SELECT setval('issues_id_seq', (SELECT COALESCE(MAX(id), 1) FROM issues))");
        jdbcTemplate.execute("SELECT setval('claims_id_seq', (SELECT COALESCE(MAX(id), 1) FROM claims))");
    }

    @Test
    @DisplayName("Acceptance Criteria 1: Inserting a claim with status = 'changes_requested' succeeds")
    void shouldAllowInsertingChangesRequestedStatus() {
        assertDoesNotThrow(() -> {
            jdbcTemplate.update(
                "INSERT INTO claims (issue_id, user_id, status, pull_request_url, pull_request_state) " +
                "VALUES (?, ?, ?, ?, ?)",
                testIssueId1,
                testUserId,
                "changes_requested",
                "https://github.com/codemaster/v8-repo/pull/10",
                "open"
            );
        });
    }

    @Test
    @DisplayName("Acceptance Criteria 2: Inserting a claim with pull_request_state = 'merging' (invalid) is rejected")
    void shouldRejectInvalidPullRequestStateCheckConstraint() {
        DataIntegrityViolationException ex = assertThrows(
            DataIntegrityViolationException.class,
            () -> jdbcTemplate.update(
                "INSERT INTO claims (issue_id, user_id, status, pull_request_state) VALUES (?, ?, ?, ?)",
                testIssueId1,
                testUserId,
                "active",
                "merging"
            )
        );

        String cause = ex.getMostSpecificCause().getMessage();
        assertTrue(
            cause.contains("chk_claims_pull_request_state") || cause.contains("violates check constraint"),
            "Expected check constraint failure on pull_request_state, but got: " + cause
        );
    }

    @Test
    @DisplayName("Acceptance Criteria 3a: Duplicate active claim on same issue by same user is rejected by partial index")
    void shouldRejectDuplicateActiveClaimOnSameIssue() {
        // Step A: Insert first ACTIVE claim -> SUCCEEDS
        jdbcTemplate.update(
            "INSERT INTO claims (issue_id, user_id, status) VALUES (?, ?, ?)",
            testIssueId1,
            testUserId,
            "active"
        );

        // Step B: Second ACTIVE claim on same issue by same user must be rejected by partial unique index
        DuplicateKeyException dupEx = assertThrows(
            DuplicateKeyException.class,
            () -> jdbcTemplate.update(
                "INSERT INTO claims (issue_id, user_id, status) VALUES (?, ?, ?)",
                testIssueId1,
                testUserId,
                "active"
            )
        );
        assertTrue(dupEx.getMostSpecificCause().getMessage().contains("uq_claims_active_per_user_issue"));
    }

    @Test
    @DisplayName("Acceptance Criteria 3b: Partial index does not collide with non-active claims or claims on other issues")
    void shouldAllowActiveAlongsideChangesRequestedAndMultiIssueClaims() {
        // Step A: First ACTIVE claim on Issue 1
        jdbcTemplate.update(
            "INSERT INTO claims (issue_id, user_id, status) VALUES (?, ?, ?)",
            testIssueId1,
            testUserId,
            "active"
        );

        // Step B: A user having a 'changes_requested' claim on the SAME issue -> SUCCEEDS (partial index ignores non-active)
        assertDoesNotThrow(() -> {
            jdbcTemplate.update(
                "INSERT INTO claims (issue_id, user_id, status) VALUES (?, ?, ?)",
                testIssueId1,
                testUserId,
                "changes_requested"
            );
        });

        // Step C: A user with one 'active' and one 'changes_requested' claim on DIFFERENT issues -> SUCCEEDS
        assertDoesNotThrow(() -> {
            jdbcTemplate.update(
                "INSERT INTO claims (issue_id, user_id, status) VALUES (?, ?, ?)",
                testIssueId2,
                testUserId,
                "changes_requested"
            );
        });
    }
}