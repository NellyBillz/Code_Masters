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
import za.codemaster.backend.domain.model.Comment;
import za.codemaster.backend.domain.model.Issue;
import za.codemaster.backend.domain.model.Project;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.dto.report.PagedReports;
import za.codemaster.backend.dto.report.ReportDto;
import za.codemaster.backend.dto.report.ReportReviewDecision;
import za.codemaster.backend.dto.report.ReportStatus;
import za.codemaster.backend.dto.report.ReportTargetType;
import za.codemaster.backend.dto.report.ReviewReportRequest;
import za.codemaster.backend.exception.ApiException;
import za.codemaster.backend.repository.ClaimRepository;
import za.codemaster.backend.repository.CommentRepository;
import za.codemaster.backend.repository.IssueRepository;
import za.codemaster.backend.repository.ProjectRepository;
import za.codemaster.backend.repository.ReportRepository;
import za.codemaster.backend.security.SiteAdminGuard;
import za.codemaster.backend.repository.UserRepository;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies API-03.9's acceptance criteria (round3-tickets.md): reporting the
 * same comment twice by the same user -> 409; a second, different user
 * reporting the same comment -> 201/success; non-admin hitting the admin
 * queue -> 403; resolving a report leaves the reported comment untouched.
 * <p>
 * Runs against a real (test) Postgres database, same pattern as every other
 * service test in this package.
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
class ReportServiceTest {

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private UserRepository userRepository;

    private ReportService service;
    private ProjectQueryServiceFixtures fixtures;

    @BeforeEach
    void setUp() {
        // A generous limit — this class isn't testing API-03.10's rate limiting.
        service = new ReportService(reportRepository, commentRepository, projectRepository, claimRepository,
                new SiteAdminGuard(), new RateLimitService(1_000_000, 1_000_000, 1_000_000));
        fixtures = ProjectQueryServiceFixtures.seed(projectRepository, issueRepository);
    }

    private User newUser(String prefix) {
        return newUser(prefix, false);
    }

    private User newUser(String prefix, boolean isSiteAdmin) {
        return userRepository.save(User.builder()
                .githubId(System.nanoTime())
                .username(prefix + "_" + System.nanoTime())
                .displayName(prefix)
                .isSiteAdmin(isSiteAdmin)
                .build());
    }

    private Comment newComment() {
        Issue issue = issueRepository.findById(fixtures.issueId(0)).orElseThrow();
        Comment comment = new Comment();
        comment.setIssue(issue);
        comment.setUser(newUser("author"));
        comment.setBody("A comment worth reporting");
        return commentRepository.save(comment);
    }

    @Test
    void reportCommentSucceeds() {
        Comment comment = newComment();
        User reporter = newUser("reporter");

        ReportDto report = service.reportComment(comment.getId(), "This is spam.", reporter);

        assertEquals(ReportTargetType.COMMENT, report.targetType());
        assertEquals(comment.getId(), report.targetId());
        assertEquals("This is spam.", report.reason());
        assertEquals(ReportStatus.OPEN, report.status());
        assertEquals(reporter.getUsername(), report.reporter().username());
    }

    @Test
    void reportingTheSameCommentTwiceBySameUserThrows409() {
        Comment comment = newComment();
        User reporter = newUser("reporter");
        service.reportComment(comment.getId(), "This is spam.", reporter);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.reportComment(comment.getId(), "Still spam.", reporter));

        assertEquals("REPORT_ALREADY_EXISTS", ex.getCode());
        assertEquals(org.springframework.http.HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void aDifferentUserReportingTheSameCommentSucceeds() {
        Comment comment = newComment();
        User reporterA = newUser("reporterA");
        User reporterB = newUser("reporterB");

        service.reportComment(comment.getId(), "Spam from A's view.", reporterA);
        ReportDto secondReport = service.reportComment(comment.getId(), "Spam from B's view.", reporterB);

        assertEquals(reporterB.getUsername(), secondReport.reporter().username());
    }

    @Test
    void reportingAMissingCommentThrowsCommentNotFound() {
        User reporter = newUser("reporter");

        ApiException ex = assertThrows(ApiException.class,
                () -> service.reportComment(-999L, "This is spam.", reporter));

        assertEquals("COMMENT_NOT_FOUND", ex.getCode());
    }

    @Test
    void reportingASoftDeletedCommentThrowsCommentNotFound() {
        Comment comment = newComment();
        comment.setDeletedAt(OffsetDateTime.now());
        commentRepository.save(comment);
        User reporter = newUser("reporter");

        ApiException ex = assertThrows(ApiException.class,
                () -> service.reportComment(comment.getId(), "This is spam.", reporter));

        assertEquals("COMMENT_NOT_FOUND", ex.getCode());
    }

    @Test
    void reportProjectSucceeds() {
        Long projectId = fixtures.projectId(0);
        User reporter = newUser("reporter");

        ReportDto report = service.reportProject(projectId, "Not actually African open source.", reporter);

        assertEquals(ReportTargetType.PROJECT, report.targetType());
        assertEquals(projectId, report.targetId());
    }

    @Test
    void reportingTheSameProjectTwiceBySameUserThrows409() {
        Long projectId = fixtures.projectId(0);
        User reporter = newUser("reporter");
        service.reportProject(projectId, "Reason one.", reporter);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.reportProject(projectId, "Reason two.", reporter));

        assertEquals("REPORT_ALREADY_EXISTS", ex.getCode());
    }

    @Test
    void reportingAMissingProjectThrowsProjectNotFound() {
        User reporter = newUser("reporter");

        ApiException ex = assertThrows(ApiException.class,
                () -> service.reportProject(-999L, "This is spam.", reporter));

        assertEquals("PROJECT_NOT_FOUND", ex.getCode());
    }

    @Test
    void listReportsByNonAdminIsForbidden() {
        User nonAdmin = newUser("nonadmin");

        ApiException ex = assertThrows(ApiException.class,
                () -> service.listReports(null, null, null, nonAdmin));

        assertEquals("FORBIDDEN", ex.getCode());
        assertEquals(org.springframework.http.HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void listReportsDefaultsToOpenStatus() {
        Comment comment = newComment();
        User reporter = newUser("reporter");
        User admin = newUser("admin", true);
        ReportDto filed = service.reportComment(comment.getId(), "This is spam.", reporter);

        PagedReports openReports = service.listReports(null, null, null, admin);

        assertTrue(openReports.items().stream().anyMatch(r -> r.id().equals(filed.id())));
        assertTrue(openReports.items().stream().allMatch(r -> r.status() == ReportStatus.OPEN));
    }

    @Test
    void listReportsFiltersByExplicitStatus() {
        Comment comment = newComment();
        User reporter = newUser("reporter");
        User admin = newUser("admin", true);
        ReportDto filed = service.reportComment(comment.getId(), "This is spam.", reporter);
        service.reviewReport(filed.id(), new ReviewReportRequest(ReportReviewDecision.DISMISSED, "Not actually spam."), admin);

        PagedReports dismissedReports = service.listReports(ReportStatus.DISMISSED, null, null, admin);
        PagedReports openReports = service.listReports(ReportStatus.OPEN, null, null, admin);

        assertTrue(dismissedReports.items().stream().anyMatch(r -> r.id().equals(filed.id())));
        assertTrue(openReports.items().stream().noneMatch(r -> r.id().equals(filed.id())));
    }

    @Test
    void reviewByNonAdminIsForbidden() {
        Comment comment = newComment();
        User reporter = newUser("reporter");
        User nonAdmin = newUser("nonadmin");
        ReportDto filed = service.reportComment(comment.getId(), "This is spam.", reporter);

        ApiException ex = assertThrows(ApiException.class, () -> service.reviewReport(
                filed.id(), new ReviewReportRequest(ReportReviewDecision.DISMISSED, null), nonAdmin));

        assertEquals("FORBIDDEN", ex.getCode());
    }

    @Test
    void resolvingAReportLeavesTheReportedCommentUntouched() {
        Comment comment = newComment();
        String originalBody = comment.getBody();
        User reporter = newUser("reporter");
        User admin = newUser("admin", true);
        ReportDto filed = service.reportComment(comment.getId(), "This is spam.", reporter);

        ReportDto reviewed = service.reviewReport(
                filed.id(), new ReviewReportRequest(ReportReviewDecision.RESOLVED, "Agreed, but left removal to the maintainer."), admin);

        assertEquals(ReportStatus.RESOLVED, reviewed.status());
        assertNotNull(reviewed.resolvedAt());

        Comment reloaded = commentRepository.findById(comment.getId()).orElseThrow();
        assertEquals(originalBody, reloaded.getBody());
        assertNull(reloaded.getDeletedAt(), "resolving a report must not delete the comment");
    }

    @Test
    void reviewingAMissingReportThrowsReportNotFound() {
        User admin = newUser("admin", true);

        ApiException ex = assertThrows(ApiException.class, () -> service.reviewReport(
                -999L, new ReviewReportRequest(ReportReviewDecision.DISMISSED, null), admin));

        assertEquals("REPORT_NOT_FOUND", ex.getCode());
        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, ex.getStatus());
    }
}
