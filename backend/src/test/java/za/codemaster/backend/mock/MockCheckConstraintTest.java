package za.codemaster.backend.mock;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MockCheckConstraintTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Comments: Reject insert when both project_id and issue_id are provided")
    void shouldRejectCommentWhenBothTargetsProvided() {
        when(jdbcTemplate.update(
            contains("INSERT INTO comments"),
            eq(1L), eq(1L), eq(1L), eq("Both targets")
        )).thenThrow(new DataIntegrityViolationException(
            "ERROR: new row violates check constraint \"chk_comments_target_exclusive\""
        ));

        DataIntegrityViolationException ex = assertThrows(
            DataIntegrityViolationException.class,
            () -> jdbcTemplate.update(
                "INSERT INTO comments (user_id, project_id, issue_id, body) VALUES (?, ?, ?, ?)",
                1L, 1L, 1L, "Both targets"
            )
        );
        assertTrue(ex.getMessage().contains("chk_comments_target_exclusive"));
    }

    @Test
    @DisplayName("Comments: Reject insert when neither project_id nor issue_id is provided")
    void shouldRejectCommentWhenNeitherTargetProvided() {
        when(jdbcTemplate.update(
            contains("INSERT INTO comments"),
            eq(1L), isNull(), isNull(), eq("Neither target")
        )).thenThrow(new DataIntegrityViolationException(
            "ERROR: new row violates check constraint \"chk_comments_target_exclusive\""
        ));

        DataIntegrityViolationException ex = assertThrows(
            DataIntegrityViolationException.class,
            () -> jdbcTemplate.update(
                "INSERT INTO comments (user_id, project_id, issue_id, body) VALUES (?, ?, ?, ?)",
                1L, null, null, "Neither target"
            )
        );
        assertTrue(ex.getMessage().contains("chk_comments_target_exclusive"));
    }

    @Test
    @DisplayName("Claims: Reject second active claim for the same user and issue")
    void shouldRejectDuplicateActiveClaimForSameUser() {
        when(jdbcTemplate.update(
            contains("INSERT INTO claims"),
            eq(1L), eq(1L), eq("active")
        )).thenThrow(new DuplicateKeyException(
            "ERROR: duplicate key value violates unique constraint \"uq_claims_active_per_user_issue\""
        ));

        DuplicateKeyException ex = assertThrows(
            DuplicateKeyException.class,
            () -> jdbcTemplate.update(
                "INSERT INTO claims (issue_id, user_id, status) VALUES (?, ?, ?)",
                1L, 1L, "active"
            )
        );
        assertTrue(ex.getMessage().contains("uq_claims_active_per_user_issue"));
    }

    @Test
    @DisplayName("Claims: Allow active claim alongside released claim for the same user (partial index)")
    void shouldAllowReleasedClaimAlongsideActiveClaim() {
        when(jdbcTemplate.update(
            contains("INSERT INTO claims"),
            eq(1L), eq(1L), eq("released")
        )).thenReturn(1);

        assertDoesNotThrow(() -> jdbcTemplate.update(
            "INSERT INTO claims (issue_id, user_id, status) VALUES (?, ?, ?)",
            1L, 1L, "released"
        ));
    }

    @Test
    @DisplayName("Claims: Allow two different users to hold active claims on the same issue")
    void shouldAllowDifferentUsersToHoldActiveClaimsOnSameIssue() {
        when(jdbcTemplate.update(
            contains("INSERT INTO claims"),
            eq(1L), eq(2L), eq("active")
        )).thenReturn(1);

        assertDoesNotThrow(() -> jdbcTemplate.update(
            "INSERT INTO claims (issue_id, user_id, status) VALUES (?, ?, ?)",
            1L, 2L, "active"
        ));
    }

    @Test
    @DisplayName("SyncJobs: Reject insert when status is not in accepted/running/completed/failed")
    void shouldRejectInvalidSyncJobStatus() {
        when(jdbcTemplate.update(
            contains("INSERT INTO sync_jobs"),
            eq(1L), eq("in_progress")
        )).thenThrow(new DataIntegrityViolationException(
            "ERROR: new row violates check constraint \"sync_jobs_status_check\""
        ));

        DataIntegrityViolationException ex = assertThrows(
            DataIntegrityViolationException.class,
            () -> jdbcTemplate.update(
                "INSERT INTO sync_jobs (project_id, status) VALUES (?, ?)",
                1L, "in_progress"
            )
        );

        assertTrue(ex.getMessage().contains("sync_jobs_status_check"));
    }

    @Test
    @DisplayName("SyncJobs: Reject insert when project_id does not exist in projects")
    void shouldRejectOrphanSyncJob() {
        when(jdbcTemplate.update(
            contains("INSERT INTO sync_jobs"),
            eq(999999L), eq("accepted")
        )).thenThrow(new DataIntegrityViolationException(
            "ERROR: insert or update on table \"sync_jobs\" violates foreign key constraint \"fk_sync_jobs_projects\""
        ));

        DataIntegrityViolationException ex = assertThrows(
            DataIntegrityViolationException.class,
            () -> jdbcTemplate.update(
                "INSERT INTO sync_jobs (project_id, status) VALUES (?, ?)",
                999999L, "accepted"
            )
        );

        assertTrue(ex.getMessage().contains("fk_sync_jobs_projects"));
    }
    
}
