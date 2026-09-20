package za.codemaster.backend.service;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.codemaster.backend.domain.model.ClaimStatus;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.dto.common.PageMeta;
import za.codemaster.backend.dto.issue.*;
import za.codemaster.backend.dto.project.*;
import za.codemaster.backend.dto.user.PublicUserProfile;
import za.codemaster.backend.exception.ApiException;
import za.codemaster.backend.repository.ClaimRepository;
import za.codemaster.backend.repository.IssueRepository;
import za.codemaster.backend.repository.ProjectMaintainerRepository;
import za.codemaster.backend.repository.ProjectRepository;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Applies filtering, sorting, and pagination for {@code GET /api/v1/projects}
 * and friends against real persistence ({@link ProjectRepository},
 * {@link IssueRepository}, {@link ClaimRepository}).
 * <p>
 * Round 2 (API-02.1): this class used to run the same logic against
 * {@code MockDataStore}'s hardcoded lists. The filter/sort/paginate helpers
 * below are unchanged from Round 1 — only where the data comes from changed.
 * Project search still filters/sorts in Java after loading the full project
 * table, because {@code q}/{@code tag}/{@code country} search spans a
 * free-text match plus two normalized child tables that {@link ProjectRepository}
 * does not yet expose as a single query; project counts are small enough for
 * a hackathon MVP that this is not a real bottleneck. Issue listing, by
 * contrast, delegates filtering and pagination to {@link IssueRepository#findWithFilters}
 * since that query already exists and matches the required filters exactly.
 */

@Service
public class ProjectQueryService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final ProjectRepository projectRepository;
    private final IssueRepository issueRepository;
    private final ClaimRepository claimRepository;
    private final ProjectMaintainerRepository projectMaintainerRepository;

    public ProjectQueryService(ProjectRepository projectRepository,
                                IssueRepository issueRepository,
                                ClaimRepository claimRepository,
                                ProjectMaintainerRepository projectMaintainerRepository) {
        this.projectRepository = projectRepository;
        this.issueRepository = issueRepository;
        this.claimRepository = claimRepository;
        this.projectMaintainerRepository = projectMaintainerRepository;
    }

    /**
     * Filters, sorts, and paginates the full project table.
     *
     * @param params the requested filters/sort/pagination; any field may be {@code null}
     * @return a {@link PagedProjects} containing one page of matching results
     */
    @Transactional(readOnly = true)
    public PagedProjects search(ProjectSearchParams params) {
        List<ProjectDto> all = projectRepository.findAll().stream()
                .map(this::toDto)
                .toList();

        List<ProjectDto> filtered = all.stream()
                .filter(p -> p.listingStatus() == ProjectListingStatus.PUBLISHED)
                .filter(p -> matchesQuery(p, params.q()))
                .filter(p -> matchesLanguage(p, params.language()))
                .filter(p -> matchesCategory(p, params.category()))
                .filter(p -> matchesTag(p, params.tag()))
                .filter(p -> matchesCountry(p, params.country()))
                .filter(p -> matchesHasBeginnerIssues(p, params.hasBeginnerIssues()))
                .toList();

        List<ProjectDto> sorted = sort(filtered, params.sort());

        int size = clampSize(params.size());
        int page = clampPage(params.page());
        List<ProjectDto> pageItems = paginate(sorted, page, size);

        return new PagedProjects(pageItems, new PageMeta(page, size, sorted.size()));
    }

    /**
     * Looks up a single project by id and assembles its {@link ProjectDetail}.
     * Maintainers are real (API-02.7 needs this for its own acceptance criteria —
     * a successful {@code POST /projects} must immediately show the caller in
     * the maintainer list). featuredIssues/recentComments stay empty lists here —
     * wiring those is other tickets' scope.
     * <p>
     * API-03.1: a non-{@code published} project (pending review, or rejected)
     * is only visible to its submitter/any maintainer — everyone else, including
     * an anonymous caller, gets the same {@code PROJECT_NOT_FOUND} a missing id
     * would produce, matching the spec's documented exception.
     *
     * @param projectId the project id from the path
     * @param caller    the requesting user, or {@code null} if anonymous
     * @return the matching project's detail view
     * @throws ApiException with code {@code PROJECT_NOT_FOUND} (404) if no project matches,
     *                       or if it exists but isn't visible to {@code caller}
     */
    @Transactional(readOnly = true)
    public ProjectDetail getProjectDetail(Long projectId, User caller) {
        za.codemaster.backend.domain.model.Project entity = findProjectEntityOrThrow(projectId);

        boolean published = entity.getListingStatus() == za.codemaster.backend.domain.model.ListingStatus.PUBLISHED;
        boolean callerIsMaintainer = caller != null
                && projectMaintainerRepository.existsByProjectIdAndUserId(projectId, caller.getId());
        if (!published && !callerIsMaintainer) {
            throw projectNotFound(projectId);
        }

        ProjectDto project = toDto(entity);
        List<ProjectMaintainerDto> maintainers = projectMaintainerRepository.findByProjectId(projectId).stream()
                .map(this::toDto)
                .toList();
        return new ProjectDetail(project, maintainers, List.of(), List.of());
    }

    /**
     * Finds a project by id, or throws the standard PROJECT_NOT_FOUND error.
     * Shared by every method that takes a projectId path variable, so there's
     * exactly one place that defines what "project not found" means.
     */
    private za.codemaster.backend.domain.model.Project findProjectEntityOrThrow(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> projectNotFound(projectId));
    }

    private ApiException projectNotFound(Long projectId) {
        return new ApiException(
                "PROJECT_NOT_FOUND",
                "No project exists with id " + projectId,
                HttpStatus.NOT_FOUND
        );
    }

    /**
     * Lists a project's issues, filtered by difficulty/label/status and paginated.
     * Delegates to {@link IssueRepository#findWithFilters}, which already does
     * this filtering and pagination in SQL.
     *
     * @param projectId the project id from the path
     * @param params    the requested filters/pagination; any field may be {@code null}
     * @return one page of matching issues for this project
     * @throws ApiException with code {@code PROJECT_NOT_FOUND} (404) if the
     *                       project itself doesn't exist — same exception,
     *                       same helper, as {@link #getProjectDetail}
     */
    @Transactional(readOnly = true)
    public PagedIssues getProjectIssues(Long projectId, ProjectIssuesSearchParams params) {
        findProjectEntityOrThrow(projectId);

        int size = clampSize(params.size());
        int page = clampPage(params.page());

        String status = params.status() == null ? null : params.status().getWireValue();
        String difficulty = params.difficulty() == null ? null : params.difficulty().getWireValue();

        Page<za.codemaster.backend.domain.model.Issue> result = issueRepository.findWithFilters(
                projectId, status, difficulty, null, params.label(), PageRequest.of(page, size));

        List<IssueDto> items = result.getContent().stream().map(this::toDto).toList();

        return new PagedIssues(items, new PageMeta(page, size, (int) result.getTotalElements()));
    }

    /**
     * Looks up a single issue by id and assembles its {@link IssueDetail},
     * including the real project it belongs to (via the issue's own FK
     * relationship, guaranteed non-null by the database). Comments/claims
     * stay empty lists here — wiring those is other tickets' scope.
     *
     * @param issueId the issue id from the path
     * @return the matching issue's detail view
     * @throws ApiException with code {@code ISSUE_NOT_FOUND} (404) if no issue matches
     */
    @Transactional(readOnly = true)
    public IssueDetail getIssueDetail(Long issueId) {
        var issueEntity = issueRepository.findById(issueId)
                .orElseThrow(() -> new ApiException(
                        "ISSUE_NOT_FOUND",
                        "No issue exists with id " + issueId,
                        HttpStatus.NOT_FOUND
                ));

        IssueDto issueDto = toDto(issueEntity);
        ProjectDto projectDto = toDto(issueEntity.getProject());

        return new IssueDetail(issueDto, projectDto, List.of(), List.of());
    }

    /**
     * Maps a persisted project row to the API's {@link ProjectDto} shape.
     * Public (same reasoning as {@link #toDto(za.codemaster.backend.domain.model.Issue)}):
     * reused by {@code ProjectService} (API-02.7) after creating/updating a project,
     * rather than duplicating this mapping there.
     */
    public ProjectDto toDto(za.codemaster.backend.domain.model.Project entity) {
        return new ProjectDto(
                entity.getId(),
                entity.getName(),
                entity.getSlug(),
                entity.getDescription(),
                entity.getGithubUrl(),
                entity.getGithubOwner(),
                entity.getPrimaryLanguage(),
                entity.getLanguages() == null ? List.of() : List.of(entity.getLanguages()),
                entity.getCategory(),
                entity.getTags() == null ? List.of() : List.copyOf(entity.getTags()),
                entity.getCountryCodes() == null ? List.of() : List.copyOf(entity.getCountryCodes()),
                ProjectConnection.valueOf(entity.getConnection().toUpperCase(Locale.ROOT)),
                entity.getLicense(),
                entity.getStars(),
                entity.getForks(),
                entity.getOpenIssues(),
                entity.getContributors(),
                Boolean.TRUE.equals(entity.getHasBeginnerFriendlyIssues()),
                entity.getLastActivityAt(),
                ProjectListingStatus.valueOf(entity.getListingStatus().name()),
                Boolean.TRUE.equals(entity.getVerified()),
                entity.getVerifiedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    /**
     * Maps a persisted issue row to the API's {@link IssueDto} shape.
     * {@code claimCount} is computed here rather than stored, since it is not
     * a column on {@code issues}. Counts only in-flight claims — {@code active}
     * or {@code changes_requested} — not {@code completed}/{@code released}
     * (API-03.5: a claim that already resolved, one way or another, shouldn't
     * still read as "N people are working on this").
     * <p>
     * Public (unlike {@link #toDto(za.codemaster.backend.domain.model.Project)}):
     * this is the single source of truth for Issue entity-to-DTO mapping, reused
     * by {@code IssueService.updateClassification} (API-02.6) after saving an
     * override, rather than duplicating the claimCount/enum-mapping logic there.
     */
    public IssueDto toDto(za.codemaster.backend.domain.model.Issue entity) {
        long inFlightClaims = claimRepository.countByIssueIdAndStatusIn(
                entity.getId(), List.of(ClaimStatus.ACTIVE, ClaimStatus.CHANGES_REQUESTED));

        return new IssueDto(
                entity.getId(),
                entity.getProject().getId(),
                entity.getGithubIssueNumber(),
                entity.getTitle(),
                entity.getBodyExcerpt(),
                entity.getGithubUrl(),
                IssueStatus.valueOf(entity.getStatus().toUpperCase(Locale.ROOT)),
                entity.getLabels() == null ? List.of() : List.of(entity.getLabels()),
                entity.getDifficulty() == null
                        ? Difficulty.UNKNOWN
                        : Difficulty.valueOf(entity.getDifficulty().toUpperCase(Locale.ROOT)),
                Boolean.TRUE.equals(entity.getIsBeginnerFriendly()),
                entity.getDifficultyOverriddenByUser() != null,
                toDouble(entity.getMaintainerResponseScore()),
                toDouble(entity.getProjectHealthScore()),
                toDouble(entity.getFreshnessScore()),
                toDouble(entity.getContributionScore()),
                (int) inFlightClaims,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    /**
     * Maps a persisted maintainer relationship row to the API's {@link ProjectMaintainerDto} shape.
     * Public (same reasoning as the {@code Project}/{@code Issue} overloads): reused by
     * {@code MaintainerService} (API-02.8) after inviting a maintainer.
     */
    public ProjectMaintainerDto toDto(za.codemaster.backend.domain.model.ProjectMaintainer entity) {
        return new ProjectMaintainerDto(
                toPublicProfile(entity.getUser()),
                MaintainerRole.valueOf(entity.getRole().toUpperCase(Locale.ROOT)),
                entity.getCreatedAt()
        );
    }

    /**
     * Maps a user to the API's {@link PublicUserProfile} shape.
     * {@code projectsCount}/{@code contributionsCount} are not yet computed anywhere
     * in the codebase (no ticket populates it); left {@code null} rather than a
     * made-up value. {@code contributionsCount} (API-03.5) counts only that
     * user's {@code completed} claims — verified contributions, not raw claim
     * activity.
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

    private Double toDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    /**
     * True if {@code q} is blank/null, or found in the project's name, description, owner, or tags
     * (case-insensitive). {@code description} is null-safe: a project submitted via
     * {@code POST /projects} (API-02.7) has no description until sync fills one in, and this
     * predicate runs against every row, so a single such project used to 500 any {@code q} search.
     */
    private boolean matchesQuery(ProjectDto p, String q) {
        if (q == null || q.isBlank()) {
            return true;
        }
        String needle = q.toLowerCase(Locale.ROOT);
        return containsIgnoreCase(p.name(), needle)
                || containsIgnoreCase(p.description(), needle)
                || containsIgnoreCase(p.owner(), needle)
                || p.tags().stream().anyMatch(t -> containsIgnoreCase(t, needle));
    }

    private boolean containsIgnoreCase(String haystack, String needle) {
        return haystack != null && haystack.toLowerCase(Locale.ROOT).contains(needle);
    }

    /** True if {@code language} is null, or matches the project's primaryLanguage (case-insensitive). */
    private boolean matchesLanguage(ProjectDto p, String language) {
        return language == null || p.primaryLanguage().equalsIgnoreCase(language);
    }

    /** True if {@code category} is null, or matches the project's category (case-insensitive). */
    private boolean matchesCategory(ProjectDto p, String category) {
        return category == null || p.category().equalsIgnoreCase(category);
    }

    /** True if {@code tag} is null, or found among the project's tags (case-insensitive). */
    private boolean matchesTag(ProjectDto p, String tag) {
        return tag == null || p.tags().stream().anyMatch(t -> t.equalsIgnoreCase(tag));
    }

    /** True if {@code country} is null, or found among the project's countryCodes (case-insensitive). */
    private boolean matchesCountry(ProjectDto p, String country) {
        return country == null || p.countryCodes().stream().anyMatch(c -> c.equalsIgnoreCase(country));
    }

    /** True if {@code hasBeginnerIssues} is null, or equals the project's hasBeginnerFriendlyIssues flag. */
    private boolean matchesHasBeginnerIssues(ProjectDto p, Boolean hasBeginnerIssues) {
        return hasBeginnerIssues == null || p.hasBeginnerFriendlyIssues() == hasBeginnerIssues;
    }

    /**
     * "relevance" has no real scoring yet (Phase 2 territory, design doc §9)
     * so it's a no-op that preserves the filtered order. Everything else
     * sorts descending; newest, most stars, or most contributors first.
     */
    private List<ProjectDto> sort(List<ProjectDto> projects, String sortParam) {
        if (sortParam == null || sortParam.equals("relevance")) {
            return projects;
        }
        Comparator<ProjectDto> comparator = switch (sortParam) {
            case "recent" -> Comparator.comparing(ProjectDto::lastActivityAt).reversed();
            case "stars" -> Comparator.comparing(ProjectDto::stars).reversed();
            case "contributors" -> Comparator.comparing(ProjectDto::contributors).reversed();
            default -> throw new IllegalArgumentException("Unknown sort value: " + sortParam);
        };
        return projects.stream().sorted(comparator).toList();
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

    /** Slices {@code items} to the requested page; returns an empty list if {@code page} is past the end. */
    private <T> List<T> paginate(List<T> items, int page, int size) {
        int fromIndex = page * size;
        if (fromIndex >= items.size()) {
            return List.of();
        }
        int toIndex = Math.min(fromIndex + size, items.size());
        return items.subList(fromIndex, toIndex);
    }
}
