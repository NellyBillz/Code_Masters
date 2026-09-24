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
import za.codemaster.backend.domain.model.Project;
import za.codemaster.backend.domain.model.ProjectMaintainer;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.dto.project.AddMaintainerRequest;
import za.codemaster.backend.dto.project.MaintainerRole;
import za.codemaster.backend.dto.project.ProjectMaintainerDto;
import za.codemaster.backend.exception.ApiException;
import za.codemaster.backend.repository.ClaimRepository;
import za.codemaster.backend.repository.IssueRepository;
import za.codemaster.backend.repository.ProjectMaintainerRepository;
import za.codemaster.backend.repository.ProjectRepository;
import za.codemaster.backend.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies API-02.8's acceptance criteria (round2-tickets.md): attempting to remove
 * the sole owner -> 409 and the project still has that owner afterward, and a second
 * invite of the same user -> 409.
 * <p>
 * Runs against a real (test) Postgres database, same pattern as
 * {@code ProjectServiceTest}/{@code ClaimServiceTest}.
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
class MaintainerServiceTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private ProjectMaintainerRepository projectMaintainerRepository;

    @Autowired
    private UserRepository userRepository;

    private MaintainerService service;
    private ProjectQueryServiceFixtures fixtures;
    private Long projectId;
    private User owner;

    @BeforeEach
    void setUp() {
        ProjectQueryService projectQueryService =
                new ProjectQueryService(projectRepository, issueRepository, claimRepository, projectMaintainerRepository);
        service = new MaintainerService(projectRepository, projectMaintainerRepository, userRepository,
                projectQueryService, new za.codemaster.backend.security.SiteAdminGuard());

        fixtures = ProjectQueryServiceFixtures.seed(projectRepository, issueRepository);
        projectId = fixtures.projectId(0);

        owner = userRepository.save(User.builder()
                .githubId(System.nanoTime())
                .username("owner_" + System.nanoTime())
                .displayName("Owner")
                .build());
        addMaintainer(projectId, owner, "owner");
    }

    private void addMaintainer(Long projectId, User user, String role) {
        Project project = projectRepository.findById(projectId).orElseThrow();
        ProjectMaintainer relationship = new ProjectMaintainer();
        relationship.setProject(project);
        relationship.setUser(user);
        relationship.setRole(role);
        projectMaintainerRepository.save(relationship);
    }

    private User newUser() {
        return userRepository.save(User.builder()
                .githubId(System.nanoTime())
                .username("user_" + System.nanoTime())
                .displayName("User")
                .build());
    }

    @Test
    void ownerCanInviteMaintainerDefaultingToMaintainerRole() {
        User invitee = newUser();
        AddMaintainerRequest request = new AddMaintainerRequest(invitee.getUsername(), null);

        ProjectMaintainerDto created = service.inviteMaintainer(projectId, request, owner);

        assertEquals(MaintainerRole.MAINTAINER, created.role());
        assertEquals(invitee.getUsername(), created.user().username());
    }

    @Test
    void ownerCanInviteMaintainerWithExplicitOwnerRole() {
        User invitee = newUser();
        AddMaintainerRequest request = new AddMaintainerRequest(invitee.getUsername(), MaintainerRole.OWNER);

        ProjectMaintainerDto created = service.inviteMaintainer(projectId, request, owner);

        assertEquals(MaintainerRole.OWNER, created.role());
    }

    @Test
    void inviteByNonOwnerIsForbidden() {
        User plainMaintainer = newUser();
        addMaintainer(projectId, plainMaintainer, "maintainer");
        User invitee = newUser();

        ApiException ex = assertThrows(ApiException.class,
                () -> service.inviteMaintainer(projectId, new AddMaintainerRequest(invitee.getUsername(), null), plainMaintainer));

        assertEquals("FORBIDDEN", ex.getCode());
        assertEquals(org.springframework.http.HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void inviteByNonMaintainerIsForbidden() {
        User stranger = newUser();
        User invitee = newUser();

        ApiException ex = assertThrows(ApiException.class,
                () -> service.inviteMaintainer(projectId, new AddMaintainerRequest(invitee.getUsername(), null), stranger));

        assertEquals("FORBIDDEN", ex.getCode());
    }

    @Test
    void inviteUnknownUsernameThrowsUserNotFound() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.inviteMaintainer(projectId, new AddMaintainerRequest("no-such-user-xyz", null), owner));

        assertEquals("USER_NOT_FOUND", ex.getCode());
        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void inviteOnMissingProjectThrowsProjectNotFound() {
        User invitee = newUser();

        ApiException ex = assertThrows(ApiException.class,
                () -> service.inviteMaintainer(-999L, new AddMaintainerRequest(invitee.getUsername(), null), owner));

        assertEquals("PROJECT_NOT_FOUND", ex.getCode());
    }

    @Test
    void secondInviteOfSameUserThrows409() {
        User invitee = newUser();
        service.inviteMaintainer(projectId, new AddMaintainerRequest(invitee.getUsername(), null), owner);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.inviteMaintainer(projectId, new AddMaintainerRequest(invitee.getUsername(), null), owner));

        assertEquals("MAINTAINER_ALREADY_EXISTS", ex.getCode());
        assertEquals(org.springframework.http.HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void ownerCanRemoveAPlainMaintainer() {
        User plainMaintainer = newUser();
        addMaintainer(projectId, plainMaintainer, "maintainer");

        service.removeMaintainer(projectId, plainMaintainer.getId(), owner);

        assertFalse(projectMaintainerRepository.existsByProjectIdAndUserId(projectId, plainMaintainer.getId()));
    }

    @Test
    void removingTheSoleOwnerIsRejectedAndOwnerRemains() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.removeMaintainer(projectId, owner.getId(), owner));

        assertEquals("CANNOT_REMOVE_LAST_OWNER", ex.getCode());
        assertEquals(org.springframework.http.HttpStatus.CONFLICT, ex.getStatus());
        assertTrue(projectMaintainerRepository.existsByProjectIdAndUserId(projectId, owner.getId()),
                "the sole owner must still be a maintainer after the rejected removal");
    }

    @Test
    void removingOneOfTwoOwnersSucceeds() {
        User secondOwner = newUser();
        addMaintainer(projectId, secondOwner, "owner");

        service.removeMaintainer(projectId, owner.getId(), secondOwner);

        assertFalse(projectMaintainerRepository.existsByProjectIdAndUserId(projectId, owner.getId()));
        assertTrue(projectMaintainerRepository.existsByProjectIdAndUserId(projectId, secondOwner.getId()));
    }

    @Test
    void removeByNonOwnerIsForbidden() {
        User plainMaintainer = newUser();
        addMaintainer(projectId, plainMaintainer, "maintainer");
        User anotherMaintainer = newUser();
        addMaintainer(projectId, anotherMaintainer, "maintainer");

        ApiException ex = assertThrows(ApiException.class,
                () -> service.removeMaintainer(projectId, anotherMaintainer.getId(), plainMaintainer));

        assertEquals("FORBIDDEN", ex.getCode());
    }

    @Test
    void removeNonMaintainerThrowsMaintainerNotFound() {
        User notAMaintainer = newUser();

        ApiException ex = assertThrows(ApiException.class,
                () -> service.removeMaintainer(projectId, notAMaintainer.getId(), owner));

        assertEquals("MAINTAINER_NOT_FOUND", ex.getCode());
        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void removeOnMissingProjectThrowsProjectNotFound() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.removeMaintainer(-999L, owner.getId(), owner));

        assertEquals("PROJECT_NOT_FOUND", ex.getCode());
    }

    private User siteAdmin() {
        return userRepository.save(User.builder()
                .githubId(System.nanoTime())
                .username("admin_" + System.nanoTime())
                .displayName("Site Admin")
                .isSiteAdmin(true)
                .build());
    }

    @Test
    void siteAdminCanAssignOwnerToAMaintainerlessProject() {
        Long maintainerlessProjectId = fixtures.projectId(1); // no maintainer added in setUp — only projectId(0) got one
        User realOwner = newUser();
        AddMaintainerRequest request = new AddMaintainerRequest(realOwner.getUsername(), null);

        ProjectMaintainerDto created = service.assignOwner(maintainerlessProjectId, request, siteAdmin());

        assertEquals(MaintainerRole.OWNER, created.role());
        assertEquals(realOwner.getUsername(), created.user().username());
        Project reloaded = projectRepository.findById(maintainerlessProjectId).orElseThrow();
        assertTrue(reloaded.getVerified(), "a site admin assigning an owner is itself a verification event");
        assertNotNull(reloaded.getVerifiedAt());
    }

    @Test
    void assignOwnerByNonSiteAdminIsForbidden() {
        Long maintainerlessProjectId = fixtures.projectId(1);
        User realOwner = newUser();
        AddMaintainerRequest request = new AddMaintainerRequest(realOwner.getUsername(), null);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.assignOwner(maintainerlessProjectId, request, owner));

        assertEquals("FORBIDDEN", ex.getCode());
        assertEquals(org.springframework.http.HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void assignOwnerRefusesWhenProjectAlreadyHasAMaintainer() {
        // projectId(0), from setUp, already has `owner` as its maintainer.
        User someone = newUser();
        AddMaintainerRequest request = new AddMaintainerRequest(someone.getUsername(), null);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.assignOwner(projectId, request, siteAdmin()));

        assertEquals("PROJECT_ALREADY_HAS_MAINTAINER", ex.getCode());
        assertEquals(org.springframework.http.HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void assignOwnerUnknownUsernameThrowsUserNotFound() {
        Long maintainerlessProjectId = fixtures.projectId(1);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.assignOwner(maintainerlessProjectId, new AddMaintainerRequest("no-such-user-xyz", null), siteAdmin()));

        assertEquals("USER_NOT_FOUND", ex.getCode());
    }

    @Test
    void assignOwnerOnMissingProjectThrowsProjectNotFound() {
        User realOwner = newUser();

        ApiException ex = assertThrows(ApiException.class,
                () -> service.assignOwner(-999L, new AddMaintainerRequest(realOwner.getUsername(), null), siteAdmin()));

        assertEquals("PROJECT_NOT_FOUND", ex.getCode());
    }
}
