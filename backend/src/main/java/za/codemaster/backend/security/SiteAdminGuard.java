package za.codemaster.backend.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import za.codemaster.backend.domain.model.User;
import za.codemaster.backend.exception.ApiException;

/**
 * The single reusable site-admin check every {@code /admin/*} endpoint uses
 * (API-03.2). Throws the same {@code FORBIDDEN} shape every other maintainer-only
 * endpoint already throws — deliberately no new error code, per that ticket's
 * contract note: "/admin/* uses the same Forbidden response every other
 * maintainer-only endpoint already uses."
 * <p>
 * There is no self-service way to become a site admin (design doc §6, "users"
 * table note) — {@code isSiteAdmin} is only ever true because it was set
 * directly in the database.
 */
@Component
public class SiteAdminGuard {

    /**
     * @param currentUser the authenticated caller (never {@code null} in practice —
     *                     every {@code /admin/*} endpoint requires {@code @AuthenticatedUser}
     *                     first, which already throws {@code UNAUTHENTICATED} for no session)
     * @throws ApiException with code {@code FORBIDDEN} (403) if the caller isn't a site admin
     */
    public void requireSiteAdmin(User currentUser) {
        if (currentUser == null || !currentUser.isSiteAdmin()) {
            throw new ApiException(
                    "FORBIDDEN", "This action requires site-admin privileges.", HttpStatus.FORBIDDEN);
        }
    }
}
