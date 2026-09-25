package za.codemaster.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.codemaster.backend.domain.model.ListingStatus;
import za.codemaster.backend.domain.model.Project;
import za.codemaster.backend.repository.ProjectMaintainerRepository;
import za.codemaster.backend.repository.ProjectRepository;

import java.util.Optional;

/**
 * Embeddable README Badge (wow-feature, 2026-09-24) — a public, cacheable
 * SVG a maintainer can paste into their own GitHub README, e.g.
 * {@code ![Code Masters](https://.../projects/my-repo/badge.svg)}. The one
 * real distribution channel this platform has that lives outside the
 * platform itself: every time someone views that README on GitHub, GitHub
 * fetches this image fresh, no login or platform visit required.
 * <p>
 * An unknown, pending, or rejected slug never produces an HTTP error —
 * always a valid "not found" SVG at 200 OK instead. A 404 wouldn't render
 * as an image at all inside someone's Markdown, silently breaking their
 * README's layout; a grey "not found" badge degrades visibly instead.
 */
@Service
public class BadgeService {

    private final ProjectRepository projectRepository;
    private final ProjectMaintainerRepository projectMaintainerRepository;

    public BadgeService(ProjectRepository projectRepository,
                         ProjectMaintainerRepository projectMaintainerRepository) {
        this.projectRepository = projectRepository;
        this.projectMaintainerRepository = projectMaintainerRepository;
    }

    /**
     * @param slug the project's slug (not numeric id) — matches this
     *             platform's other human-readable, shareable URLs
     * @return a complete SVG document; never throws
     */
    @Transactional(readOnly = true)
    public String renderMaintainerCountBadge(String slug) {
        Optional<Project> project = projectRepository.findBySlugAndListingStatus(slug, ListingStatus.PUBLISHED);
        if (project.isEmpty()) {
            return SvgBadgeRenderer.render("code masters", "not found", SvgBadgeRenderer.NEUTRAL_COLOR);
        }

        long maintainerCount = projectMaintainerRepository.countByProjectId(project.get().getId());
        String value = maintainerCount + (maintainerCount == 1 ? " maintainer" : " maintainers");
        return SvgBadgeRenderer.render("code masters", value, SvgBadgeRenderer.ACCENT_COLOR);
    }
}
