/**
 * Mock Project data for FE-01.7 card review.
 *
 * Seeded deliberately so every combination of the badge-driving
 * fields is covered at least once:
 *   verified:                    true | false
 *   hasBeginnerFriendlyIssues:   true | false
 * `connection` (south_african | community_verified) is varied across
 * entries so every label renders.
 *
 * Shape matches the Project contract exactly, no extra fields, so this
 * can be swapped for API-01's real response later with no reshaping.
 */

export const mockProjects = [
  {
    // verified + beginner-friendly
    name: "httpie",
    description:
      "A user-friendly command-line HTTP client for the API era, built for humans.",
    primaryLanguage: "Python",
    category: "CLI Tool",
    tags: ["cli", "http", "networking"],
    stars: 34200,
    contributors: 187,
    lastActivityAt: "2026-09-14T10:00:00Z",
    connection: "south_african",
    verified: true,
    hasBeginnerFriendlyIssues: true,
  },
  {
    // verified + NOT beginner-friendly
    name: "tokio",
    description:
      "An asynchronous runtime for Rust, providing the building blocks for writing network applications.",
    primaryLanguage: "Rust",
    category: "Web Framework",
    tags: ["async", "runtime", "networking"],
    stars: 27900,
    contributors: 421,
    lastActivityAt: "2026-09-16T08:30:00Z",
    connection: "south_african",
    verified: true,
    hasBeginnerFriendlyIssues: false,
  },
  {
    // unverified + beginner-friendly
    name: "first-timers-cookbook",
    description:
      "Recipes and templates that help maintainers make their projects approachable to first-time contributors.",
    primaryLanguage: "JavaScript",
    category: "Documentation",
    tags: ["docs", "community", "good-first-issue"],
    stars: 640,
    contributors: 52,
    lastActivityAt: "2026-06-02T14:00:00Z",
    connection: "community_verified",
    verified: false,
    hasBeginnerFriendlyIssues: true,
  },
  {
    // unverified + NOT beginner-friendly
    name: "legacy-etl-pipeline",
    description:
      "Internal-style batch ETL pipeline for large tabular datasets. Sparse docs, no onboarding path yet.",
    primaryLanguage: "Java",
    category: "Data / ML",
    tags: ["etl", "batch"],
    stars: 12,
    contributors: 3,
    lastActivityAt: "2024-11-20T09:00:00Z",
    connection: "community_verified",
    verified: false,
    hasBeginnerFriendlyIssues: false,
  },
  {
    // edge case: missing description + no tags + optional `verified`
    // omitted (card defaults it to false). `connection` is required by the
    // Project contract, so it stays present here.
    name: "quiet-utils",
    description: "",
    primaryLanguage: "Go",
    category: "CLI Tool",
    tags: [],
    stars: 5,
    contributors: 1,
    lastActivityAt: null,
    connection: "south_african",
    hasBeginnerFriendlyIssues: false,
  },
];
