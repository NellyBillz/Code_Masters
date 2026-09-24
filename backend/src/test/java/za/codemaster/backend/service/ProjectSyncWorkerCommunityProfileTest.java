package za.codemaster.backend.service;

import org.junit.jupiter.api.Test;
import za.codemaster.backend.client.github.GitHubClient;
import za.codemaster.backend.client.github.dto.GitHubCommunityProfile;
import za.codemaster.backend.client.github.dto.GitHubFetchResult;
import za.codemaster.backend.client.github.dto.GitHubIssueMetadata;
import za.codemaster.backend.client.github.dto.GitHubProjectMetadata;
import za.codemaster.backend.domain.model.SyncJob;
import za.codemaster.backend.repository.ClaimCollaborationRequestRepository;
import za.codemaster.backend.repository.ClaimRepository;
import za.codemaster.backend.repository.IssueSyncRepository;
import za.codemaster.backend.repository.ProjectSyncRepository;
import za.codemaster.backend.repository.SyncJobRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectSyncWorkerCommunityProfileTest {

    @Test
    void updatesCommunityFlagsEvenWhenConditionalMetadataIsUnchanged() {
        GitHubClient github = mock(GitHubClient.class);
        ProjectSyncRepository projects = mock(ProjectSyncRepository.class);
        IssueSyncRepository issues = mock(IssueSyncRepository.class);
        SyncJobRepository jobs = mock(SyncJobRepository.class);
        ClaimRepository claims = mock(ClaimRepository.class);
        ClaimCollaborationRequestRepository collaborationRequests = mock(ClaimCollaborationRequestRepository.class);
        ClaimService claimService = mock(ClaimService.class);
        UUID jobId = UUID.randomUUID();
        SyncJob job = new SyncJob(1L);
        GitHubCommunityProfile profile = new GitHubCommunityProfile(true, true);

        when(jobs.findById(jobId)).thenReturn(Optional.of(job));
        when(projects.findById(1L)).thenReturn(Optional.of(
                new ProjectSyncRepository.SyncProject(
                        1L, "owner", "repo", "metadata-v1", "issues-v1")));
        when(github.fetchProjectMetadata("owner", "repo", "metadata-v1"))
                .thenReturn(GitHubFetchResult.<GitHubProjectMetadata>notModified());
        when(github.fetchCommunityProfile("owner", "repo"))
                .thenReturn(GitHubFetchResult.modified(profile, null));
        when(github.fetchIssues("owner", "repo", "issues-v1"))
                .thenReturn(GitHubFetchResult.<List<GitHubIssueMetadata>>notModified());

        new ProjectSyncWorker(github, projects, issues, jobs, claims, collaborationRequests, claimService).run(jobId);

        verify(projects).updateCommunityProfile(1L, profile);
        assertEquals("completed", job.getStatus());
    }
}
