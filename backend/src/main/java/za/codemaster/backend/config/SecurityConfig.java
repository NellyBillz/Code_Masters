package za.codemaster.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestClient;
import za.codemaster.backend.security.ApiAccessDeniedHandler;
import za.codemaster.backend.security.ApiAuthenticationEntryPoint;
import za.codemaster.backend.security.SessionAuthenticationFilter;

/**
 * API-02.2 (round2-tickets.md): replaces the Round 1 {@code permitAll()} placeholder
 * with design doc §4's real rule — {@code GET} stays public, everything else requires
 * a valid {@code CODEMASTERS_SESSION} cookie (and, per {@link SessionAuthenticationFilter},
 * a matching {@code X-CSRF-Token} header).
 * <p>
 * Spring's own cookie-based CSRF protection is disabled: the app implements its own
 * header-vs-stored-token check instead (design doc §4), since the session cookie
 * itself already carries the CSRF token pairing this API needs.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                            SessionAuthenticationFilter sessionAuthenticationFilter,
                                            ApiAuthenticationEntryPoint authenticationEntryPoint,
                                            ApiAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(sessionAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
