package za.codemaster.backend.domain.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "sync_jobs")
public class SyncJob {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name="project_id", nullable=false) private Long projectId;
    @Column(name = "status", nullable=false) private String status;
    @Column(name="started_at") private OffsetDateTime startedAt;
    @Column(name="completed_at") private OffsetDateTime completedAt;
    @Column(name="error_message") private String errorMessage;
    @Column(name="retry_after") private OffsetDateTime retryAfter;
    @Column(name="issues_created_count") private Integer issuesCreatedCount;
    @Column(name="issues_updated_count") private Integer issuesUpdatedCount;
    @Column(name = "contributions_verified_count")private Integer contributionsVerifiedCount;
    @Column(name="created_at", insertable=false, updatable=false) private OffsetDateTime createdAt;

    public SyncJob() {}
    public SyncJob(Long projectId) { this.projectId=projectId; this.status="accepted"; this.issuesCreatedCount=0; this.issuesUpdatedCount=0; }
    public void running(){ status="running"; startedAt=OffsetDateTime.now(); }
    public void completed(int created,int updated){ status="completed"; completedAt=OffsetDateTime.now(); issuesCreatedCount=created; issuesUpdatedCount=updated; errorMessage=null; retryAfter=null; }
    public void failed(String message, OffsetDateTime retry){ status="failed"; completedAt=OffsetDateTime.now(); errorMessage=message; retryAfter=retry; }
    public UUID getId(){return id;} public Long getProjectId(){return projectId;} public String getStatus(){return status;}
    public OffsetDateTime getStartedAt(){return startedAt;} public OffsetDateTime getCompletedAt(){return completedAt;}
    public String getErrorMessage(){return errorMessage;} public OffsetDateTime getRetryAfter(){return retryAfter;}
    public Integer getIssuesCreatedCount(){return issuesCreatedCount;} public Integer getIssuesUpdatedCount(){return issuesUpdatedCount;}
    public OffsetDateTime getCreatedAt(){return createdAt;}
    public Integer getContributionsVerifiedCount(){return contributionsVerifiedCount;}
    public void setProject(Project project) {
        this.projectId = project != null ? project.getId() : null;
    }
    public void setStatus(String status){this.status = status;}
    public void setContributionsVerifiedCount(Integer contibutions){this.contributionsVerifiedCount = contibutions;}


}
