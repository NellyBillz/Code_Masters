package za.codemaster.backend.dto.user;

import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request body for {@code PATCH /users/me}.
 * <p>
 * Matches the {@code UpdateUserRequest} schema in codemasters-api-spec.yaml v2.1.
 * All fields optional; only provided (non-null) fields are changed. Deliberately
 * excludes GitHub-derived/system-managed fields ({@code username}, {@code avatarUrl},
 * {@code reputation}, {@code githubId}) — same philosophy as
 * {@code UpdateProjectRequest} excluding GitHub-derived project fields.
 */
public record UpdateUserRequest(
        String displayName,

        @Size(max = 1000, message = "bio must be at most 1000 characters")
        String bio,

        String location,

        List<String> skills
) {
}
