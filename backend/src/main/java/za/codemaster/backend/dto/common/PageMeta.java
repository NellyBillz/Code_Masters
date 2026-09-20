package za.codemaster.backend.dto.common;

/**
 * Pagination metadata attached to every paged response.
 * <p>
 * Matches the {@code PageMeta} schema in codemasters-api-spec.yaml v2.1.
 *
 * @param page  the page number returned (0-based, per the spec's {@code Page} parameter)
 * @param size  the page size actually applied (after clamping to the 1–50 range)
 * @param total the total number of items matching the filters, across all pages
 */
public record PageMeta(
        int page,
        int size,
        int total
) {
}
