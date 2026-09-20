package za.codemaster.backend.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method parameter as "the currently authenticated user".
 * <p>
 * Resolved by {@link AuthenticatedUserArgumentResolver} from the {@link org.springframework.security.core.context.SecurityContextHolder}
 * (populated per-request by {@link SessionAuthenticationFilter} from the
 * {@code CODEMASTERS_SESSION} cookie). Every write endpoint can declare
 * {@code @AuthenticatedUser User currentUser} instead of re-resolving the
 * session/cookie itself — see API-02.2's contract note in round2-tickets.md.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthenticatedUser {
}
