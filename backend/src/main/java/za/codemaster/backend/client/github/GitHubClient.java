package za.codemaster.backend.client.github;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import za.codemaster.backend.dto.GitHubIssueMetadata;
import za.codemaster.backend.dto.GitHubIssueResponse;
import za.codemaster.backend.dto.GitHubProjectMetadata;
import za.codemaster.backend.dto.GitHubRepositoryResponse;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Thin wrapper around the GitHub REST API.
 * <p>
 * {@link #verifyAuthenticatedCall} is the original GH-1.2 spike: it proves
 * {@code GITHUB_API_TOKEN} authenticates a real call and deliberately does
 * no response mapping. {@link #fetchProjectMetadata} is GH-1: it reuses that
 * same proven auth plumbing but actually parses the response into the
 * plain, decoupled {@link GitHubProjectMetadata}. {@link #fetchIssues}
 * applies that same pattern to issues, walking every page of results.
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

    /**
     * Matches one entry of an RFC 8288 {@code Link} header, e.g.
     * {@code <https://api.github.com/repositories/123/issues?page=2>; rel="next"}.
     * Group 1 is the URL, group 2 is the {@code rel} value.
     */
    private static final Pattern LINK_HEADER_ENTRY = Pattern.compile("<([^>]+)>;\\s*rel=\"([^\"]+)\"");

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
        requireToken();

        ResponseEntity<String> response = restClient.get()
                .uri("/repos/{owner}/{repo}", owner, repo)
                .headers(this::attachAuthHeaders)
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

    /**
     * Fetches and normalizes metadata for {@code owner/repo} into a plain,
     * unit-testable {@link GitHubProjectMetadata} - the real GH-1 deliverable
     * that turns the {@link #verifyAuthenticatedCall} spike into something
     * callers can actually use.
     * <p>
     * Makes two calls: {@code GET /repos/{owner}/{repo}} for the core fields
     * (mapped into {@link GitHubRepositoryResponse}, GitHub's raw shape) and
     * {@code GET /repos/{owner}/{repo}/languages} for the per-language byte
     * breakdown (GitHub returns that as a flat {@code language -> bytes}
     * object, so a {@code Map<String, Long>} maps it directly - no extra DTO
     * needed). The two are merged into one {@link GitHubProjectMetadata}.
     * <p>
     * Numeric/license/language fields fall back to {@code 0} / {@code null}
     * / an empty map when GitHub omits them, rather than throwing - GitHub
     * legitimately returns nulls here (e.g. no LICENSE file, no detected
     * language, empty repo has no languages).
     *
     * @param owner repo owner/org, e.g. "octocat"
     * @param repo  repo name, e.g. "Hello-World"
     * @return normalized, plain project metadata (see its javadoc for the
     *         GH-1.7 field/type contract)
     * @throws IllegalStateException if {@code github.api.token} is unset, or
     *                                GitHub returns an empty repo body
     */
    public GitHubProjectMetadata fetchProjectMetadata(String owner, String repo) {
        requireToken();

        GitHubRepositoryResponse repository = restClient.get()
                .uri("/repos/{owner}/{repo}", owner, repo)
                .headers(this::attachAuthHeaders)
                .retrieve()
                .body(GitHubRepositoryResponse.class);

        if (repository == null) {
            throw new IllegalStateException(
                    "GitHub returned an empty repository body for " + owner + "/" + repo);
        }

        Map<String, Long> languageBreakdown = restClient.get()
                .uri("/repos/{owner}/{repo}/languages", owner, repo)
                .headers(this::attachAuthHeaders)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Long>>() {
                });

        return new GitHubProjectMetadata(
                repository.name(),
                repository.description(),
                repository.language(),
                languageBreakdown == null ? Map.of() : languageBreakdown,
                repository.stargazersCount() == null ? 0 : repository.stargazersCount(),
                repository.forksCount() == null ? 0 : repository.forksCount(),
                repository.openIssuesCount() == null ? 0 : repository.openIssuesCount(),
                repository.license() == null ? null : repository.license().spdxId(),
                repository.pushedAt() == null ? null : OffsetDateTime.parse(repository.pushedAt())
        );
    }

    /**
     * Fetches every open issue for {@code owner/repo} into plain,
     * unit-testable {@link GitHubIssueMetadata} records - extends GH-1.3's
     * {@code fetchOpenIssues} (now folded into this method under the name
     * the ticket contract calls for) by walking every page instead of
     * stopping after the first.
     * <p>
     * Starts at {@code GET /repos/{owner}/{repo}/issues?state=open} and then
     * follows the {@code Link} response header's {@code rel="next"} entry
     * (RFC 8288 - the same pagination scheme GitHub uses everywhere) until
     * a response has no {@code next} link left, at which point every page
     * has been collected. No {@code page}/{@code per_page} params are sent
     * on the first request, so GitHub's default page size applies; later
     * requests reuse whatever URL GitHub itself hands back in {@code Link},
     * which already encodes the next page/cursor.
     * <p>
     * GitHub's {@code /issues} endpoint also returns pull requests (a PR is
     * a special kind of issue in GitHub's model) - those entries carry a
     * non-null {@code pull_request} field and are filtered out after all
     * pages are collected, so the result only contains real issues.
     *
     * @param owner repo owner/org, e.g. "expressjs"
     * @param repo  repo name, e.g. "express"
     * @return every open issue across every page, in the order GitHub
     *         returned them
     * @throws IllegalStateException if {@code github.api.token} is unset
     */
    public List<GitHubIssueMetadata> fetchIssues(String owner, String repo) {
        requireToken();

        List<GitHubIssueResponse> allIssues = new ArrayList<>();

        ResponseEntity<List<GitHubIssueResponse>> response = restClient.get()
                .uri("/repos/{owner}/{repo}/issues?state=open", owner, repo)
                .headers(this::attachAuthHeaders)
                .retrieve()
                .toEntity(new ParameterizedTypeReference<List<GitHubIssueResponse>>() {
                });

        while (true) {
            List<GitHubIssueResponse> page = response.getBody();
            if (page != null) {
                allIssues.addAll(page);
            }

            String nextLink = extractNextLink(response.getHeaders().getFirst(HttpHeaders.LINK));
            if (nextLink == null) {
                break;
            }

            response = restClient.get()
                    .uri(URI.create(nextLink))
                    .headers(this::attachAuthHeaders)
                    .retrieve()
                    .toEntity(new ParameterizedTypeReference<List<GitHubIssueResponse>>() {
                    });
        }

        return allIssues.stream()
                .filter(issue -> issue.pullRequest() == null)
                .map(issue -> new GitHubIssueMetadata(
                        issue.number(),
                        issue.title(),
                        issue.body(),
                        issue.labels() == null
                                ? List.of()
                                : issue.labels().stream()
                                        .map(GitHubIssueResponse.Label::name)
                                        .toList(),
                        issue.htmlUrl(),
                        issue.createdAt() == null ? null : OffsetDateTime.parse(issue.createdAt()),
                        issue.updatedAt() == null ? null : OffsetDateTime.parse(issue.updatedAt())
                ))
                .toList();
    }

    /**
     * Pulls the {@code rel="next"} URL out of a {@code Link} header value,
     * or {@code null} if the header is absent or has no {@code next} entry
     * (meaning the current page was the last one).
     */
    private String extractNextLink(String linkHeader) {
        if (linkHeader == null) {
            return null;
        }

        Matcher matcher = LINK_HEADER_ENTRY.matcher(linkHeader);
        while (matcher.find()) {
            if ("next".equals(matcher.group(2))) {
                return matcher.group(1);
            }
        }
        return null;
    }

    private void attachAuthHeaders(HttpHeaders headers) {
        headers.set("Authorization", "Bearer " + token);
        headers.set("Accept", "application/vnd.github+json");
        headers.set("X-GitHub-Api-Version", GITHUB_API_VERSION);
    }

    private void requireToken() {
        if (token == null || token.isBlank()) {
            throw new IllegalStateException(
                    "github.api.token is not set - export GITHUB_API_TOKEN (see backend/.env.example)");
        }
    }
}