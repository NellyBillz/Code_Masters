package za.codemaster.backend.security;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import za.codemaster.backend.domain.model.User;

/**
 * Resolves an {@code @OptionalAuthenticatedUser User caller} controller parameter,
 * mirroring {@link AuthenticatedUserArgumentResolver} except it returns {@code null}
 * rather than throwing when there's no resolved session — {@link SessionAuthenticationFilter}
 * populates the {@code SecurityContext} from the session cookie on every request,
 * including plain {@code GET}s, so this works the same whether or not the endpoint
 * itself requires authentication.
 */
@Component
public class OptionalAuthenticatedUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(OptionalAuthenticatedUser.class)
                && User.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                   NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user;
        }
        return null;
    }
}
