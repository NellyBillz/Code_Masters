package za.codemaster.backend.client.github;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import za.codemaster.backend.dto.GitHubFetchResult;
import za.codemaster.backend.dto.GitHubProjectMetadata;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GH-1 acceptance test: a real call to the real GitHub API for a fixed,
 * well-known public repo, with each {@link GitHubProjectMetadata} field
 * checked against what actually shows on
 * <a href="https://github.com/octocat/Hello-World">github.com/octocat/Hello-World</a>.
 * <p>
 * Deliberately plain JUnit, no Spring context ({@code @SpringBootTest}) -
 * {@link GitHubClient} is instantiated directly with its two constructor
 * args, exactly like a caller would in a unit test. That's the point of
 * "plain, unit-testable Java": you don't need to boot Spring, a database, or
 * anything else to exercise the real HTTP call and mapping logic.
 * <p>
 * Requires {@code GITHUB_API_TOKEN} in the environment (same token used for
 * the GH-1.2 spike / backend/.env). Skips automatically when it's absent, so
 * it never breaks a plain {@code mvn test} for someone without the token set
 * up - run it explicitly with the token exported to actually verify GH-1.
 * <p>
 * <b>Chose octocat/Hello-World on purpose:</b> it's GitHub's own tutorial
 * repo, effectively frozen (3 commits, one README, no activity in years),
 * so most fields below are stable rather than live/growing metrics. Values
 * were last eyeballed against the live page on 2026-09-16:
 * <ul>
 *   <li>About panel description: "My first repository on GitHub!"</li>
 *   <li>No language badge shown - the repo's only file (a plain-text
 *       {@code README}) has no detectable language, so GitHub's API
 *       reports {@code language: null}</li>
 *   <li>~3.7k stars, ~6.2k forks, 5k+ open issues (these DO grow over
 *       time since randos star/fork it constantly - asserted as floors,
 *       not exact numbers)</li>
 *   <li>No license shown in the About panel (no LICENSE file in the repo)</li>
 * </ul>
 * If a numeric floor assertion below ever fails, re-check the live page
 * before assuming the mapping is broken - it's far more likely the repo's
 * live stats than this code.
 */
@EnabledIfEnvironmentVariable(named = "GITHUB_API_TOKEN", matches = ".+")
class GitHubClientIntegrationTest {

    private static final String OWNER = "octocat";
    private static final String REPO = "Hello-World";

    private GitHubClient client;

    @BeforeEach
    void setUp() {
        String token = System.getenv("GITHUB_API_TOKEN");
        client = new GitHubClient("https://api.github.com", token);
    }

    @Test
    void fetchProjectMetadataMapsEveryFieldAgainstTheRealRepoPage() {
        // Updated call site only (unconditional fetch: sinceEtag null) - fetchProjectMetadata's
        // signature and GitHubFetchResult wrapper are new in the ETag/If-None-Match ticket;
        // this test's own field assertions below are unchanged.
        GitHubFetchResult<GitHubProjectMetadata> result = client.fetchProjectMetadata(OWNER, REPO, null);

        assertTrue(result.isModified());
        GitHubProjectMetadata metadata = result.data();

        assertNotNull(metadata);

        // Repo name - shown as the "Hello-World" part of "octocat / Hello-World" in the header.
        assertEquals("Hello-World", metadata.name());

        // About panel text, verbatim, unchanged for years.
        assertEquals("My first repository on GitHub!", metadata.description());

        // The repo's only content is a plain-text README with no extension -
        // GitHub's linguist can't detect a language from that, and the repo
        // page shows no language badge at all. null is the correct mapping.
        assertNull(metadata.primaryLanguage());

        // No detectable language -> GitHub's /languages endpoint returns {}.
        assertTrue(metadata.languageBreakdown().isEmpty());

        // Compare against the "Star"/"Fork" counts in the repo header, and the
        // issue count on the "Issues" tab. Floors, not exact numbers - they
        // only ever go up for this famous repo.
        assertTrue(metadata.stars() > 3000, "expected > 3000 stars, got " + metadata.stars());
        assertTrue(metadata.forks() > 5000, "expected > 5000 forks, got " + metadata.forks());
        assertTrue(metadata.openIssueCount() > 1000,
                "expected > 1000 open issues, got " + metadata.openIssueCount());

        // About panel shows no license (no LICENSE file in the repo).
        assertNull(metadata.license());

        // pushed_at must have parsed into a real, past instant.
        assertNotNull(metadata.lastActivityAt());
        assertTrue(metadata.lastActivityAt().isBefore(OffsetDateTime.now()));
    }
}