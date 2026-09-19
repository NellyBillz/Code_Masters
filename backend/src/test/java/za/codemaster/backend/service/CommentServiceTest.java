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
import za.codemaster.backend.dto.Comment;
import za.codemaster.backend.dto.PagedComments;
import za.codemaster.backend.exception.ApiException;
import za.codemaster.backend.repository.ClaimRepository;
import za.codemaster.backend.repository.CommentRepository;
import za.codemaster.backend.repository.IssueRepository;
import za.codemaster.backend.repository.ProjectRepository;
import za.codemaster.backend.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies API-02.3's service-layer behavior (round2-tickets.md): comments can be
 * created and listed for both projects and issues, and each lookup 404s on a
 * missing parent exactly like {@link ProjectQueryService}'s existing endpoints do.
 * <p>
 * Runs against a real (test) Postgres database, same pattern as
 * {@code ProjectIssuesTest}/{@code IssueDetailTest}.
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
class CommentServiceTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private UserRepository userRepository;

    private CommentService service;
    private ProjectQueryServiceFixtures fixtures;
    private User author;

    @BeforeEach
    void setUp() {
        service = new CommentService(commentRepository, projectRepository, issueRepository);
        fixtures = ProjectQueryServiceFixtures.seed(projectRepository, issueRepository);
        author = userRepository.save(User.builder()
                .githubId(System.nanoTime())
                .username("commenter_" + System.nanoTime())
                .displayName("Commenter")
                .build());
    }

    @Test
    void createProjectCommentPersistsAndReturnsAuthorAndBody() {
        Long projectId = fixtures.projectId(0);

        Comment created = service.createProjectComment(projectId, "Great project!", author);

        assertNotNull(created.id());
        assertEquals("Great project!", created.body());
        assertEquals(author.getUsername(), created.author().username());
        assertFalse(created.edited());
    }

    @Test
    void createProjectCommentOnMissingProjectThrowsProjectNotFound() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.createProjectComment(-999L, "hello", author));

        assertEquals("PROJECT_NOT_FOUND", ex.getCode());
        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void createIssueCommentPersistsAndReturnsAuthorAndBody() {
        Long issueId = fixtures.issueId(0);

        Comment created = service.createIssueComment(issueId, "I can help with this.", author);

        assertNotNull(created.id());
        assertEquals("I can help with this.", created.body());
        assertEquals(author.getUsername(), created.author().username());
    }

    @Test
    void createIssueCommentOnMissingIssueThrowsIssueNotFound() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.createIssueComment(-999L, "hello", author));

        assertEquals("ISSUE_NOT_FOUND", ex.getCode());
        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void createdProjectCommentAppearsInProjectCommentList() {
        Long projectId = fixtures.projectId(0);
        service.createProjectComment(projectId, "First comment", author);

        PagedComments page = service.getProjectComments(projectId, null, null);

        assertEquals(1, page.items().size());
        assertEquals("First comment", page.items().get(0).body());
        assertEquals(1, page.meta().total());
    }

    @Test
    void createdIssueCommentAppearsInIssueCommentList() {
        Long issueId = fixtures.issueId(0);
        service.createIssueComment(issueId, "Second comment", author);

        PagedComments page = service.getIssueComments(issueId, null, null);

        assertEquals(1, page.items().size());
        assertEquals("Second comment", page.items().get(0).body());
        assertEquals(1, page.meta().total());
    }

    @Test
    void getProjectCommentsOnMissingProjectThrowsProjectNotFound() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.getProjectComments(-999L, null, null));

        assertEquals("PROJECT_NOT_FOUND", ex.getCode());
    }

    @Test
    void getIssueCommentsOnMissingIssueThrowsIssueNotFound() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.getIssueComments(-999L, null, null));

        assertEquals("ISSUE_NOT_FOUND", ex.getCode());
    }

    @Test
    void issueCommentDoesNotLeakIntoProjectCommentListForSameIssuesParentProject() {
        Long projectId = fixtures.projectId(0);
        Long issueId = fixtures.issueId(0);

        service.createIssueComment(issueId, "Issue-only comment", author);

        PagedComments projectComments = service.getProjectComments(projectId, null, null);

        assertEquals(0, projectComments.items().size(),
                "a comment posted to an issue must not appear in its parent project's comment list");
    }
}
