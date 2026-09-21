package za.codemaster.backend.service;

import org.junit.jupiter.api.Test;
import za.codemaster.backend.exception.ApiException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies API-03.10's core limiter in isolation, no Spring context or database
 * needed — {@link RateLimitService} is pure in-memory logic. Covers the
 * ticket's own acceptance criteria verbatim: requests under the limit are
 * unaffected, requests past it -> {@code RATE_LIMITED} (429).
 */
class RateLimitServiceTest {

    @Test
    void requestsUnderTheLimitAreUnaffected() {
        RateLimitService service = new RateLimitService(3, 3, 3);

        assertDoesNotThrow(() -> {
            service.checkCommentLimit(1L);
            service.checkCommentLimit(1L);
            service.checkCommentLimit(1L);
        });
    }

    @Test
    void theRequestThatExceedsTheLimitIsRejectedWith429RateLimited() {
        RateLimitService service = new RateLimitService(3, 3, 3);
        service.checkCommentLimit(1L);
        service.checkCommentLimit(1L);
        service.checkCommentLimit(1L);

        ApiException ex = assertThrows(ApiException.class, () -> service.checkCommentLimit(1L));

        assertEquals("RATE_LIMITED", ex.getCode());
        assertEquals(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());
    }

    @Test
    void claimLimitIsEnforcedIndependentlyFromCommentLimit() {
        RateLimitService service = new RateLimitService(3, 1, 3);

        service.checkClaimLimit(1L);
        ApiException ex = assertThrows(ApiException.class, () -> service.checkClaimLimit(1L));
        assertEquals("RATE_LIMITED", ex.getCode());

        // The same user's comment bucket is untouched by exhausting the claim bucket.
        assertDoesNotThrow(() -> {
            service.checkCommentLimit(1L);
            service.checkCommentLimit(1L);
            service.checkCommentLimit(1L);
        });
    }

    @Test
    void reportLimitIsEnforcedIndependentlyFromOtherActions() {
        RateLimitService service = new RateLimitService(3, 3, 1);

        service.checkReportLimit(1L);
        ApiException ex = assertThrows(ApiException.class, () -> service.checkReportLimit(1L));
        assertEquals("RATE_LIMITED", ex.getCode());
    }

    @Test
    void differentUsersHaveIndependentBuckets() {
        RateLimitService service = new RateLimitService(1, 1, 1);

        service.checkCommentLimit(1L);
        ApiException ex = assertThrows(ApiException.class, () -> service.checkCommentLimit(1L));
        assertEquals("RATE_LIMITED", ex.getCode());

        // A different user is completely unaffected by user 1 exhausting their bucket.
        assertDoesNotThrow(() -> service.checkCommentLimit(2L));
    }
}
