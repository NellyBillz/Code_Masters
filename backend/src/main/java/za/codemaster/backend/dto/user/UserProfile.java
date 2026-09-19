package za.codemaster.backend.dto.user;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * Response shape for {@code GET /users/me}.
 * <p>
 * Matches the {@code UserProfile} schema in codemasters-api-spec.yaml v2.1,
 * an {@code allOf} of every {@link PublicUserProfile} field flattened via
 * {@code @JsonUnwrapped} (same technique as {@code ProjectDetail}/{@code IssueDetail}),
 * plus {@code email} and {@code githubAccess} — the two fields never returned
 * from the public {@code GET /users/{username}} endpoint.
 */
public record UserProfile(
        @JsonUnwrapped PublicUserProfile publicProfile,
        String email,
        boolean githubAccess
) {
}
