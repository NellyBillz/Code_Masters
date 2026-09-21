package za.codemaster.backend.dto.user;

import za.codemaster.backend.dto.common.PageMeta;

import java.util.List;

/**
 * Response shape for {@code GET /users/{username}/contributions} (API-03.6).
 * <p>
 * Matches the {@code PagedContributions} schema in codemasters-api-spec.yaml v3.
 * Same {@code items}/{@code meta} pattern as every other paged list in this API.
 */
public record PagedContributions(
        List<Contribution> items,
        PageMeta meta
) {
}
