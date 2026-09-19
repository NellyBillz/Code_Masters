package za.codemaster.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a comment violates business invariants (such as XOR project/issue association).
 * Mapped to a clean 400 Bad Request.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class CommentValidationException extends RuntimeException {
    public CommentValidationException(String message) {
        super(message);
    }
}