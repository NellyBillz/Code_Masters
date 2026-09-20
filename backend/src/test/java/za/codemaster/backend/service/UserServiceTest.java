package za.codemaster.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import za.codemaster.backend.BackendApplication;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.dto.user.PublicUserProfile;
import za.codemaster.backend.dto.user.UpdateUserRequest;
import za.codemaster.backend.dto.user.UserProfile;
import za.codemaster.backend.exception.ApiException;
import za.codemaster.backend.repository.UserRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies API-02.9's acceptance criteria (round2-tickets.md): the caller's own
 * profile includes {@code email}, which the public profile endpoint never returns.
 * Also verifies API-02.10's: updating only one field leaves the others untouched.
 * <p>
 * Runs against a real (test) Postgres database, same pattern as
 * {@code CommentServiceTest}/{@code ClaimServiceTest}.
 */
@SpringBootTest(
    classes = BackendApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@EnableAutoConfiguration(exclude = {
    SecurityAutoConfiguration.class,
    OAuth2ResourceServerAutoConfiguration.class,
    OAuth2ClientAutoConfiguration.class
})
@Transactional
class UserServiceTest {

    @Autowired
    private UserRepository userRepository;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository);
    }

    private User createUser(String username, String email) {
        return userRepository.save(User.builder()
                .githubId(System.nanoTime())
                .username(username)
                .displayName("Display " + username)
                .email(email)
                .bio("bio")
                .location("Cape Town")
                .skills(new String[]{"Java", "TypeScript"})
                .build());
    }

    @Test
    void currentUserProfileIncludesEmailAndGithubAccess() {
        String suffix = String.valueOf(System.nanoTime());
        User user = createUser("me_" + suffix, "me_" + suffix + "@example.com");

        UserProfile profile = service.getCurrentUserProfile(user);

        assertEquals("me_" + suffix + "@example.com", profile.email());
        assertTrue(profile.githubAccess());
        assertEquals(user.getUsername(), profile.publicProfile().username());
        assertEquals(user.getId(), profile.publicProfile().id());
    }

    @Test
    void publicProfileNeverIncludesEmail() {
        String suffix = String.valueOf(System.nanoTime());
        User user = createUser("pub_" + suffix, "pub_" + suffix + "@example.com");

        PublicUserProfile profile = service.getPublicProfile(user.getUsername());

        assertEquals(user.getUsername(), profile.username());
        assertEquals(user.getDisplayName(), profile.displayName());
        assertEquals(user.getBio(), profile.bio());
        assertEquals(user.getLocation(), profile.location());
        assertEquals(java.util.List.of("Java", "TypeScript"), profile.skills());
        // PublicUserProfile has no email field at all — the type system itself
        // enforces this isn't returned, this just documents the intent.
    }

    @Test
    void publicProfileOnUnknownUsernameThrowsUserNotFound() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.getPublicProfile("no-such-user-xyz"));

        assertEquals("USER_NOT_FOUND", ex.getCode());
        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void updatingOnlyBioLeavesOtherFieldsUntouched() {
        String suffix = String.valueOf(System.nanoTime());
        User user = createUser("update_" + suffix, "update_" + suffix + "@example.com");

        UserProfile updated = service.updateCurrentUserProfile(user, new UpdateUserRequest(null, "new bio", null, null));

        assertEquals("new bio", updated.publicProfile().bio());
        assertEquals("Display update_" + suffix, updated.publicProfile().displayName(), "displayName wasn't provided, should be untouched");
        assertEquals("Cape Town", updated.publicProfile().location(), "location wasn't provided, should be untouched");
        assertEquals(List.of("Java", "TypeScript"), updated.publicProfile().skills(), "skills weren't provided, should be untouched");
    }

    @Test
    void updateCanChangeAllFieldsAtOnce() {
        String suffix = String.valueOf(System.nanoTime());
        User user = createUser("updateall_" + suffix, "updateall_" + suffix + "@example.com");

        UpdateUserRequest request = new UpdateUserRequest(
                "New Display Name", "New bio", "Nairobi", List.of("Go", "Rust"));
        UserProfile updated = service.updateCurrentUserProfile(user, request);

        assertEquals("New Display Name", updated.publicProfile().displayName());
        assertEquals("New bio", updated.publicProfile().bio());
        assertEquals("Nairobi", updated.publicProfile().location());
        assertEquals(List.of("Go", "Rust"), updated.publicProfile().skills());
    }

    @Test
    void updatePersistsToTheDatabase() {
        String suffix = String.valueOf(System.nanoTime());
        User user = createUser("persist_" + suffix, "persist_" + suffix + "@example.com");

        service.updateCurrentUserProfile(user, new UpdateUserRequest("Persisted Name", null, null, null));

        User reloaded = userRepository.findById(user.getId()).orElseThrow();
        assertEquals("Persisted Name", reloaded.getDisplayName());
    }
}
