export function formatRelativeTime(isoString) {
  if (!isoString) return null;

  const date = new Date(isoString);
  if (Number.isNaN(date.getTime())) return null;

  const diffMs = Date.now() - date.getTime();
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

  if (diffDays <= 0) return "Active today";
  if (diffDays === 1) return "Active 1 day ago";
  if (diffDays < 30) return `Active ${diffDays} days ago`;

  const diffMonths = Math.floor(diffDays / 30);
  if (diffMonths < 12) {
    return `Active ${diffMonths} month${diffMonths === 1 ? "" : "s"} ago`;
  }

  const diffYears = Math.floor(diffMonths / 12);
  return `Active ${diffYears} year${diffYears === 1 ? "" : "s"} ago`;
}

export function formatCount(n) {
  if (typeof n !== "number" || Number.isNaN(n)) return "0";
  if (n >= 1000) {
    return `${(n / 1000).toFixed(n % 1000 >= 100 ? 1 : 0)}k`;
  }
  return String(n);
}

export function formatDate(isoString) {
  if (!isoString) return null;
  const date = new Date(isoString);
  if (Number.isNaN(date.getTime())) return null;
  return date.toLocaleDateString(undefined, { year: "numeric", month: "short", day: "numeric" });
}

export function formatDateTime(isoString) {
  if (!isoString) return null;
  const date = new Date(isoString);
  if (Number.isNaN(date.getTime())) return null;
  return date.toLocaleString(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "numeric",
    minute: "2-digit",
  });
}

export const LANGUAGE_COLORS = {
  JavaScript: "#eab308",
  TypeScript: "#3b82f6",
  Python: "#22c55e",
  Go: "#06b6d4",
  Rust: "#f97316",
  Java: "#f43f5e",
  "C++": "#a855f7",
  Ruby: "#dc2626",
  PHP: "#818cf8",
  Kotlin: "#f472b6",
};
