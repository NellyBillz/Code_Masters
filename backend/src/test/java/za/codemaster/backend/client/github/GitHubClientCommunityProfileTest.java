package za.codemaster.backend.client.github;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import za.codemaster.backend.client.github.dto.GitHubCommunityProfile;
import za.codemaster.backend.client.github.dto.GitHubFetchResult;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitHubClientCommunityProfileTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void mapsRecognizedOnboardingFilesAsPresent() throws IOException {
        GitHubCommunityProfile profile = fetch("""
                {"files": {
                  "contributing": {"url": "https://api.github.com/contributing"},
                  "code_of_conduct": {"url": "https://api.github.com/code-of-conduct"}
                }}
                """);

        assertTrue(profile.hasContributingGuide());
        assertTrue(profile.hasCodeOfConduct());
    }

    @Test
    void mapsMissingOnboardingFilesAsFalseInsteadOfFailing() throws IOException {
        GitHubCommunityProfile profile = fetch("""
                {"files": {"contributing": null, "code_of_conduct": null}}
                """);

        assertFalse(profile.hasContributingGuide());
        assertFalse(profile.hasCodeOfConduct());
    }

    private GitHubCommunityProfile fetch(String responseBody) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/repos/owner/repo/community/profile", exchange ->
                json(exchange, responseBody));
        server.start();

        GitHubClient client = new GitHubClient(
                "http://localhost:" + server.getAddress().getPort(), "test-token");
        GitHubFetchResult<GitHubCommunityProfile> result =
                client.fetchCommunityProfile("owner", "repo");
        return result.data();
    }

    private static void json(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
