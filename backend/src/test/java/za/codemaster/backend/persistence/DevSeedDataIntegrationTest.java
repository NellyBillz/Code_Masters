package za.codemaster.backend.persistence;

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
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitConfig(DevSeedDataIntegrationTest.TestDbConfig.class)
public class DevSeedDataIntegrationTest {

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

        @Bean
        public DataSource dataSource() {
            PGSimpleDataSource ds = new PGSimpleDataSource();
            ds.setServerNames(new String[]{"localhost"});
            ds.setPortNumbers(new int[]{5432});
            ds.setDatabaseName("codemaster_db");
            ds.setUser("postgres");
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

    @Test
    @DisplayName("Dev Seed: Verify exactly 2 users are seeded")
    void shouldVerifySeedUserCount() {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users WHERE username IN ('winter_dev', 'montic_codes')",
            Integer.class
        );

        assertEquals(2, count, "Seed data must contain exactly 2 users");
    }

    @Test
    @DisplayName("Dev Seed: Verify 4 projects spanning at least 2 connections and 2 primary languages")
    void shouldVerifyProjectDiversity() {
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
        assertTrue(connections.size() >= 2, "Must contain at least 2 distinct connections (e.g., south_african, africa_led)");
        assertTrue(languages.size() >= 2, "Must contain at least 2 distinct primary languages (e.g., Java, Python)");
    }

    @Test
    @DisplayName("Dev Seed: Verify 6-8 issues seeded with at least 3 marked beginner")
    void shouldVerifyIssuesAndBeginnerFriendlyRequirements() {
        Integer totalIssues = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM issues WHERE github_url LIKE 'https://github.com/codemaster/%'",
            Integer.class
        );

        Integer beginnerIssues = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM issues WHERE difficulty = 'beginner' AND github_url LIKE 'https://github.com/codemaster/%'",
            Integer.class
        );

        assertTrue(totalIssues != null && totalIssues >= 6 && totalIssues <= 8, 
            "Issues count must be between 6 and 8, found: " + totalIssues);
        assertTrue(beginnerIssues != null && beginnerIssues >= 3, 
            "At least 3 issues must have difficulty = 'beginner', found: " + beginnerIssues);
    }

    @Test
    @DisplayName("Dev Seed: Verify comments mix (project-attached and issue-attached)")
    void shouldVerifyCommentsTargetDistribution() {
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

        assertTrue(totalComments != null && totalComments >= 3 && totalComments <= 4, 
            "Must seed 3 to 4 comments, found: " + totalComments);
        assertTrue(projectComments != null && projectComments >= 1, 
            "Must contain at least 1 project-attached comment");
        assertTrue(issueComments != null && issueComments >= 1, 
            "Must contain at least 1 issue-attached comment");
    }

    @Test
    @DisplayName("Dev Seed: Verify 1-2 claims with maximum one active claim per user")
    void shouldVerifyClaimsSeedIntegrity() {
        Integer totalClaims = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM claims WHERE note LIKE '[dev-seed]%'",
            Integer.class
        );

        Integer activeClaims = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM claims WHERE status = 'active' AND note LIKE '[dev-seed]%'",
            Integer.class
        );

        assertTrue(totalClaims != null && totalClaims >= 1 && totalClaims <= 2, 
            "Must seed 1 to 2 claims, found: " + totalClaims);
        assertEquals(1, activeClaims, "Should contain exactly 1 active claim adhering to partial index");
    }
}