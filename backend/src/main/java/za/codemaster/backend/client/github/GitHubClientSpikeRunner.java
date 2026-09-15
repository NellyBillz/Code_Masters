package za.codemaster.backend.client.github;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Manual trigger for {@link GitHubClient#verifyAuthenticatedCall}.
 * <p>
 * Only active on the {@code spike} profile, so it never runs during normal
 * {@code mvn spring-boot:run}, in tests, or in any other environment - this
 * is throwaway verification for GH-1.2, not a real startup behaviour.
 * <p>
 * Run it with:
 * {@code ./mvnw spring-boot:run -Dspring-boot.run.profiles=spike}
 * (with GITHUB_API_TOKEN set in backend/.env or the shell environment) and
 * check the logs for "X-RateLimit-Limit=5000".
 */
@Component
@Profile("spike")
public class GitHubClientSpikeRunner implements CommandLineRunner {

    private final GitHubClient gitHubClient;
    private final String owner;
    private final String repo;

    public GitHubClientSpikeRunner(GitHubClient gitHubClient,
                                    @Value("${github.spike.owner:octocat}") String owner,
                                    @Value("${github.spike.repo:Hello-World}") String repo) {
        this.gitHubClient = gitHubClient;
        this.owner = owner;
        this.repo = repo;
    }

    @Override
    public void run(String... args) {
        gitHubClient.verifyAuthenticatedCall(owner, repo);
    }
}
