package za.codemaster.backend.service;

import org.junit.jupiter.api.Test;
import za.codemaster.backend.dto.ProjectDetail;
import za.codemaster.backend.exception.ApiException;
import za.codemaster.backend.mock.MockDataStore;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies API-01.4's two acceptance criteria against ProjectQueryService.getProjectDetail:
 * a valid id returns full ProjectDetail shape, an invalid id throws PROJECT_NOT_FOUND.
 */
class ProjectDetailTest {

    private final MockDataStore mockDataStore = new MockDataStore();
    private final ProjectQueryService service = new ProjectQueryService(mockDataStore);

    @Test
    void validIdReturnsProjectDetailWithAllFourExtraFieldsPresent() {
        Long existingId = mockDataStore.projects().get(0).id();

        ProjectDetail detail = service.getProjectDetail(existingId);

        assertNotNull(detail.project(), "project fields should be present (via @JsonUnwrapped)");
        assertEquals(existingId, detail.project().id());
        assertNotNull(detail.maintainers(), "maintainers should be present, even if empty");
        assertNotNull(detail.featuredIssues(), "featuredIssues should be present, even if empty");
        assertNotNull(detail.recentComments(), "recentComments should be present, even if empty");
    }

    @Test
    void invalidIdThrowsProjectNotFound() {
        Long missingId = -999L;

        ApiException ex = assertThrows(ApiException.class, () -> service.getProjectDetail(missingId));

        assertEquals("PROJECT_NOT_FOUND", ex.getCode());
        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, ex.getStatus());
    }
}