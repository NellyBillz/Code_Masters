package za.codemaster.backend.security;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.exception.ApiException;

/**
 * Resolves an {@code @AuthenticatedUser User currentUser} controller parameter from the
 * request's {@link org.springframework.security.core.context.SecurityContext}, which
 * {@link SessionAuthenticationFilter} populates from the session cookie earlier in the
 * filter chain.
 * <p>
 * The {@code SecurityFilterChain} already rejects any non-GET request with no
 * resolved session before it reaches a controller, so the {@code UNAUTHENTICATED} throw
 * here is a safety net (e.g. a GET endpoint that opts into {@code @AuthenticatedUser}),
 * not the primary enforcement point.
 */
@Component
public class AuthenticatedUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthenticatedUser.class)
                && User.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                   NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            throw new ApiException("UNAUTHENTICATED", "Authentication is required.", HttpStatus.UNAUTHORIZED);
        }
        return user;
    }
}
