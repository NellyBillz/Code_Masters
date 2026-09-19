package za.codemaster.backend.client.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Raw 1:1 mapping of GitHub's {@code GET /repos/{owner}/{repo}} response
 * (only the fields GH-1 needs - GitHub's real payload has far more).
 * <p>
 * This is intentionally the "ugly" GitHub-shaped layer: snake_case source
 * fields, GitHub's own null semantics (e.g. {@code language} is null when
 * linguist can't detect one, {@code license} is null when there's no
 * LICENSE file). {@link GitHubClient} maps this into the plain, decoupled
 * {@link GitHubProjectMetadata} that callers/tests actually use - nothing
 * outside the client package should depend on this class directly.
 * <p>
 * {@code ignoreUnknown = true} because GitHub's real response has dozens of
 * fields we don't map; failing on unmapped fields would be brittle.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubRepositoryResponse(
        String name,
        String description,
        String language,
        @JsonProperty("stargazers_count") Integer stargazersCount,
        @JsonProperty("forks_count") Integer forksCount,
        @JsonProperty("open_issues_count") Integer openIssuesCount,
        License license,
        @JsonProperty("pushed_at") String pushedAt
) {

    /** Only null when the repo has no detected license (no LICENSE file). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record License(
            @JsonProperty("spdx_id") String spdxId,
            String name
    ) {
    }
}
