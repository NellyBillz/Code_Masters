package za.codemaster.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import za.codemaster.backend.repository.IssueRepository;
import za.codemaster.backend.repository.ProjectRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression test for a real bug discovered while building API-02.11: Spring's
 * default {@code @RequestParam} enum binding calls plain (case-sensitive)
 * {@code Enum.valueOf}, which only matches the Java constant name
 * ({@code BEGINNER}), never the spec's lowercase wire value ({@code beginner}) —
 * see {@code WireValued}. This means {@code GET /projects/{id}/issues?difficulty=beginner}
 * (the exact query shape the spec documents) 500'd in the real app, even though
 * every existing test for this filter passed — because those tests all called
 * {@link ProjectQueryService#getProjectIssues} directly (bypassing HTTP binding
 * entirely), never through a real MockMvc request with the actual wire-format
 * query string. This test closes that gap for {@code GET /projects/{projectId}/issues};
 * {@code SearchControllerIntegrationTest} covers the same fix for {@code GET /search}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProjectIssuesControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private IssueRepository issueRepository;

    private Long projectId;

    @BeforeEach
    void setUp() {
        ProjectQueryServiceFixtures fixtures = ProjectQueryServiceFixtures.seed(projectRepository, issueRepository);
        projectId = fixtures.projectId(0);
    }

    @Test
    @DisplayName("GET /projects/{id}/issues?difficulty=beginner (real HTTP, lowercase wire value) succeeds")
    void filtersByDifficultyOverRealHttp() throws Exception {
        mockMvc.perform(get("/api/v1/projects/{projectId}/issues", projectId)
                        .param("difficulty", "beginner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].difficulty",
                        org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.equalTo("beginner"))));
    }

    @Test
    @DisplayName("GET /projects/{id}/issues?status=open (real HTTP, lowercase wire value) succeeds")
    void filtersByStatusOverRealHttp() throws Exception {
        mockMvc.perform(get("/api/v1/projects/{projectId}/issues", projectId)
                        .param("status", "open"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].status",
                        org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.equalTo("open"))));
    }
}
