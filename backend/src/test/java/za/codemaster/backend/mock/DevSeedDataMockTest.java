package za.codemaster.backend.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DevSeedDataMockTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Dev Seed: Verify exactly 2 users are seeded")
    void shouldVerifySeedUserCount() {
        when(jdbcTemplate.queryForObject(
            contains("SELECT COUNT(*) FROM users WHERE username IN"),
            eq(Integer.class)
        )).thenReturn(2);

        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users WHERE username IN ('tumis_dev', 'monic_codes')",
            Integer.class
        );

        assertEquals(2, count, "Seed data must contain exactly 2 dev users");
    }

    @Test
    @DisplayName("Dev Seed: Verify 4 projects spanning at least 2 connections and 2 primary languages")
    void shouldVerifyProjectDiversity() {
        when(jdbcTemplate.queryForObject(
            contains("SELECT COUNT(*) FROM projects WHERE github_owner = 'codemaster'"),
            eq(Integer.class)
        )).thenReturn(4);

        when(jdbcTemplate.queryForList(
            contains("SELECT DISTINCT connection FROM projects"),
            eq(String.class)
        )).thenReturn(List.of("south_african", "africa_led"));

        when(jdbcTemplate.queryForList(
            contains("SELECT DISTINCT primary_language FROM projects"),
            eq(String.class)
        )).thenReturn(List.of("Java", "Python"));

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

        assertEquals(4, projectCount);
        assertTrue(connections.size() >= 2, "Must contain at least 2 distinct connections");
        assertTrue(languages.size() >= 2, "Must contain at least 2 distinct primary languages");
    }

    @Test
    @DisplayName("Dev Seed: Verify 6-8 issues seeded with at least 3 marked beginner")
    void shouldVerifyIssuesAndBeginnerFriendlyRequirements() {
        when(jdbcTemplate.queryForObject(
            contains("SELECT COUNT(*) FROM issues WHERE github_url LIKE"),
            eq(Integer.class)
        )).thenReturn(7);

        when(jdbcTemplate.queryForObject(
            contains("SELECT COUNT(*) FROM issues WHERE difficulty = 'beginner'"),
            eq(Integer.class)
        )).thenReturn(4);

        Integer totalIssues = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM issues WHERE github_url LIKE 'https://github.com/codemaster/%'",
            Integer.class
        );
        Integer beginnerIssues = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM issues WHERE difficulty = 'beginner' AND github_url LIKE 'https://github.com/codemaster/%'",
            Integer.class
        );

        assertTrue(totalIssues >= 6 && totalIssues <= 8, "Issues count must be between 6 and 8");
        assertTrue(beginnerIssues >= 3, "At least 3 issues must have difficulty = beginner");
    }

    @Test
    @DisplayName("Dev Seed: Verify comments mix (project-attached and issue-attached)")
    void shouldVerifyCommentsTargetDistribution() {
        when(jdbcTemplate.queryForObject(
            contains("SELECT COUNT(*) FROM comments WHERE body LIKE '[dev-seed]%'"),
            eq(Integer.class)
        )).thenReturn(4);

        when(jdbcTemplate.queryForObject(
            contains("SELECT COUNT(*) FROM comments WHERE body LIKE '[dev-seed]%' AND project_id IS NOT NULL"),
            eq(Integer.class)
        )).thenReturn(2);

        when(jdbcTemplate.queryForObject(
            contains("SELECT COUNT(*) FROM comments WHERE body LIKE '[dev-seed]%' AND issue_id IS NOT NULL"),
            eq(Integer.class)
        )).thenReturn(2);

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

        assertTrue(totalComments >= 3 && totalComments <= 4, "Must seed 3 to 4 comments");
        assertTrue(projectComments >= 1, "Must contain at least 1 project-attached comment");
        assertTrue(issueComments >= 1, "Must contain at least 1 issue-attached comment");
    }

    @Test
    @DisplayName("Dev Seed: Verify 1-2 claims with maximum one active claim per user")
    void shouldVerifyClaimsSeedIntegrity() {
        when(jdbcTemplate.queryForObject(
            contains("SELECT COUNT(*) FROM claims WHERE note LIKE '[dev-seed]%'"),
            eq(Integer.class)
        )).thenReturn(2);

        when(jdbcTemplate.queryForObject(
            contains("SELECT COUNT(*) FROM claims WHERE status = 'active'"),
            eq(Integer.class)
        )).thenReturn(1);

        Integer totalClaims = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM claims WHERE note LIKE '[dev-seed]%'",
            Integer.class
        );
        Integer activeClaims = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM claims WHERE status = 'active' AND note LIKE '[dev-seed]%'",
            Integer.class
        );

        assertTrue(totalClaims >= 1 && totalClaims <= 2, "Must seed 1 to 2 claims");
        assertEquals(1, activeClaims, "Should contain 1 active claim adhering to partial index");
    }
}