package za.codemaster.backend.persistence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import za.codemaster.backend.BackendApplication;
import za.codemaster.backend.domain.model.Report;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.repository.ReportRepository;
import za.codemaster.backend.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
    classes = BackendApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
        "spring.autoconfigure.exclude=" +
            "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration," +
            "org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration," +
            "org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration"
    }
)
public class ReportsMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private UserRepository userRepository;

    private Long user1Id;
    private Long user2Id;
    private Long targetProjectId;
    private String suffix;

    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString().substring(0, 8);

        user1Id = jdbcTemplate.queryForObject(
            "INSERT INTO users (github_id, username, display_name) VALUES (?, ?, ?) RETURNING id",
            Long.class,
            System.nanoTime(),
            "reporter_1_" + suffix,
            "Reporter One"
        );

        user2Id = jdbcTemplate.queryForObject(
            "INSERT INTO users (github_id, username, display_name) VALUES (?, ?, ?) RETURNING id",
            Long.class,
            System.nanoTime() + 1,
            "reporter_2_" + suffix,
            "Reporter Two"
        );

        targetProjectId = jdbcTemplate.queryForObject(
            "INSERT INTO projects (github_owner, github_repo, github_url, name, slug, connection) " +
            "VALUES (?, ?, ?, ?, ?, ?) RETURNING id",
            Long.class,
            "codemaster",
            "report-target-" + suffix,
            "https://github.com/codemaster/report-target-" + suffix,
            "Target Project",
            "slug-target-" + suffix,
            "south_african"
        );
    }

    @AfterEach
    void tearDown() {
        if (targetProjectId != null) {
            jdbcTemplate.update("DELETE FROM reports WHERE target_id = ?", targetProjectId);
            jdbcTemplate.update("DELETE FROM projects WHERE id = ?", targetProjectId);
        }
        if (user1Id != null) {
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", user1Id);
        }
        if (user2Id != null) {
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", user2Id);
        }
    }

    @Test
    @DisplayName("Acceptance Criteria: Unique constraint rejects second report from same user on same target, allows different user")
    void shouldEnforceUniqueConstraintPerUserAndTarget() {
        // Step 1: User 1 reports the project -> SUCCEEDS
        assertDoesNotThrow(() -> {
            jdbcTemplate.update(
                "INSERT INTO reports (reporter_user_id, target_type, target_id, reason) VALUES (?, ?, ?, ?)",
                user1Id,
                "project",
                targetProjectId,
                "Spam or misleading content"
            );
        });

        // Step 2: User 2 reports the same project -> SUCCEEDS
        assertDoesNotThrow(() -> {
            jdbcTemplate.update(
                "INSERT INTO reports (reporter_user_id, target_type, target_id, reason) VALUES (?, ?, ?, ?)",
                user2Id,
                "project",
                targetProjectId,
                "Copyright infringement"
            );
        });

        // Step 3: User 1 attempts a duplicate report on the same project -> REJECTED by unique constraint
        DuplicateKeyException ex = assertThrows(
            DuplicateKeyException.class,
            () -> jdbcTemplate.update(
                "INSERT INTO reports (reporter_user_id, target_type, target_id, reason) VALUES (?, ?, ?, ?)",
                user1Id,
                "project",
                targetProjectId,
                "Duplicate report attempt"
            )
        );

        String rootCause = ex.getMostSpecificCause().getMessage();
        assertTrue(
            rootCause.contains("uq_reports_reporter_target"),
            "Expected uq_reports_reporter_target constraint violation, got: " + rootCause
        );
    }

    @Test
    @DisplayName("Check Constraints: Invalid target_type or status is rejected")
    void shouldEnforceCheckConstraints() {
        // Invalid target_type
        assertThrows(
            DataIntegrityViolationException.class,
            () -> jdbcTemplate.update(
                "INSERT INTO reports (reporter_user_id, target_type, target_id, reason) VALUES (?, ?, ?, ?)",
                user1Id,
                "issue", // Only 'comment' or 'project' permitted
                targetProjectId,
                "Invalid target type"
            )
        );

        // Invalid status
        assertThrows(
            DataIntegrityViolationException.class,
            () -> jdbcTemplate.update(
                "INSERT INTO reports (reporter_user_id, target_type, target_id, reason, status) VALUES (?, ?, ?, ?, ?)",
                user1Id,
                "project",
                targetProjectId,
                "Invalid status test",
                "pending" // Only 'open', 'resolved', 'dismissed' permitted
            )
        );
    }

    @Test
    @DisplayName("Contract: ReportRepository findByReporterUserIdAndTargetTypeAndTargetId and findByStatus")
    void shouldVerifyRepositoryContracts() {
        User user1 = userRepository.findById(user1Id).orElseThrow();

        Report report = new Report();
        report.setReporterUser(user1);
        report.setTargetType("project");
        report.setTargetId(targetProjectId);
        report.setReason("Harassment in project description");
        report.setStatus("open");

        reportRepository.save(report);

        // Pre-check verification
        Optional<Report> existingOpt = reportRepository
            .findByReporterUserIdAndTargetTypeAndTargetId(user1Id, "project", targetProjectId);
        assertTrue(existingOpt.isPresent());
        assertEquals("Harassment in project description", existingOpt.get().getReason());

        // Admin queue pagination verification
        Page<Report> openQueue = reportRepository.findByStatus("open", PageRequest.of(0, 10));
        assertNotNull(openQueue);
        assertTrue(openQueue.getTotalElements() >= 1);
    }
}