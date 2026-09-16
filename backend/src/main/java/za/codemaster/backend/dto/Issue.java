package za.codemaster.backend.dto;


import java.time.OffsetDateTime;
import java.util.List;

/**
 * A contribution opportunity linked to a real GitHub issue.
 * <p>
 * Matches the {@code Issue} schema in codemasters-api-spec.yaml v2.1.
 * The four *Score fields are placeholders for Phase 2 scoring intelligence
 * (design doc §9); they're nullable {@link Double}s here on purpose, since
 * mock data (and real data, for a while) won't always have them computed.
 */
public record Issue(
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
