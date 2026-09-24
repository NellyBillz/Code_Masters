package za.codemaster.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import za.codemaster.backend.client.github.GitHubClient;
import za.codemaster.backend.domain.model.Claim;
import za.codemaster.backend.domain.model.ClaimStatus;
import za.codemaster.backend.domain.model.CompletionSource;
import za.codemaster.backend.domain.model.PullRequestState;
import za.codemaster.backend.domain.model.SyncJob;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.repository.ClaimCollaborationRequestRepository;
import za.codemaster.backend.repository.ClaimRepository;
import za.codemaster.backend.repository.IssueSyncRepository;
import za.codemaster.backend.repository.ProjectSyncRepository;
import za.codemaster.backend.repository.SyncJobRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Live worker acceptance test using hbldh/bleak#1280, closed by PR #1281. */
@EnabledIfEnvironmentVariable(named = "GITHUB_API_TOKEN", matches = ".+")
class ProjectSyncWorkerGitHubIntegrationTest {

    @Test
    void syncVerifiesClaimWhoseGithubIdentityAuthoredTheClosingPullRequest() {
        GitHubClient github = new GitHubClient(
                "https://api.github.com", System.getenv("GITHUB_API_TOKEN"));
        ProjectSyncRepository projects = mock(ProjectSyncRepository.class);
        IssueSyncRepository issues = mock(IssueSyncRepository.class);
        SyncJobRepository jobs = mock(SyncJobRepository.class);
        ClaimRepository claims = mock(ClaimRepository.class);
        ClaimCollaborationRequestRepository collaborationRequests = mock(ClaimCollaborationRequestRepository.class);

        UUID jobId = UUID.randomUUID();
        SyncJob job = new SyncJob(1L);
        when(jobs.findById(jobId)).thenReturn(Optional.of(job));
        when(projects.findById(1L)).thenReturn(Optional.of(
                new ProjectSyncRepository.SyncProject(1L, "hbldh", "bleak", null, null)));

        var localIssue = new IssueSyncRepository.ExistingIssue(42L, 1280, null);
        when(issues.findOpenByProjectId(1L)).thenReturn(List.of(localIssue));
        when(issues.findByProjectIdAndGithubIssueNumber(anyLong(), anyInt()))
                .thenReturn(Optional.empty());
        when(issues.markClosed(42L)).thenReturn(true);

        Claim claim = Claim.builder()
                .id(7L)
                .status(ClaimStatus.ACTIVE)
                .user(User.builder().githubId(963645L).username("dlech").build())
                .build();
        when(claims.findByIssueIdAndStatusIn(
                42L, List.of(ClaimStatus.ACTIVE, ClaimStatus.CHANGES_REQUESTED)))
                .thenReturn(List.of(claim));

        new ProjectSyncWorker(github, projects, issues, jobs, claims, collaborationRequests).run(jobId);

        assertEquals("completed", job.getStatus());
        assertEquals(1, job.getContributionsVerifiedCount());
        assertEquals(ClaimStatus.COMPLETED, claim.getStatus());
        assertEquals(CompletionSource.GITHUB_VERIFIED, claim.getCompletionSource());
        assertEquals(PullRequestState.MERGED, claim.getPullRequestState());
        assertEquals("https://github.com/hbldh/bleak/pull/1281", claim.getPullRequestUrl());
        assertNotNull(claim.getCompletedAt());
        verify(claims).save(claim);
    }
}
