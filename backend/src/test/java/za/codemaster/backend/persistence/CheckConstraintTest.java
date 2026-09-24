package za.codemaster.backend.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitConfig(CheckConstraintTest.TestDbConfig.class)
@Transactional
public class CheckConstraintTest {

    @Configuration
    static class TestDbConfig {

        private String resolvePassword() {
            // 1. Check OS environment variable
            String pass = System.getenv("LOCAL_DB_PASSWORD");
            if (pass != null && !pass.isBlank()) {
                return pass;
            }

            // 2. Read from root .env file if present
            File envFile = new File(".env");
            if (envFile.exists()) {
                try (InputStream input = new FileInputStream(envFile)) {
                    Properties props = new Properties();
                    props.load(input);
                    String filePass = props.getProperty("LOCAL_DB_PASSWORD");
                    if (filePass != null && !filePass.isBlank()) {
                        return filePass.replace("'", "").replace("\"", "").trim();
                    }
                } catch (Exception ignored) {
                }
            }

            // 3. Fallback default
            return "postgres";
        }

        private String resolveUsername() {
            // 1. Check OS environment variable
            String user = System.getenv("LOCAL_DB_USERNAME");
            if (user != null && !user.isBlank()) {
                return user;
            }

            // 2. Read from root .env file if present
            File envFile = new File(".env");
            if (envFile.exists()) {
                try (InputStream input = new FileInputStream(envFile)) {
                    Properties props = new Properties();
                    props.load(input);
                    String fileUser = props.getProperty("LOCAL_DB_USERNAME");
                    if (fileUser != null && !fileUser.isBlank()) {
                        return fileUser.replace("'", "").replace("\"", "").trim();
                    }
                } catch (Exception ignored) {
                }
            }

            // 3. Fallback default
            return "postgres";
        }

        @Bean
        public DataSource dataSource() {
            PGSimpleDataSource ds = new PGSimpleDataSource();
            ds.setServerNames(new String[]{"localhost"});
            ds.setPortNumbers(new int[]{5432});
            ds.setDatabaseName("codemaster_db");
            ds.setUser(resolveUsername());
            ds.setPassword(resolvePassword());
            return ds;
        }

        @Bean
        public PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        public JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long userId1;
    private Long userId2;
    private Long projectId;
    private Long issueId;

    @BeforeEach
    void setUpTestData() {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);

        // 1. Create two test users
        userId1 = jdbcTemplate.queryForObject(
            "INSERT INTO users (github_id, username, display_name) " +
            "VALUES (?, ?, ?) RETURNING id",
            Long.class,
            System.nanoTime(),
            "check_user_1_" + uniqueSuffix,
            "Check User One"
        );

        userId2 = jdbcTemplate.queryForObject(
            "INSERT INTO users (github_id, username, display_name) " +
            "VALUES (?, ?, ?) RETURNING id",
            Long.class,
            System.nanoTime() + 1,
            "check_user_2_" + uniqueSuffix,
            "Check User Two"
        );

        // 2. Create parent project
        projectId = jdbcTemplate.queryForObject(
            "INSERT INTO projects (github_owner, github_repo, github_url, name, slug, connection) " +
            "VALUES (?, ?, ?, ?, ?, ?) RETURNING id",
            Long.class,
            "codemaster",
            "repo-" + uniqueSuffix,
            "https://github.com/codemaster/repo-" + uniqueSuffix,
            "Constraint Repo",
            "slug-" + uniqueSuffix,
            "south_african"
        );

        // 3. Create parent issue
        issueId = jdbcTemplate.queryForObject(
            "INSERT INTO issues (project_id, github_issue_number, github_url, title, status) " +
            "VALUES (?, ?, ?, ?, ?) RETURNING id",
            Long.class,
            projectId,
            101,
            "https://github.com/codemaster/repo-" + uniqueSuffix + "/issues/101",
            "Test Issue",
            "open"
        );
    }

    @Test
    @DisplayName("Comments: Reject insert when both project_id and issue_id are provided")
    void shouldRejectCommentWhenBothTargetsProvided() {
        DataIntegrityViolationException ex = assertThrows(
            DataIntegrityViolationException.class,
            () -> jdbcTemplate.update(
                "INSERT INTO comments (user_id, project_id, issue_id, body) VALUES (?, ?, ?, ?)",
                userId1, projectId, issueId, "Both targets provided"
            )
        );

        String rootCause = ex.getMostSpecificCause().getMessage();
        assertTrue(rootCause.contains("chk_comments_target_exclusive"), 
            "Expected chk_comments_target_exclusive check constraint violation, but got: " + rootCause);
    }

    @Test
    @DisplayName("Comments: Reject insert when neither project_id nor issue_id is provided")
    void shouldRejectCommentWhenNeitherTargetProvided() {
        DataIntegrityViolationException ex = assertThrows(
            DataIntegrityViolationException.class,
            () -> jdbcTemplate.update(
                "INSERT INTO comments (user_id, project_id, issue_id, body) VALUES (?, ?, ?, ?)",
                userId1, null, null, "Neither target provided"
            )
        );

        String rootCause = ex.getMostSpecificCause().getMessage();
        assertTrue(rootCause.contains("chk_comments_target_exclusive"), 
            "Expected chk_comments_target_exclusive check constraint violation, but got: " + rootCause);
    }

    @Test
    @DisplayName("Claims: Reject second active claim for the same user and issue")
    void shouldRejectDuplicateActiveClaimForSameUser() {
        jdbcTemplate.update(
            "INSERT INTO claims (issue_id, user_id, status) VALUES (?, ?, ?)",
            issueId, userId1, "active"
        );

        DuplicateKeyException ex = assertThrows(
            DuplicateKeyException.class,
            () -> jdbcTemplate.update(
                "INSERT INTO claims (issue_id, user_id, status) VALUES (?, ?, ?)",
                issueId, userId1, "active"
            )
        );

        String rootCause = ex.getMostSpecificCause().getMessage();
        assertTrue(rootCause.contains("uq_claims_active_per_user_issue"),
            "Expected uq_claims_active_per_user_issue index violation, but got: " + rootCause);
    }

    @Test
    @DisplayName("Claims: Allow active claim alongside released claim for the same user (partial index)")
    void shouldAllowReleasedClaimAlongsideActiveClaim() {
        jdbcTemplate.update(
            "INSERT INTO claims (issue_id, user_id, status) VALUES (?, ?, ?)",
            issueId, userId1, "active"
        );

        assertDoesNotThrow(() -> jdbcTemplate.update(
            "INSERT INTO claims (issue_id, user_id, status) VALUES (?, ?, ?)",
            issueId, userId1, "released"
        ));
    }

    @Test
    @DisplayName("Claims: Allow two different users to hold active claims on the same issue")
    void shouldAllowDifferentUsersToHoldActiveClaimsOnSameIssue() {
        jdbcTemplate.update(
            "INSERT INTO claims (issue_id, user_id, status) VALUES (?, ?, ?)",
            issueId, userId1, "active"
        );

        assertDoesNotThrow(() -> jdbcTemplate.update(
            "INSERT INTO claims (issue_id, user_id, status) VALUES (?, ?, ?)",
            issueId, userId2, "active"
        ));
    }

    @Test
    @DisplayName("SyncJobs: Reject insert when status is not in accepted/running/completed/failed")
    void shouldRejectInvalidSyncJobStatus() {
        DataIntegrityViolationException ex = assertThrows(
            DataIntegrityViolationException.class,
            () -> jdbcTemplate.update(
                "INSERT INTO sync_jobs (project_id, status) VALUES (?, ?)",
                projectId, "in_progress"
            )
        );

        String rootCause = ex.getMostSpecificCause().getMessage();
        assertTrue(rootCause.contains("sync_jobs_status_check"),
            "Expected sync_jobs_status_check check constraint violation, but got: " + rootCause);
    }

    @Test
    @DisplayName("SyncJobs: Reject insert when project_id does not exist in projects")
    void shouldRejectOrphanSyncJob() {
        DataIntegrityViolationException ex = assertThrows(
            DataIntegrityViolationException.class,
            () -> jdbcTemplate.update(
                "INSERT INTO sync_jobs (project_id, status) VALUES (?, ?)",
                999_999L, "accepted"
            )
        );

        String rootCause = ex.getMostSpecificCause().getMessage();
        assertTrue(rootCause.contains("fk_sync_jobs_projects"),
            "Expected fk_sync_jobs_projects foreign key violation, but got: " + rootCause);
    }
}