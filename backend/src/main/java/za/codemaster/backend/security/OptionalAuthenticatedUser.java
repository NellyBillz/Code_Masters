package za.codemaster.backend.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method parameter as "the currently authenticated user, if any."
 * <p>
 * Unlike {@link AuthenticatedUser}, resolves to {@code null} instead of throwing
 * {@code UNAUTHENTICATED} when no valid session cookie is present — for public
 * (GET) endpoints whose response legitimately differs for a logged-in caller
 * without requiring one, e.g. {@code GET /projects/{projectId}} showing a
 * pending project to its own submitter (API-03.1).
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface OptionalAuthenticatedUser {
}
