package za.codemaster.backend.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import za.codemaster.backend.BackendApplication;
import za.codemaster.backend.domain.model.Project;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
public class PostgresSearchIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ProjectRepository projectRepository;

    private String suffix;

    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString().substring(0, 8);
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM project_tags WHERE project_id IN (SELECT id FROM projects WHERE slug LIKE ?)", "%" + suffix + "%");
        jdbcTemplate.update("DELETE FROM project_countries WHERE project_id IN (SELECT id FROM projects WHERE slug LIKE ?)", "%" + suffix + "%");
        jdbcTemplate.update("DELETE FROM projects WHERE slug LIKE ?", "%" + suffix + "%");
    }

    @Test
    @DisplayName("Acceptance Criteria 1: EXPLAIN on a q=-driven query shows GIN index used, not sequential scan")
    void shouldVerifyGinIndexUsedForSearchQuery() {
        // Force PostgreSQL query planner to prefer index paths on low-volume test tables
        jdbcTemplate.execute("SET enable_seqscan = OFF;");

        List<String> queryPlan = jdbcTemplate.queryForList(
            "EXPLAIN SELECT id FROM projects WHERE tsv @@ plainto_tsquery('english', 'react')",
            String.class
        );

        String fullPlan = String.join("\n", queryPlan);

        assertTrue(
            fullPlan.contains("idx_projects_tsv") || fullPlan.contains("Bitmap Index Scan"),
            "Expected plan to use idx_projects_tsv. Plan was:\n" + fullPlan
        );
        assertFalse(
            fullPlan.contains("Seq Scan on projects"),
            "Plan should not execute a sequential scan. Plan was:\n" + fullPlan
        );

        // Reset planner state
        jdbcTemplate.execute("SET enable_seqscan = ON;");
    }

    @Test
    @DisplayName("Acceptance Criteria 2: Small typo (raect for react) returns intended project via trigram fallback")
    void shouldReturnProjectViaTrigramFallbackWhenTsQueryIsEmpty() {
        Long projectId = jdbcTemplate.queryForObject(
            "INSERT INTO projects (github_owner, github_repo, github_url, name, slug, connection, listing_status, description) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id",
            Long.class,
            "codemaster",
            "react-starter-" + suffix,
            "https://github.com/codemaster/react-starter-" + suffix,
            "React",
            "react-starter-" + suffix,
            "south_african",
            "published",
            "A starter project template for frontend engineering"
        );

        // Verify that exact tsquery for "raect" does not match the tsvector stem of "react"
        Integer exactTsMatches = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM projects WHERE id = ? AND tsv @@ plainto_tsquery('english', 'raect')",
            Integer.class,
            projectId
        );
        assertEquals(0, exactTsMatches, "tsquery for misspelled 'raect' should not match 'react'");

        // Verify repository query triggers trigram similarity fallback
        Page<Project> results = projectRepository.searchProjects(
            "raect",
            "published",
            null, null, null, null, null, null,
            true,
            PageRequest.of(0, 10)
        );

        assertFalse(results.isEmpty(), "Trigram fallback should have retrieved 'React'");
        assertEquals(projectId, results.getContent().get(0).getId());
    }

    @Test
    @DisplayName("Acceptance Criteria 3: sort=relevance with q= present orders by rank, not insertion order")
    void shouldOrderByRelevanceRankNotInsertionOrder() {
        // Project 1 inserted FIRST: Low relevance match (mentions 'react' only in description)
        Long firstId = jdbcTemplate.queryForObject(
            "INSERT INTO projects (github_owner, github_repo, github_url, name, slug, connection, listing_status, description) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id",
            Long.class,
            "codemaster",
            "tools-" + suffix,
            "https://github.com/codemaster/tools-" + suffix,
            "General Web Utilities",
            "tools-" + suffix,
            "south_african",
            "published",
            "A collection of utilities that happens to integrate with react"
        );

        // Project 2 inserted SECOND: High relevance match ('React' weighted 'A' in name + in description)
        Long secondId = jdbcTemplate.queryForObject(
            "INSERT INTO projects (github_owner, github_repo, github_url, name, slug, connection, listing_status, description) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id",
            Long.class,
            "codemaster",
            "react-core-" + suffix,
            "https://github.com/codemaster/react-core-" + suffix,
            "React Core Framework",
            "react-core-" + suffix,
            "south_african",
            "published",
            "Core components and hooks for React applications"
        );

        // Query with q = 'React' and sortByRelevance = true
        Page<Project> page = projectRepository.searchProjects(
            "React",
            "published",
            null, null, null, null, null, null,
            true,
            PageRequest.of(0, 10)
        );

        List<Project> content = page.getContent();
        assertTrue(content.size() >= 2);

        // Second inserted project must rank first due to higher ts_rank + trigram name similarity
        assertEquals(secondId, content.get(0).getId(), "High relevance project must appear first");
        assertEquals(firstId, content.get(1).getId(), "Lower relevance project must appear second");
    }

    @Test
    @DisplayName("In-Query Filtering: Tag and country filters executed in SQL without row duplication")
    void shouldFilterByTagAndCountryInSameQuery() {
        Long p1Id = jdbcTemplate.queryForObject(
            "INSERT INTO projects (github_owner, github_repo, github_url, name, slug, connection, listing_status, description) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id",
            Long.class,
            "codemaster", "p1-" + suffix, "https://github.com/codemaster/p1-" + suffix,
            "React SA", "p1-" + suffix, "south_african", "published", "React ecosystem"
        );

        Long p2Id = jdbcTemplate.queryForObject(
            "INSERT INTO projects (github_owner, github_repo, github_url, name, slug, connection, listing_status, description) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id",
            Long.class,
            "codemaster", "p2-" + suffix, "https://github.com/codemaster/p2-" + suffix,
            "React Kenya", "p2-" + suffix, "africa_led", "published", "React ecosystem"
        );

        jdbcTemplate.update("INSERT INTO project_tags (project_id, tag) VALUES (?, ?)", p1Id, "ui");
        jdbcTemplate.update("INSERT INTO project_tags (project_id, tag) VALUES (?, ?)", p2Id, "backend");
        jdbcTemplate.update("INSERT INTO project_countries (project_id, country_code) VALUES (?, ?)", p1Id, "ZA");
        jdbcTemplate.update("INSERT INTO project_countries (project_id, country_code) VALUES (?, ?)", p2Id, "KE");

        Page<Project> results = projectRepository.searchProjects(
            "React", "published", null, null, null, null, "ui", "ZA", true, PageRequest.of(0, 10)
        );

        assertEquals(1, results.getTotalElements());
        assertEquals(p1Id, results.getContent().get(0).getId());
    }
}