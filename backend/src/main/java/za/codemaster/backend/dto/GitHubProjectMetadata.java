package za.codemaster.backend.dto;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Normalized GitHub repository metadata for GH-1.
 * <p>
 * Built by {@link GitHubClient#fetchProjectMetadata(String, String)} by
 * combining {@code GET /repos/{owner}/{repo}} (core fields) with
 * {@code GET /repos/{owner}/{repo}/languages} (per-language byte counts).
 * <p>
 * Deliberately a plain record: no JPA annotations, no Jackson annotations,
 * no dependency on Spring or on {@link GitHubRepositoryResponse} (the raw
 * GitHub-shaped layer). It doesn't know it came from GitHub or that it's
 * headed for a database - a caller could build one by hand in a test with
 * no framework involved at all.
 * <p>
 * <b>GH-1.7 contract - field name/type &rarr; likely {@code projects} column:</b>
 * <ul>
 *   <li>{@code name}                (String)              &rarr; {@code name}</li>
 *   <li>{@code description}         (String, nullable)     &rarr; {@code description}</li>
 *   <li>{@code primaryLanguage}     (String, nullable)     &rarr; {@code primary_language}</li>
 *   <li>{@code languageBreakdown}   (Map&lt;String,Long&gt;) &rarr; likely a separate
 *       {@code project_languages} table (language -&gt; bytes), not a single column</li>
 *   <li>{@code stars}               (int)                   &rarr; {@code stars}</li>
 *   <li>{@code forks}               (int)                   &rarr; {@code forks}</li>
 *   <li>{@code openIssueCount}      (int)                   &rarr; {@code open_issues}
 *       (note: this is GitHub's {@code open_issues_count}, which bundles open PRs
 *       in with open issues - flag this in the sync-up, it may need splitting)</li>
 *   <li>{@code license}             (String, nullable, SPDX id) &rarr; {@code license}</li>
 *   <li>{@code lastActivityAt}      (OffsetDateTime, from {@code pushed_at}) &rarr;
 *       {@code last_activity_at}</li>
 * </ul>
 */
public record GitHubProjectMetadata(
        String name,
        String description,
        String primaryLanguage,
        Map<String, Long> languageBreakdown,
        int stars,
        int forks,
        int openIssueCount,
        String license,
        OffsetDateTime lastActivityAt
) {
}
