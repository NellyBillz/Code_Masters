package za.codemaster.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GitHubOAuthUser(
        long id,
        String login,
        String name,
        @JsonProperty("avatar_url") String avatarUrl,
        String email,
        String bio,
        String location
) {}
