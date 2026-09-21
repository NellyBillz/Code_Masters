import Link from "next/link";
import styles from "./ProjectCard.module.css";

const LANGUAGE_COLORS = {
  JavaScript: "#eab308",
  TypeScript: "#3b82f6",
  Python: "#22c55e",
  Go: "#06b6d4",
  Rust: "#f97316",
  Java: "#f43f5e",
  "C++": "#a855f7",
  Ruby: "#dc2626",
};

const CONNECTION_META = {
  south_african: {
    label: "South African",
    className: styles.connSouthAfrican,
  },
  community_verified: {
    label: "Community Verified",
    className: styles.connCommunityVerified,
  },
};

function formatRelativeTime(isoString) {
  if (!isoString) return "unknown";

  const date = new Date(isoString);

  if (Number.isNaN(date.getTime())) return "unknown";

  const diffMs = Date.now() - date.getTime();
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

  if (diffDays <= 0) return "today";
  if (diffDays === 1) return "1 day ago";
  if (diffDays < 30) return `${diffDays} days ago`;

  const diffMonths = Math.floor(diffDays / 30);

  if (diffMonths < 12) {
    return `${diffMonths} month${diffMonths === 1 ? "" : "s"} ago`;
  }

  const diffYears = Math.floor(diffMonths / 12);

  return `${diffYears} year${diffYears === 1 ? "" : "s"} ago`;
}

function formatCount(n) {
  if (typeof n !== "number" || Number.isNaN(n)) return "0";

  if (n >= 1000) {
    return `${(n / 1000).toFixed(n % 1000 >= 100 ? 1 : 0)}k`;
  }

  return String(n);
}

function ConnectionBadge({ connection }) {
  const meta = CONNECTION_META[connection];

  if (!meta) {
    return (
      <span className={`${styles.badge} ${styles.connUnclassified}`}>
        Unclassified
      </span>
    );
  }

  return (
    <span
      className={`${styles.badge} ${
        meta.className || styles.connUnclassified
      }`}
      title={`connection: ${connection}`}
    >
      {meta.label}
    </span>
  );
}

function VerifiedBadge({ verified }) {
  return (
    <span
      className={`${styles.badge} ${
        verified ? styles.verifiedTrue : styles.verifiedFalse
      }`}
      title={
        verified
          ? "Issue claim confirmed"
          : "Issue claim not confirmed"
      }
    >
      <svg
        viewBox="0 0 20 20"
        fill="currentColor"
        aria-hidden="true"
      >
        {verified ? (
          <path d="M16.7 5.3a1 1 0 0 1 0 1.4l-7.4 7.4a1 1 0 0 1-1.4 0L3.3 9.5a1 1 0 1 1 1.4-1.4l3.9 3.9 6.7-6.7a1 1 0 0 1 1.4 0z" />
        ) : (
          <circle cx="10" cy="10" r="4" />
        )}
      </svg>

      {verified ? "Verified" : "Unverified"}
    </span>
  );
}

function BeginnerBadge({ hasBeginnerFriendlyIssues }) {
  return (
    <span
      className={`${styles.badge} ${
        hasBeginnerFriendlyIssues
          ? styles.beginnerTrue
          : styles.beginnerFalse
      }`}
      title={
        hasBeginnerFriendlyIssues
          ? "Has open beginner-friendly issues"
          : "No beginner-friendly issues right now"
      }
    >
      <svg
        viewBox="0 0 20 20"
        fill="currentColor"
        aria-hidden="true"
      >
        <path d="M10 2l2.2 4.9 5.3.6-4 3.7 1.1 5.3L10 13.9l-4.6 2.6 1.1-5.3-4-3.7 5.3-.6L10 2z" />
      </svg>

      {hasBeginnerFriendlyIssues
        ? "Beginner friendly"
        : "No beginner issues"}
    </span>
  );
}

export default function ProjectCard({ project }) {
  const {
    id,
    name = "Untitled project",
    description = "",
    primaryLanguage,
    category,
    tags = [],
    stars = 0,
    contributors = 0,
    lastActivityAt,
    connection,
    verified = false,
    hasBeginnerFriendlyIssues = false,
  } = project || {};

  const languageColor =
    LANGUAGE_COLORS[primaryLanguage] || "#94a3b8";

  return (
    <Link
      href={`/projects/${id}`}
      className={styles.card}
    >
      <div className={styles.header}>
        <h3 className={styles.name}>{name}</h3>

        <div className={styles.badgeColumn}>
          <ConnectionBadge connection={connection} />
          <VerifiedBadge verified={verified} />
        </div>
      </div>

      <div className={styles.meta}>
        {category && <span>{category}</span>}

        {primaryLanguage && (
          <span className={styles.languageLabel}>
            <span
              className={styles.languageDot}
              style={{ backgroundColor: languageColor }}
              aria-hidden="true"
            />
            {primaryLanguage}
          </span>
        )}
      </div>

      {description && (
        <p className={styles.description}>{description}</p>
      )}

      {tags.length > 0 && (
        <div className={styles.tags}>
          {tags.map((tag) => (
            <span key={tag} className={styles.tag}>
              {tag}
            </span>
          ))}
        </div>
      )}

      <div className={styles.footerSection}>
        <div className={styles.beginnerRow}>
          <BeginnerBadge
            hasBeginnerFriendlyIssues={
              hasBeginnerFriendlyIssues
            }
          />
        </div>

        <div className={styles.footer}>
          <div className={styles.stats}>
            <span className={styles.stat} title="Stars">
              <svg
                viewBox="0 0 20 20"
                fill="currentColor"
                aria-hidden="true"
              >
                <path d="M10 2l2.2 4.9 5.3.6-4 3.7 1.1 5.3L10 13.9l-4.6 2.6 1.1-5.3-4-3.7 5.3-.6L10 2z" />
              </svg>

              {formatCount(stars)}
            </span>

            <span
              className={styles.stat}
              title="Contributors"
            >
              <svg
                viewBox="0 0 20 20"
                fill="currentColor"
                aria-hidden="true"
              >
                <path d="M10 10a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7zM3.5 17a6.5 6.5 0 0 1 13 0 1 1 0 0 1-1 1h-11a1 1 0 0 1-1-1z" />
              </svg>

              {formatCount(contributors)}
            </span>
          </div>

          <span>{formatRelativeTime(lastActivityAt)}</span>
        </div>
      </div>
    </Link>
  );
}