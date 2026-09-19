package za.codemaster.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import za.codemaster.backend.security.AuthenticatedUserArgumentResolver;

import java.util.List;

/**
 * Registers {@link AuthenticatedUserArgumentResolver} so controllers can declare an
 * {@code @AuthenticatedUser User currentUser} parameter (API-02.2).
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthenticatedUserArgumentResolver authenticatedUserArgumentResolver;

    public WebMvcConfig(AuthenticatedUserArgumentResolver authenticatedUserArgumentResolver) {
        this.authenticatedUserArgumentResolver = authenticatedUserArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(authenticatedUserArgumentResolver);
    }
}
