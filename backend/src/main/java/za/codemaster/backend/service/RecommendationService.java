package za.codemaster.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.codemaster.backend.domain.model.Claim;
import za.codemaster.backend.domain.model.Issue;
import za.codemaster.backend.domain.model.Project;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.dto.recommendation.RecommendationProjectSummary;
import za.codemaster.backend.dto.recommendation.RecommendedIssue;
import za.codemaster.backend.repository.ClaimRepository;
import za.codemaster.backend.repository.IssueRepository;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Skill-Matching Recommendation Engine (wow-feature, 2026-09-24) —
 * {@code GET /users/me/recommended-issues}. Deterministic weighted scoring,
 * not ML: safer to build and demo reliably than a bolted-on model call, and
 * "explainable, not a black box" is itself a stronger technical/ethics story
 * (design doc's own reasoning — see {@link RecommendedIssue}'s Javadoc).
 * <p>
 * An issue is only ever recommended when it has at least one concrete,
 * shown reason — recency alone never qualifies an otherwise-irrelevant
 * issue, it only breaks ties among issues that already matched on
 * something real. A caller with no skills recorded and prior contributions
 * (so the beginner boost doesn't apply either) legitimately gets an empty
 * list rather than invented picks.
 */
@Service
public class RecommendationService {

    /** Primary language match — the single strongest signal a contributor can act on immediately. */
    private static final int PRIMARY_LANGUAGE_SCORE = 50;
    /** A secondary language on the project (not primary) still matters, just less. */
    private static final int SECONDARY_LANGUAGE_SCORE = 20;
    /** Per matching tag — capped implicitly by how many tags a project realistically has. */
    private static final int TAG_MATCH_SCORE = 10;
    /** Beginner-friendly issue, for a caller with zero prior verified contributions. */
    private static final int BEGINNER_BOOST_SCORE = 30;
    /** Recency tiebreaker ceiling — never enough on its own to qualify an issue, see class Javadoc. */
    private static final int RECENCY_MAX_SCORE = 10;
    private static final int RECENCY_WINDOW_DAYS = 90;
    private static final int DEFAULT_LIMIT = 10;

    private final IssueRepository issueRepository;
    private final ClaimRepository claimRepository;
    private final ProjectQueryService projectQueryService;

    public RecommendationService(IssueRepository issueRepository,
                                  ClaimRepository claimRepository,
                                  ProjectQueryService projectQueryService) {
        this.issueRepository = issueRepository;
        this.claimRepository = claimRepository;
        this.projectQueryService = projectQueryService;
    }

    /**
     * @param caller the authenticated caller, injected via {@code @AuthenticatedUser}
     * @return up to 10 open issues ranked by fit against the caller's skills,
     *         each with the plain-language reasons it matched; never includes
     *         an issue the caller already holds/held any claim on
     */
    @Transactional(readOnly = true)
    public List<RecommendedIssue> getRecommendedIssues(User caller) {
        Set<String> normalizedSkills = caller.getSkills() == null
                ? Set.of()
                : java.util.Arrays.stream(caller.getSkills())
                        .filter(java.util.Objects::nonNull)
                        .map(skill -> skill.toLowerCase(Locale.ROOT))
                        .collect(Collectors.toSet());

        boolean hasNoPriorContributions = claimRepository.countCreditedContributions(caller.getId()) == 0;

        Set<Long> alreadyClaimedIssueIds = claimRepository.findByUserId(caller.getId()).stream()
                .map(Claim::getIssue)
                .map(Issue::getId)
                .collect(Collectors.toSet());

        List<Scored> scored = new ArrayList<>();
        for (Issue issue : issueRepository.findOpenIssuesOnPublishedAcceptingProjects()) {
            if (alreadyClaimedIssueIds.contains(issue.getId())) {
                continue;
            }
            Scored candidate = score(issue, normalizedSkills, hasNoPriorContributions);
            if (candidate != null) {
                scored.add(candidate);
            }
        }

        return scored.stream()
                .sorted(Comparator.comparingInt(Scored::score).reversed())
                .limit(DEFAULT_LIMIT)
                .map(this::toRecommendedIssue)
                .toList();
    }

    /** @return {@code null} if this issue has no concrete match reason at all — not a candidate. */
    private Scored score(Issue issue, Set<String> normalizedSkills, boolean hasNoPriorContributions) {
        Project project = issue.getProject();
        List<String> reasons = new ArrayList<>();
        int score = 0;

        String primaryLanguage = project.getPrimaryLanguage();
        if (primaryLanguage != null && normalizedSkills.contains(primaryLanguage.toLowerCase(Locale.ROOT))) {
            score += PRIMARY_LANGUAGE_SCORE;
            reasons.add("Matches your " + primaryLanguage + " skill");
        } else if (project.getLanguages() != null) {
            for (String language : project.getLanguages()) {
                if (language != null && normalizedSkills.contains(language.toLowerCase(Locale.ROOT))) {
                    score += SECONDARY_LANGUAGE_SCORE;
                    reasons.add("Uses " + language + ", one of your skills");
                    break;
                }
            }
        }

        if (project.getTags() != null) {
            for (String tag : project.getTags()) {
                if (tag != null && normalizedSkills.contains(tag.toLowerCase(Locale.ROOT))) {
                    score += TAG_MATCH_SCORE;
                    reasons.add("Tagged \"" + tag + "\", matching your skills");
                }
            }
        }

        if (hasNoPriorContributions && Boolean.TRUE.equals(issue.getIsBeginnerFriendly())) {
            score += BEGINNER_BOOST_SCORE;
            reasons.add("Beginner-friendly — good for your first contribution");
        }

        if (reasons.isEmpty()) {
            return null;
        }

        score += recencyBonus(issue);
        return new Scored(issue, score, reasons);
    }

    /** A small tiebreaker only — never enough alone to make an issue a candidate (see {@link #score}). */
    private int recencyBonus(Issue issue) {
        OffsetDateTime reference = issue.getUpdatedAt() != null ? issue.getUpdatedAt() : issue.getCreatedAt();
        if (reference == null) {
            return 0;
        }
        long daysOld = Duration.between(reference, OffsetDateTime.now()).toDays();
        if (daysOld < 0 || daysOld >= RECENCY_WINDOW_DAYS) {
            return 0;
        }
        return (int) Math.round(RECENCY_MAX_SCORE * (1 - (daysOld / (double) RECENCY_WINDOW_DAYS)));
    }

    private RecommendedIssue toRecommendedIssue(Scored scored) {
        Project project = scored.issue().getProject();
        RecommendationProjectSummary projectSummary = new RecommendationProjectSummary(
                project.getId(), project.getName(), project.getSlug(), project.getPrimaryLanguage());
        return new RecommendedIssue(projectQueryService.toDto(scored.issue()), projectSummary, scored.reasons());
    }

    private record Scored(Issue issue, int score, List<String> reasons) {
    }
}
