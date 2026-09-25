package za.codemaster.backend.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import za.codemaster.backend.service.BadgeService;

import java.util.concurrent.TimeUnit;

/**
 * Embeddable README Badge (wow-feature, 2026-09-24) — public, unauthenticated,
 * no session/CSRF machinery at all, since GitHub itself (not a browser with
 * our cookies) is the actual caller once this is pasted into a README.
 */
@RestController
public class BadgeController {

    private static final MediaType SVG = MediaType.valueOf("image/svg+xml");

    /** Short enough that a maintainer count change shows up same-day; long enough to spare the server on a popular repo. */
    private static final CacheControl CACHE_POLICY = CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic();

    private final BadgeService badgeService;

    public BadgeController(BadgeService badgeService) {
        this.badgeService = badgeService;
    }

    /**
     * {@code GET /api/v1/projects/{slug}/badge.svg}: the project's real
     * maintainer count, rendered as an SVG. Always 200 with a valid image —
     * see {@code BadgeService}'s Javadoc for why an unknown slug never
     * produces an HTTP error here.
     */
    @GetMapping(value = "/api/v1/projects/{slug}/badge.svg", produces = "image/svg+xml")
    public ResponseEntity<String> getMaintainerCountBadge(@PathVariable String slug) {
        return ResponseEntity.ok()
                .contentType(SVG)
                .cacheControl(CACHE_POLICY)
                .body(badgeService.renderMaintainerCountBadge(slug));
    }
}
