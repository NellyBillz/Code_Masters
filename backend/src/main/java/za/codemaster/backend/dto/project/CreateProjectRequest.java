package za.codemaster.backend.dto.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

/**
 * Request body for {@code POST /projects}.
 * <p>
 * Matches the {@code CreateProjectRequest} schema in codemasters-api-spec.yaml v2.1.
 * Deliberately does not accept GitHub-derived fields (name, description, stars,
 * license, etc.) — per product doc Feature 10, "we should not ask users to
 * manually type information that GitHub can provide." Those are filled in by
 * a maintainer triggering {@code POST /projects/{id}/issues/sync} (GH-02.3)
 * after submission, not by this endpoint.
 */
public record CreateProjectRequest(
        @NotBlank(message = "githubUrl must not be blank")
        @Pattern(
                regexp = "^https://github\\.com/[\\w.-]+/[\\w.-]+/?$",
                message = "githubUrl must be a GitHub repository URL, e.g. https://github.com/owner/repo"
        )
        String githubUrl,

        @NotNull(message = "connection is required")
        ProjectConnection connection,

        @NotBlank(message = "category is required")
        String category,

        List<String> tags,

        List<String> countryCodes
) {
}
