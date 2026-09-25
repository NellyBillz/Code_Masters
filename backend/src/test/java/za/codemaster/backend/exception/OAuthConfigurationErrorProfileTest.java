package za.codemaster.backend.exception;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.client.RestClient;
import za.codemaster.backend.controller.AuthController;
import za.codemaster.backend.repository.SessionRepository;
import za.codemaster.backend.service.AuthPersistenceService;
import za.codemaster.backend.service.GitHubOAuthService;
import za.codemaster.backend.service.RateLimitService;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class OAuthConfigurationErrorProfileTest {

    @Test
    void devProfileReturnsActionableOAuthConfigurationError() throws Exception {
        Profile profile = AnnotatedElementUtils.findMergedAnnotation(
                DevOAuthConfigurationExceptionHandler.class, Profile.class);
        assertNotNull(profile);
        assertArrayEquals(new String[]{"dev"}, profile.value());

        standaloneSetup(controllerWithMissingOAuthConfiguration())
                .setControllerAdvice(
                        new DevOAuthConfigurationExceptionHandler(),
                        new GlobalExceptionHandler())
                .build()
                .perform(get("/auth/github"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("OAUTH_NOT_CONFIGURED"))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("GITHUB_OAUTH_CLIENT_ID"),
                                org.hamcrest.Matchers.containsString("GITHUB_OAUTH_CLIENT_SECRET"),
                                org.hamcrest.Matchers.containsString("backend/AUTH.md"))))
                .andExpect(jsonPath("$.path").value("/auth/github"));
    }

    @Test
    void nonDevProfileKeepsGenericInternalErrorWithoutConfigurationDetails() throws Exception {
        standaloneSetup(controllerWithMissingOAuthConfiguration())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build()
                .perform(get("/auth/github"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value(
                        "Something went wrong. Please try again."))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.not(
                                org.hamcrest.Matchers.containsString("GITHUB_OAUTH_CLIENT_ID"))))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.not(
                                org.hamcrest.Matchers.containsString("GITHUB_OAUTH_CLIENT_SECRET"))));
    }

    private AuthController controllerWithMissingOAuthConfiguration() {
        GitHubOAuthService oauth = new GitHubOAuthService(
                RestClient.builder(), "", "", "http://localhost:8080/auth/github/callback");
        return new AuthController(
                oauth,
                mock(AuthPersistenceService.class),
                "http://localhost:3000",
                Duration.ofDays(7),
                false,
                mock(SessionRepository.class),
                new RateLimitService(1_000_000, 1_000_000, 1_000_000, 1_000_000, 1_000_000, 1_000_000));
    }
}
