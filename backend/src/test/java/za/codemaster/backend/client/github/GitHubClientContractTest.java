package za.codemaster.backend.client.github;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import za.codemaster.backend.client.github.dto.GitHubFetchResult;
import za.codemaster.backend.client.github.dto.GitHubIssueMetadata;
import za.codemaster.backend.client.github.dto.GitHubProjectMetadata;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GH-1.7 consolidated contract suite. Covers the three outcomes Round 2's
 * sync controller is expected to branch on: success, not-modified, and
 * rate-limited with retryAfter populated. All HTTP responses are local mocks,
 * so this suite does not consume GitHub quota and does not need Spring or a DB.
 */
class GitHubClientContractTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void successfulFetchMapsProjectAndIssueDtos() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/repos/owner/repo/languages", exchange ->
                json(exchange, 200, "{\"Java\":1200,\"HTML\":300}"));
        server.createContext("/repos/owner/repo/issues", exchange -> {
            exchange.getResponseHeaders().add("ETag", "\"issues-v1\"");
            json(exchange, 200, """
                    [{
                      "number": 42,
                      "title": "Typed GitHub result",
                      "body": "Map this issue",
                      "labels": [{"name":"backend"},{"name":"github"}],
                      "html_url": "https://github.com/owner/repo/issues/42",
                      "created_at": "2026-09-15T08:00:00Z",
                      "updated_at": "2026-09-16T09:30:00Z",
                      "pull_request": null
                    }]
                    """);
        });
        server.createContext("/repos/owner/repo", exchange -> {
            exchange.getResponseHeaders().add("ETag", "\"repo-v1\"");
            json(exchange, 200, """
                    {
                      "name": "repo",
                      "description": "Contract test repo",
                      "language": "Java",
                      "stargazers_count": 12,
                      "forks_count": 3,
                      "open_issues_count": 1,
                      "license": {"spdx_id":"MIT"},
                      "pushed_at": "2026-09-16T10:15:30Z"
                    }
                    """);
        });
        server.start();
        GitHubClient client = client();

        GitHubFetchResult<GitHubProjectMetadata> projectResult =
                client.fetchProjectMetadata("owner", "repo", null);
        assertInstanceOf(GitHubFetchResult.Modified.class, projectResult);
        assertEquals("\"repo-v1\"", projectResult.etag());
        assertEquals(new GitHubProjectMetadata(
                "repo", "Contract test repo", "Java",
                java.util.Map.of("Java", 1200L, "HTML", 300L),
                12, 3, 1, "MIT", OffsetDateTime.parse("2026-09-16T10:15:30Z")),
                projectResult.data());

        GitHubFetchResult<List<GitHubIssueMetadata>> issueResult =
                client.fetchIssues("owner", "repo", null);
        assertInstanceOf(GitHubFetchResult.Modified.class, issueResult);
        assertEquals("\"issues-v1\"", issueResult.etag());
        assertEquals(List.of(new GitHubIssueMetadata(
                42, "Typed GitHub result", "Map this issue",
                List.of("backend", "github"),
                "https://github.com/owner/repo/issues/42",
                OffsetDateTime.parse("2026-09-15T08:00:00Z"),
                OffsetDateTime.parse("2026-09-16T09:30:00Z"))), issueResult.data());
    }

    @Test
    void conditionalFetchReturnsTypedNotModifiedWithoutFetchingLanguages() throws IOException {
        AtomicInteger languageCalls = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/repos/owner/repo/languages", exchange -> {
            languageCalls.incrementAndGet();
            json(exchange, 500, "{}");
        });
        server.createContext("/repos/owner/repo", exchange -> {
            assertEquals("\"repo-v1\"", exchange.getRequestHeaders().getFirst("If-None-Match"));
            exchange.sendResponseHeaders(304, -1);
            exchange.close();
        });
        server.start();

        GitHubFetchResult<GitHubProjectMetadata> result =
                assertDoesNotThrow(() -> client().fetchProjectMetadata("owner", "repo", "\"repo-v1\""));

        assertInstanceOf(GitHubFetchResult.NotModified.class, result);
        assertFalse(result.isModified());
        assertFalse(result.isRateLimited());
        assertEquals(0, languageCalls.get(), "304 must short-circuit before the languages request");
    }

    @Test
    void simulatedRateLimitReturnsTypedFailureWithRetryAfterAndNoException() throws IOException {
        long fallbackReset = Instant.now().plusSeconds(3600).getEpochSecond();
        server = serverReturning(429, "Retry-After", "120",
                "X-RateLimit-Reset", Long.toString(fallbackReset));
        Instant earliestExpected = Instant.now().plusSeconds(119);

        GitHubFetchResult<GitHubProjectMetadata> result = assertDoesNotThrow(
                () -> client().fetchProjectMetadata("owner", "repo", null));

        assertInstanceOf(GitHubFetchResult.RateLimited.class, result);
        assertTrue(result.isRateLimited());
        assertNotNull(result.retryAfter());
        assertFalse(result.retryAfter().isBefore(earliestExpected));
        assertTrue(result.retryAfter().isBefore(Instant.now().plusSeconds(125)));
        assertNotEquals(Instant.ofEpochSecond(fallbackReset), result.retryAfter(),
                "Retry-After must take precedence over X-RateLimit-Reset");
    }

    @Test
    void rateLimitFallsBackToXRateLimitReset() throws IOException {
        Instant reset = Instant.now().plusSeconds(600)
                .truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        server = serverReturning(403, "X-RateLimit-Reset", Long.toString(reset.getEpochSecond()));

        GitHubFetchResult<List<GitHubIssueMetadata>> result = assertDoesNotThrow(
                () -> client().fetchIssues("owner", "repo", null));

        assertInstanceOf(GitHubFetchResult.RateLimited.class, result);
        assertEquals(reset, result.retryAfter());
    }

    private GitHubClient client() {
        return new GitHubClient("http://localhost:" + server.getAddress().getPort(), "test-token");
    }

    private HttpServer serverReturning(int status, String... headers) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        httpServer.createContext("/", exchange -> {
            for (int i = 0; i < headers.length; i += 2) {
                exchange.getResponseHeaders().add(headers[i], headers[i + 1]);
            }
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        httpServer.start();
        return httpServer;
    }

    private static void json(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
