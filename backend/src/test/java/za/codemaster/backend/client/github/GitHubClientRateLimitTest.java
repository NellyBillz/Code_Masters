package za.codemaster.backend.client.github;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import za.codemaster.backend.dto.GitHubFetchResult;
import za.codemaster.backend.dto.GitHubIssueMetadata;
import za.codemaster.backend.dto.GitHubProjectMetadata;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GitHubClientRateLimitTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void fetchProjectMetadataReturnsTypedRateLimitAndUsesRetryAfter() throws IOException {
        long fallbackReset = Instant.now().plusSeconds(3600).getEpochSecond();
        server = serverReturning(429, "Retry-After", "120", "X-RateLimit-Reset", Long.toString(fallbackReset));
        GitHubClient client = client();
        Instant before = Instant.now().plusSeconds(119);

        GitHubFetchResult<GitHubProjectMetadata> result =
                assertDoesNotThrow(() -> client.fetchProjectMetadata("owner", "repo", null));

        assertInstanceOf(GitHubFetchResult.RateLimited.class, result);
        assertNotNull(result.retryAfter());
        assertFalse(result.retryAfter().isBefore(before));
        assertTrue(result.retryAfter().isBefore(Instant.now().plusSeconds(125)));
        assertNotEquals(Instant.ofEpochSecond(fallbackReset), result.retryAfter(),
                "Retry-After must take precedence over X-RateLimit-Reset");
    }

    @Test
    void fetchIssuesReturnsTypedRateLimitAndFallsBackToRateLimitReset() throws IOException {
        Instant reset = Instant.now().plusSeconds(600).truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        server = serverReturning(403, "X-RateLimit-Reset", Long.toString(reset.getEpochSecond()));
        GitHubClient client = client();

        GitHubFetchResult<List<GitHubIssueMetadata>> result =
                assertDoesNotThrow(() -> client.fetchIssues("owner", "repo", null));

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
}
