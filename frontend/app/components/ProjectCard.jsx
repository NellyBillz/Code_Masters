import Link from "next/link";
import { Star, Users, ArrowUpRight, BadgeCheck, AlertTriangle } from "lucide-react";
import styles from "./ProjectCard.module.css";

/**
 * A project this thin on maintainers relative to its popularity is fragile
 * infrastructure — one person leaving stalls everything (security-by-design
 * signal, Project Health wow-feature, 2026-09-24). 20 stars is a deliberately
 * low bar given this platform's real data currently ranges ~0-150 stars;
 * documented here so the number is never just eyeballed again.
 */
function isThinlyMaintained(maintainerCount, stars) {
  return maintainerCount <= 1 && stars >= 20;
}

const LANGUAGE_COLORS = {
  JavaScript: "#F48C3C",
  TypeScript: "#2DD4BF",
  Python: "#C8FF64",
  Go: "#2DD4BF",
  Rust: "#F48C3C",
  Java: "#F48C3C",
  "C++": "#2DD4BF",
  Ruby: "#F48C3C",
};

// connection describes *what kind* of African link a project has,
// distinct from `verified`, which describes whether that claim has been
// confirmed. Kept as a short label rather than a loud badge so it doesn't
// compete visually with the verified pill.
const CONNECTION_LABELS = {
  south_african: "South African",
  africa_focused: "Africa-focused",
  africa_led: "Africa-led",
  community_verified: "Community verified",
};

function formatRelativeTime(isoString) {
  if (!isoString) return "unknown";

  const date = new Date(isoString);
  if (Number.isNaN(date.getTime())) return "unknown";

  const diffDays = Math.floor((Date.now() - date.getTime()) / (1000 * 60 * 60 * 24));

  if (diffDays <= 0) return "today";
  if (diffDays === 1) return "1 day ago";
  if (diffDays < 30) return `${diffDays} days ago`;

  const diffMonths = Math.floor(diffDays / 30);
  if (diffMonths < 12) return `${diffMonths} month${diffMonths === 1 ? "" : "s"} ago`;

  const diffYears = Math.floor(diffMonths / 12);
  return `${diffYears} year${diffYears === 1 ? "" : "s"} ago`;
}

function formatCount(n) {
  if (typeof n !== "number" || Number.isNaN(n)) return "0";
  if (n >= 1000) return `${(n / 1000).toFixed(n % 1000 >= 100 ? 1 : 0)}k`;
  return String(n);
}

export default function ProjectCard({ project }) {
  const {
    id,
    name = "Untitled project",
    description = "",
    primaryLanguage,
    tags = [],
    stars = 0,
    maintainerCount = 0,
    lastActivityAt,
    connection,
    verified = false,
  } = project || {};

  const languageColor = LANGUAGE_COLORS[primaryLanguage] || "var(--cm-teal)";
  const connectionLabel = CONNECTION_LABELS[connection];
  const thinlyMaintained = isThinlyMaintained(maintainerCount, stars);

  return (
    <Link href={`/projects/${id}`} className={`${styles.card} cm-glass`}>
      <span aria-hidden="true" className={styles.blob} style={{ background: languageColor }} />

      <div className={styles.topRow}>
        <span className={styles.starPill} title={`${stars} stars`}>
          <Star size={11} strokeWidth={0} fill="currentColor" aria-hidden="true" />
          {formatCount(stars)}
        </span>

        <div className={styles.topRowRight}>
          {connectionLabel && (
            <span className={styles.connectionPill} title={`Connection: ${connectionLabel}`}>
              {connectionLabel}
            </span>
          )}

          {verified && (
            <span className={styles.verifiedPill} title="Verified project">
              <BadgeCheck size={12} strokeWidth={2} aria-hidden="true" />
              Verified
            </span>
          )}

          {thinlyMaintained && (
            <span
              className={styles.thinMaintainerPill}
              title={`Only ${maintainerCount} maintainer${maintainerCount === 1 ? "" : "s"} for ${stars} stars — consider inviting a co-maintainer`}
            >
              <AlertTriangle size={11} strokeWidth={2} aria-hidden="true" />
              Needs maintainers
            </span>
          )}
        </div>
      </div>

      <h3 className={styles.name}>{name}</h3>
      {description && <p className={styles.description}>{description}</p>}

      {tags.length > 0 && (
        <div className={styles.tags}>
          {tags.slice(0, 3).map((tag) => (
            <span key={tag} className={styles.tag}>
              {tag}
            </span>
          ))}
        </div>
      )}

      <div className={styles.footer}>
        <div className={styles.footerStats}>
          {primaryLanguage && (
            <span className={styles.stat}>
              <span className={styles.languageDot} style={{ backgroundColor: languageColor }} aria-hidden="true" />
              {primaryLanguage}
            </span>
          )}
          <span className={styles.stat} title={`${maintainerCount} maintainer${maintainerCount === 1 ? "" : "s"}`}>
            <Users size={12} strokeWidth={1.8} aria-hidden="true" />
            {formatCount(maintainerCount)}
          </span>
          <span className={styles.stat}>{formatRelativeTime(lastActivityAt)}</span>
        </div>

        <span className={styles.arrowButton} aria-hidden="true">
          <ArrowUpRight size={15} strokeWidth={2} />
        </span>
      </div>
    </Link>
  );
}
