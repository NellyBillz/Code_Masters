package za.codemaster.backend.service;

import za.codemaster.backend.domain.model.Issue;
import za.codemaster.backend.domain.model.ListingStatus;
import za.codemaster.backend.domain.model.Project;
import za.codemaster.backend.repository.IssueRepository;
import za.codemaster.backend.repository.ProjectRepository;

import java.util.List;
import java.util.UUID;

/**
 * Seeds a real (test) database with the same shape of data
 * {@code MockDataStore} used to hold in memory: 5 projects spanning every
 * {@link za.codemaster.backend.dto.project.ProjectConnection} value, 8 issues
 * spanning every {@link za.codemaster.backend.dto.issue.Difficulty} value.
 * <p>
 * Exists so {@code ProjectQueryServiceTest}/{@code ProjectDetailTest}/
 * {@code ProjectIssuesTest}/{@code IssueDetailTest} can keep their original
 * API-01.3–.6 assertions after API-02.1 swapped {@code MockDataStore} for
 * real repositories — per that ticket's acceptance criteria, only *how* test
 * data is set up should change, never the assertions. A random suffix is
 * mixed into every unique column (slug, github_url) so repeated runs against
 * the same Postgres instance never collide on a unique constraint.
 */
final class ProjectQueryServiceFixtures {

    private final List<Project> projects;
    private final List<Issue> issues;

    private ProjectQueryServiceFixtures(List<Project> projects, List<Issue> issues) {
        this.projects = projects;
        this.issues = issues;
    }

    /** Index matches {@code MockDataStore.projects()}'s original order (0 = OpenLearn SA, ...). */
    Long projectId(int index) {
        return projects.get(index).getId();
    }

    /** Index matches {@code MockDataStore.issues()}'s original order (0 = issue 101, ...). */
    Long issueId(int index) {
        return issues.get(index).getId();
    }

    static ProjectQueryServiceFixtures seed(ProjectRepository projectRepository, IssueRepository issueRepository) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        Project openLearnSa = projectRepository.save(project(
                "OpenLearn SA", "openlearn-sa-" + suffix,
                "Open learning platform for South African students",
                "example-org", "openlearn-sa-" + suffix,
                "Java", new String[]{"Java", "TypeScript"}, "Education",
                List.of("education", "lms"), List.of("ZA"),
                "south_african", "MIT", 120, 15, 8, 12, true, true));

        Project mzansiDevTools = projectRepository.save(project(
                "Mzansi DevTools", "mzansi-devtools-" + suffix,
                "CLI tooling for South African developers",
                "sa-devs", "mzansi-devtools-" + suffix,
                "Go", new String[]{"Go"}, "Developer Tools",
                List.of("cli", "tooling"), List.of("ZA"),
                "south_african", "Apache-2.0", 340, 40, 25, 30, true, false));

        Project saOpenDataHub = projectRepository.save(project(
                "SA Open Data Hub", "sa-open-data-hub-" + suffix,
                "Open datasets curated across South Africa",
                "sa-data-collective", "data-hub-" + suffix,
                "Python", new String[]{"Python", "SQL"}, "Data",
                List.of("data", "opendata"), List.of("ZA"),
                "community_verified", "MIT", 560, 80, 45, 60, false, true));

        Project communityHealthTracker = projectRepository.save(project(
                "Community Health Tracker", "community-health-tracker-" + suffix,
                "Health reporting tool used by community volunteers",
                "healthtrack-community", "tracker-" + suffix,
                "JavaScript", new String[]{"JavaScript", "TypeScript"}, "Health",
                List.of("health", "community"), List.of("ZA"),
                "community_verified", "GPL-3.0", 90, 10, 5, 8, true, true));

        Project eduBridge = projectRepository.save(project(
                "EduBridge", "edubridge-" + suffix,
                "Bridging rural schools to online curricula",
                "edubridge-org", "edubridge-" + suffix,
                "Java", new String[]{"Java"}, "Education",
                List.of("education"), List.of("ZA"),
                "south_african", "MIT", 45, 6, 3, 4, false, false));

        List<Project> projects = List.of(
                openLearnSa, mzansiDevTools, saOpenDataHub, communityHealthTracker, eduBridge);

        List<Issue> issues = List.of(
                issueRepository.save(issue(openLearnSa, 12,
                        "Add Afrikaans translation for onboarding flow",
                        "We need i18n support for the onboarding wizard...",
                        "open", new String[]{"good-first-issue", "i18n"}, "beginner", true)),
                issueRepository.save(issue(openLearnSa, 15,
                        "Migrate build to Gradle 9",
                        "Our current build is on an old Gradle version...",
                        "open", new String[]{"build"}, "advanced", false)),
                issueRepository.save(issue(mzansiDevTools, 8,
                        "Fix flag parsing edge case in CLI",
                        "Passing --flag=value with an equals sign breaks parsing...",
                        "claimed", new String[]{"bug", "good-first-issue"}, "beginner", true)),
                issueRepository.save(issue(mzansiDevTools, 20,
                        "Add shell completion scripts",
                        "Would be great to have bash/zsh completions...",
                        "open", new String[]{"enhancement"}, "intermediate", false)),
                issueRepository.save(issue(saOpenDataHub, 33,
                        "Normalize inconsistent date formats across datasets",
                        "Some datasets use DD/MM/YYYY, others ISO-8601...",
                        "open", new String[]{"data-quality"}, "intermediate", false)),
                issueRepository.save(issue(saOpenDataHub, 40,
                        "Investigate flaky ETL pipeline test",
                        "The nightly ETL test fails intermittently...",
                        "closed", new String[]{"bug"}, "unknown", false)),
                issueRepository.save(issue(communityHealthTracker, 5,
                        "Add offline mode for low-connectivity clinics",
                        "Clinics in rural areas often lose connectivity mid-session...",
                        "open", new String[]{"good-first-issue", "feature"}, "beginner", true)),
                issueRepository.save(issue(eduBridge, 2,
                        "Set up CI pipeline",
                        "No automated tests run on pull requests yet...",
                        "open", new String[]{"infra"}, "intermediate", false))
        );

        return new ProjectQueryServiceFixtures(projects, issues);
    }

    private static Project project(String name, String slug, String description,
                                    String githubOwner, String githubRepo,
                                    String primaryLanguage, String[] languages, String category,
                                    List<String> tags, List<String> countryCodes,
                                    String connection, String license,
                                    int stars, int forks, int openIssues, int contributors,
                                    boolean hasBeginnerFriendlyIssues, boolean verified) {
        Project p = new Project();
        p.setGithubOwner(githubOwner);
        p.setGithubRepo(githubRepo);
        p.setGithubUrl("https://github.com/" + githubOwner + "/" + githubRepo);
        p.setName(name);
        p.setSlug(slug);
        p.setDescription(description);
        p.setPrimaryLanguage(primaryLanguage);
        p.setLanguages(languages);
        p.setCategory(category);
        p.setConnection(connection);
        p.setLicense(license);
        p.setStars(stars);
        p.setForks(forks);
        p.setOpenIssues(openIssues);
        p.setContributors(contributors);
        p.setHasBeginnerFriendlyIssues(hasBeginnerFriendlyIssues);
        p.setVerified(verified);
        // API-03.1: these fixtures represent already-live projects, not new
        // submissions — published, same as DB-03.1's backfill for pre-existing rows.
        p.setListingStatus(ListingStatus.PUBLISHED);
        p.setTags(tags);
        p.setCountryCodes(countryCodes);
        return p;
    }

    private static Issue issue(Project project, int githubIssueNumber, String title, String bodyExcerpt,
                                String status, String[] labels, String difficulty, boolean isBeginnerFriendly) {
        Issue i = new Issue();
        i.setProject(project);
        i.setGithubIssueNumber(githubIssueNumber);
        i.setGithubUrl(project.getGithubUrl() + "/issues/" + githubIssueNumber);
        i.setTitle(title);
        i.setBodyExcerpt(bodyExcerpt);
        i.setStatus(status);
        i.setLabels(labels);
        i.setDifficulty(difficulty);
        i.setIsBeginnerFriendly(isBeginnerFriendly);
        return i;
    }
}
