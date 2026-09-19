package za.codemaster.backend.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code PATCH /comments/{commentId}}.
 * <p>
 * Matches the {@code UpdateCommentRequest} schema in codemasters-api-spec.yaml v2.1.
 * Same shape as {@link CreateCommentRequest} but kept as its own class since the two
 * requests are independent schemas in the spec.
 */
public record UpdateCommentRequest(
        @NotBlank(message = "body must not be blank")
        @Size(min = 1, max = 5000, message = "body must be between 1 and 5000 characters")
        String body
) {
}
