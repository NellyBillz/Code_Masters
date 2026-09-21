package za.codemaster.backend.client.github;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import za.codemaster.backend.client.github.dto.ClosingPullRequestResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Live acceptance test for hbldh/bleak#1280, closed by merged PR #1281.
 * GitHub's issue and PR pages show that the PR was merged by dlech.
 */
@EnabledIfEnvironmentVariable(named = "GITHUB_API_TOKEN", matches = ".+")
class GitHubClientClosingPullRequestIntegrationTest {

    @Test
    void findsRealKeywordLinkedMergedPullRequestAndAuthor() {
        GitHubClient client = new GitHubClient(
                "https://api.github.com", System.getenv("GITHUB_API_TOKEN"));

        ClosingPullRequestResult result =
                client.fetchClosingPullRequest("hbldh", "bleak", 1280);
        ClosingPullRequestResult.Found found =
                assertInstanceOf(ClosingPullRequestResult.Found.class, result);

        assertEquals(1281, found.pullRequestNumber());
        assertTrue(found.merged());
        assertEquals("dlech", found.authorLogin());
        assertEquals(963645L, found.authorId());
    }
}
