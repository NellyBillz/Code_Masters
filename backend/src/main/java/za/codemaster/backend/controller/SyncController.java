package za.codemaster.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import za.codemaster.backend.domain.model.SyncJob;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.security.AuthenticatedUser;
import za.codemaster.backend.service.ProjectSyncService;

import java.util.UUID;

@RestController
public class SyncController {
    private final ProjectSyncService sync;

    public SyncController(ProjectSyncService sync) {
        this.sync = sync;
    }

    @PostMapping("/api/v1/projects/{projectId}/issues/sync")
    public ResponseEntity<SyncJob> sync(@PathVariable long projectId, @AuthenticatedUser User currentUser) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(sync.accept(projectId, currentUser.getId()));
    }

    @GetMapping("/api/v1/sync-jobs/{jobId}")
    public SyncJob get(@PathVariable UUID jobId) {
        return sync.get(jobId);
    }
}
