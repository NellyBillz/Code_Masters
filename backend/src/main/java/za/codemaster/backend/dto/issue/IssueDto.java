package za.codemaster.backend.dto.issue;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * A contribution opportunity linked to a real GitHub issue.
 * <p>
 * Matches the {@code Issue} schema in codemasters-api-spec.yaml v2.1.
 * Named {@code IssueDto} (not {@code Issue}) to avoid colliding with the JPA
 * entity {@link za.codemaster.backend.domain.model.Issue} of the same spec
 * name — see {@code ProjectQueryService} for the entity-to-DTO mapping.
 * The four *Score fields are placeholders for Phase 2 scoring intelligence
 * (design doc §9); they're nullable {@link Double}s here on purpose, since
 * mock data (and real data, for a while) won't always have them computed.
 */
public record IssueDto(
        Long id,
        Long projectId,
        Integer githubIssueNumber,
        String title,
        String bodyExcerpt,
        String githubUrl,
        IssueStatus status,
        List<String> labels,
        Difficulty difficulty,
        boolean isBeginnerFriendly,
        boolean difficultyOverridden,
        Double maintainerResponseScore,
        Double projectHealthScore,
        Double freshnessScore,
        Double contributionScore,
        Integer claimCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
