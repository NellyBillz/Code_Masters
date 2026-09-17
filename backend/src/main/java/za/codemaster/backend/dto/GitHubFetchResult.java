package za.codemaster.backend.dto;

/**
 * Result of a conditional GitHub fetch (an {@code ETag} sent back as
 * {@code If-None-Match}): either GitHub had fresh data ({@code 200}) or
 * confirmed nothing changed ({@code 304 Not Modified}).
 * <p>
 * Shared by {@link za.codemaster.backend.client.github.GitHubClient#fetchProjectMetadata(String, String, String)}
 * and {@link za.codemaster.backend.client.github.GitHubClient#fetchIssues(String, String, String)}
 * so both methods report "nothing changed" the same, typed way instead of
 * throwing or handing back a re-parsed, possibly-empty body.
 * <p>
 * Deliberately a plain sealed interface: no JPA annotations, no Jackson
 * annotations, no dependency on Spring. Callers do not need to pattern-match
 * on the two record cases - {@link #isModified()}, {@link #data()} and
 * {@link #etag()} are enough to handle either outcome:
 * <pre>{@code
 * GitHubFetchResult<GitHubProjectMetadata> result =
 *         client.fetchProjectMetadata(owner, repo, storedEtag);
 *
 * if (result.isModified()) {
 *     save(result.data());
 *     storedEtag = result.etag();
 * }
 * // else: nothing changed - keep using storedEtag as-is.
 * }</pre>
 *
 * @param <T> the fetched data's type when modified - {@link GitHubProjectMetadata}
 *            or {@code List<GitHubIssueMetadata>}
 */
public sealed interface GitHubFetchResult<T> {

    /** True when GitHub returned fresh data (a 200); false when it returned 304 Not Modified. */
    boolean isModified();

    /**
     * The fetched data. Only meaningful when {@link #isModified()} is true -
     * throws {@link IllegalStateException} otherwise, since GitHub sends no
     * body on a 304. Check {@link #isModified()} first.
     */
    T data();

    /**
     * The ETag to store and send back as {@code sinceEtag} on the next call.
     * Present only when {@link #isModified()} is true. When GitHub returns
     * 304, this is {@code null} - the caller should keep using whichever
     * ETag it already had, since by definition nothing changed.
     */
    String etag();

    /** GitHub had fresh data: a 200 response, successfully parsed. */
    record Modified<T>(T data, String etag) implements GitHubFetchResult<T> {
        @Override
        public boolean isModified() {
            return true;
        }
    }

    /** GitHub confirmed nothing changed since the ETag sent as If-None-Match: a 304, no body. */
    record NotModified<T>() implements GitHubFetchResult<T> {
        @Override
        public boolean isModified() {
            return false;
        }

        @Override
        public T data() {
            throw new IllegalStateException(
                    "GitHub responded 304 Not Modified - there is no data. Check isModified() first.");
        }

        @Override
        public String etag() {
            return null;
        }
    }

    static <T> GitHubFetchResult<T> modified(T data, String etag) {
        return new Modified<>(data, etag);
    }

    static <T> GitHubFetchResult<T> notModified() {
        return new NotModified<>();
    }
}