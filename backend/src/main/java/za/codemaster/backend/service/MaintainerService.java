package za.codemaster.backend.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.codemaster.backend.domain.model.Project;
import za.codemaster.backend.domain.model.ProjectMaintainer;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.dto.project.AddMaintainerRequest;
import za.codemaster.backend.dto.project.MaintainerRole;
import za.codemaster.backend.dto.project.ProjectMaintainerDto;
import za.codemaster.backend.exception.ApiException;
import za.codemaster.backend.repository.ProjectMaintainerRepository;
import za.codemaster.backend.repository.ProjectRepository;
import za.codemaster.backend.repository.UserRepository;

/**
 * Service for owner-only maintainer invite/removal (API-02.8, design doc §8's
 * "bonus beat" of a maintainer inviting a co-maintainer live).
 */
@Service
public class MaintainerService {

    private final ProjectRepository projectRepository;
    private final ProjectMaintainerRepository projectMaintainerRepository;
    private final UserRepository userRepository;
    private final ProjectQueryService projectQueryService;

    public MaintainerService(ProjectRepository projectRepository,
                              ProjectMaintainerRepository projectMaintainerRepository,
                              UserRepository userRepository,
                              ProjectQueryService projectQueryService) {
        this.projectRepository = projectRepository;
        this.projectMaintainerRepository = projectMaintainerRepository;
        this.userRepository = userRepository;
        this.projectQueryService = projectQueryService;
    }

    /**
     * Invites a user to maintain a project, by username. Owner-only.
     * {@code role} defaults to {@code maintainer} when not provided on the request.
     *
     * @param projectId the project id from the path
     * @param request   the username to invite, and an optional role
     * @param caller    the authenticated caller, injected via {@code @AuthenticatedUser}
     * @return the new maintainer relationship, in API shape
     * @throws ApiException with code {@code PROJECT_NOT_FOUND} (404) if the project doesn't exist,
     *                       {@code FORBIDDEN} (403) if the caller isn't an owner of the project,
     *                       {@code USER_NOT_FOUND} (404) if no user has that username, or
     *                       {@code MAINTAINER_ALREADY_EXISTS} (409) if that user already maintains it
     */
    @Transactional
    public ProjectMaintainerDto inviteMaintainer(Long projectId, AddMaintainerRequest request, User caller) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException(
                        "PROJECT_NOT_FOUND", "No project exists with id " + projectId, HttpStatus.NOT_FOUND));

        requireOwner(projectId, caller.getId());

        User invitee = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new ApiException(
                        "USER_NOT_FOUND", "No user exists with username " + request.username(), HttpStatus.NOT_FOUND));

        // Pre-checked (unlike claims' partial-index case): DB-02.2's own contract for this
        // table says to check first so a duplicate invite fails with a clean 409, not a raw
        // SQL exception. The saveAndFlush catch below is a defensive backstop for the race
        // window between this check and the insert, not the primary mechanism.
        if (projectMaintainerRepository.existsByProjectIdAndUserId(projectId, invitee.getId())) {
            throw new ApiException(
                    "MAINTAINER_ALREADY_EXISTS",
                    "This user is already a maintainer on this project.",
                    HttpStatus.CONFLICT);
        }

        MaintainerRole role = request.role() == null ? MaintainerRole.MAINTAINER : request.role();

        ProjectMaintainer maintainer = new ProjectMaintainer();
        maintainer.setProject(project);
        maintainer.setUser(invitee);
        maintainer.setRole(role.getWireValue());

        ProjectMaintainer saved;
        try {
            saved = projectMaintainerRepository.saveAndFlush(maintainer);
        } catch (DataIntegrityViolationException ex) {
            throw new ApiException(
                    "MAINTAINER_ALREADY_EXISTS",
                    "This user is already a maintainer on this project.",
                    HttpStatus.CONFLICT);
        }

        return projectQueryService.toDto(saved);
    }

    /**
     * Removes a maintainer from a project. Owner-only. Refuses to remove the last
     * remaining {@code owner}, so a project can never end up with zero owners.
     *
     * @param projectId the project id from the path
     * @param userId    the id of the maintainer to remove
     * @param caller    the authenticated caller, injected via {@code @AuthenticatedUser}
     * @throws ApiException with code {@code PROJECT_NOT_FOUND} (404) if the project doesn't exist,
     *                       {@code FORBIDDEN} (403) if the caller isn't an owner of the project,
     *                       {@code MAINTAINER_NOT_FOUND} (404) if that user doesn't maintain it, or
     *                       {@code CANNOT_REMOVE_LAST_OWNER} (409) if removing them would leave zero owners
     */
    @Transactional
    public void removeMaintainer(Long projectId, Long userId, User caller) {
        if (!projectRepository.existsById(projectId)) {
            throw new ApiException(
                    "PROJECT_NOT_FOUND", "No project exists with id " + projectId, HttpStatus.NOT_FOUND);
        }

        requireOwner(projectId, caller.getId());

        ProjectMaintainer toRemove = projectMaintainerRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ApiException(
                        "MAINTAINER_NOT_FOUND", "This user does not maintain this project.", HttpStatus.NOT_FOUND));

        if (MaintainerRole.OWNER.getWireValue().equals(toRemove.getRole())) {
            long ownerCount = projectMaintainerRepository.countByProjectIdAndRole(
                    projectId, MaintainerRole.OWNER.getWireValue());
            if (ownerCount <= 1) {
                throw new ApiException(
                        "CANNOT_REMOVE_LAST_OWNER",
                        "A project must have at least one owner.",
                        HttpStatus.CONFLICT);
            }
        }

        projectMaintainerRepository.delete(toRemove);
    }

    /** Shared owner check for both operations: caller must be a maintainer with role owner. */
    private void requireOwner(Long projectId, Long userId) {
        ProjectMaintainer membership = projectMaintainerRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ApiException(
                        "FORBIDDEN", "Only a project owner may manage maintainers.", HttpStatus.FORBIDDEN));

        if (!MaintainerRole.OWNER.getWireValue().equals(membership.getRole())) {
            throw new ApiException(
                    "FORBIDDEN", "Only a project owner may manage maintainers.", HttpStatus.FORBIDDEN);
        }
    }
}
