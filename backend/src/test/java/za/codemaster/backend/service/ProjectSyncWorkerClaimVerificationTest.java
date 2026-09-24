package za.codemaster.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.codemaster.backend.client.github.GitHubClient;
import za.codemaster.backend.client.github.dto.ClosingPullRequestResult;
import za.codemaster.backend.client.github.dto.GitHubCommunityProfile;
import za.codemaster.backend.client.github.dto.GitHubFetchResult;
import za.codemaster.backend.client.github.dto.GitHubIssueMetadata;
import za.codemaster.backend.client.github.dto.GitHubProjectMetadata;
import za.codemaster.backend.domain.model.Claim;
import za.codemaster.backend.domain.model.ClaimCollaborationRequest;
import za.codemaster.backend.domain.model.ClaimStatus;
import za.codemaster.backend.domain.model.CollaborationRequestStatus;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectSyncWorkerClaimVerificationTest {

    @Mock private GitHubClient github;
    @Mock private ProjectSyncRepository projects;
    @Mock private IssueSyncRepository issues;
    @Mock private SyncJobRepository jobs;
    @Mock private ClaimRepository claims;
    @Mock private ClaimCollaborationRequestRepository collaborationRequests;
    @Mock private ClaimService claimService;

    private ProjectSyncWorker worker;
    private UUID jobId;
    private SyncJob job;

    @BeforeEach
    void setUp() {
        worker = new ProjectSyncWorker(github, projects, issues, jobs, claims, collaborationRequests, claimService);
        jobId = UUID.randomUUID();
        job = new SyncJob(1L);
        when(jobs.findById(jobId)).thenReturn(Optional.of(job));
        when(projects.findById(1L)).thenReturn(Optional.of(
                new ProjectSyncRepository.SyncProject(1L, "owner", "repo", null, null)));
        when(github.fetchProjectMetadata("owner", "repo", null))
                .thenReturn(GitHubFetchResult.<GitHubProjectMetadata>notModified());
        when(github.fetchCommunityProfile("owner", "repo"))
                .thenReturn(GitHubFetchResult.modified(
                        new GitHubCommunityProfile(false, false), null));
        when(github.fetchIssues("owner", "repo", null))
                .thenReturn(GitHubFetchResult.modified(List.<GitHubIssueMetadata>of(), "issues-v2"));
    }

    @Test
    void verifiesMatchingEligibleClaimWhenOpenIssueCloses() {
        var issue = new IssueSyncRepository.ExistingIssue(42L, 17, null);
        when(issues.findOpenByProjectId(1L)).thenReturn(List.of(issue));
        when(issues.markClosed(42L)).thenReturn(true);
        when(github.fetchClosingPullRequest("owner", "repo", 17))
                .thenReturn(new ClosingPullRequestResult.Found(99, true, "contributor", 1234L));

        Claim claim = Claim.builder()
                .id(7L)
                .status(ClaimStatus.CHANGES_REQUESTED)
                .user(User.builder().githubId(1234L).username("contributor").build())
                .build();
        when(claims.findByIssueIdAndStatusIn(42L,
                List.of(ClaimStatus.ACTIVE, ClaimStatus.CHANGES_REQUESTED)))
                .thenReturn(List.of(claim));

        worker.run(jobId);

        assertEquals(ClaimStatus.COMPLETED, claim.getStatus());
        assertEquals(CompletionSource.GITHUB_VERIFIED, claim.getCompletionSource());
        assertEquals(PullRequestState.MERGED, claim.getPullRequestState());
        assertEquals("https://github.com/owner/repo/pull/99", claim.getPullRequestUrl());
        assertNotNull(claim.getCompletedAt());
        assertEquals(1, job.getContributionsVerifiedCount());
        verify(issues).markClosed(42L);
        verify(claims).save(claim);
        verify(claimService).notifyClaimCompleted(claim, claim.getIssue());
    }

    @Test
    void verifiesClaimWhenClosingAuthorMatchesAnAcceptedCollaboratorInstead() {
        var issue = new IssueSyncRepository.ExistingIssue(42L, 17, null);
        when(issues.findOpenByProjectId(1L)).thenReturn(List.of(issue));
        when(issues.markClosed(42L)).thenReturn(true);
        when(github.fetchClosingPullRequest("owner", "repo", 17))
                .thenReturn(new ClosingPullRequestResult.Found(99, true, "collaborator", 5678L));

        // The claim's owner's GitHub identity does NOT match the merged PR's
        // author — only an accepted collaborator's does.
        Claim claim = Claim.builder()
                .id(7L)
                .status(ClaimStatus.ACTIVE)
                .user(User.builder().githubId(1234L).username("owner").build())
                .build();
        when(claims.findByIssueIdAndStatusIn(42L,
                List.of(ClaimStatus.ACTIVE, ClaimStatus.CHANGES_REQUESTED)))
                .thenReturn(List.of(claim));

        ClaimCollaborationRequest accepted = new ClaimCollaborationRequest();
        accepted.setClaim(claim);
        accepted.setRequester(User.builder().githubId(5678L).username("collaborator").build());
        accepted.setStatus(CollaborationRequestStatus.ACCEPTED);
        when(collaborationRequests.findByClaimIdAndStatus(7L, CollaborationRequestStatus.ACCEPTED))
                .thenReturn(List.of(accepted));

        worker.run(jobId);

        // The claim itself is what gets marked completed — crediting both
        // the owner and the accepted collaborator falls out of
        // ClaimRepository.countCreditedContributions, not out of anything
        // this worker does directly.
        assertEquals(ClaimStatus.COMPLETED, claim.getStatus());
        assertEquals(CompletionSource.GITHUB_VERIFIED, claim.getCompletionSource());
        verify(claims).save(claim);
        verify(claimService).notifyClaimCompleted(claim, claim.getIssue());
    }

    @Test
    void leavesClaimsUntouchedWhenClosingAuthorDoesNotMatch() {
        var issue = new IssueSyncRepository.ExistingIssue(42L, 17, null);
        when(issues.findOpenByProjectId(1L)).thenReturn(List.of(issue));
        when(issues.markClosed(42L)).thenReturn(true);
        when(github.fetchClosingPullRequest("owner", "repo", 17))
                .thenReturn(new ClosingPullRequestResult.Found(99, true, "someone-else", 9999L));

        Claim claim = Claim.builder()
                .id(7L)
                .status(ClaimStatus.ACTIVE)
                .user(User.builder().githubId(1234L).username("contributor").build())
                .build();
        when(claims.findByIssueIdAndStatusIn(42L,
                List.of(ClaimStatus.ACTIVE, ClaimStatus.CHANGES_REQUESTED)))
                .thenReturn(List.of(claim));

        worker.run(jobId);

        assertEquals(ClaimStatus.ACTIVE, claim.getStatus());
        assertNull(claim.getCompletionSource());
        assertNull(claim.getCompletedAt());
        assertEquals(0, job.getContributionsVerifiedCount());
        verify(claims, never()).save(any());
        verify(claimService, never()).notifyClaimCompleted(any(), any());
    }

    @Test
    void doesNotReprocessAnIssueThatIsAlreadyClosedLocally() {
        when(issues.findOpenByProjectId(1L)).thenReturn(List.of());

        worker.run(jobId);

        assertEquals(0, job.getContributionsVerifiedCount());
        verify(github, never()).fetchClosingPullRequest(any(), any(), anyInt());
        verify(claims, never()).findByIssueIdAndStatusIn(any(), any());
        verify(claims, never()).save(any());
        verify(claimService, never()).notifyClaimCompleted(any(), any());
    }
}
