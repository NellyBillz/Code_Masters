package za.codemaster.backend.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.dto.report.PagedReports;
import za.codemaster.backend.dto.report.ReportDto;
import za.codemaster.backend.dto.report.ReportStatus;
import za.codemaster.backend.dto.report.ReviewReportRequest;
import za.codemaster.backend.security.AuthenticatedUser;
import za.codemaster.backend.service.ReportService;

/**
 * The site-admin abuse-report queue (API-03.9). Every endpoint here is
 * site-admin only, enforced by {@link za.codemaster.backend.security.SiteAdminGuard}
 * inside {@link ReportService} — same pattern as {@link AdminProjectController}.
 */
@RestController
public class AdminReportController {

    private final ReportService reportService;

    public AdminReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * {@code GET /api/v1/admin/reports}: the moderation queue, paginated,
     * defaulting to {@code open} reports.
     */
    @GetMapping("/api/v1/admin/reports")
    public PagedReports listReports(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) ReportStatus status,
            @AuthenticatedUser User currentUser) {
        return reportService.listReports(status, page, size, currentUser);
    }

    /**
     * {@code PATCH /api/v1/admin/reports/{reportId}}: resolve or dismiss a report.
     */
    @PatchMapping("/api/v1/admin/reports/{reportId}")
    public ReportDto reviewReport(
            @PathVariable Long reportId,
            @Valid @RequestBody ReviewReportRequest request,
            @AuthenticatedUser User currentUser) {
        return reportService.reviewReport(reportId, request, currentUser);
    }
}
