package za.codemaster.backend.service;

import org.junit.jupiter.api.Test;
import za.codemaster.backend.dto.IssueDetail;
import za.codemaster.backend.exception.ApiException;
import za.codemaster.backend.mock.MockDataStore;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies API-01.6's acceptance criteria: valid id returns full IssueDetail
 * shape, invalid id throws ISSUE_NOT_FOUND.
 */
class IssueDetailTest {

    private final MockDataStore mockDataStore = new MockDataStore();
    private final ProjectQueryService service = new ProjectQueryService(mockDataStore);

    @Test
    void validIdReturnsIssueDetailWithAllThreeExtraFieldsPresent() {
        Long existingId = mockDataStore.issues().get(0).id();

        IssueDetail detail = service.getIssueDetail(existingId);

        assertNotNull(detail.issue(), "issue fields should be present (via @JsonUnwrapped)");
        assertEquals(existingId, detail.issue().id());
        assertNotNull(detail.project(), "project should be populated, not null, since we have the data");
        assertEquals(detail.issue().projectId(), detail.project().id(),
                "project returned should actually be the issue's own project");
        assertNotNull(detail.comments(), "comments should be present, even if empty");
        assertNotNull(detail.claims(), "claims should be present, even if empty");
    }

    @Test
    void invalidIdThrowsIssueNotFound() {
        Long missingId = -999L;

        ApiException ex = assertThrows(ApiException.class, () -> service.getIssueDetail(missingId));

        assertEquals("ISSUE_NOT_FOUND", ex.getCode());
        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, ex.getStatus());
    }
}