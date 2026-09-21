package za.codemaster.backend.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.codemaster.backend.domain.model.Claim;
import za.codemaster.backend.domain.model.ClaimStatus;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.dto.claim.ClaimCompletionSourceDto;
import za.codemaster.backend.dto.claim.ClaimStatusDto;
import za.codemaster.backend.dto.common.PageMeta;
import za.codemaster.backend.dto.user.Contribution;
import za.codemaster.backend.dto.user.PagedContributions;
import za.codemaster.backend.dto.user.PublicUserProfile;
import za.codemaster.backend.dto.user.UpdateUserRequest;
import za.codemaster.backend.dto.user.UserProfile;
import za.codemaster.backend.exception.ApiException;
import za.codemaster.backend.repository.ClaimRepository;
import za.codemaster.backend.repository.UserRepository;

import java.util.List;

/**
 * Service for the developer-profile surface: the read side (API-02.9) — the
 * minimum the frontend needs to know "who's logged in" after OAuth, plus public
 * profiles — and self-editing (API-02.10).
 */
@Service
public class UserService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final UserRepository userRepository;
    private final ClaimRepository claimRepository;
    private final ProjectQueryService projectQueryService;

    public UserService(UserRepository userRepository, ClaimRepository claimRepository,
                        ProjectQueryService projectQueryService) {
        this.userRepository = userRepository;
        this.claimRepository = claimRepository;
        this.projectQueryService = projectQueryService;
    }

    /**
     * Builds the caller's own profile, including {@code email} — never returned
     * from {@link #getPublicProfile}.
     *
     * @param currentUser the authenticated caller, injected via {@code @AuthenticatedUser}
     * @return the caller's profile, in API shape
     */
    @Transactional(readOnly = true)
    public UserProfile getCurrentUserProfile(User currentUser) {
        // Every account is created via GitHub OAuth (GH-02.1 finds-or-creates a User
        // row keyed on github_id) — there is no other signup path — so githubAccess
        // is unconditionally true for any User row that exists at all.
        return new UserProfile(toPublicProfile(currentUser), currentUser.getEmail(), true);
    }

    /**
     * Looks up a user's public profile by username.
     *
     * @param username the username from the path
     * @return the matching user's public profile
     * @throws ApiException with code {@code USER_NOT_FOUND} (404) if no user has that username
     */
    @Transactional(readOnly = true)
    public PublicUserProfile getPublicProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(
                        "USER_NOT_FOUND", "No user exists with username " + username, HttpStatus.NOT_FOUND));
        return toPublicProfile(user);
    }

    /**
     * Updates the caller's own profile. Only fields present (non-null) on the
     * request are changed; GitHub-derived/system-managed fields aren't accepted
     * here at all — same philosophy as {@code ProjectService.updateProject}.
     * <p>
     * Re-fetches the user by id inside this method's own transaction rather than
     * mutating the {@code currentUser} passed in directly — that reference came
     * from {@code SessionAuthenticationFilter}, resolved outside any request
     * transaction, so it isn't guaranteed to be a managed entity here.
     *
     * @param currentUser the authenticated caller, injected via {@code @AuthenticatedUser}
     * @param request     the fields to change; all optional
     * @return the caller's updated profile, in API shape
     */
    @Transactional
    public UserProfile updateCurrentUserProfile(User currentUser, UpdateUserRequest request) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ApiException(
                        "USER_NOT_FOUND", "No user exists with id " + currentUser.getId(), HttpStatus.NOT_FOUND));

        if (request.displayName() != null) {
            user.setDisplayName(request.displayName());
        }
        if (request.bio() != null) {
            user.setBio(request.bio());
        }
        if (request.location() != null) {
            user.setLocation(request.location());
        }
        if (request.skills() != null) {
            user.setSkills(request.skills().toArray(new String[0]));
        }

        User saved = userRepository.save(user);
        return new UserProfile(toPublicProfile(saved), saved.getEmail(), true);
    }

    /**
     * A developer's verified contribution history (API-03.6): only claims with
     * status {@code completed} — active/changes_requested/released claims never
     * appear here — most recent {@code completedAt} first. This is the "did
     * this person actually ship something" record (product doc Feature 9),
     * distinct from raw claim activity.
     *
     * @param username the username from the path
     * @param page     zero-based page number, defaults to 0
     * @param size     page size, defaults to 20, clamped to a max of 50
     * @return one page of the user's verified contributions
     * @throws ApiException with code {@code USER_NOT_FOUND} (404) if no user has that username
     */
    @Transactional(readOnly = true)
    public PagedContributions getContributions(String username, Integer page, Integer size) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException(
                        "USER_NOT_FOUND", "No user exists with username " + username, HttpStatus.NOT_FOUND));

        int resolvedPage = clampPage(page);
        int resolvedSize = clampSize(size);

        Page<Claim> result = claimRepository.findByUserIdAndStatusOrderByCompletedAtDesc(
                user.getId(), ClaimStatus.COMPLETED, PageRequest.of(resolvedPage, resolvedSize));

        List<Contribution> items = result.getContent().stream().map(this::toContribution).toList();
        return new PagedContributions(items, new PageMeta(resolvedPage, resolvedSize, (int) result.getTotalElements()));
    }

    /** Maps a completed claim to the API's {@link Contribution} shape. */
    private Contribution toContribution(Claim claim) {
        return new Contribution(
                projectQueryService.toDto(claim.getIssue()),
                projectQueryService.toDto(claim.getIssue().getProject()),
                ClaimStatusDto.valueOf(claim.getStatus().name()),
                claim.getPullRequestUrl(),
                claim.getCompletionSource() == null
                        ? null
                        : ClaimCompletionSourceDto.valueOf(claim.getCompletionSource().name()),
                claim.getCompletedAt()
        );
    }

    /** Clamps {@code size} to the spec's max of 50; defaults to 20 if not provided. */
    private int clampSize(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        return Math.max(1, Math.min(size, MAX_SIZE));
    }

    /** Defaults to page 0 if not provided or negative. */
    private int clampPage(Integer page) {
        if (page == null || page < 0) {
            return DEFAULT_PAGE;
        }
        return page;
    }

    /**
     * Maps a user to the API's {@link PublicUserProfile} shape.
     * {@code projectsCount} is not yet computed anywhere in the codebase (no
     * ticket populates it); left {@code null} rather than a made-up value.
     * {@code contributionsCount} (API-03.5) counts only this user's
     * {@code completed} claims — verified contributions, not raw claim activity.
     */
    private PublicUserProfile toPublicProfile(User user) {
        return new PublicUserProfile(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getBio(),
                user.getLocation(),
                user.getSkills() == null ? List.of() : List.of(user.getSkills()),
                null,
                (int) claimRepository.countByUserIdAndStatus(user.getId(), ClaimStatus.COMPLETED),
                user.getReputation()
        );
    }
}
