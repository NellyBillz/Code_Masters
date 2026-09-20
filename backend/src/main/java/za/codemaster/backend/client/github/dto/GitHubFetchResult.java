package za.codemaster.backend.client.github.dto;

import java.time.Instant;

/**
 * Typed outcome of a GitHub fetch: fresh data (200), not modified (304), or
 * rate limited (403/429). This is the contract consumed by the sync layer.
 */
public sealed interface GitHubFetchResult<T> {

    boolean isModified();

    default boolean isRateLimited() {
        return this instanceof RateLimited<?>;
    }

    T data();

    String etag();

    /** Retry time supplied by GitHub for a rate-limited response; null otherwise. */
    default Instant retryAfter() {
        return null;
    }

    record Modified<T>(T data, String etag) implements GitHubFetchResult<T> {
        @Override public boolean isModified() { return true; }
    }

    record NotModified<T>() implements GitHubFetchResult<T> {
        @Override public boolean isModified() { return false; }
        @Override public T data() {
            throw new IllegalStateException("GitHub responded 304 Not Modified - there is no data.");
        }
        @Override public String etag() { return null; }
    }

    record RateLimited<T>(Instant retryAfter) implements GitHubFetchResult<T> {
        @Override public boolean isModified() { return false; }
        @Override public boolean isRateLimited() { return true; }
        @Override public T data() {
            throw new IllegalStateException("GitHub rate limited the request - there is no data.");
        }
        @Override public String etag() { return null; }
    }

    static <T> GitHubFetchResult<T> modified(T data, String etag) { return new Modified<>(data, etag); }
    static <T> GitHubFetchResult<T> notModified() { return new NotModified<>(); }
    static <T> GitHubFetchResult<T> rateLimited(Instant retryAfter) { return new RateLimited<>(retryAfter); }
}
