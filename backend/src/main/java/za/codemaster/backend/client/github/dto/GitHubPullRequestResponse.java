package za.codemaster.backend.client.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Minimal mapping of {@code GET /repos/{owner}/{repo}/pulls/{number}}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubPullRequestResponse(
        Integer number,
        Boolean merged,
        @JsonProperty("merge_commit_sha") String mergeCommitSha,
        User user
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record User(String login, Long id) {
    }
}
