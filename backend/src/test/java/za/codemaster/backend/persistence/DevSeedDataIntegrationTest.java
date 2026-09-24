package za.codemaster.backend.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the dev seed script's (db/dev/R__seed_dev_data.sql) actual
 * content and idempotency.
 * <p>
 * Runs against a dedicated {@code codemaster_test_db}, not the shared
 * {@code codemaster_db} — see the comment on {@link TestDbConfig#dataSource()}.
 * This class's cleanup does real, non-transactional deletes and rewrites
 * {@code flyway_schema_history} directly, since it has to exercise real
 * Flyway migration behavior; none of that can be wrapped in a
 * Spring-managed rollback the way the rest of this test suite's writes are.
 */
@SpringJUnitConfig(DevSeedDataIntegrationTest.TestDbConfig.class)
public class DevSeedDataIntegrationTest {

    @Configuration
    static class TestDbConfig {

        private String resolvePassword() {
            String pass = System.getenv("LOCAL_DB_PASSWORD");
            if (pass != null && !pass.isBlank()) {
                return pass;
            }

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

            return "postgres";
        }

        private String resolveUsername() {
            String user = System.getenv("LOCAL_DB_USERNAME");
            if (user != null && !user.isBlank()) {
                return user;
            }

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

            return "postgres";
        }

        @Bean
        public DataSource dataSource() {
            PGSimpleDataSource ds = new PGSimpleDataSource();
            ds.setServerNames(new String[]{"localhost"});
            ds.setPortNumbers(new int[]{5432});
            // A dedicated database, deliberately NOT codemaster_db (the shared
            // local dev database): this test's cleanup deletes every
            // github_owner = 'codemaster' project and manipulates
            // flyway_schema_history directly, outside of any Spring-managed
            // transaction (it has to be, to test real Flyway migration
            // behavior) — running it against the shared dev database
            // permanently wiped out real demo data once already. Create it
            // once with `createdb codemaster_test_db` (or
            // `psql -c "CREATE DATABASE codemaster_test_db;"`) and apply the
            // base schema with
            // `./mvnw flyway:migrate -Dflyway.user=<user> -Dflyway.password=<password> -Dflyway.url=jdbc:postgresql://localhost:5432/codemaster_test_db`
            // — see DATABASE.md.
            ds.setDatabaseName("codemaster_test_db");
            ds.setUser(resolveUsername());
            ds.setPassword(resolvePassword());
            return ds;
        }

        @Bean
        public JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void setUp() {
        cleanDevSeedData();
    }

    @AfterEach
    void tearDown() {
        cleanDevSeedData();
    }

    /**
     * Cleans up all rows seeded by the dev seed script across all tables
     * and clears the repeatable migration entry from flyway_schema_history.
     */
    private void cleanDevSeedData() {
        // Child tables first
        jdbcTemplate.update("DELETE FROM claims WHERE note LIKE '[dev-seed]%'");
        jdbcTemplate.update("DELETE FROM comments WHERE body LIKE '[dev-seed]%'");
        jdbcTemplate.update("DELETE FROM issues WHERE github_url LIKE 'https://github.com/codemaster/%'");
        jdbcTemplate.update("DELETE FROM project_tags WHERE project_id IN (SELECT id FROM projects WHERE github_owner = 'codemaster')");
        jdbcTemplate.update("DELETE FROM project_countries WHERE project_id IN (SELECT id FROM projects WHERE github_owner = 'codemaster')");
        jdbcTemplate.update("DELETE FROM project_maintainers WHERE project_id IN (SELECT id FROM projects WHERE github_owner = 'codemaster')");

        // Parent tables
        jdbcTemplate.update("DELETE FROM projects WHERE github_owner = 'codemaster'");
        jdbcTemplate.update("DELETE FROM users WHERE username IN ('winter_stone', 'winter_dev', 'montic_codes')");

        // Clear Flyway tracking for dev seed so repeatable scripts can re-run fresh
        jdbcTemplate.update("DELETE FROM flyway_schema_history WHERE script LIKE '%seed_dev_data%' OR description ILIKE '%seed%dev%data%'");
    }

    /**
     * Triggers Flyway migration targeting base schema and dev seed locations.
     */
    private void migrateDevSeed() {
        jdbcTemplate.update("DELETE FROM flyway_schema_history WHERE script LIKE '%seed_dev_data%' OR description ILIKE '%seed%dev%data%'");

        Flyway devFlyway = Flyway.configure()
            .dataSource(dataSource)
            .locations(
                "classpath:db/migration",
                "classpath:db/dev"
            )
            .load();
        devFlyway.migrate();
    }

    @Test
    @DisplayName("Dev Seed: Verify exactly 2 users are seeded (winter_stone, montic_codes)")
    void shouldVerifySeedUserCount() {
        migrateDevSeed();

        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users WHERE username IN ('winter_stone', 'montic_codes')",
            Integer.class
        );

        assertEquals(2, count, "Seed data must contain exactly 2 dev users");
    }

    @Test
    @DisplayName("Dev Seed: Verify 4 projects spanning at least 2 connections and 2 primary languages")
    void shouldVerifyProjectDiversity() {
        migrateDevSeed();

        Integer projectCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM projects WHERE github_owner = 'codemaster'",
            Integer.class
        );

        List<String> connections = jdbcTemplate.queryForList(
            "SELECT DISTINCT connection FROM projects WHERE github_owner = 'codemaster'",
            String.class
        );

        List<String> languages = jdbcTemplate.queryForList(
            "SELECT DISTINCT primary_language FROM projects WHERE github_owner = 'codemaster'",
            String.class
        );

        assertEquals(4, projectCount, "Expected 4 seeded projects");
        assertTrue(connections.size() >= 2, "Must contain at least 2 distinct connections (e.g., south_african, community_verified)");
        assertTrue(languages.size() >= 2, "Must contain at least 2 distinct primary languages (e.g., Java, Python)");
    }

    @Test
    @DisplayName("Dev Seed: Verify 18-20 issues seeded with at least 8 marked beginner")
    void shouldVerifyIssuesAndBeginnerFriendlyRequirements() {
        migrateDevSeed();

        Integer totalIssues = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM issues WHERE github_url LIKE 'https://github.com/codemaster/%'",
            Integer.class
        );

        Integer beginnerIssues = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM issues WHERE difficulty = 'beginner' AND github_url LIKE 'https://github.com/codemaster/%'",
            Integer.class
        );

        assertTrue(totalIssues != null && totalIssues >= 18 && totalIssues <= 20,
            "Issues count must be between 18 and 20, found: " + totalIssues);
        assertTrue(beginnerIssues != null && beginnerIssues >= 8,
            "At least 8 issues must have difficulty = 'beginner', found: " + beginnerIssues);
    }

    @Test
    @DisplayName("Dev Seed: Verify comments mix (project-attached and issue-attached)")
    void shouldVerifyCommentsTargetDistribution() {
        migrateDevSeed();

        Integer totalComments = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM comments WHERE body LIKE '[dev-seed]%'",
            Integer.class
        );

        Integer projectComments = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM comments WHERE body LIKE '[dev-seed]%' AND project_id IS NOT NULL",
            Integer.class
        );

        Integer issueComments = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM comments WHERE body LIKE '[dev-seed]%' AND issue_id IS NOT NULL",
            Integer.class
        );

        assertTrue(totalComments != null && totalComments >= 5 && totalComments <= 7,
            "Must seed 5 to 7 comments, found: " + totalComments);
        assertTrue(projectComments != null && projectComments >= 1, 
            "Must contain at least 1 project-attached comment");
        assertTrue(issueComments != null && issueComments >= 1, 
            "Must contain at least 1 issue-attached comment");
    }

    @Test
    @DisplayName("Dev Seed: Verify 18-20 claims with exactly 2 active claims (different users/issues, doesn't violate the partial index)")
    void shouldVerifyClaimsSeedIntegrity() {
        migrateDevSeed();

        Integer totalClaims = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM claims WHERE note LIKE '[dev-seed]%'",
            Integer.class
        );

        Integer activeClaims = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM claims WHERE status = 'active' AND note LIKE '[dev-seed]%'",
            Integer.class
        );

        assertTrue(totalClaims != null && totalClaims >= 18 && totalClaims <= 20,
            "Must seed 18 to 20 claims, found: " + totalClaims);
        assertEquals(2, activeClaims, "Should contain exactly 2 active claims (distinct issue/user pairs)");
    }

    @Test
    @DisplayName("Acceptance Criteria: Base profile has no seed migration available; dev profile seeds DB-01.6 specifications")
    void shouldVerifyProfileIsolationAndDevProfileSeeding() {
        // --- 1. BASE CONFIGURATION VERIFICATION ---
        Flyway baseFlyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load();
        
        baseFlyway.repair();

        boolean baseSeesDevSeedMigration = java.util.Arrays.stream(baseFlyway.info().all())
            .anyMatch(info -> info.getDescription().toLowerCase().contains("seed"));

        assertFalse(baseSeesDevSeedMigration,
            "Base migration locations must not include the dev seed migration");

        // --- 2. DEV PROFILE SEEDING VERIFICATION ---
        migrateDevSeed();

        // 3. Verify DB-01.6 specifications
        Integer devUsers = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users WHERE username IN ('winter_stone', 'winter_dev', 'montic_codes')", Integer.class);
        Integer devProjects = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM projects WHERE github_owner = 'codemaster'", Integer.class);
        Integer devIssues = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM issues WHERE github_url LIKE 'https://github.com/codemaster/%'", Integer.class);
        Integer devBeginnerIssues = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM issues WHERE difficulty = 'beginner' AND github_url LIKE 'https://github.com/codemaster/%'", Integer.class);
        Integer devComments = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM comments WHERE body LIKE '[dev-seed]%'", Integer.class);
        Integer devClaims = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM claims WHERE note LIKE '[dev-seed]%'", Integer.class);
        Integer devActiveClaims = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM claims WHERE status = 'active' AND note LIKE '[dev-seed]%'", Integer.class);

        assertEquals(2, devUsers, "Dev profile must seed exactly 2 users");
        assertEquals(4, devProjects, "Dev profile must seed exactly 4 projects");
        assertTrue(devIssues != null && devIssues >= 18 && devIssues <= 20, "Expected 18–20 issues");
        assertTrue(devBeginnerIssues != null && devBeginnerIssues >= 8, "Expected at least 8 beginner issues");
        assertTrue(devComments != null && devComments >= 5 && devComments <= 7, "Expected 5–7 comments");
        assertTrue(devClaims != null && devClaims >= 18 && devClaims <= 20, "Expected 18–20 claims");
        assertEquals(2, devActiveClaims, "Expected exactly 2 active claims");
    }
}