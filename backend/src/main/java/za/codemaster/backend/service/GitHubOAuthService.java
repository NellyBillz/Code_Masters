package za.codemaster.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import za.codemaster.backend.dto.GitHubOAuthUser;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class GitHubOAuthService {
    private final RestClient githubApi;
    private final RestClient githubWeb;
    private final String clientId;
    private final String clientSecret;
    private final String callbackUrl;

    public GitHubOAuthService(
            RestClient.Builder restClientBuilder,
            @Value("${github.oauth.client-id:}") String clientId,
            @Value("${github.oauth.client-secret:}") String clientSecret,
            @Value("${github.oauth.callback-url:http://localhost:8080/auth/github/callback}") String callbackUrl) {
        this.githubApi = restClientBuilder.baseUrl("https://api.github.com").build();
        this.githubWeb = restClientBuilder.baseUrl("https://github.com").build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.callbackUrl = callbackUrl;
    }

    public URI authorizationUri(String state) {
        requireConfiguration();
        return URI.create("https://github.com/login/oauth/authorize"
                + "?client_id=" + encode(clientId)
                + "&redirect_uri=" + encode(callbackUrl)
                + "&scope=" + encode("read:user user:email")
                + "&state=" + encode(state));
    }

    public String exchangeCode(String code) {
        requireConfiguration();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("code", code);
        form.add("redirect_uri", callbackUrl);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = githubWeb.post()
                .uri("/login/oauth/access_token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .body(form)
                .retrieve()
                .body(Map.class);

        Object token = response == null ? null : response.get("access_token");
        if (!(token instanceof String value) || value.isBlank()) {
            Object error = response == null ? null : response.get("error");
            throw new IllegalStateException("GitHub OAuth token exchange failed"
                    + (error == null ? "" : ": " + error));
        }
        return value;
    }

    public GitHubOAuthUser fetchUser(String accessToken) {
        GitHubOAuthUser user = githubApi.get()
                .uri("/user")
                .headers(headers -> {
                    headers.setBearerAuth(accessToken);
                    headers.setAccept(java.util.List.of(MediaType.valueOf("application/vnd.github+json")));
                    headers.set("X-GitHub-Api-Version", "2022-11-28");
                })
                .retrieve()
                .body(GitHubOAuthUser.class);
        if (user == null || user.id() <= 0 || user.login() == null || user.login().isBlank()) {
            throw new IllegalStateException("GitHub returned an invalid user profile");
        }
        return user;
    }

    private void requireConfiguration() {
        if (clientId.isBlank() || clientSecret.isBlank()) {
            throw new IllegalStateException("GitHub OAuth is not configured; set GITHUB_OAUTH_CLIENT_ID and GITHUB_OAUTH_CLIENT_SECRET");
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
