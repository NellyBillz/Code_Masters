package za.codemaster.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.codemaster.backend.domain.model.ClaimStatus;
import za.codemaster.backend.domain.model.ListingStatus;
import za.codemaster.backend.dto.stats.PlatformStats;
import za.codemaster.backend.repository.ClaimRepository;
import za.codemaster.backend.repository.ProjectRepository;

import java.time.OffsetDateTime;

/**
 * {@code GET /stats} (API-03.12, design doc §17): public, non-personal,
 * aggregate platform-impact metrics. Every figure is a plain {@code COUNT()}-style
 * query against a single table — deliberately not one query joining the full
 * {@code claims}/{@code comments} tables together, per the ticket's own
 * instruction, so this stays cheap regardless of how large those tables get.
 * A {@code pending} project counts toward nothing here, matching how it's
 * invisible everywhere else in public discovery.
 */
@Service
public class StatsService {

    private final ProjectRepository projectRepository;
    private final ClaimRepository claimRepository;

    public StatsService(ProjectRepository projectRepository, ClaimRepository claimRepository) {
        this.projectRepository = projectRepository;
        this.claimRepository = claimRepository;
    }

    @Transactional(readOnly = true)
    public PlatformStats getStats() {
        long publishedProjects = projectRepository.countByListingStatus(ListingStatus.PUBLISHED);
        long activeProjectsAcceptingContributions =
                projectRepository.countByListingStatusAndAcceptingContributions(ListingStatus.PUBLISHED, true);
        long totalContributorsEngaged = claimRepository.countDistinctUsersIncludingCollaborators();
        long totalActiveClaims = claimRepository.countByStatus(ClaimStatus.ACTIVE);
        long totalContributionsCompleted = claimRepository.countByStatus(ClaimStatus.COMPLETED);

        return new PlatformStats(
                publishedProjects,
                activeProjectsAcceptingContributions,
                totalContributorsEngaged,
                totalActiveClaims,
                totalContributionsCompleted,
                OffsetDateTime.now()
        );
    }
}
