package za.codemaster.backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import za.codemaster.backend.domain.model.SyncJob;
import za.codemaster.backend.exception.ApiException;
import za.codemaster.backend.repository.ProjectSyncRepository;
import za.codemaster.backend.repository.SyncJobRepository;
import java.util.UUID;

@Service
public class ProjectSyncService {
    private final ProjectSyncRepository projects; private final SyncJobRepository jobs; private final ProjectSyncWorker worker;
    public ProjectSyncService(ProjectSyncRepository projects,SyncJobRepository jobs,ProjectSyncWorker worker){this.projects=projects;this.jobs=jobs;this.worker=worker;}
    public SyncJob accept(long projectId,long userId){
        projects.findById(projectId).orElseThrow(()->new ApiException("PROJECT_NOT_FOUND","The requested project does not exist.",HttpStatus.NOT_FOUND));
        if(!projects.isMaintainer(projectId,userId)) throw new ApiException("FORBIDDEN","Only a project maintainer can trigger a sync.",HttpStatus.FORBIDDEN);
        SyncJob job=jobs.save(new SyncJob(projectId));
        worker.run(job.getId());
        return job;
    }
    public SyncJob get(UUID id){return jobs.findById(id).orElseThrow(()->new ApiException("SYNC_JOB_NOT_FOUND","The requested sync job does not exist.",HttpStatus.NOT_FOUND));}
}
