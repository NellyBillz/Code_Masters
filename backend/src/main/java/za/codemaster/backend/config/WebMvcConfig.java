package za.codemaster.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import za.codemaster.backend.dto.issue.Difficulty;
import za.codemaster.backend.dto.issue.IssueStatus;
import za.codemaster.backend.dto.search.SearchType;
import za.codemaster.backend.security.AuthenticatedUserArgumentResolver;

import java.util.List;

/**
 * Registers {@link AuthenticatedUserArgumentResolver} so controllers can declare an
 * {@code @AuthenticatedUser User currentUser} parameter (API-02.2), and one
 * {@code Converter<String, T>} per {@link za.codemaster.backend.dto.common.WireValued}
 * enum used as a {@code @RequestParam} type — see that interface's Javadoc for why:
 * Spring's default enum binding only matches the Java constant name
 * ({@code DIFFICULTY}), not the spec's lowercase wire value ({@code difficulty}),
 * so {@code ?difficulty=beginner} 500s without this. Registered per-type (not via a
 * single generic {@code ConverterFactory<String, WireValued>}) so there's no
 * ambiguity with Spring's own built-in enum converter over which one wins.
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

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, Difficulty.class, s -> WireValueMatcher.match(Difficulty.class, s));
        registry.addConverter(String.class, IssueStatus.class, s -> WireValueMatcher.match(IssueStatus.class, s));
        registry.addConverter(String.class, SearchType.class, s -> WireValueMatcher.match(SearchType.class, s));
    }
}
