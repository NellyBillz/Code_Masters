package za.codemaster.backend.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.codemaster.backend.domain.model.Project;
import za.codemaster.backend.domain.model.ProjectMaintainer;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.dto.project.CreateProjectRequest;
import za.codemaster.backend.dto.project.MaintainerRole;
import za.codemaster.backend.dto.project.ProjectDto;
import za.codemaster.backend.dto.project.UpdateProjectRequest;
import za.codemaster.backend.exception.ApiException;
import za.codemaster.backend.repository.ProjectMaintainerRepository;
import za.codemaster.backend.repository.ProjectRepository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for project submission and maintainer-only project updates (API-02.7).
 */
@Service
public class ProjectService {

    /**
     * Same shape {@link CreateProjectRequest#githubUrl()} is already validated against
     * (via {@code @Pattern}); re-matched here (not just re-validated) to actually pull
     * the owner/repo segments out, since the request DTO's validation only confirms the
     * shape, it doesn't hand back the parsed groups.
     */
    private static final Pattern GITHUB_URL_PATTERN =
            Pattern.compile("^https://github\\.com/([\\w.-]+)/([\\w.-]+?)(?:\\.git)?/?$");

    private final ProjectRepository projectRepository;
    private final ProjectMaintainerRepository projectMaintainerRepository;
    private final ProjectQueryService projectQueryService;

    public ProjectService(ProjectRepository projectRepository,
                           ProjectMaintainerRepository projectMaintainerRepository,
                           ProjectQueryService projectQueryService) {
        this.projectRepository = projectRepository;
        this.projectMaintainerRepository = projectMaintainerRepository;
        this.projectQueryService = projectQueryService;
    }

    /**
     * Creates a project and adds the submitter as its {@code owner} maintainer, in one
     * transaction (design doc's stated behavior — not two calls a client could interleave
     * badly). GitHub-derived fields (name is a placeholder from the repo slug, description,
     * stars, license, etc. are left unset) are filled in later by the submitter triggering
     * {@code POST /projects/{id}/issues/sync} (GH-02.3), not fetched synchronously here.
     * <p>
     * {@code verified}/{@code verifiedAt} (security audit finding, 2026-09-24) are set when
     * the submitted URL's owner segment matches the submitter's own GitHub username — free,
     * no API call, and covers the common "I'm submitting my own personal repo" case. This is
     * deliberately the *only* automated signal: GitHub's collaborator-permission endpoint
     * (which would otherwise cover org-owned repos and real co-maintainers who aren't the
     * literal repo owner) was investigated and ruled out — confirmed live, it 403s with
     * "Must have push access to view collaborator permission" regardless of token type
     * (tested against both a fine-grained and a classic PAT with {@code repo} scope).
     * That's not a config problem, it's GitHub's actual design for that endpoint: the
     * *calling* token must already have push access to the target repo before it can check
     * anyone's permission on it, which a general-purpose server-side credential never will
     * for an arbitrary third-party repo. The only way to check that at all is with the
     * submitter's *own* OAuth token, not a server-wide one — a bigger change (wider login
     * scope, persisting a user access token) deliberately deferred past the hackathon.
     * Org repos and genuine co-maintainers therefore land unverified today and route to a
     * site admin, same as any other unverified submission — see below.
     * <p>
     * The submitter is only made the project's {@code owner} maintainer when
     * {@code ownershipVerified} — this is the actual access-control decision the
     * verification check exists to gate (security audit finding, 2026-09-24): an
     * unverified submission still gets listed (discovery isn't blocked), but sits with
     * zero maintainers until either (a) a later resync/resubmit signal verifies it, or
     * (b) a site admin confirms real ownership out-of-band and assigns the first owner
     * via {@code POST /projects/{id}/maintainers/assign-owner}
     * ({@link MaintainerService#assignOwner}) — the normal owner-only invite endpoint
     * can't help here since, by definition, an unverified project has no owner yet to
     * call it.
     *
     * @param request   the submission; githubUrl/connection/category required, tags/countryCodes optional
     * @param submitter the authenticated caller, injected via {@code @AuthenticatedUser}
     * @return the created project, in API shape
     * @throws ApiException with code {@code INVALID_GITHUB_URL} (400) if the URL isn't a
     *                       recognizable {@code https://github.com/owner/repo} shape, or
     *                       {@code PROJECT_ALREADY_EXISTS} (409) if that githubUrl is already listed
     */
    @Transactional
    public ProjectDto createProject(CreateProjectRequest request, User submitter) {
        Matcher matcher = GITHUB_URL_PATTERN.matcher(request.githubUrl());
        if (!matcher.matches()) {
            throw new ApiException(
                    "INVALID_GITHUB_URL",
                    "githubUrl must be a GitHub repository URL, e.g. https://github.com/owner/repo",
                    HttpStatus.BAD_REQUEST);
        }
        String owner = matcher.group(1);
        String repo = matcher.group(2);

        boolean ownershipVerified = owner.equalsIgnoreCase(submitter.getUsername());

        Project project = new Project();
        project.setGithubOwner(owner);
        project.setGithubRepo(repo);
        project.setGithubUrl(request.githubUrl());
        project.setName(repo);
        // owner+repo is as unique as the githubUrl itself (GitHub never allows two repos
        // the same owner/repo pair), so this can't collide independently of the
        // PROJECT_ALREADY_EXISTS check below.
        project.setSlug((owner + "-" + repo).toLowerCase(Locale.ROOT));
        project.setCategory(request.category());
        project.setConnection(request.connection().getWireValue());
        project.setVerified(ownershipVerified);
        if (ownershipVerified) {
            project.setVerifiedAt(OffsetDateTime.now());
        }
        if (request.tags() != null) {
            project.setTags(new ArrayList<>(request.tags()));
        }
        if (request.countryCodes() != null) {
            project.setCountryCodes(new ArrayList<>(request.countryCodes()));
        }

        Project saved;
        try {
            // saveAndFlush forces the insert (and any unique-constraint violation on
            // github_url) to happen right here, not pre-checked with a separate query
            // that could race — same pattern as ClaimService.createClaim (API-02.5).
            saved = projectRepository.saveAndFlush(project);
        } catch (DataIntegrityViolationException ex) {
            throw new ApiException(
                    "PROJECT_ALREADY_EXISTS",
                    "A project with this GitHub URL is already listed.",
                    HttpStatus.CONFLICT);
        }

        if (ownershipVerified) {
            ProjectMaintainer maintainer = new ProjectMaintainer();
            maintainer.setProject(saved);
            maintainer.setUser(submitter);
            maintainer.setRole(MaintainerRole.OWNER.getWireValue());
            projectMaintainerRepository.save(maintainer);
        }

        return projectQueryService.toDto(saved);
    }

    /**
     * Updates a project's Code-Masters-specific metadata. Maintainer-only (any role).
     * Only fields present (non-null) on the request are changed; GitHub-derived fields
     * (stars, description, etc.) aren't accepted here at all — that's sync's job.
     * {@code acceptingContributions} (API-03.8) lets a maintainer pause new
     * contributor intake without unpublishing the project — independent of
     * {@code listingStatus}, which isn't a recognized field on this request at
     * all (API-03.1) and so is silently ignored if a client sends it.
     *
     * @param projectId the project id from the path
     * @param request   the fields to change; all optional
     * @param caller    the authenticated caller, injected via {@code @AuthenticatedUser}
     * @return the updated project, in API shape
     * @throws ApiException with code {@code PROJECT_NOT_FOUND} (404) if the project doesn't exist, or
     *                       {@code FORBIDDEN} (403) if the caller isn't a maintainer of the project
     */
    @Transactional
    public ProjectDto updateProject(Long projectId, UpdateProjectRequest request, User caller) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException(
                        "PROJECT_NOT_FOUND", "No project exists with id " + projectId, HttpStatus.NOT_FOUND));

        if (!projectMaintainerRepository.existsByProjectIdAndUserId(projectId, caller.getId())) {
            throw new ApiException(
                    "FORBIDDEN", "Only a maintainer of this project may update it.", HttpStatus.FORBIDDEN);
        }

        if (request.category() != null) {
            project.setCategory(request.category());
        }
        if (request.tags() != null) {
            project.setTags(new ArrayList<>(request.tags()));
        }
        if (request.connection() != null) {
            project.setConnection(request.connection().getWireValue());
        }
        if (request.countryCodes() != null) {
            project.setCountryCodes(new ArrayList<>(request.countryCodes()));
        }
        if (request.acceptingContributions() != null) {
            project.setAcceptingContributions(request.acceptingContributions());
        }

        Project saved = projectRepository.save(project);
        return projectQueryService.toDto(saved);
    }
}
