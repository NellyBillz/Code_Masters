package za.codemaster.backend.client.github;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import za.codemaster.backend.client.github.dto.GitHubFetchResult;
import za.codemaster.backend.client.github.dto.GitHubIssueMetadata;
import za.codemaster.backend.client.github.dto.GitHubProjectMetadata;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Acceptance test for this ticket ("don't count against quota if nothing
 * changed"): call {@link GitHubClient#fetchProjectMetadata} /
 * {@link GitHubClient#fetchIssues} once to get data plus an ETag, call again
 * immediately with that exact ETag, and confirm the second call comes back
 * as the typed "not modified" branch of {@link GitHubFetchResult} - not an
 * exception, and not a body re-parsed from nothing.
 * <p>
 * Deliberately plain JUnit, no Spring context, same rationale as the other
 * GitHubClient integration tests: {@link GitHubClient} is instantiated
 * directly, so no database or app context is needed to exercise this.
 * Requires {@code GITHUB_API_TOKEN} in the environment; skips automatically
 * when it's absent.
 * <p>
 * Uses octocat/Hello-World for both methods - GitHub's own long-frozen
 * tutorial repo (see {@link GitHubClientIntegrationTest}'s javadoc), chosen
 * here specifically because two calls made milliseconds apart need the
 * underlying data to genuinely not change in between; a very quiet repo
 * makes that close to certain, whereas the busier repos used elsewhere in
 * this test suite (chosen instead for having plenty of open issues to
 * exercise pagination/field-mapping) are a less certain fit for this.
 */
@EnabledIfEnvironmentVariable(named = "GITHUB_API_TOKEN", matches = ".+")
class GitHubClientConditionalFetchIntegrationTest {

    private static final String OWNER = "octocat";
    private static final String REPO = "Hello-World";

    private GitHubClient client;

    @BeforeEach
    void setUp() {
        String token = System.getenv("GITHUB_API_TOKEN");
        client = new GitHubClient("https://api.github.com", token);
    }

    @Test
    void fetchProjectMetadataReturnsNotModifiedOnSecondCallWithSameEtag() {
        GitHubFetchResult<GitHubProjectMetadata> first = client.fetchProjectMetadata(OWNER, REPO, null);

        assertTrue(first.isModified(), "first, unconditional call should always return fresh data");
        assertNotNull(first.data());
        assertNotNull(first.etag(), "GitHub should have sent an ETag to key the next call off of");

        GitHubFetchResult<GitHubProjectMetadata> second =
                client.fetchProjectMetadata(OWNER, REPO, first.etag());

        assertFalse(second.isModified(),
                "sending back the ETag we just received should get a 304 - repo didn't change "
                        + "in the milliseconds between these two calls");
        assertNull(second.etag(), "not-modified result carries no new ETag - keep using the one you had");
    }

    @Test
    void fetchIssuesReturnsNotModifiedOnSecondCallWithSameEtag() {
        GitHubFetchResult<List<GitHubIssueMetadata>> first = client.fetchIssues(OWNER, REPO, null);

        assertTrue(first.isModified(), "first, unconditional call should always return fresh data");
        assertNotNull(first.data());
        assertNotNull(first.etag(), "GitHub should have sent an ETag to key the next call off of");

        GitHubFetchResult<List<GitHubIssueMetadata>> second =
                client.fetchIssues(OWNER, REPO, first.etag());

        assertFalse(second.isModified(),
                "sending back the ETag we just received should get a 304 - open issues on this "
                        + "repo didn't change in the milliseconds between these two calls");
        assertNull(second.etag(), "not-modified result carries no new ETag - keep using the one you had");
    }
}
