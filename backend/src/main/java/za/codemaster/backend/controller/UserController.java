package za.codemaster.backend.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.dto.user.PagedContributions;
import za.codemaster.backend.dto.user.PublicUserProfile;
import za.codemaster.backend.dto.user.UpdateUserRequest;
import za.codemaster.backend.dto.user.UserProfile;
import za.codemaster.backend.security.AuthenticatedUser;
import za.codemaster.backend.service.UserService;

/**
 * Developer profile endpoints: the read surface the frontend needs to show
 * "logged in as X" after OAuth, plus public profiles (API-02.9), and self-editing
 * (API-02.10).
 */
@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * {@code GET /api/v1/users/me}: the caller's own profile, including {@code email}.
     * Session-authenticated; {@code @AuthenticatedUser} throws {@code UNAUTHENTICATED}
     * (401) if there's no valid session — GET requests are public by default at the
     * {@code SecurityFilterChain} level, so this resolver is the actual enforcement
     * point here, not just a safety net.
     */
    @GetMapping("/api/v1/users/me")
    public UserProfile getCurrentUser(@AuthenticatedUser User currentUser) {
        return userService.getCurrentUserProfile(currentUser);
    }

    /**
     * {@code GET /api/v1/users/{username}}: a public developer profile. Never
     * includes {@code email}. Throws {@code USER_NOT_FOUND} (404) via
     * GlobalExceptionHandler if the username doesn't exist.
     */
    @GetMapping("/api/v1/users/{username}")
    public PublicUserProfile getPublicProfile(@PathVariable String username) {
        return userService.getPublicProfile(username);
    }

    /**
     * {@code GET /api/v1/users/{username}/contributions}: a developer's verified
     * contribution history (API-03.6) — only {@code completed} claims, most
     * recent first. Throws {@code USER_NOT_FOUND} (404) via GlobalExceptionHandler
     * if the username doesn't exist.
     */
    @GetMapping("/api/v1/users/{username}/contributions")
    public PagedContributions getContributions(
            @PathVariable String username,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return userService.getContributions(username, page, size);
    }

    /**
     * {@code PATCH /api/v1/users/me}: edit the caller's own profile (API-02.10).
     * Only {@code displayName}/{@code bio}/{@code location}/{@code skills} are
     * editable — GitHub-derived/system-managed fields aren't accepted here.
     */
    @PatchMapping("/api/v1/users/me")
    public UserProfile updateCurrentUser(
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticatedUser User currentUser) {
        return userService.updateCurrentUserProfile(currentUser, request);
    }
}
