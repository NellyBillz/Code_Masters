package za.codemaster.backend.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.codemaster.backend.client.github.GitHubClient;
import za.codemaster.backend.dto.*;
import za.codemaster.backend.domain.model.SyncJob;
import za.codemaster.backend.repository.*;
import java.time.*;
import java.util.UUID;

@Service
public class ProjectSyncWorker {
    private final GitHubClient github; private final ProjectSyncRepository projects; private final IssueSyncRepository issues; private final SyncJobRepository jobs;
    public ProjectSyncWorker(GitHubClient github,ProjectSyncRepository projects,IssueSyncRepository issues,SyncJobRepository jobs){this.github=github;this.projects=projects;this.issues=issues;this.jobs=jobs;}

    @Async
    @Transactional
    public void run(UUID jobId){
        SyncJob job=jobs.findById(jobId).orElse(null); if(job==null)return;
        var p=projects.findById(job.getProjectId()).orElse(null); if(p==null){job.failed("Project no longer exists.",null);jobs.save(job);return;}
        job.running(); jobs.save(job);
        try {
            GitHubFetchResult<GitHubProjectMetadata> meta=github.fetchProjectMetadata(p.owner(),p.repo(),p.metadataEtag());
            if(meta.isRateLimited()){failRate(job,meta.retryAfter());return;}
            GitHubFetchResult<java.util.List<GitHubIssueMetadata>> fetched=github.fetchIssues(p.owner(),p.repo(),p.issuesEtag());
            if(fetched.isRateLimited()){failRate(job,fetched.retryAfter());return;}

            if(!meta.isModified() && !fetched.isModified()){job.completed(0,0);jobs.save(job);return;}
            if(meta.isModified()) projects.updateMetadata(p.id(),meta.data(),meta.etag());
            int created=0,updated=0;
            if(fetched.isModified()){
                for(var i:fetched.data()){
                    var existing=issues.findByProjectIdAndGithubIssueNumber(p.id(),i.issueNumber());
                    if(existing.isEmpty()){issues.insert(p.id(),i);created++;}
                    else if(existing.get().overriddenBy()==null){issues.update(existing.get().id(),i);updated++;}
                }
                projects.updateIssuesEtag(p.id(),fetched.etag());
            }
            job.completed(created,updated);jobs.save(job);
        } catch(Exception e){
            // No GitHub-derived rows are cleared on failure. The transaction only writes
            // after both fetches have succeeded, so typed rate-limit failures preserve data.
            job.failed("GitHub sync failed: "+e.getMessage(),null);jobs.save(job);
        }
    }
    private void failRate(SyncJob job,Instant retry){job.failed("GitHub API rate limit exceeded.",retry==null?null:OffsetDateTime.ofInstant(retry,ZoneOffset.UTC));jobs.save(job);}
}
