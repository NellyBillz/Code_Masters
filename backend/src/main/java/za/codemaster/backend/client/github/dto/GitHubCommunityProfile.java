package za.codemaster.backend.client.github.dto;

/** Onboarding files reported by GitHub's repository community profile. */
public record GitHubCommunityProfile(
        boolean hasContributingGuide,
        boolean hasCodeOfConduct
) {
}
