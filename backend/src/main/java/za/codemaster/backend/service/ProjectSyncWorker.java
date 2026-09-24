package za.codemaster.backend.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.codemaster.backend.client.github.GitHubClient;
import za.codemaster.backend.client.github.dto.ClosingPullRequestResult;
import za.codemaster.backend.client.github.dto.GitHubCommunityProfile;
import za.codemaster.backend.client.github.dto.GitHubFetchResult;
import za.codemaster.backend.client.github.dto.GitHubIssueMetadata;
import za.codemaster.backend.client.github.dto.GitHubProjectMetadata;
import za.codemaster.backend.domain.model.Claim;
import za.codemaster.backend.domain.model.ClaimStatus;
import za.codemaster.backend.domain.model.CollaborationRequestStatus;
import za.codemaster.backend.domain.model.CompletionSource;
import za.codemaster.backend.domain.model.PullRequestState;
import za.codemaster.backend.domain.model.SyncJob;
import za.codemaster.backend.repository.ClaimCollaborationRequestRepository;
import za.codemaster.backend.repository.ClaimRepository;
import za.codemaster.backend.repository.IssueSyncRepository;
import za.codemaster.backend.repository.ProjectSyncRepository;
import za.codemaster.backend.repository.SyncJobRepository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProjectSyncWorker {

    private static final List<ClaimStatus> VERIFIABLE_STATUSES =
            List.of(ClaimStatus.ACTIVE, ClaimStatus.CHANGES_REQUESTED);

    private final GitHubClient github;
    private final ProjectSyncRepository projects;
    private final IssueSyncRepository issues;
    private final SyncJobRepository jobs;
    private final ClaimRepository claims;
    private final ClaimCollaborationRequestRepository collaborationRequests;
    private final ClaimService claimService;

    public ProjectSyncWorker(
            GitHubClient github,
            ProjectSyncRepository projects,
            IssueSyncRepository issues,
            SyncJobRepository jobs,
            ClaimRepository claims,
            ClaimCollaborationRequestRepository collaborationRequests,
            ClaimService claimService) {
        this.github = github;
        this.projects = projects;
        this.issues = issues;
        this.jobs = jobs;
        this.claims = claims;
        this.collaborationRequests = collaborationRequests;
        this.claimService = claimService;
    }

    @Async
    @Transactional
    public void run(UUID jobId) {
        SyncJob job = jobs.findById(jobId).orElse(null);
        if (job == null) {
            return;
        }

        var project = projects.findById(job.getProjectId()).orElse(null);
        if (project == null) {
            job.failed("Project no longer exists.", null);
            jobs.save(job);
            return;
        }

        job.running();
        jobs.save(job);

        try {
            GitHubFetchResult<GitHubProjectMetadata> metadata = github.fetchProjectMetadata(
                    project.owner(), project.repo(), project.metadataEtag());
            if (metadata.isRateLimited()) {
                failRate(job, metadata.retryAfter());
                return;
            }

            GitHubFetchResult<GitHubCommunityProfile> communityProfile =
                    github.fetchCommunityProfile(project.owner(), project.repo());
            if (communityProfile.isRateLimited()) {
                failRate(job, communityProfile.retryAfter());
                return;
            }
            projects.updateCommunityProfile(project.id(), communityProfile.data());

            GitHubFetchResult<List<GitHubIssueMetadata>> fetched = github.fetchIssues(
                    project.owner(), project.repo(), project.issuesEtag());
            if (fetched.isRateLimited()) {
                failRate(job, fetched.retryAfter());
                return;
            }

            if (!metadata.isModified() && !fetched.isModified()) {
                job.completed(0, 0, 0);
                jobs.save(job);
                return;
            }

            if (metadata.isModified()) {
                projects.updateMetadata(project.id(), metadata.data(), metadata.etag());
            }

            int created = 0;
            int updated = 0;
            int verified = 0;

            if (fetched.isModified()) {
                List<IssueSyncRepository.ExistingIssue> previouslyOpen =
                        issues.findOpenByProjectId(project.id());
                Set<Integer> stillOpen = fetched.data().stream()
                        .map(GitHubIssueMetadata::issueNumber)
                        .collect(Collectors.toSet());

                for (GitHubIssueMetadata issue : fetched.data()) {
                    var existing = issues.findByProjectIdAndGithubIssueNumber(
                            project.id(), issue.issueNumber());
                    if (existing.isEmpty()) {
                        issues.insert(project.id(), issue);
                        created++;
                    } else if (existing.get().overriddenBy() == null) {
                        issues.update(existing.get().id(), issue);
                        updated++;
                    }
                }

                for (IssueSyncRepository.ExistingIssue issue : previouslyOpen) {
                    if (stillOpen.contains(issue.githubIssueNumber())) {
                        continue;
                    }

                    if (!issues.markClosed(issue.id())) {
                        continue;
                    }
                    updated++;
                    if (verifyClosingContribution(
                            project.owner(), project.repo(), issue)) {
                        verified++;
                    }
                }

                projects.updateIssuesEtag(project.id(), fetched.etag());
            }

            job.completed(created, updated, verified);
            jobs.save(job);
        } catch (Exception exception) {
            job.failed("GitHub sync failed: " + exception.getMessage(), null);
            jobs.save(job);
        }
    }

    private boolean verifyClosingContribution(
            String owner,
            String repo,
            IssueSyncRepository.ExistingIssue issue) {
        ClosingPullRequestResult result = github.fetchClosingPullRequest(
                owner, repo, issue.githubIssueNumber());
        if (!(result instanceof ClosingPullRequestResult.Found found) || !found.merged()) {
            return false;
        }

        Claim matchingClaim = claims.findByIssueIdAndStatusIn(issue.id(), VERIFIABLE_STATUSES)
                .stream()
                .filter(claim -> claimMatchesGithubAuthor(claim, found.authorId()))
                .findFirst()
                .orElse(null);
        if (matchingClaim == null) {
            return false;
        }

        matchingClaim.setStatus(ClaimStatus.COMPLETED);
        matchingClaim.setCompletionSource(CompletionSource.GITHUB_VERIFIED);
        matchingClaim.setCompletedAt(OffsetDateTime.now(ZoneOffset.UTC));
        matchingClaim.setPullRequestState(PullRequestState.MERGED);
        if (matchingClaim.getPullRequestUrl() == null
                || matchingClaim.getPullRequestUrl().isBlank()) {
            matchingClaim.setPullRequestUrl(
                    "https://github.com/" + owner + "/" + repo + "/pull/"
                            + found.pullRequestNumber());
        }
        claims.save(matchingClaim);
        claimService.notifyClaimCompleted(matchingClaim, matchingClaim.getIssue());
        return true;
    }

    /**
     * True if the merged PR's GitHub author is either the claim's owner, or
     * an accepted collaborator on the claim — either way, the whole claim
     * (owner and every accepted collaborator) is credited when it completes,
     * see {@link ClaimRepository#countCreditedContributions}.
     */
    private boolean claimMatchesGithubAuthor(Claim claim, long authorId) {
        if (claim.getUser() != null
                && claim.getUser().getGithubId() != null
                && claim.getUser().getGithubId() == authorId) {
            return true;
        }
        return collaborationRequests.findByClaimIdAndStatus(claim.getId(), CollaborationRequestStatus.ACCEPTED)
                .stream()
                .anyMatch(request -> request.getRequester() != null
                        && request.getRequester().getGithubId() != null
                        && request.getRequester().getGithubId() == authorId);
    }

    private void failRate(SyncJob job, Instant retry) {
        job.failed(
                "GitHub API rate limit exceeded.",
                retry == null ? null : OffsetDateTime.ofInstant(retry, ZoneOffset.UTC));
        jobs.save(job);
    }
}
