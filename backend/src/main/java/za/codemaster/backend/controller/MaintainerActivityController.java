package za.codemaster.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.dto.user.MaintainerActivitySummary;
import za.codemaster.backend.security.AuthenticatedUser;
import za.codemaster.backend.service.MaintainerActivityService;

/**
 * The maintainer-sanity activity rollup endpoint (API-03.7).
 */
@RestController
public class MaintainerActivityController {

    private final MaintainerActivityService maintainerActivityService;

    public MaintainerActivityController(MaintainerActivityService maintainerActivityService) {
        this.maintainerActivityService = maintainerActivityService;
    }

    /**
     * {@code GET /api/v1/users/me/maintainer-activity}: aggregated activity across
     * every project the caller maintains.
     */
    @GetMapping("/api/v1/users/me/maintainer-activity")
    public MaintainerActivitySummary getMaintainerActivity(@AuthenticatedUser User currentUser) {
        return maintainerActivityService.getActivity(currentUser);
    }
}
