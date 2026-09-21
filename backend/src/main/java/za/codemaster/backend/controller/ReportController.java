package za.codemaster.backend.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.dto.report.CreateReportRequest;
import za.codemaster.backend.dto.report.ReportDto;
import za.codemaster.backend.security.AuthenticatedUser;
import za.codemaster.backend.service.ReportService;

/**
 * The abuse-reporting endpoints (API-03.9, the {@code Moderation} tag's
 * public-facing half): any authenticated user may flag a comment or a
 * project listing once. Kept separate from {@link CommentController}/
 * {@link ProjectController} — same reasoning as {@link MaintainerController}/
 * {@link AdminProjectController} — since this is one self-contained concern
 * spanning two parent resources, not an extension of either resource's own
 * endpoint set.
 */
@RestController
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * {@code POST /api/v1/comments/{commentId}/reports}: report a comment.
     */
    @PostMapping("/api/v1/comments/{commentId}/reports")
    public ResponseEntity<ReportDto> reportComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CreateReportRequest request,
            @AuthenticatedUser User currentUser) {
        ReportDto created = reportService.reportComment(commentId, request.reason(), currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * {@code POST /api/v1/projects/{projectId}/reports}: report a project listing.
     */
    @PostMapping("/api/v1/projects/{projectId}/reports")
    public ResponseEntity<ReportDto> reportProject(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateReportRequest request,
            @AuthenticatedUser User currentUser) {
        ReportDto created = reportService.reportProject(projectId, request.reason(), currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
