package za.codemaster.backend.dto.search;

/**
 * Matches the {@code SearchResultItem} {@code oneOf}/discriminator schema in
 * codemasters-api-spec.yaml v2.1 — either a {@link ProjectSearchResult} or an
 * {@link IssueSearchResult}, told apart by {@code resultType}. Same technique
 * as {@link za.codemaster.backend.client.github.dto.GitHubFetchResult}'s sealed
 * interface, minus the shared behavior methods — this one exists purely so
 * {@code PagedSearchResults.items} has a common declared type; Jackson
 * serializes each element by its actual runtime record.
 */
public sealed interface SearchResultItem permits ProjectSearchResult, IssueSearchResult {
}
