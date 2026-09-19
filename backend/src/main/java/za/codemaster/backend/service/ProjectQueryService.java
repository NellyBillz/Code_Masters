package za.codemaster.backend.service;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.codemaster.backend.domain.model.ClaimStatus;
import za.codemaster.backend.dto.*;
import za.codemaster.backend.exception.ApiException;
import za.codemaster.backend.repository.ClaimRepository;
import za.codemaster.backend.repository.IssueRepository;
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

    public ProjectQueryService(ProjectRepository projectRepository,
                                IssueRepository issueRepository,
                                ClaimRepository claimRepository) {
        this.projectRepository = projectRepository;
        this.issueRepository = issueRepository;
        this.claimRepository = claimRepository;
    }

    /**
     * Filters, sorts, and paginates the full project table.
     *
     * @param params the requested filters/sort/pagination; any field may be {@code null}
     * @return a {@link PagedProjects} containing one page of matching results
     */
    @Transactional(readOnly = true)
    public PagedProjects search(ProjectSearchParams params) {
        List<Project> all = projectRepository.findAll().stream()
                .map(this::toDto)
                .toList();

        List<Project> filtered = all.stream()
                .filter(p -> matchesQuery(p, params.q()))
                .filter(p -> matchesLanguage(p, params.language()))
                .filter(p -> matchesCategory(p, params.category()))
                .filter(p -> matchesTag(p, params.tag()))
                .filter(p -> matchesCountry(p, params.country()))
                .filter(p -> matchesHasBeginnerIssues(p, params.hasBeginnerIssues()))
                .toList();

        List<Project> sorted = sort(filtered, params.sort());

        int size = clampSize(params.size());
        int page = clampPage(params.page());
        List<Project> pageItems = paginate(sorted, page, size);

        return new PagedProjects(pageItems, new PageMeta(page, size, sorted.size()));
    }

    /**
     * Looks up a single project by id and assembles its {@link ProjectDetail}.
     * Maintainers/featuredIssues/recentComments stay empty lists here — wiring
     * those is other tickets' scope; this ticket only swaps the project's own
     * data source from mock to real.
     *
     * @param projectId the project id from the path
     * @return the matching project's detail view
     * @throws ApiException with code {@code PROJECT_NOT_FOUND} (404) if no project matches
     */
    @Transactional(readOnly = true)
    public ProjectDetail getProjectDetail(Long projectId) {
        Project project = toDto(findProjectEntityOrThrow(projectId));
        return new ProjectDetail(project, List.of(), List.of(), List.of());
    }

    /**
     * Finds a project by id, or throws the standard PROJECT_NOT_FOUND error.
     * Shared by every method that takes a projectId path variable, so there's
     * exactly one place that defines what "project not found" means.
     */
    private za.codemaster.backend.domain.model.Project findProjectEntityOrThrow(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException(
                        "PROJECT_NOT_FOUND",
                        "No project exists with id " + projectId,
                        HttpStatus.NOT_FOUND
                ));
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

        List<Issue> items = result.getContent().stream().map(this::toDto).toList();

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

        Issue issueDto = toDto(issueEntity);
        Project projectDto = toDto(issueEntity.getProject());

        return new IssueDetail(issueDto, projectDto, List.of(), List.of());
    }

    /** Maps a persisted project row to the API's {@link Project} shape. */
    private Project toDto(za.codemaster.backend.domain.model.Project entity) {
        return new Project(
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
                Boolean.TRUE.equals(entity.getVerified()),
                entity.getVerifiedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    /**
     * Maps a persisted issue row to the API's {@link Issue} shape.
     * {@code claimCount} is computed here (active claims only) rather than
     * stored, since it is not a column on {@code issues}.
     * <p>
     * Public (unlike {@link #toDto(za.codemaster.backend.domain.model.Project)}):
     * this is the single source of truth for Issue entity-to-DTO mapping, reused
     * by {@code IssueService.updateClassification} (API-02.6) after saving an
     * override, rather than duplicating the claimCount/enum-mapping logic there.
     */
    public Issue toDto(za.codemaster.backend.domain.model.Issue entity) {
        long activeClaims = claimRepository.countByIssueIdAndStatus(entity.getId(), ClaimStatus.ACTIVE);

        return new Issue(
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
                (int) activeClaims,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private Double toDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    /** True if {@code q} is blank/null, or found in the project's name, description, owner, or tags (case-insensitive). */
    private boolean matchesQuery(Project p, String q) {
        if (q == null || q.isBlank()) {
            return true;
        }
        String needle = q.toLowerCase(Locale.ROOT);
        return p.name().toLowerCase(Locale.ROOT).contains(needle)
                || p.description().toLowerCase(Locale.ROOT).contains(needle)
                || p.owner().toLowerCase(Locale.ROOT).contains(needle)
                || p.tags().stream().anyMatch(t -> t.toLowerCase(Locale.ROOT).contains(needle));
    }

    /** True if {@code language} is null, or matches the project's primaryLanguage (case-insensitive). */
    private boolean matchesLanguage(Project p, String language) {
        return language == null || p.primaryLanguage().equalsIgnoreCase(language);
    }

    /** True if {@code category} is null, or matches the project's category (case-insensitive). */
    private boolean matchesCategory(Project p, String category) {
        return category == null || p.category().equalsIgnoreCase(category);
    }

    /** True if {@code tag} is null, or found among the project's tags (case-insensitive). */
    private boolean matchesTag(Project p, String tag) {
        return tag == null || p.tags().stream().anyMatch(t -> t.equalsIgnoreCase(tag));
    }

    /** True if {@code country} is null, or found among the project's countryCodes (case-insensitive). */
    private boolean matchesCountry(Project p, String country) {
        return country == null || p.countryCodes().stream().anyMatch(c -> c.equalsIgnoreCase(country));
    }

    /** True if {@code hasBeginnerIssues} is null, or equals the project's hasBeginnerFriendlyIssues flag. */
    private boolean matchesHasBeginnerIssues(Project p, Boolean hasBeginnerIssues) {
        return hasBeginnerIssues == null || p.hasBeginnerFriendlyIssues() == hasBeginnerIssues;
    }

    /**
     * "relevance" has no real scoring yet (Phase 2 territory, design doc §9)
     * so it's a no-op that preserves the filtered order. Everything else
     * sorts descending; newest, most stars, or most contributors first.
     */
    private List<Project> sort(List<Project> projects, String sortParam) {
        if (sortParam == null || sortParam.equals("relevance")) {
            return projects;
        }
        Comparator<Project> comparator = switch (sortParam) {
            case "recent" -> Comparator.comparing(Project::lastActivityAt).reversed();
            case "stars" -> Comparator.comparing(Project::stars).reversed();
            case "contributors" -> Comparator.comparing(Project::contributors).reversed();
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
