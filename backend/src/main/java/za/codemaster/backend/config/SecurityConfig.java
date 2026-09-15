package za.codemaster.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
public class SecurityConfig {

    // TEMPORARY for Phase 1 Round 1: every endpoint we build in this round is
    // public read-only discovery (see API-01.3–.6 / design doc §3 "Public discovery").
    // Real session-based auth (design doc §4) gets wired in for the write
    // endpoints in Round 2 ; this bean will be replaced then, not extended.

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
