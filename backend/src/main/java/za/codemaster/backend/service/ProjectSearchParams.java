package za.codemaster.backend.service;

/**
 * Bundles the query parameters accepted by {@code GET /api/v1/projects}.
 * <p>
 * Not part of the OpenAPI spec's schemas (the spec defines these as
 * individual query parameters, not a named object) — this exists purely to
 * keep {@link ProjectQueryService}'s method signature and tests readable.
 * All fields are nullable: {@code null} means "this filter/param wasn't
 * provided," not "match nothing."
 */
public record ProjectSearchParams (
        Integer page,
        Integer size,
        String q,
        String language,
        String category,
        String tag,
        String country,
        String sort,
        Boolean hasBeginnerIssues
) {
}
