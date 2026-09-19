package za.codemaster.backend.client.github;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import za.codemaster.backend.client.github.dto.GitHubFetchResult;
import za.codemaster.backend.client.github.dto.GitHubIssueMetadata;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GH-1.4 acceptance test: {@link GitHubClient#fetchIssues} against a real
 * public repo with more open issues than fit on a single page, proving it
 * actually walks every page via the {@code Link} response header rather
 * than stopping after the first (GH-1.3's behavior).
 * <p>
 * Uses expressjs/express, which reliably has well over GitHub's default
 * page size (30) worth of open, non-pull-request issues - over a hundred
 * as of when this was written, so it has comfortable headroom before
 * shrinking anywhere near the one-page threshold this test checks against.
 * Deliberately not octocat/Spoon-Knife or github/docs (used by {@link
 * GitHubClientIssuesIntegrationTest}) - those are picked for having just
 * enough open issues to exercise field mapping quickly, not for having
 * many pages' worth.
 * <p>
 * This test does not re-verify per-field mapping correctness - that is
 * {@link GitHubClientIssuesIntegrationTest}'s job. It only checks the two
 * things specific to pagination: the combined result is longer than one
 * page's worth, and walking multiple pages didn't introduce duplicates
 * (each issue number appears exactly once).
 * <p>
 * Deliberately plain JUnit, no Spring context - same rationale as the
 * other GitHubClient integration tests. Requires {@code GITHUB_API_TOKEN}
 * in the environment; skips automatically when it's absent.
 */
@EnabledIfEnvironmentVariable(named = "GITHUB_API_TOKEN", matches = ".+")
class GitHubClientPaginationIntegrationTest {

    private static final String OWNER = "expressjs";
    private static final String REPO = "express";

    /** GitHub's default page size when no {@code per_page} is requested. */
    private static final int GITHUB_DEFAULT_PAGE_SIZE = 30;

    private GitHubClient client;

    @BeforeEach
    void setUp() {
        String token = System.getenv("GITHUB_API_TOKEN");
        client = new GitHubClient("https://api.github.com", token);
    }

    @Test
    void fetchIssuesWalksEveryPageWithNoDuplicates() {
        // Updated call site only (unconditional fetch: sinceEtag null) - fetchIssues's signature
        // and GitHubFetchResult wrapper are new in the ETag/If-None-Match ticket; this test's own
        // pagination/duplicate assertions below are unchanged.
        GitHubFetchResult<List<GitHubIssueMetadata>> result = client.fetchIssues(OWNER, REPO, null);
        assertTrue(result.isModified());
        List<GitHubIssueMetadata> issues = result.data();

        assertNotNull(issues);
        assertTrue(issues.size() > GITHUB_DEFAULT_PAGE_SIZE,
                "expected more than " + GITHUB_DEFAULT_PAGE_SIZE + " open issues in "
                        + OWNER + "/" + REPO + " to prove pagination actually ran (got "
                        + issues.size() + ") - if this repo has quieted down, point OWNER/REPO "
                        + "at another public repo with a few hundred open issues");

        Set<Integer> distinctNumbers = new HashSet<>();
        for (GitHubIssueMetadata issue : issues) {
            distinctNumbers.add(issue.issueNumber());
        }

        assertEquals(issues.size(), distinctNumbers.size(),
                "fetchIssues returned duplicate issue numbers - pagination likely "
                        + "re-fetched a page it had already collected");
    }
}