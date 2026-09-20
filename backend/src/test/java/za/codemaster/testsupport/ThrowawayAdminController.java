package za.codemaster.testsupport;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.security.AuthenticatedUser;
import za.codemaster.backend.security.SiteAdminGuard;

/**
 * A throwaway endpoint for API-03.2's own acceptance test, mirroring
 * {@link ThrowawayWriteController}'s reasoning for API-02.2: proves
 * {@link SiteAdminGuard} behaves correctly in isolation, without coupling the
 * test to any specific real {@code /admin/*} feature's business logic.
 * Deliberately lives outside {@code za.codemaster.backend}'s component-scan
 * tree and is registered only via explicit {@code @Import} in the test that
 * needs it, so it never appears in the real application context.
 */
@RestController
public class ThrowawayAdminController {

    public static final String PATH = "/api/v1/_test/throwaway-admin";

    private final SiteAdminGuard siteAdminGuard;

    public ThrowawayAdminController(SiteAdminGuard siteAdminGuard) {
        this.siteAdminGuard = siteAdminGuard;
    }

    @GetMapping(PATH)
    public ResponseEntity<Void> adminOnly(@AuthenticatedUser User currentUser) {
        siteAdminGuard.requireSiteAdmin(currentUser);
        return ResponseEntity.ok().build();
    }
}
