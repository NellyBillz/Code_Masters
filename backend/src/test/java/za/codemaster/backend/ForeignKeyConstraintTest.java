package za.codemaster.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = BackendApplication.class)
public class ForeignKeyConstraintTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Manual test: inserting a sessions row with a user_id that doesn't exist in users is rejected by the FK constraint")
    void shouldRejectSessionWithNonExistentUser() {
        // Non-existent user_id for BIGINT/BIGSERIAL users.id
        long fakeUserId = 999_999L;
        Timestamp expiresAt = Timestamp.from(Instant.now().plus(1, ChronoUnit.DAYS));

        // Assert that executing the insert statement throws DataIntegrityViolationException
        DataIntegrityViolationException exception = assertThrows(
            DataIntegrityViolationException.class,
            () -> {
                jdbcTemplate.update(
                    "INSERT INTO sessions (csrf_token, expires_at, user_id) VALUES (?, ?, ?)",
                    "test_csrf_token_xyz",
                    expiresAt,
                    fakeUserId
                );
            }
        );

        // Verify that the underlying root cause mentions the foreign key violation
        String rootMessage = exception.getMostSpecificCause().getMessage();
        assertTrue(
            rootMessage.contains("violates foreign key constraint") || 
            rootMessage.contains("fk_sessions_users"),
            "Expected foreign key constraint violation, but got: " + rootMessage
        );
    }
}