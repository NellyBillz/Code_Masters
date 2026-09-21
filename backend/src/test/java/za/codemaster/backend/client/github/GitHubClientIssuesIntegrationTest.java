package za.codemaster.backend.client.github;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import za.codemaster.backend.client.github.dto.GitHubFetchResult;
import za.codemaster.backend.client.github.dto.GitHubIssueMetadata;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * GH-1.3 acceptance test: a real call to the real GitHub API for a fixed,
 * well-known public repo, with every {@link GitHubIssueMetadata} field
 * checked for at least 2 issues.
 * <p>
 * Unlike {@link GitHubClientIntegrationTest} (GH-1's repo-metadata sibling,
 * which hardcodes expected values because octocat/Hello-World's About-panel
 * text is effectively frozen), this test does not hardcode issue titles or
 * bodies. github/docs is a large, actively edited repo, so its open issues
 * churn regularly - any specific title or body text captured today could
 * easily change or close by the time this test next runs.
 * <p>
 * Instead this test independently re-fetches the exact same GitHub endpoint
 * with a plain, hand-rolled {@link HttpClient} call (deliberately bypassing
 * {@link GitHubClient} and {@link GitHubIssueMetadata} entirely) and
 * compares the raw JSON against what {@link GitHubClient#fetchIssues}
 * mapped, field by field, keyed by issue number rather than list position
 * so a new issue landing between the two calls can't desync the
 * comparison. That proves the mapping itself is correct against whatever
 * live data happens to be there, without depending on that data staying
 * still.
 * <p>
 * Deliberately points at a repo with few enough open issues (a couple of
 * pages at most) that {@link GitHubClient#fetchIssues} - which now walks
 * every page, per GH-1.4 - stays fast here. The raw comparison fetch below
 * deliberately only reads page one: since the fields checked are always the
 * first {@value #MIN_ISSUES_TO_CHECK} real issues {@code fetchIssues}
 * returns, and pages come back in order, those will always be on page one.
 * GH-1.4's own pagination behavior (walking every page, not just the first)
 * is covered separately in {@link GitHubClientPaginationIntegrationTest}.
 * <p>
 * Deliberately plain JUnit, no Spring context - same rationale as {@link
 * GitHubClientIntegrationTest}. Requires {@code GITHUB_API_TOKEN} in the
 * environment; skips automatically when it's absent.
 */
@EnabledIfEnvironmentVariable(named = "GITHUB_API_TOKEN", matches = ".+")
class GitHubClientIssuesIntegrationTest {

    private static final String OWNER = "github";
    private static final String REPO = "docs";
    private static final int MIN_ISSUES_TO_CHECK = 2;

    private GitHubClient client;
    private String token;

    @BeforeEach
    void setUp() {
        token = System.getenv("GITHUB_API_TOKEN");
        client = new GitHubClient("https://api.github.com", token);
    }

    @Test
    void fetchIssuesMapsEveryFieldForAtLeastTwoRealIssues() throws Exception {
        // Updated call site only (unconditional fetch: sinceEtag null) - fetchIssues's signature
        // and GitHubFetchResult wrapper are new in the ETag/If-None-Match ticket; this test's own
        // field-mapping assertions below are unchanged.
        GitHubFetchResult<List<GitHubIssueMetadata>> result = client.fetchIssues(OWNER, REPO, null);
        assertTrue(result.isModified());
        List<GitHubIssueMetadata> issues = result.data();

        assertNotNull(issues);
        assertTrue(issues.size() >= MIN_ISSUES_TO_CHECK,
                "need at least " + MIN_ISSUES_TO_CHECK + " open issues in " + OWNER + "/" + REPO
                        + " to exercise this test - it's a busy repo, but if it's gone quiet, "
                        + "point OWNER/REPO at another public repo with open issues");

        Map<Integer, JsonNode> rawIssuesByNumber = fetchRawOpenIssuesByNumber();

        int checked = 0;
        for (GitHubIssueMetadata actual : issues) {
            JsonNode expected = rawIssuesByNumber.get(actual.issueNumber());
            if (expected == null) {
                // Vanishingly rare: the issue closed in the instant between
                // GitHubClient's call and this test's own raw call. Skip it
                // rather than fail the test on a timing fluke.
                continue;
            }

            assertEquals(expected.get("number").asInt(), actual.issueNumber());
            assertEquals(expected.get("title").asString(), actual.title());
            assertEquals(
                    expected.get("body").isNull() ? null : expected.get("body").asString(),
                    actual.body());
            assertEquals(expected.get("html_url").asString(), actual.htmlUrl());
            assertEquals(OffsetDateTime.parse(expected.get("created_at").asString()), actual.createdAt());
            assertEquals(OffsetDateTime.parse(expected.get("updated_at").asString()), actual.updatedAt());

            List<String> expectedLabels = StreamSupport.stream(expected.get("labels").spliterator(), false)
                    .map(label -> label.get("name").asString())
                    .toList();
            assertEquals(expectedLabels, actual.labels());

            checked++;
            if (checked >= MIN_ISSUES_TO_CHECK) {
                break;
            }
        }

        if (checked < MIN_ISSUES_TO_CHECK) {
            fail("only matched " + checked + " issue(s) between GitHubClient's fetch and the raw "
                    + "re-fetch (needed " + MIN_ISSUES_TO_CHECK + ") - re-run, this repo churns fast");
        }
    }

    /**
     * Re-fetches {@code GET /repos/{owner}/{repo}/issues?state=open}
     * directly, with the same auth GitHubClient uses but none of its
     * mapping, filters out pull requests the same way GitHubClient does,
     * and indexes the result by issue number.
     */
    private Map<Integer, JsonNode> fetchRawOpenIssuesByNumber() throws Exception {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/repos/" + OWNER + "/" + REPO + "/issues?state=open"))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), "raw GitHub call failed: " + response.body());

        JsonNode rawIssues = JsonMapper.builder().build().readTree(response.body());
        assertTrue(rawIssues.isArray());

        return StreamSupport.stream(rawIssues.spliterator(), false)
                .filter(node -> node.get("pull_request") == null || node.get("pull_request").isNull())
                .collect(Collectors.toMap(node -> node.get("number").asInt(), node -> node));
    }
}