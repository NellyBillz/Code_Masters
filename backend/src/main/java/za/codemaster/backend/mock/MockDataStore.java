package za.codemaster.backend.mock;

import org.springframework.stereotype.Component;
import za.codemaster.backend.dto.*;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * In-memory stand-in for real project/issue persistence.
 * <p>
 * Round-1-only scaffolding: {@link #projects()} and {@link #issues()} are the
 * single source every discovery endpoint (API-01.3–.6) queries against; no
 * endpoint should build its own separate mock list. This class is deleted,
 * not extended, once Round 2 wires real JPA repositories on top of DB-01's
 * schema; the {@link Project}/{@link Issue} types themselves stay; only
 * where their data comes from changes.
 */
@Component
public class MockDataStore {

    private final List<Project> projects = List.of(
            new Project(
                    1L, "OpenLearn SA", "openlearn-sa",
                    "Open learning platform for South African students",
                    "https://github.com/example-org/openlearn-sa", "example-org",
                    "Java", List.of("Java", "TypeScript"), "Education",
                    List.of("education", "lms"), List.of("ZA"),
                    ProjectConnection.SOUTH_AFRICAN, "MIT",
                    120, 15, 8, 12, true,
                    OffsetDateTime.parse("2026-08-01T10:00:00Z"),
                    true, OffsetDateTime.parse("2026-01-10T09:00:00Z"),
                    OffsetDateTime.parse("2025-01-01T09:00:00Z"),
                    OffsetDateTime.parse("2026-08-01T10:00:00Z")
            ),
            new Project(
                    2L, "Naija DevTools", "naija-devtools",
                    "CLI tooling for Nigerian developers",
                    "https://github.com/naija-devs/naija-devtools", "naija-devs",
                    "Go", List.of("Go"), "Developer Tools",
                    List.of("cli", "tooling"), List.of("NG"),
                    ProjectConnection.AFRICA_LED, "Apache-2.0",
                    340, 40, 25, 30, true,
                    OffsetDateTime.parse("2026-09-01T08:00:00Z"),
                    false, null,
                    OffsetDateTime.parse("2024-05-01T08:00:00Z"),
                    OffsetDateTime.parse("2026-09-01T08:00:00Z")
            ),
            new Project(
                    3L, "PanAfrica Data Hub", "panafrica-data-hub",
                    "Open datasets curated across Africa",
                    "https://github.com/panafrica-org/data-hub", "panafrica-org",
                    "Python", List.of("Python", "SQL"), "Data",
                    List.of("data", "opendata"), List.of("KE", "GH", "ZA"),
                    ProjectConnection.AFRICA_FOCUSED, "MIT",
                    560, 80, 45, 60, false,
                    OffsetDateTime.parse("2026-07-15T12:00:00Z"),
                    true, OffsetDateTime.parse("2025-11-01T12:00:00Z"),
                    OffsetDateTime.parse("2023-02-01T12:00:00Z"),
                    OffsetDateTime.parse("2026-07-15T12:00:00Z")
            ),
            new Project(
                    4L, "Community Health Tracker", "community-health-tracker",
                    "Health reporting tool used by community volunteers",
                    "https://github.com/healthtrack-community/tracker", "healthtrack-community",
                    "JavaScript", List.of("JavaScript", "TypeScript"), "Health",
                    List.of("health", "community"), List.of("ZA", "ZW"),
                    ProjectConnection.COMMUNITY_VERIFIED, "GPL-3.0",
                    90, 10, 5, 8, true,
                    OffsetDateTime.parse("2026-06-20T14:00:00Z"),
                    true, OffsetDateTime.parse("2026-02-01T14:00:00Z"),
                    OffsetDateTime.parse("2025-09-01T14:00:00Z"),
                    OffsetDateTime.parse("2026-06-20T14:00:00Z")
            ),
            new Project(
                    5L, "EduBridge", "edubridge",
                    "Bridging rural schools to online curricula",
                    "https://github.com/edubridge-org/edubridge", "edubridge-org",
                    "Java", List.of("Java"), "Education",
                    List.of("education"), List.of("ZA"),
                    ProjectConnection.SOUTH_AFRICAN, "MIT",
                    45, 6, 3, 4, false,
                    OffsetDateTime.parse("2026-03-10T11:00:00Z"),
                    false, null,
                    OffsetDateTime.parse("2025-11-01T11:00:00Z"),
                    OffsetDateTime.parse("2026-03-10T11:00:00Z")
            )
    );

    private final List<Issue> issues = List.of(
            new Issue(
                    101L, 1L, 12, "Add Afrikaans translation for onboarding flow",
                    "We need i18n support for the onboarding wizard...",
                    "https://github.com/example-org/openlearn-sa/issues/12",
                    IssueStatus.OPEN, List.of("good-first-issue", "i18n"),
                    Difficulty.BEGINNER, true, false,
                    null, null, null, null, 1,
                    OffsetDateTime.parse("2026-07-01T10:00:00Z"),
                    OffsetDateTime.parse("2026-08-01T10:00:00Z")
            ),
            new Issue(
                    102L, 1L, 15, "Migrate build to Gradle 9",
                    "Our current build is on an old Gradle version...",
                    "https://github.com/example-org/openlearn-sa/issues/15",
                    IssueStatus.OPEN, List.of("build"),
                    Difficulty.ADVANCED, false, false,
                    null, null, null, null, 0,
                    OffsetDateTime.parse("2026-07-20T10:00:00Z"),
                    OffsetDateTime.parse("2026-07-20T10:00:00Z")
            ),
            new Issue(
                    103L, 2L, 8, "Fix flag parsing edge case in CLI",
                    "Passing --flag=value with an equals sign breaks parsing...",
                    "https://github.com/naija-devs/naija-devtools/issues/8",
                    IssueStatus.CLAIMED, List.of("bug", "good-first-issue"),
                    Difficulty.BEGINNER, true, false,
                    null, null, null, null, 2,
                    OffsetDateTime.parse("2026-06-01T08:00:00Z"),
                    OffsetDateTime.parse("2026-09-01T08:00:00Z")
            ),
            new Issue(
                    104L, 2L, 20, "Add shell completion scripts",
                    "Would be great to have bash/zsh completions...",
                    "https://github.com/naija-devs/naija-devtools/issues/20",
                    IssueStatus.OPEN, List.of("enhancement"),
                    Difficulty.INTERMEDIATE, false, false,
                    null, null, null, null, 0,
                    OffsetDateTime.parse("2026-08-15T08:00:00Z"),
                    OffsetDateTime.parse("2026-08-15T08:00:00Z")
            ),
            new Issue(
                    105L, 3L, 33, "Normalize inconsistent date formats across datasets",
                    "Some datasets use DD/MM/YYYY, others ISO-8601...",
                    "https://github.com/panafrica-org/data-hub/issues/33",
                    IssueStatus.OPEN, List.of("data-quality"),
                    Difficulty.INTERMEDIATE, false, false,
                    null, null, null, null, 1,
                    OffsetDateTime.parse("2026-05-01T12:00:00Z"),
                    OffsetDateTime.parse("2026-07-15T12:00:00Z")
            ),
            new Issue(
                    106L, 3L, 40, "Investigate flaky ETL pipeline test",
                    "The nightly ETL test fails intermittently...",
                    "https://github.com/panafrica-org/data-hub/issues/40",
                    IssueStatus.CLOSED, List.of("bug"),
                    Difficulty.UNKNOWN, false, false,
                    null, null, null, null, 0,
                    OffsetDateTime.parse("2026-04-01T12:00:00Z"),
                    OffsetDateTime.parse("2026-06-01T12:00:00Z")
            ),
            new Issue(
                    107L, 4L, 5, "Add offline mode for low-connectivity clinics",
                    "Clinics in rural areas often lose connectivity mid-session...",
                    "https://github.com/healthtrack-community/tracker/issues/5",
                    IssueStatus.OPEN, List.of("good-first-issue", "feature"),
                    Difficulty.BEGINNER, true, false,
                    null, null, null, null, 1,
                    OffsetDateTime.parse("2026-06-01T14:00:00Z"),
                    OffsetDateTime.parse("2026-06-20T14:00:00Z")
            ),
            new Issue(
                    108L, 5L, 2, "Set up CI pipeline",
                    "No automated tests run on pull requests yet...",
                    "https://github.com/edubridge-org/edubridge/issues/2",
                    IssueStatus.OPEN, List.of("infra"),
                    Difficulty.INTERMEDIATE, false, false,
                    null, null, null, null, 0,
                    OffsetDateTime.parse("2026-02-01T11:00:00Z"),
                    OffsetDateTime.parse("2026-03-10T11:00:00Z")
            )
    );

    /** @return all mock projects, spanning every {@link ProjectConnection} value. */
    public List<Project> projects() {
        return projects;
    }

    /** @return all mock issues, spanning every {@link Difficulty} value. */
    public List<Issue> issues() {
        return issues;
    }
}