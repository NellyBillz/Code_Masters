package za.codemaster.backend.dto.search;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import za.codemaster.backend.dto.issue.IssueDto;

/**
 * Matches the {@code IssueSearchResult} schema in codemasters-api-spec.yaml v2.1:
 * an {@code allOf} of {@code resultType: "issue"} and every {@link IssueDto}
 * field, flattened via {@code @JsonUnwrapped} — same technique as {@code IssueDetail}.
 */
public record IssueSearchResult(
        SearchResultType resultType,
        @JsonUnwrapped IssueDto issue
) implements SearchResultItem {

    public IssueSearchResult(IssueDto issue) {
        this(SearchResultType.ISSUE, issue);
    }
}
