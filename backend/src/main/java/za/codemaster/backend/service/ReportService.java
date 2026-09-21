package za.codemaster.backend.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.codemaster.backend.domain.model.ClaimStatus;
import za.codemaster.backend.domain.model.Report;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.dto.common.PageMeta;
import za.codemaster.backend.dto.report.PagedReports;
import za.codemaster.backend.dto.report.ReportDto;
import za.codemaster.backend.dto.report.ReportStatus;
import za.codemaster.backend.dto.report.ReportTargetType;
import za.codemaster.backend.dto.report.ReviewReportRequest;
import za.codemaster.backend.dto.user.PublicUserProfile;
import za.codemaster.backend.exception.ApiException;
import za.codemaster.backend.repository.ClaimRepository;
import za.codemaster.backend.repository.CommentRepository;
import za.codemaster.backend.repository.ProjectRepository;
import za.codemaster.backend.repository.ReportRepository;
import za.codemaster.backend.security.SiteAdminGuard;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * The platform's answer to "what happens when someone posts something harmful"
 * (API-03.9, design doc §6.3/§15.4): any authenticated user can flag a comment
 * or project once, and a site admin triages the resulting queue. Filing a
 * report never itself hides or deletes anything — see {@link #reviewReport}.
 */
@Service
public class ReportService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final ReportRepository reportRepository;
    private final CommentRepository commentRepository;
    private final ProjectRepository projectRepository;
    private final ClaimRepository claimRepository;
    private final SiteAdminGuard siteAdminGuard;
    private final RateLimitService rateLimitService;

    public ReportService(ReportRepository reportRepository,
                          CommentRepository commentRepository,
                          ProjectRepository projectRepository,
                          ClaimRepository claimRepository,
                          SiteAdminGuard siteAdminGuard,
                          RateLimitService rateLimitService) {
        this.reportRepository = reportRepository;
        this.commentRepository = commentRepository;
        this.projectRepository = projectRepository;
        this.claimRepository = claimRepository;
        this.siteAdminGuard = siteAdminGuard;
        this.rateLimitService = rateLimitService;
    }

    /**
     * {@code POST /comments/{commentId}/reports}: flag a comment.
     *
     * @throws ApiException with code {@code RATE_LIMITED} (429, API-03.10) if the caller has filed
     *                       too many reports in the last hour,
     *                       {@code COMMENT_NOT_FOUND} (404) if the comment doesn't exist
     *                       (or is already soft-deleted), or {@code REPORT_ALREADY_EXISTS} (409) if
     *                       the caller already reported this comment
     */
    @Transactional
    public ReportDto reportComment(Long commentId, String reason, User caller) {
        rateLimitService.checkReportLimit(caller.getId());

        boolean commentExists = commentRepository.findById(commentId)
                .filter(c -> c.getDeletedAt() == null)
                .isPresent();
        if (!commentExists) {
            throw new ApiException(
                    "COMMENT_NOT_FOUND", "No comment exists with id " + commentId, HttpStatus.NOT_FOUND);
        }

        return createReport(ReportTargetType.COMMENT, commentId, reason, caller);
    }

    /**
     * {@code POST /projects/{projectId}/reports}: flag a project listing.
     *
     * @throws ApiException with code {@code RATE_LIMITED} (429, API-03.10) if the caller has filed
     *                       too many reports in the last hour,
     *                       {@code PROJECT_NOT_FOUND} (404) if the project doesn't exist, or
     *                       {@code REPORT_ALREADY_EXISTS} (409) if the caller already reported this project
     */
    @Transactional
    public ReportDto reportProject(Long projectId, String reason, User caller) {
        rateLimitService.checkReportLimit(caller.getId());

        if (!projectRepository.existsById(projectId)) {
            throw new ApiException(
                    "PROJECT_NOT_FOUND", "No project exists with id " + projectId, HttpStatus.NOT_FOUND);
        }

        return createReport(ReportTargetType.PROJECT, projectId, reason, caller);
    }

    /**
     * Shared creation path for both report endpoints: pre-checks for a duplicate
     * (same reporter, same target) for a clean 409, then attempts the insert —
     * the real unique constraint on {@code (reporter_user_id, target_type, target_id)}
     * is the defensive backstop for the race window between the two, same pattern
     * as {@code MaintainerService.inviteMaintainer}.
     */
    private ReportDto createReport(ReportTargetType targetType, Long targetId, String reason, User caller) {
        String wireTargetType = targetType.getWireValue();

        if (reportRepository.findByReporterUserIdAndTargetTypeAndTargetId(caller.getId(), wireTargetType, targetId)
                .isPresent()) {
            throw new ApiException(
                    "REPORT_ALREADY_EXISTS",
                    "You have already reported this " + wireTargetType + ".",
                    HttpStatus.CONFLICT);
        }

        Report report = new Report();
        report.setReporterUser(caller);
        report.setTargetType(wireTargetType);
        report.setTargetId(targetId);
        report.setReason(reason);

        try {
            return toDto(reportRepository.saveAndFlush(report));
        } catch (DataIntegrityViolationException ex) {
            throw new ApiException(
                    "REPORT_ALREADY_EXISTS",
                    "You have already reported this " + wireTargetType + ".",
                    HttpStatus.CONFLICT);
        }
    }

    /**
     * {@code GET /admin/reports}: site-admin only. Defaults to {@code open} reports
     * when no status filter is given.
     *
     * @throws ApiException with code {@code FORBIDDEN} (403) if the caller isn't a site admin
     */
    @Transactional(readOnly = true)
    public PagedReports listReports(ReportStatus statusFilter, Integer page, Integer size, User caller) {
        siteAdminGuard.requireSiteAdmin(caller);

        String status = (statusFilter == null ? ReportStatus.OPEN : statusFilter).getWireValue();
        int resolvedPage = clampPage(page);
        int resolvedSize = clampSize(size);

        Page<Report> result = reportRepository.findByStatus(status, PageRequest.of(resolvedPage, resolvedSize));
        List<ReportDto> items = result.getContent().stream().map(this::toDto).toList();
        return new PagedReports(items, new PageMeta(resolvedPage, resolvedSize, (int) result.getTotalElements()));
    }

    /**
     * {@code PATCH /admin/reports/{reportId}}: site-admin only. Resolves or dismisses
     * a report. Updates the report row only — never deletes the reported content
     * itself; an admin who agrees with the report uses the existing
     * {@code DELETE /comments/{commentId}} separately (design doc §15.4's
     * "one deletion mechanism" rule).
     *
     * @throws ApiException with code {@code FORBIDDEN} (403) if the caller isn't a site admin, or
     *                       {@code REPORT_NOT_FOUND} (404) if the report doesn't exist
     */
    @Transactional
    public ReportDto reviewReport(Long reportId, ReviewReportRequest request, User caller) {
        siteAdminGuard.requireSiteAdmin(caller);

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ApiException(
                        "REPORT_NOT_FOUND", "No report exists with id " + reportId, HttpStatus.NOT_FOUND));

        report.setStatus(request.status().getWireValue());
        report.setResolution(request.resolution());
        report.setResolvedAt(OffsetDateTime.now());

        return toDto(reportRepository.save(report));
    }

    /** Maps a persisted report row to the API's {@link ReportDto} shape. */
    private ReportDto toDto(Report entity) {
        return new ReportDto(
                entity.getId(),
                ReportTargetType.valueOf(entity.getTargetType().toUpperCase(java.util.Locale.ROOT)),
                entity.getTargetId(),
                toPublicProfile(entity.getReporterUser()),
                entity.getReason(),
                ReportStatus.valueOf(entity.getStatus().toUpperCase(java.util.Locale.ROOT)),
                entity.getResolution(),
                entity.getCreatedAt(),
                entity.getResolvedAt()
        );
    }

    /**
     * Maps a report's author to the API's {@link PublicUserProfile} shape.
     * {@code projectsCount} is not yet computed anywhere in the codebase (no
     * ticket populates it); left {@code null} rather than a made-up value.
     * {@code contributionsCount} (API-03.5) counts only this user's
     * {@code completed} claims.
     */
    private PublicUserProfile toPublicProfile(User user) {
        return new PublicUserProfile(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getBio(),
                user.getLocation(),
                user.getSkills() == null ? List.of() : List.of(user.getSkills()),
                null,
                (int) claimRepository.countByUserIdAndStatus(user.getId(), ClaimStatus.COMPLETED),
                user.getReputation()
        );
    }

    private int clampSize(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        return Math.max(1, Math.min(size, MAX_SIZE));
    }

    private int clampPage(Integer page) {
        if (page == null || page < 0) {
            return DEFAULT_PAGE;
        }
        return page;
    }
}
