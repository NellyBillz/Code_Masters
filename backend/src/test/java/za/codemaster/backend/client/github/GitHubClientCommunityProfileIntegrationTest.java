package za.codemaster.backend.client.github;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import za.codemaster.backend.client.github.dto.GitHubCommunityProfile;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Live acceptance coverage for repositories with and without onboarding files. */
@EnabledIfEnvironmentVariable(named = "GITHUB_API_TOKEN", matches = ".+")
class GitHubClientCommunityProfileIntegrationTest {

    private GitHubClient client;

    @BeforeEach
    void setUp() {
        client = new GitHubClient(
                "https://api.github.com", System.getenv("GITHUB_API_TOKEN"));
    }

    @Test
    void repositoryWithBothFilesReturnsBothFlagsTrue() {
        GitHubCommunityProfile profile =
                client.fetchCommunityProfile("github", "docs").data();

        assertTrue(profile.hasContributingGuide());
        assertTrue(profile.hasCodeOfConduct());
    }

    @Test
    void repositoryWithoutEitherFileReturnsBothFlagsFalse() {
        GitHubCommunityProfile profile =
                client.fetchCommunityProfile("octocat", "Hello-World").data();

        assertFalse(profile.hasContributingGuide());
        assertFalse(profile.hasCodeOfConduct());
    }
}
