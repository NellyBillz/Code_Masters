package za.codemaster.backend.client.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Raw 1:1 mapping of a single element of GitHub's
 * {@code GET /repos/{owner}/{repo}/issues} response (only the fields GH-1
 * (issues) needs - GitHub's real payload has far more).
 * <p>
 * Same "ugly" GitHub-shaped layer as {@link GitHubRepositoryResponse}:
 * snake_case source fields, GitHub's own null semantics. {@code
 * GitHubClient} maps this into the plain, decoupled {@link
 * GitHubIssueMetadata} that callers/tests actually use - nothing outside
 * the client package should depend on this class directly.
 * <p>
 * <b>Note:</b> GitHub's {@code /issues} endpoint returns pull requests too
 * (a PR is a special kind of issue in GitHub's model) - those entries carry
 * a non-null {@code pull_request} field. {@code GitHubClient} filters them
 * out so this DTO only ever represents real issues; the field is mapped
 * here purely so the client can check it.
 * <p>
 * {@code ignoreUnknown = true} because GitHub's real response has dozens of
 * fields we don't map; failing on unmapped fields would be brittle.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubIssueResponse(
        Integer number,
        String title,
        String body,
        List<Label> labels,
        @JsonProperty("html_url") String htmlUrl,
        @JsonProperty("created_at") String createdAt,
        @JsonProperty("updated_at") String updatedAt,
        @JsonProperty("pull_request") Object pullRequest
) {

    /**
     * GitHub represents each label as an object, not a bare string - only
     * {@code name} is needed here.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Label(String name) {
    }
}
