package za.codemaster.backend.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Base exception for any deliberate, expected API error (a "not found", a validation
 * failure, a forbidden action, etc.).
 * <p>
 * Throwing this from a controller or service lets {@link GlobalExceptionHandler}
 * translate it into the standard {@code Error} JSON shape automatically; callers
 * should not write their own {@code try/catch} + manual response-building for these
 * cases. Each ticket that needs a new error case only needs to pick a {@code code}
 * string and an {@link HttpStatus}, not write new exception-handling logic.
 *
 * <p>Example:
 * <pre>{@code
 * throw new ApiException("PROJECT_NOT_FOUND", "The requested project does not exist.", HttpStatus.NOT_FOUND);
 * }</pre>
 */
public class ApiException extends RuntimeException{
    private final String code;
    private final HttpStatus status;
    private final Map<String, Object> details;


    public ApiException(String code, String message, HttpStatus status) {
        this(code, message, status, null);
    }

    public ApiException(String code, String message, HttpStatus status, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.status = status;
        this.details = details;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

}
