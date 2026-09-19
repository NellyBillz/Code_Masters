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
 * Full-stack acceptance tests for API-02.11 (round2-tickets.md). This endpoint is
 * public (no {@code security} block in the spec), so unlike most other controller
 * integration tests in this package, no session/CSRF setup is needed here — just
 * the real {@code SecurityFilterChain} confirming GET stays public as expected.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SearchControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private IssueRepository issueRepository;

    @BeforeEach
    void setUp() {
        ProjectQueryServiceFixtures.seed(projectRepository, issueRepository);
    }

    @Test
    @DisplayName("q under 2 characters -> 400 VALIDATION_ERROR")
    void queryUnderTwoCharactersIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/search").param("q", "a"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("missing q -> 400 VALIDATION_ERROR, not a 500")
    void missingQueryIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("type=projects returns only project results")
    void typeProjectsReturnsOnlyProjectResults() throws Exception {
        mockMvc.perform(get("/api/v1/search").param("q", "open").param("type", "projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.items[*].resultType", org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.equalTo("project"))));
    }

    @Test
    @DisplayName("type=issues returns only issue results")
    void typeIssuesReturnsOnlyIssueResults() throws Exception {
        mockMvc.perform(get("/api/v1/search").param("q", "translation").param("type", "issues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.items[*].resultType", org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.equalTo("issue"))));
    }

    @Test
    @DisplayName("type=all (or omitted) merges both project and issue results")
    void typeAllMergesBothResultTypes() throws Exception {
        mockMvc.perform(get("/api/v1/search").param("q", "in"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].resultType",
                        org.hamcrest.Matchers.hasItem("project")))
                .andExpect(jsonPath("$.items[*].resultType",
                        org.hamcrest.Matchers.hasItem("issue")));
    }

    @Test
    @DisplayName("A project search result carries full project fields alongside resultType")
    void projectResultCarriesFullProjectFields() throws Exception {
        mockMvc.perform(get("/api/v1/search").param("q", "open").param("type", "projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].resultType").value("project"))
                .andExpect(jsonPath("$.items[0].name").exists())
                .andExpect(jsonPath("$.items[0].githubUrl").exists());
    }

    @Test
    @DisplayName("An issue search result carries full issue fields alongside resultType")
    void issueResultCarriesFullIssueFields() throws Exception {
        mockMvc.perform(get("/api/v1/search").param("q", "translation").param("type", "issues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].resultType").value("issue"))
                .andExpect(jsonPath("$.items[0].title").exists())
                .andExpect(jsonPath("$.items[0].githubUrl").exists());
    }

    @Test
    @DisplayName("No results is a clean empty list, not an error")
    void noMatchesReturnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/v1/search").param("q", "zzzznomatchzzzz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.meta.total").value(0));
    }
}
