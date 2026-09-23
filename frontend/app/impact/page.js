"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { GitMerge, FolderGit2, Users, Clock3 } from "lucide-react";
import { getStats } from "../../lib/api";

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
            <p style={{ fontSize: "13px", color: "var(--cm-text-muted)" }}>Couldn't load impact numbers right now.</p>
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