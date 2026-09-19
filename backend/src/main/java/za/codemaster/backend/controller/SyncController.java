package za.codemaster.backend.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import za.codemaster.backend.domain.model.SyncJob;
import za.codemaster.backend.exception.ApiException;
import za.codemaster.backend.service.ProjectSyncService;
import za.codemaster.backend.service.SessionUserResolver;
import java.util.*;

@RestController
public class SyncController {
    private final ProjectSyncService sync; private final SessionUserResolver sessions;
    public SyncController(ProjectSyncService sync,SessionUserResolver sessions){this.sync=sync;this.sessions=sessions;}
    @PostMapping("/api/v1/projects/{projectId}/issues/sync")
    public ResponseEntity<SyncJob> sync(@PathVariable long projectId,HttpServletRequest request){
        var user=sessions.resolve(cookie(request,"CODEMASTERS_SESSION")).orElseThrow(()->new ApiException("UNAUTHORIZED","Authentication is required.",HttpStatus.UNAUTHORIZED));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(sync.accept(projectId,user.getId()));
    }
    @GetMapping("/api/v1/sync-jobs/{jobId}") public SyncJob get(@PathVariable UUID jobId){return sync.get(jobId);}
    private static String cookie(HttpServletRequest r,String name){Cookie[] cs=r.getCookies();if(cs==null)return null;return Arrays.stream(cs).filter(c->name.equals(c.getName())).map(Cookie::getValue).findFirst().orElse(null);}
}
