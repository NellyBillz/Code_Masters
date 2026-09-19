package za.codemaster.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /projects/{projectId}/comments} and
 * {@code POST /issues/{issueId}/comments}.
 * <p>
 * Matches the {@code CreateCommentRequest} schema in codemasters-api-spec.yaml v2.1.
 * Which parent (project or issue) a comment attaches to is determined entirely by
 * which URL it was posted to — this request body carries only the comment text.
 */
public record CreateCommentRequest(
        @NotBlank(message = "body must not be blank")
        @Size(min = 1, max = 5000, message = "body must be between 1 and 5000 characters")
        String body
) {
}
