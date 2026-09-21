package za.codemaster.backend.client.github;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import za.codemaster.backend.client.github.dto.GitHubFetchResult;
import za.codemaster.backend.client.github.dto.GitHubCommunityProfile;
import za.codemaster.backend.client.github.dto.GitHubCommunityProfileResponse;
import za.codemaster.backend.client.github.dto.ClosingPullRequestResult;
import za.codemaster.backend.client.github.dto.GitHubIssueMetadata;
import za.codemaster.backend.client.github.dto.GitHubIssueResponse;
import za.codemaster.backend.client.github.dto.GitHubProjectMetadata;
import za.codemaster.backend.client.github.dto.GitHubPullRequestResponse;
import za.codemaster.backend.client.github.dto.GitHubRepositoryResponse;
import za.codemaster.backend.client.github.dto.GitHubTimelineEventResponse;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
 * applies that same pattern to issues, walking every page of results
 * (GH-1.4). Both methods now also support conditional requests via
 * {@code sinceEtag} / {@link GitHubFetchResult} (this ticket): pass back the
 * ETag from a previous call and, if nothing changed, GitHub answers with a
 * cheap {@code 304} that does not count against rate-limit quota the way a
 * full {@code 200} does, and skips re-parsing a body that was not sent.
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

    /** GitHub's "nothing changed since your ETag" status - see the class javadoc. */
    private static final int NOT_MODIFIED = 304;
    private static final int FORBIDDEN = 403;
    private static final int TOO_MANY_REQUESTS = 429;

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
     * unit-testable {@link GitHubProjectMetadata}, wrapped in a
     * {@link GitHubFetchResult} so a conditional-request 304 comes back as a
     * typed "nothing changed" result instead of an exception or an empty
     * object built from a body that was never sent.
     * <p>
     * Pass {@code sinceEtag} as {@code null} (or blank) for a normal,
     * unconditional fetch - typically the first call for a given repo, when
     * there is no previous ETag to compare against. Pass the ETag returned
     * by a previous call ({@link GitHubFetchResult#etag()}) to make this a
     * conditional request: GitHub compares it against the repo's current
     * state and, if unchanged, returns {@code 304} instead of the full body.
     * That 304 does not count against the token's rate-limit quota the way
     * a full {@code 200} does - see GitHub's conditional-requests docs.
     * <p>
     * On a fresh {@code 200}, makes a second call, {@code GET
     * /repos/{owner}/{repo}/languages}, for the per-language byte breakdown
     * (GitHub returns that as a flat {@code language -> bytes} object, so a
     * {@code Map<String, Long>} maps it directly - no extra DTO needed).
     * That second call is unconditional every time; only the main repo call
     * participates in the ETag exchange, since a language breakdown without
     * the repo body it belongs to is not a coherent "nothing changed" story.
     * <p>
     * Numeric/license/language fields on a fresh result fall back to
     * {@code 0} / {@code null} / an empty map when GitHub omits them, rather
     * than throwing - GitHub legitimately returns nulls here (e.g. no
     * LICENSE file, no detected language, empty repo has no languages).
     *
     * @param owner     repo owner/org, e.g. "octocat"
     * @param repo      repo name, e.g. "Hello-World"
     * @param sinceEtag the ETag from a previous call, or {@code null}/blank
     *                  for an unconditional fetch
     * @return {@link GitHubFetchResult#isModified()} true with the normalized
     *         metadata and a fresh ETag on a {@code 200}; false (no data) on
     *         a {@code 304}
     * @throws IllegalStateException if {@code github.api.token} is unset, if
     *                                GitHub returns an empty repo body on a
     *                                {@code 200}, or if GitHub returns any
     *                                other unexpected status
     */
    public GitHubFetchResult<GitHubProjectMetadata> fetchProjectMetadata(
            String owner, String repo, String sinceEtag) {
        requireToken();

        GitHubFetchResult<GitHubRepositoryResponse> repositoryResult = restClient.get()
                .uri("/repos/{owner}/{repo}", owner, repo)
                .headers(headers -> attachConditionalHeaders(headers, sinceEtag))
                .exchange((request, response) -> {
                    int status = response.getStatusCode().value();

                    if (status == NOT_MODIFIED) {
                        return GitHubFetchResult.<GitHubRepositoryResponse>notModified();
                    }
                    if (isRateLimited(status)) {
                        return GitHubFetchResult.<GitHubRepositoryResponse>rateLimited(
                                parseRetryAfter(response.getHeaders()));
                    }
                    if (status != 200) {
                        throw new IllegalStateException(
                                "GitHub GET /repos/" + owner + "/" + repo
                                        + " returned unexpected status " + status);
                    }

                    GitHubRepositoryResponse repository = response.bodyTo(GitHubRepositoryResponse.class);
                    if (repository == null) {
                        throw new IllegalStateException(
                                "GitHub returned an empty repository body for " + owner + "/" + repo);
                    }

                    return GitHubFetchResult.modified(
                            repository, response.getHeaders().getFirst(HttpHeaders.ETAG));
                });

        if (repositoryResult.isRateLimited()) {
            return GitHubFetchResult.rateLimited(repositoryResult.retryAfter());
        }
        if (!repositoryResult.isModified()) {
            return GitHubFetchResult.notModified();
        }

        GitHubRepositoryResponse repository = repositoryResult.data();

        GitHubFetchResult<Map<String, Long>> languageResult = restClient.get()
                .uri("/repos/{owner}/{repo}/languages", owner, repo)
                .headers(this::attachAuthHeaders)
                .exchange((request, response) -> {
                    int status = response.getStatusCode().value();
                    if (isRateLimited(status)) {
                        return GitHubFetchResult.<Map<String, Long>>rateLimited(parseRetryAfter(response.getHeaders()));
                    }
                    if (status != 200) {
                        throw new IllegalStateException("GitHub languages request returned unexpected status " + status);
                    }
                    Map<String, Long> body = response.bodyTo(new ParameterizedTypeReference<Map<String, Long>>() {});
                    return GitHubFetchResult.modified(body == null ? Map.of() : body, null);
                });
        if (languageResult.isRateLimited()) {
            return GitHubFetchResult.rateLimited(languageResult.retryAfter());
        }
        Map<String, Long> languageBreakdown = languageResult.data();

        GitHubProjectMetadata metadata = new GitHubProjectMetadata(
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

        return GitHubFetchResult.modified(metadata, repositoryResult.etag());
    }

    /**
     * Fetches the onboarding files GitHub recognizes for a repository.
     * Missing files are represented by null fields in a successful response,
     * so repositories without either file return two false flags.
     */
    public GitHubFetchResult<GitHubCommunityProfile> fetchCommunityProfile(
            String owner, String repo) {
        requireToken();

        return restClient.get()
                .uri("/repos/{owner}/{repo}/community/profile", owner, repo)
                .headers(this::attachAuthHeaders)
                .exchange((request, response) -> {
                    int status = response.getStatusCode().value();
                    if (isRateLimited(status)) {
                        return GitHubFetchResult.<GitHubCommunityProfile>rateLimited(
                                parseRetryAfter(response.getHeaders()));
                    }
                    if (status != 200) {
                        throw new IllegalStateException(
                                "GitHub community profile request returned unexpected status " + status);
                    }

                    GitHubCommunityProfileResponse body =
                            response.bodyTo(GitHubCommunityProfileResponse.class);
                    if (body == null) {
                        throw new IllegalStateException(
                                "GitHub returned an empty community profile body");
                    }

                    GitHubCommunityProfileResponse.Files files = body.files();
                    return GitHubFetchResult.modified(
                            new GitHubCommunityProfile(
                                    files != null && files.contributing() != null,
                                    files != null && files.codeOfConduct() != null),
                            null);
                });
    }

    /**
     * Fetches every open issue for {@code owner/repo} into plain,
     * unit-testable {@link GitHubIssueMetadata} records, wrapped in a
     * {@link GitHubFetchResult} for the same conditional-request reason as
     * {@link #fetchProjectMetadata}: extends GH-1.3's {@code fetchOpenIssues}
     * (now folded into this method under the name the ticket contract calls
     * for) by walking every page (GH-1.4) and, on top of that, by skipping
     * the walk entirely when nothing changed since {@code sinceEtag}.
     * <p>
     * Only the first page request carries {@code If-None-Match}. If that
     * first page comes back {@code 304}, the whole set is unchanged - there
     * is no reason to walk further pages, so none are requested and this
     * returns {@link GitHubFetchResult#isModified()} false immediately. If
     * the first page comes back {@code 200}, every subsequent page is
     * fetched as before (GH-1.4) via the {@code Link} response header's
     * {@code rel="next"} entry (RFC 8288) until a response has no
     * {@code next} link left, and the ETag returned on the whole result is
     * the one from that first page.
     * <p>
     * GitHub's {@code /issues} endpoint also returns pull requests (a PR is
     * a special kind of issue in GitHub's model) - those entries carry a
     * non-null {@code pull_request} field and are filtered out after all
     * pages are collected, so the result only contains real issues.
     *
     * @param owner     repo owner/org, e.g. "expressjs"
     * @param repo      repo name, e.g. "express"
     * @param sinceEtag the ETag from a previous call, or {@code null}/blank
     *                  for an unconditional fetch
     * @return {@link GitHubFetchResult#isModified()} true with every open
     *         issue across every page and a fresh ETag, if anything changed
     *         since {@code sinceEtag}; false (no data) if nothing did
     * @throws IllegalStateException if {@code github.api.token} is unset, or
     *                                if GitHub returns any unexpected status
     */
    public GitHubFetchResult<List<GitHubIssueMetadata>> fetchIssues(
            String owner, String repo, String sinceEtag) {
        requireToken();

        GitHubFetchResult<FirstIssuesPage> firstPageResult = restClient.get()
                .uri("/repos/{owner}/{repo}/issues?state=open", owner, repo)
                .headers(headers -> attachConditionalHeaders(headers, sinceEtag))
                .exchange((request, response) -> {
                    int status = response.getStatusCode().value();

                    if (status == NOT_MODIFIED) {
                        return GitHubFetchResult.<FirstIssuesPage>notModified();
                    }
                    if (isRateLimited(status)) {
                        return GitHubFetchResult.<FirstIssuesPage>rateLimited(parseRetryAfter(response.getHeaders()));
                    }
                    if (status != 200) {
                        throw new IllegalStateException(
                                "GitHub GET /repos/" + owner + "/" + repo
                                        + "/issues returned unexpected status " + status);
                    }

                    List<GitHubIssueResponse> page = response.bodyTo(
                            new ParameterizedTypeReference<List<GitHubIssueResponse>>() {
                            });
                    String nextLink = extractNextLink(response.getHeaders().getFirst(HttpHeaders.LINK));

                    return GitHubFetchResult.modified(
                            new FirstIssuesPage(page == null ? List.of() : page, nextLink),
                            response.getHeaders().getFirst(HttpHeaders.ETAG));
                });

        if (firstPageResult.isRateLimited()) {
            return GitHubFetchResult.rateLimited(firstPageResult.retryAfter());
        }
        if (!firstPageResult.isModified()) {
            return GitHubFetchResult.notModified();
        }

        FirstIssuesPage firstPage = firstPageResult.data();
        List<GitHubIssueResponse> allIssues = new ArrayList<>(firstPage.issues());
        String nextLink = firstPage.nextLink();

        while (nextLink != null) {
            GitHubFetchResult<FirstIssuesPage> pageResult = restClient.get()
                    .uri(URI.create(nextLink))
                    .headers(this::attachAuthHeaders)
                    .exchange((request, response) -> {
                        int status = response.getStatusCode().value();
                        if (isRateLimited(status)) {
                            return GitHubFetchResult.<FirstIssuesPage>rateLimited(parseRetryAfter(response.getHeaders()));
                        }
                        if (status != 200) {
                            throw new IllegalStateException("GitHub issues pagination request returned unexpected status " + status);
                        }
                        List<GitHubIssueResponse> page = response.bodyTo(
                                new ParameterizedTypeReference<List<GitHubIssueResponse>>() {});
                        return GitHubFetchResult.modified(
                                new FirstIssuesPage(page == null ? List.of() : page,
                                        extractNextLink(response.getHeaders().getFirst(HttpHeaders.LINK))), null);
                    });
            if (pageResult.isRateLimited()) {
                return GitHubFetchResult.rateLimited(pageResult.retryAfter());
            }
            FirstIssuesPage page = pageResult.data();
            allIssues.addAll(page.issues());
            nextLink = page.nextLink();
        }

        List<GitHubIssueMetadata> issues = allIssues.stream()
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

        return GitHubFetchResult.modified(issues, firstPageResult.etag());
    }

    /**
     * Finds the merged pull request that closed an issue via a closing keyword.
     * GitHub represents that relationship as a {@code closed} timeline event
     * with the closing commit SHA plus a {@code cross-referenced} event whose
     * source is the pull request. Matching the PR's merge SHA prevents an
     * unrelated PR that merely mentioned the issue from being misidentified.
     */
    public ClosingPullRequestResult fetchClosingPullRequest(
            String owner, String repo, int issueNumber) {
        requireToken();

        List<GitHubTimelineEventResponse> events = new ArrayList<>();
        String nextLink = "/repos/" + owner + "/" + repo + "/issues/"
                + issueNumber + "/timeline?per_page=100";

        while (nextLink != null) {
            String requestUri = nextLink;
            TimelinePage page = restClient.get()
                    .uri(requestUri)
                    .headers(this::attachAuthHeaders)
                    .exchange((request, response) -> {
                        int status = response.getStatusCode().value();
                        if (status != 200) {
                            throw new IllegalStateException(
                                    "GitHub issue timeline request returned unexpected status " + status);
                        }
                        List<GitHubTimelineEventResponse> body = response.bodyTo(
                                new ParameterizedTypeReference<List<GitHubTimelineEventResponse>>() {});
                        return new TimelinePage(
                                body == null ? List.of() : body,
                                extractNextLink(response.getHeaders().getFirst(HttpHeaders.LINK)));
                    });
            events.addAll(page.events());
            nextLink = page.nextLink();
        }

        List<String> closingCommitShas = events.stream()
                .filter(event -> "closed".equals(event.event()))
                .map(GitHubTimelineEventResponse::commitId)
                .filter(sha -> sha != null && !sha.isBlank())
                .toList();
        if (closingCommitShas.isEmpty()) {
            return new ClosingPullRequestResult.NotFound();
        }

        for (GitHubTimelineEventResponse event : events) {
            if (!"cross-referenced".equals(event.event())
                    || event.source() == null
                    || event.source().issue() == null
                    || event.source().issue().pullRequest() == null
                    || event.source().issue().pullRequest().url() == null) {
                continue;
            }

            GitHubPullRequestResponse pullRequest = fetchPullRequest(
                    URI.create(event.source().issue().pullRequest().url()));
            if (pullRequest.mergeCommitSha() == null
                    || !closingCommitShas.contains(pullRequest.mergeCommitSha())) {
                continue;
            }
            if (pullRequest.number() == null || pullRequest.merged() == null
                    || pullRequest.user() == null || pullRequest.user().login() == null
                    || pullRequest.user().id() == null) {
                throw new IllegalStateException("GitHub returned an incomplete closing pull request");
            }
            return new ClosingPullRequestResult.Found(
                    pullRequest.number(), pullRequest.merged(),
                    pullRequest.user().login(), pullRequest.user().id());
        }

        return new ClosingPullRequestResult.NotFound();
    }

    private GitHubPullRequestResponse fetchPullRequest(URI pullRequestUrl) {
        return restClient.get()
                .uri(pullRequestUrl)
                .headers(this::attachAuthHeaders)
                .exchange((request, response) -> {
                    int status = response.getStatusCode().value();
                    if (status != 200) {
                        throw new IllegalStateException(
                                "GitHub pull request request returned unexpected status " + status);
                    }
                    GitHubPullRequestResponse body = response.bodyTo(GitHubPullRequestResponse.class);
                    if (body == null) {
                        throw new IllegalStateException("GitHub returned an empty pull request body");
                    }
                    return body;
                });
    }

    /**
     * The first page of a {@code fetchIssues} call, before pull-request
     * filtering and before mapping to {@link GitHubIssueMetadata} - just
     * enough to (a) know the ETag and ability to short-circuit on 304, and
     * (b) keep walking with the same {@code Link}-header logic GH-1.4
     * already established, once we know the first page was a real 200.
     */
    private record FirstIssuesPage(List<GitHubIssueResponse> issues, String nextLink) {
    }

    private record TimelinePage(List<GitHubTimelineEventResponse> events, String nextLink) {
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

    private static boolean isRateLimited(int status) {
        return status == FORBIDDEN || status == TOO_MANY_REQUESTS;
    }

    /** Retry-After wins; X-RateLimit-Reset is GitHub's fallback (epoch seconds). */
    private static Instant parseRetryAfter(HttpHeaders headers) {
        String retryAfter = headers.getFirst(HttpHeaders.RETRY_AFTER);
        if (retryAfter != null && !retryAfter.isBlank()) {
            try {
                return Instant.now().plusSeconds(Long.parseLong(retryAfter.trim()));
            } catch (NumberFormatException ignored) {
                try {
                    return ZonedDateTime.parse(retryAfter.trim(), DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
                } catch (DateTimeParseException ignoredDate) {
                    // Fall through to GitHub's epoch-seconds header.
                }
            }
        }

        String reset = headers.getFirst("X-RateLimit-Reset");
        if (reset != null && !reset.isBlank()) {
            try {
                return Instant.ofEpochSecond(Long.parseLong(reset.trim()));
            } catch (NumberFormatException ignored) {
                // Malformed/missing retry metadata is represented as null.
            }
        }
        return null;
    }

    private void attachAuthHeaders(HttpHeaders headers) {
        headers.set("Authorization", "Bearer " + token);
        headers.set("Accept", "application/vnd.github+json");
        headers.set("X-GitHub-Api-Version", GITHUB_API_VERSION);
    }

    /** Auth headers plus, when a prior ETag is available, If-None-Match to make the request conditional. */
    private void attachConditionalHeaders(HttpHeaders headers, String sinceEtag) {
        attachAuthHeaders(headers);
        if (sinceEtag != null && !sinceEtag.isBlank()) {
            headers.set(HttpHeaders.IF_NONE_MATCH, sinceEtag);
        }
    }

    private void requireToken() {
        if (token == null || token.isBlank()) {
            throw new IllegalStateException(
                    "github.api.token is not set - export GITHUB_API_TOKEN (see backend/.env.example)");
        }
    }
}
