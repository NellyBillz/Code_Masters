package za.codemaster.backend.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import za.codemaster.backend.BackendApplication;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
    classes = BackendApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@Transactional
public class AdminAndProjectLifecycleTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Acceptance Criteria 1: Invalid listing_status ('archived') is rejected by check constraint")
    void shouldRejectArchivedListingStatusCheckConstraint() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        DataIntegrityViolationException ex = assertThrows(
            DataIntegrityViolationException.class,
            () -> jdbcTemplate.update(
                "INSERT INTO projects (" +
                "  github_owner, github_repo, github_url, name, slug, connection, listing_status" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?)",
                "codemaster",
                "archived-" + suffix,
                "https://github.com/codemaster/archived-" + suffix,
                "Archived Project",
                "slug-archived-" + suffix,
                "south_african",
                "archived"
            )
        );

        String rootCause = ex.getMostSpecificCause().getMessage();
        assertTrue(
            rootCause.contains("chk_projects_listing_status") || rootCause.contains("violates check constraint"),
            "Expected check constraint failure on listing_status, but got: " + rootCause
        );
    }

    @Test
    @DisplayName("Acceptance Criteria 2: New projects receive default 'pending' listing status and accepting_contributions true")
    void shouldApplyDefaultsToNewProjects() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        Long projectId = jdbcTemplate.queryForObject(
            "INSERT INTO projects (" +
            "  github_owner, github_repo, github_url, name, slug, connection" +
            ") VALUES (?, ?, ?, ?, ?, ?) RETURNING id",
            Long.class,
            "codemaster",
            "pending-" + suffix,
            "https://github.com/codemaster/pending-" + suffix,
            "Pending Project",
            "slug-pending-" + suffix,
            "south_african"
        );

        Map<String, Object> projectRow = jdbcTemplate.queryForMap(
            "SELECT listing_status, accepting_contributions, has_contributing_guide, has_code_of_conduct " +
            "FROM projects WHERE id = ?",
            projectId
        );

        assertEquals("pending", projectRow.get("listing_status"));
        assertEquals(true, projectRow.get("accepting_contributions"));
        assertEquals(false, projectRow.get("has_contributing_guide"));
        assertEquals(false, projectRow.get("has_code_of_conduct"));
    }

    @Test
    @DisplayName("Acceptance Criteria 3: Users receive is_site_admin default false")
    void shouldApplyDefaultToUsersIsSiteAdmin() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        Long userId = jdbcTemplate.queryForObject(
            "INSERT INTO users (github_id, username, display_name) VALUES (?, ?, ?) RETURNING id",
            Long.class,
            System.nanoTime(),
            "admin_test_" + suffix,
            "Admin Test User"
        );

        Boolean isSiteAdmin = jdbcTemplate.queryForObject(
            "SELECT is_site_admin FROM users WHERE id = ?",
            Boolean.class,
            userId
        );

        assertFalse(isSiteAdmin, "New users must have is_site_admin set to false by default");
    }
}