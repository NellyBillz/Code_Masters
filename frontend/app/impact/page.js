"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { GitMerge, FolderGit2, Users, Clock3, Star, Code2, Trophy } from "lucide-react";
import { getStats } from "../../lib/api";

/** Dataviz skill's validated default categorical palette, slots 1-6 (see app/globals.css). */
const CHART_COLORS = [
  "var(--cm-chart-1)",
  "var(--cm-chart-2)",
  "var(--cm-chart-3)",
  "var(--cm-chart-4)",
  "var(--cm-chart-5)",
  "var(--cm-chart-6)",
];

function formatTimestamp(isoString) {
  if (!isoString) return "";
  const date = new Date(isoString);
  if (Number.isNaN(date.getTime())) return "";
  return date.toLocaleString(undefined, { dateStyle: "medium", timeStyle: "short" });
}

const SUPPORTING_STATS = [
  { key: "publishedProjects", label: "Published projects", Icon: FolderGit2 },
  { key: "activeProjectsAcceptingContributions", label: "Accepting contributions right now", Icon: FolderGit2 },
  { key: "totalContributorsEngaged", label: "Contributors engaged", Icon: Users },
  { key: "totalActiveClaims", label: "Issues actively being worked on", Icon: Clock3 },
  { key: "totalStars", label: "GitHub stars across the ecosystem", Icon: Star },
];

export default function ImpactPage() {
  const [stats, setStats] = useState(null);
  const [error, setError] = useState(false);

  useEffect(() => {
    let cancelled = false;
    getStats()
      .then((result) => !cancelled && setStats(result))
      .catch(() => !cancelled && setError(true));
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div>
      <section className="cm-glass" style={{ borderRadius: "24px", padding: "40px 28px", marginBottom: "20px", textAlign: "center" }}>
        <h1 style={{ fontSize: "22px", fontWeight: 700, margin: 0, color: "var(--cm-text-primary)" }}>
          Platform impact
        </h1>
        <p style={{ fontSize: "13.5px", color: "var(--cm-text-secondary)", margin: "8px auto 0", maxWidth: "480px" }}>
          Not a pitch, a live number. This only goes up when a contribution is actually verified,
          by a GitHub merge or a maintainer confirming it by hand.
        </p>

        <div style={{ margin: "28px 0 4px" }}>
          {stats ? (
            <>
              <p style={{ fontSize: "56px", fontWeight: 800, margin: 0, color: "var(--cm-lime-text)", lineHeight: 1 }}>
                {stats.totalContributionsCompleted.toLocaleString()}
              </p>
              <p style={{ fontSize: "13px", fontWeight: 600, color: "var(--cm-text-primary)", margin: "6px 0 0", display: "flex", alignItems: "center", justifyContent: "center", gap: "6px" }}>
                <GitMerge size={14} strokeWidth={2} aria-hidden="true" />
                Contributions completed
              </p>
            </>
          ) : error ? (
            <p style={{ fontSize: "13px", color: "var(--cm-text-muted)" }}>Couldn&apos;t load impact numbers right now.</p>
          ) : (
            <p style={{ fontSize: "13px", color: "var(--cm-text-muted)" }}>Loading…</p>
          )}
        </div>

        {stats?.generatedAt && (
          <p style={{ fontSize: "11px", color: "var(--cm-text-muted)", margin: "10px 0 0" }}>
            As of {formatTimestamp(stats.generatedAt)}
          </p>
        )}
      </section>

      {stats && (
        <section style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))", gap: "14px", marginBottom: "20px" }}>
          {SUPPORTING_STATS.map(({ key, label, Icon }) => (
            <div key={key} className="cm-glass" style={{ borderRadius: "18px", padding: "18px", textAlign: "center" }}>
              <div
                style={{
                  display: "inline-flex",
                  alignItems: "center",
                  justifyContent: "center",
                  width: "30px",
                  height: "30px",
                  borderRadius: "999px",
                  background: "var(--cm-surface-alt)",
                  color: "var(--cm-text-primary)",
                  marginBottom: "10px",
                }}
              >
                <Icon size={14} strokeWidth={2} aria-hidden="true" />
              </div>
              <p style={{ fontSize: "24px", fontWeight: 700, margin: 0, color: "var(--cm-text-primary)" }}>
                {stats[key].toLocaleString()}
              </p>
              <p style={{ fontSize: "12px", color: "var(--cm-text-secondary)", margin: "4px 0 0" }}>{label}</p>
            </div>
          ))}
        </section>
      )}

      {stats && <LanguageBreakdownChart entries={stats.languageBreakdown} />}
      {stats && <TopProjectsList projects={stats.topProjects} />}

      <section className="cm-glass" style={{ borderRadius: "20px", padding: "24px", textAlign: "center" }}>
        <p style={{ fontSize: "14px", fontWeight: 600, color: "var(--cm-text-primary)", margin: "0 0 14px" }}>
          Want to move that headline number?
        </p>
        <div style={{ display: "flex", justifyContent: "center", gap: "12px", flexWrap: "wrap" }}>
          <Link
            href="/contribute"
            style={{
              fontSize: "13px",
              fontWeight: 700,
              padding: "10px 20px",
              borderRadius: "999px",
              background: "var(--cm-lime)",
              color: "#0A0A0A",
              textDecoration: "none",
            }}
          >
            Start contributing
          </Link>
          <Link
            href="/community"
            style={{
              fontSize: "13px",
              fontWeight: 700,
              padding: "10px 20px",
              borderRadius: "999px",
              background: "var(--cm-surface-alt)",
              color: "var(--cm-text-primary)",
              textDecoration: "none",
            }}
          >
            See the community
          </Link>
        </div>
      </section>
    </div>
  );
}

/**
 * Horizontal bar chart, part-to-whole across published projects' primary
 * languages (dataviz skill: part-to-whole -> horizontal bars for long-named
 * categories). Categorical color per language from the validated default
 * palette (app/globals.css --cm-chart-1..6); a 7th+ language folds into
 * "Other" rather than generating a new hue. Direct labels (name + count)
 * carry identity, so no separate legend box is needed.
 */
function LanguageBreakdownChart({ entries }) {
  if (!entries || entries.length === 0) return null;

  const top = entries.slice(0, CHART_COLORS.length);
  const rest = entries.slice(CHART_COLORS.length);
  const otherCount = rest.reduce((sum, entry) => sum + entry.projectCount, 0);
  const rows = otherCount > 0 ? [...top, { language: "Other", projectCount: otherCount }] : top;
  const max = Math.max(...rows.map((row) => row.projectCount));

  return (
    <section className="cm-glass" style={{ borderRadius: "20px", padding: "20px 24px", marginBottom: "20px" }}>
      <h2 style={{ display: "flex", alignItems: "center", gap: "8px", fontSize: "14px", fontWeight: 700, margin: "0 0 16px", color: "var(--cm-text-primary)" }}>
        <Code2 size={15} strokeWidth={2} aria-hidden="true" />
        Languages across published projects
      </h2>
      <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
        {rows.map((row, index) => {
          const widthPct = max > 0 ? Math.max((row.projectCount / max) * 100, 6) : 0;
          const color = index < CHART_COLORS.length ? CHART_COLORS[index] : "var(--cm-text-muted)";
          return (
            <div
              key={row.language}
              title={`${row.language}: ${row.projectCount} project${row.projectCount === 1 ? "" : "s"}`}
              style={{ display: "grid", gridTemplateColumns: "110px 1fr 28px", alignItems: "center", gap: "10px" }}
            >
              <span
                style={{
                  fontSize: "12.5px",
                  fontWeight: 600,
                  color: "var(--cm-text-primary)",
                  overflow: "hidden",
                  textOverflow: "ellipsis",
                  whiteSpace: "nowrap",
                }}
              >
                {row.language}
              </span>
              <div style={{ position: "relative", height: "16px", background: "var(--cm-surface-alt)", borderRadius: "4px" }}>
                <div
                  style={{
                    position: "absolute",
                    inset: 0,
                    width: `${widthPct}%`,
                    background: color,
                    borderRadius: "0 4px 4px 0",
                  }}
                />
              </div>
              <span style={{ fontSize: "12px", fontWeight: 600, color: "var(--cm-text-secondary)", textAlign: "right" }}>
                {row.projectCount}
              </span>
            </div>
          );
        })}
      </div>
    </section>
  );
}

/** Simple ranked list, not a chart — the data's job here is "which projects," not magnitude comparison. */
function TopProjectsList({ projects }) {
  if (!projects || projects.length === 0) return null;

  return (
    <section className="cm-glass" style={{ borderRadius: "20px", padding: "20px 24px", marginBottom: "20px" }}>
      <h2 style={{ display: "flex", alignItems: "center", gap: "8px", fontSize: "14px", fontWeight: 700, margin: "0 0 16px", color: "var(--cm-text-primary)" }}>
        <Trophy size={15} strokeWidth={2} aria-hidden="true" />
        Most-starred projects
      </h2>
      <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
        {projects.map((project, index) => (
          <Link
            key={project.id}
            href={`/projects/${project.id}`}
            style={{
              display: "flex",
              alignItems: "center",
              justifyContent: "space-between",
              gap: "10px",
              padding: "10px 12px",
              borderRadius: "12px",
              background: "var(--cm-surface-alt)",
              textDecoration: "none",
            }}
          >
            <span style={{ display: "flex", alignItems: "center", gap: "10px", minWidth: 0 }}>
              <span style={{ fontSize: "12px", fontWeight: 700, color: "var(--cm-text-muted)", width: "16px", flexShrink: 0 }}>
                {index + 1}
              </span>
              <span
                style={{
                  fontSize: "13px",
                  fontWeight: 600,
                  color: "var(--cm-text-primary)",
                  overflow: "hidden",
                  textOverflow: "ellipsis",
                  whiteSpace: "nowrap",
                }}
              >
                {project.name}
              </span>
              {project.primaryLanguage && (
                <span style={{ fontSize: "11px", color: "var(--cm-text-muted)", flexShrink: 0 }}>
                  {project.primaryLanguage}
                </span>
              )}
            </span>
            <span style={{ display: "flex", alignItems: "center", gap: "4px", fontSize: "12.5px", fontWeight: 700, color: "var(--cm-text-secondary)", flexShrink: 0 }}>
              <Star size={12} strokeWidth={2} aria-hidden="true" fill="currentColor" />
              {project.stars.toLocaleString()}
            </span>
          </Link>
        ))}
      </div>
    </section>
  );
}