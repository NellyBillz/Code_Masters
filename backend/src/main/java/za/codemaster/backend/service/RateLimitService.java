package za.codemaster.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import za.codemaster.backend.exception.ApiException;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-user, per-action-type rate limiting on public writes (API-03.10, design
 * doc §16) — closes a gap that's existed in the API contract, unused, since
 * Round 2 (an {@code Error.code = RATE_LIMITED} response was documented but
 * nothing ever enforced it).
 * <p>
 * A plain in-memory token bucket per {@code (key, action)} pair — no new
 * infrastructure, per the ticket's own instruction that this is sufficient at
 * this scale. Each bucket holds up to {@code limit} tokens, continuously
 * refilling back to full over one hour; each write attempt spends one token.
 * Limits are configuration values (see {@code application.properties}), not
 * hardcoded, so the team can tune them post-launch without a code change. The
 * key is normally the caller's user id; {@link #checkOAuthLimit(String)} is
 * the one exception, keyed by client IP instead, since that flow runs before
 * there's an authenticated user to key a bucket by.
 * <p>
 * Deliberately keyed by action <em>type</em> (comment/claim/report), not by
 * endpoint: a comment on a project and a comment on an issue are the same
 * action for rate-limiting purposes, so a user can't dodge the limit by
 * alternating between the two creation endpoints.
 */
@Service
public class RateLimitService {

    private static final long HOUR_NANOS = Duration.ofHours(1).toNanos();

    private final int commentsPerHour;
    private final int claimsPerHour;
    private final int reportsPerHour;
    private final int collaborationRequestsPerHour;
    private final int projectSubmissionsPerHour;
    private final int oauthAttemptsPerHour;

    private final ConcurrentHashMap<Long, TokenBucket> commentBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, TokenBucket> claimBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, TokenBucket> reportBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, TokenBucket> collaborationRequestBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, TokenBucket> projectSubmissionBuckets = new ConcurrentHashMap<>();
    /** Keyed by client IP, not user id — the OAuth flow has no authenticated caller yet (design doc §16 follow-up, 2026-09-25). */
    private final ConcurrentHashMap<String, TokenBucket> oauthAttemptBuckets = new ConcurrentHashMap<>();

    public RateLimitService(
            @Value("${app.rate-limit.comments-per-hour:20}") int commentsPerHour,
            @Value("${app.rate-limit.claims-per-hour:10}") int claimsPerHour,
            @Value("${app.rate-limit.reports-per-hour:5}") int reportsPerHour,
            @Value("${app.rate-limit.collaboration-requests-per-hour:10}") int collaborationRequestsPerHour,
            @Value("${app.rate-limit.project-submissions-per-hour:5}") int projectSubmissionsPerHour,
            @Value("${app.rate-limit.oauth-attempts-per-hour:20}") int oauthAttemptsPerHour) {
        this.commentsPerHour = commentsPerHour;
        this.claimsPerHour = claimsPerHour;
        this.reportsPerHour = reportsPerHour;
        this.collaborationRequestsPerHour = collaborationRequestsPerHour;
        this.projectSubmissionsPerHour = projectSubmissionsPerHour;
        this.oauthAttemptsPerHour = oauthAttemptsPerHour;
    }

    /**
     * @throws ApiException with code {@code RATE_LIMITED} (429) if the caller has
     *                       created too many comments in the last hour
     */
    public void checkCommentLimit(Long userId) {
        enforce(commentBuckets, userId, commentsPerHour, "comment");
    }

    /**
     * @throws ApiException with code {@code RATE_LIMITED} (429) if the caller has
     *                       created too many claims in the last hour
     */
    public void checkClaimLimit(Long userId) {
        enforce(claimBuckets, userId, claimsPerHour, "claim");
    }

    /**
     * @throws ApiException with code {@code RATE_LIMITED} (429) if the caller has
     *                       filed too many reports in the last hour
     */
    public void checkReportLimit(Long userId) {
        enforce(reportBuckets, userId, reportsPerHour, "report");
    }

    /**
     * @throws ApiException with code {@code RATE_LIMITED} (429) if the caller has
     *                       sent too many collaboration requests in the last hour
     */
    public void checkCollaborationLimit(Long userId) {
        enforce(collaborationRequestBuckets, userId, collaborationRequestsPerHour, "collaboration request");
    }

    /**
     * @throws ApiException with code {@code RATE_LIMITED} (429) if the caller has
     *                       submitted too many projects in the last hour
     */
    public void checkProjectSubmissionLimit(Long userId) {
        enforce(projectSubmissionBuckets, userId, projectSubmissionsPerHour, "project submission");
    }

    /**
     * Covers both {@code /auth/github} (starts a server-side session per hit)
     * and {@code /auth/github/callback} — the one pair of public write-adjacent
     * endpoints with no authenticated caller to key a bucket by, so this is
     * keyed by client IP instead (security audit finding, 2026-09-24: neither
     * endpoint had any rate limit at all).
     *
     * @throws ApiException with code {@code RATE_LIMITED} (429) if this IP has
     *                       attempted too many logins in the last hour
     */
    public void checkOAuthLimit(String clientIp) {
        enforce(oauthAttemptBuckets, clientIp, oauthAttemptsPerHour, "login");
    }

    private <K> void enforce(ConcurrentHashMap<K, TokenBucket> buckets, K key, int limit, String actionName) {
        TokenBucket bucket = buckets.computeIfAbsent(key, id -> new TokenBucket(limit));
        if (!bucket.tryConsume()) {
            throw new ApiException(
                    "RATE_LIMITED",
                    "Too many " + actionName + " requests. Please slow down and try again later.",
                    HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    /**
     * A single user's token bucket for one action type. Not static-per-JVM
     * capacity — each instance is created with the limit active at the time,
     * which never changes at runtime, so this is safe.
     */
    private static final class TokenBucket {

        private final int capacity;
        private double tokens;
        private long lastRefillNanos;

        TokenBucket(int capacity) {
            this.capacity = capacity;
            this.tokens = capacity;
            this.lastRefillNanos = System.nanoTime();
        }

        synchronized boolean tryConsume() {
            refill();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.nanoTime();
            long elapsedNanos = now - lastRefillNanos;
            if (elapsedNanos <= 0) {
                return;
            }
            double refillAmount = (elapsedNanos / (double) HOUR_NANOS) * capacity;
            tokens = Math.min(capacity, tokens + refillAmount);
            lastRefillNanos = now;
        }
    }
}
