package za.codemaster.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class ForeignKeyConstraintTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Manual test: inserting a submission with a non-existent user_id is rejected by the FK constraint")
    void shouldRejectSubmissionWithNonExistentUser() {
        UUID fakeUserId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        // Assert that executing the insert statement throws a DataIntegrityViolationException
        DataIntegrityViolationException exception = assertThrows(
            DataIntegrityViolationException.class,
            () -> {
                jdbcTemplate.update(
                    "INSERT INTO public.submissions (user_id, challenge_id, code_submitted) " +
                    "VALUES (?, 1, 'print(\"hello\")')",
                    fakeUserId
                );
            }
        );

        // Verify that the underlying root cause mentions the foreign key violation
        String rootMessage = exception.getMostSpecificCause().getMessage();
        assertTrue(
            rootMessage.contains("violates foreign key constraint") || 
            rootMessage.contains("submissions_user_id_fkey"),
            "Expected foreign key constraint violation, but got: " + rootMessage
        );
    }
}