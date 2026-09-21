package za.codemaster.backend.dto.report;

import za.codemaster.backend.dto.common.PageMeta;

import java.util.List;

/**
 * Response shape for {@code GET /admin/reports} (API-03.9).
 * <p>
 * Matches the {@code PagedReports} schema in codemasters-api-spec.yaml v3.
 */
public record PagedReports(
        List<ReportDto> items,
        PageMeta meta
) {
}
