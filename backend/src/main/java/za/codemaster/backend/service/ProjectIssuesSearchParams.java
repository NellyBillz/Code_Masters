package za.codemaster.backend.service;

import za.codemaster.backend.dto.issue.Difficulty;
import za.codemaster.backend.dto.issue.IssueStatus;

/**
 * Bundles the query parameters accepted by
 * {@code GET /api/v1/projects/{projectId}/issues}. Same pattern as
 * {@link ProjectSearchParams} — not a named schema in the spec, exists to
 * keep the service method signature and tests readable. All fields nullable:
 * {@code null} means "not provided," not "match nothing."
 */
public record ProjectIssuesSearchParams(
        Integer page,
        Integer size,
        Difficulty difficulty,
        String label,
        IssueStatus status
) {
}
