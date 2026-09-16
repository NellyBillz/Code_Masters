package za.codemaster.backend.mock;

import org.junit.jupiter.api.Test;
import za.codemaster.backend.dto.Difficulty;
import za.codemaster.backend.dto.Issue;
import za.codemaster.backend.dto.Project;
import za.codemaster.backend.dto.ProjectConnection;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies MockDataStore satisfies API-01.2's acceptance criteria: enough
 * rows, and enough variety across key fields, for API-01.3–.6's filters to
 * have something real to filter against.
 */
class MockDataStoreTest {

    private final MockDataStore store = new MockDataStore();

    @Test
    void hasAtLeastFiveProjects() {
        List<Project> projects = store.projects();
        assertTrue(projects.size() >= 5,
                "Expected at least 5 mock projects, found " + projects.size());
    }

    @Test
    void hasAtLeastEightIssues() {
        List<Issue> issues = store.issues();
        assertTrue(issues.size() >= 8,
                "Expected at least 8 mock issues, found " + issues.size());
    }

    @Test
    void projectsSpanAtLeastTwoConnectionValues() {
        Set<ProjectConnection> connections = store.projects().stream()
                .map(Project::connection)
                .collect(Collectors.toSet());
        assertTrue(connections.size() >= 2,
                "Expected projects to span multiple ProjectConnection values, found only: " + connections);
    }

    @Test
    void projectsSpanAtLeastTwoLanguages() {
        Set<String> languages = store.projects().stream()
                .map(Project::primaryLanguage)
                .collect(Collectors.toSet());
        assertTrue(languages.size() >= 2,
                "Expected projects to span multiple primaryLanguage values, found only: " + languages);
    }

    @Test
    void issuesSpanAtLeastTwoDifficultyValues() {
        Set<Difficulty> difficulties = store.issues().stream()
                .map(Issue::difficulty)
                .collect(Collectors.toSet());
        assertTrue(difficulties.size() >= 2,
                "Expected issues to span multiple Difficulty values, found only: " + difficulties);
    }

    @Test
    void everyProjectHasHasBeginnerFriendlyIssuesFieldPresent() {
        // hasBeginnerFriendlyIssues is a primitive boolean, so "present" really
        // means "at least one project is true and at least one is false" —
        // otherwise the field exists but nobody would notice if it were
        // hardcoded to a single value.
        long trueCount = store.projects().stream().filter(Project::hasBeginnerFriendlyIssues).count();
        long falseCount = store.projects().size() - trueCount;
        assertTrue(trueCount > 0 && falseCount > 0,
                "Expected a mix of true/false hasBeginnerFriendlyIssues, got true="
                        + trueCount + " false=" + falseCount);
    }
}