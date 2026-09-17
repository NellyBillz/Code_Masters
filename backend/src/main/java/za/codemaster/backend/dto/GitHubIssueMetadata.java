package za.codemaster.backend.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Normalized GitHub issue metadata for GH-1 (issues).
 * <p>
 * Built by {@link za.codemaster.backend.client.github.GitHubClient#fetchOpenIssues(String, String)}
 * from the first page of {@code GET /repos/{owner}/{repo}/issues?state=open}.
 * Deliberately scoped to that first page only - pagination is out of scope
 * here and lands in the next ticket.
 * <p>
 * Same shape as {@link GitHubProjectMetadata}: a plain record, no JPA
 * annotations, no Jackson annotations, no dependency on Spring or on
 * {@link GitHubIssueResponse} (the raw GitHub-shaped layer). It doesn't know
 * it came from GitHub - a caller could build one by hand in a test with no
 * framework involved at all.
 * <p>
 * Not to be confused with the domain {@link Issue} record, which is shaped
 * around the {@code issues} DB table/API spec (project linkage, claim
 * status, difficulty scoring, etc). This type is the raw-but-normalized
 * GitHub view that a future mapping step would read from to help populate
 * that domain record - see GH-1.7 for how the two line up.
 *
 * @param issueNumber GitHub's per-repo issue number (the {@code #123} you
 *                     see in the UI), from {@code number}
 * @param title       issue title, verbatim
 * @param body        issue description, verbatim; GitHub returns
 *                     {@code null} for an issue opened with no description
 * @param labels      label names only (not GitHub's full label objects)
 * @param htmlUrl     the issue's browser URL, from {@code html_url}
 * @param createdAt   from {@code created_at}
 * @param updatedAt   from {@code updated_at}
 */
public record GitHubIssueMetadata(
        int issueNumber,
        String title,
        String body,
        List<String> labels,
        String htmlUrl,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
