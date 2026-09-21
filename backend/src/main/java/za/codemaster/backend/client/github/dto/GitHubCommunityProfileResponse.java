package za.codemaster.backend.client.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Minimal mapping of GitHub's repository community-profile response. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubCommunityProfileResponse(Files files) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Files(
            Object contributing,
            @JsonProperty("code_of_conduct") Object codeOfConduct
    ) {
    }
}
