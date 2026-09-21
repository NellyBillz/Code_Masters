package za.codemaster.backend.client.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Fields used from GitHub's heterogeneous issue-timeline event payloads. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubTimelineEventResponse(
        String event,
        @JsonProperty("commit_id") String commitId,
        Source source
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Source(Issue issue) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Issue(Integer number, @JsonProperty("pull_request") PullRequest pullRequest) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PullRequest(String url) {
    }
}
