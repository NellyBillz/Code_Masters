package za.codemaster.backend.dto.user;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * Response shape for {@code GET /users/me}.
 * <p>
 * Matches the {@code UserProfile} schema in codemasters-api-spec.yaml v3,
 * an {@code allOf} of every {@link PublicUserProfile} field flattened via
 * {@code @JsonUnwrapped} (same technique as {@code ProjectDetail}/{@code IssueDetail}),
 * plus {@code email}, {@code githubAccess}, and {@code isSiteAdmin} — fields
 * never returned from the public {@code GET /users/{username}} endpoint.
 * {@code isSiteAdmin} is what the frontend's admin moderation queue (FE-03.1)
 * checks to decide whether to render at all.
 */
public record UserProfile(
        @JsonUnwrapped PublicUserProfile publicProfile,
        String email,
        boolean githubAccess,
        boolean isSiteAdmin
) {
}
