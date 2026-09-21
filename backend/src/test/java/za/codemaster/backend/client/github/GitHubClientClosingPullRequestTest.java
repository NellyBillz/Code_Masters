package za.codemaster.backend.client.github;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import za.codemaster.backend.client.github.dto.ClosingPullRequestResult;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class GitHubClientClosingPullRequestTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void returnsOnlyThePullRequestWhoseMergeCommitClosedTheIssue() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        server.createContext("/repos/owner/repo/issues/42/timeline", exchange -> json(exchange, 200, """
                [
                  {"event":"cross-referenced","source":{"issue":{"number":7,
                    "pull_request":{"url":"%s/repos/owner/repo/pulls/7"}}}},
                  {"event":"cross-referenced","source":{"issue":{"number":9,
                    "pull_request":{"url":"%s/repos/owner/repo/pulls/9"}}}},
                  {"event":"closed","commit_id":"closing-sha"}
                ]
                """.formatted(baseUrl, baseUrl)));
        server.createContext("/repos/owner/repo/pulls/7", exchange -> json(exchange, 200, """
                {"number":7,"merged":true,"merge_commit_sha":"unrelated-sha",
                 "user":{"login":"mentioner","id":12}}
                """));
        server.createContext("/repos/owner/repo/pulls/9", exchange -> json(exchange, 200, """
                {"number":9,"merged":true,"merge_commit_sha":"closing-sha",
                 "user":{"login":"closer","id":34}}
                """));
        server.start();

        ClosingPullRequestResult result = new GitHubClient(baseUrl, "test-token")
                .fetchClosingPullRequest("owner", "repo", 42);

        assertEquals(new ClosingPullRequestResult.Found(9, true, "closer", 34), result);
    }

    @Test
    void returnsTypedNotFoundWhenIssueWasClosedManually() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/repos/owner/repo/issues/42/timeline", exchange -> json(exchange, 200, """
                [{"event":"closed","commit_id":null}]
                """));
        server.start();

        ClosingPullRequestResult result = new GitHubClient(
                "http://localhost:" + server.getAddress().getPort(), "test-token")
                .fetchClosingPullRequest("owner", "repo", 42);

        assertInstanceOf(ClosingPullRequestResult.NotFound.class, result);
    }

    private static void json(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
