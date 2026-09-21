package za.codemaster.backend.client.github.dto;

/**
 * Exhaustive result consumed by claim verification: either the PR whose merge
 * closed the issue, or an explicit indication that the issue has no such PR.
 */
public sealed interface ClosingPullRequestResult {

    record Found(int pullRequestNumber, boolean merged, String authorLogin, long authorId)
            implements ClosingPullRequestResult {
    }

    record NotFound() implements ClosingPullRequestResult {
    }
}
