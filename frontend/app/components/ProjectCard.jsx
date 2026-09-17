/**
 * ProjectCard — FE-01.7
 *
 * Renders a single Project. Deliberately takes ONE prop, `project`,
 * shaped exactly per spec so swapping mock data for API-01's real
 * response later requires no prop reshaping:
 *
 * Project = {
 *   name: string
 *   description: string
 *   primaryLanguage: string
 *   category: string
 *   tags: string[]
 *   stars: number
 *   contributors: number
 *   lastActivityAt: string          // ISO 8601 date
 *   connectionStatus: "verified" | "unverified"
 *   hasBeginnerFriendlyIssues: boolean
 * }
 *
 * No other fields are read. If a field is missing/undefined, the card
 * renders a neutral fallback rather than throwing, so partially-mocked
 * data during FE-01.6 wiring doesn't break the page.
 */

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
  if (diffMonths < 12) return `${diffMonths} month${diffMonths === 1 ? "" : "s"} ago`;
  const diffYears = Math.floor(diffMonths / 12);
  return `${diffYears} year${diffYears === 1 ? "" : "s"} ago`;
}

function formatCount(n) {
  if (typeof n !== "number" || Number.isNaN(n)) return "0";
  if (n >= 1000) return `${(n / 1000).toFixed(n % 1000 >= 100 ? 1 : 0)}k`;
  return String(n);
}

function ConnectionBadge({ status }) {
  const verified = status === "verified";
  return (
    <span
      className={
        "inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium " +
        (verified
          ? "bg-emerald-50 text-emerald-700 ring-1 ring-inset ring-emerald-200"
          : "bg-slate-100 text-slate-500 ring-1 ring-inset ring-slate-200")
      }
      title={verified ? "Connection verified" : "Connection not verified"}
    >
      <svg width="10" height="10" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
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
      className={
        "inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium " +
        (hasBeginnerFriendlyIssues
          ? "bg-violet-50 text-violet-700 ring-1 ring-inset ring-violet-200"
          : "bg-white text-slate-400 ring-1 ring-inset ring-slate-200")
      }
      title={
        hasBeginnerFriendlyIssues
          ? "Has open beginner-friendly issues"
          : "No beginner-friendly issues right now"
      }
    >
      <svg width="10" height="10" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
        <path d="M10 2l2.2 4.9 5.3.6-4 3.7 1.1 5.3L10 13.9l-4.6 2.6 1.1-5.3-4-3.7 5.3-.6L10 2z" />
      </svg>
      {hasBeginnerFriendlyIssues ? "Beginner friendly" : "No beginner issues"}
    </span>
  );
}

export default function ProjectCard({ project }) {
  const {
    name = "Untitled project",
    description = "",
    primaryLanguage,
    category,
    tags = [],
    stars = 0,
    contributors = 0,
    lastActivityAt,
    connectionStatus,
    hasBeginnerFriendlyIssues = false,
  } = project || {};

  const languageColor = LANGUAGE_COLORS[primaryLanguage] || "#94a3b8";

  return (
    <div className="flex h-full flex-col rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
      {/* Header: name + connection badge */}
      <div className="mb-1.5 flex items-start justify-between gap-2">
        <h3 className="text-base font-semibold leading-snug text-slate-900">{name}</h3>
        <ConnectionBadge status={connectionStatus} />
      </div>

      {/* Category + language */}
      <div className="mb-2 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-slate-500">
        {category && <span>{category}</span>}
        {primaryLanguage && (
          <span className="inline-flex items-center gap-1.5">
            <span
              className="inline-block h-2 w-2 rounded-full"
              style={{ backgroundColor: languageColor }}
              aria-hidden="true"
            />
            {primaryLanguage}
          </span>
        )}
      </div>

      {/* Description */}
      {description && (
        <p className="mb-3 line-clamp-2 text-sm leading-relaxed text-slate-600">{description}</p>
      )}

      {/* Tags */}
      {tags.length > 0 && (
        <div className="mb-3 flex flex-wrap gap-1.5">
          {tags.map((tag) => (
            <span
              key={tag}
              className="rounded-md bg-slate-100 px-2 py-0.5 text-xs text-slate-600"
            >
              {tag}
            </span>
          ))}
        </div>
      )}

      <div className="mt-auto pt-2">
        {/* Beginner-friendly badge, own row so it's always visible for review */}
        <div className="mb-3">
          <BeginnerBadge hasBeginnerFriendlyIssues={hasBeginnerFriendlyIssues} />
        </div>

        {/* Stats footer */}
        <div className="flex items-center justify-between border-t border-slate-100 pt-3 text-xs text-slate-500">
          <div className="flex items-center gap-3">
            <span className="inline-flex items-center gap-1" title="Stars">
              <svg width="12" height="12" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                <path d="M10 2l2.2 4.9 5.3.6-4 3.7 1.1 5.3L10 13.9l-4.6 2.6 1.1-5.3-4-3.7 5.3-.6L10 2z" />
              </svg>
              {formatCount(stars)}
            </span>
            <span className="inline-flex items-center gap-1" title="Contributors">
              <svg width="12" height="12" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
                <path d="M10 10a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7zM3.5 17a6.5 6.5 0 0 1 13 0 1 1 0 0 1-1 1h-11a1 1 0 0 1-1-1z" />
              </svg>
              {formatCount(contributors)}
            </span>
          </div>
          <span>{formatRelativeTime(lastActivityAt)}</span>
        </div>
      </div>
    </div>
  );
}
