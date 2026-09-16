package za.codemaster.backend.client.github;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Thin wrapper around the GitHub REST API.
 * <p>
 * GH-1.2 spike scope only: prove that {@code GITHUB_API_TOKEN} authenticates
 * a real call. There is deliberately no response mapping / DTO parsing here
 * yet - that lands in the follow-up ticket once the plumbing is confirmed.
 * <p>
 * Auth header format confirmed against GitHub's current REST API auth docs
 * (docs.github.com/en/rest/authentication/authenticating-to-the-rest-api,
 * apiVersion=2022-11-28): {@code Authorization: Bearer <token>} is the
 * documented form (the older {@code token <token>} form still works too,
 * but Bearer is what GitHub's own examples use today).
 */
@Component
public class GitHubClient {

    private static final Logger log = LoggerFactory.getLogger(GitHubClient.class);

    /** Pinned per GitHub's docs so responses don't silently change shape under us. */
    private static final String GITHUB_API_VERSION = "2022-11-28";

    private final RestClient restClient;
    private final String token;

    public GitHubClient(@Value("${github.api.base-url}") String baseUrl,
                         @Value("${github.api.token}") String token) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.token = token;
    }

    /**
     * Calls {@code GET /repos/{owner}/{repo}} with the token attached and
     * logs the raw response body plus the {@code X-RateLimit-Limit} header.
     * <p>
     * That header is the tell: 5000 means the token authenticated, 60 means
     * it silently fell back to unauthenticated. This method does not parse
     * or return the body - it exists purely to prove the token works
     * end-to-end (GH-1.2). No callers yet; GH-1's data mapping is separate.
     *
     * @param owner repo owner/org, e.g. "octocat"
     * @param repo  repo name, e.g. "Hello-World"
     */
    public void verifyAuthenticatedCall(String owner, String repo) {
        if (token == null || token.isBlank()) {
            throw new IllegalStateException(
                    "github.api.token is not set - export GITHUB_API_TOKEN (see backend/.env.example)");
        }

        ResponseEntity<String> response = restClient.get()
                .uri("/repos/{owner}/{repo}", owner, repo)
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", GITHUB_API_VERSION)
                .retrieve()
                .toEntity(String.class);

        String rateLimit = response.getHeaders().getFirst("X-RateLimit-Limit");

        log.info("GH-1.2 spike: GET /repos/{}/{} -> status={}, X-RateLimit-Limit={}",
                owner, repo, response.getStatusCode(), rateLimit);
        log.info("GH-1.2 spike: raw response body: {}", response.getBody());

        if (!"5000".equals(rateLimit)) {
            log.warn("X-RateLimit-Limit was '{}', not 5000 - token is likely NOT authenticating "
                    + "(60 means unauthenticated fallback). Check GITHUB_API_TOKEN.", rateLimit);
        }
    }
}
