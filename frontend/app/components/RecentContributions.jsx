"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { GitMerge, UserCheck, Compass } from "lucide-react";
import { getUserContributions } from "../../lib/api";

const COMPLETION_LABELS = {
  github_verified: { label: "GitHub merge verified", icon: GitMerge, bg: "var(--cm-lime-soft)", text: "var(--cm-lime-text)" },
  maintainer_confirmed: { label: "Maintainer confirmed", icon: UserCheck, bg: "var(--cm-orange-soft)", text: "var(--cm-orange-text)" },
};

function formatDate(isoString) {
  if (!isoString) return "";
  const date = new Date(isoString);
  if (Number.isNaN(date.getTime())) return "";
  return date.toLocaleDateString(undefined, { day: "numeric", month: "short", year: "numeric" });
}

/**
 * A developer's verified contribution history (GET /users/{username}/contributions),
 * most recent first. Used on both /profile (own) and /users/{username} (public).
 *
 * @param {string} username
 * @param {boolean} [ownProfile] Swaps the empty-state copy/CTA for "you" vs "this developer".
 */
export default function RecentContributions({ username, ownProfile = false }) {
  const [contributions, setContributions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError("");
      try {
        const result = await getUserContributions(username, { size: 5 });
        if (!cancelled) setContributions(result.items || []);
      } catch (err) {
        if (!cancelled) setError(err.message || "Failed to load contributions.");
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, [username]);

  return (
    <section className="cm-glass" style={{ borderRadius: "24px", padding: "8px" }}>
      <h2 style={{ fontSize: "15px", fontWeight: 700, margin: "12px 16px", color: "var(--cm-text-primary)" }}>
        Recent contributions
      </h2>

      {loading && (
        <p style={{ fontSize: "12.5px", color: "var(--cm-text-muted)", margin: "0 16px 16px" }}>Loading…</p>
      )}

      {!loading && error && (
        <p role="alert" style={{ fontSize: "12.5px", color: "var(--cm-orange-text)", margin: "0 16px 16px" }}>
          {error}
        </p>
      )}

      {!loading && !error && contributions.length === 0 && (
        <div style={{ margin: "0 16px 16px" }}>
          <p style={{ fontSize: "12.5px", color: "var(--cm-text-muted)", margin: "0 0 10px" }}>
            {ownProfile
              ? "No verified contributions yet — claim an issue and see it here once it merges."
              : "No verified contributions yet."}
          </p>
          {ownProfile && (
            <Link
              href="/projects"
              style={{
                display: "inline-flex",
                alignItems: "center",
                gap: "6px",
                fontSize: "12.5px",
                fontWeight: 600,
                color: "var(--cm-lime-text)",
              }}
            >
              <Compass size={13} strokeWidth={2} aria-hidden="true" />
              Browse projects to find an issue
            </Link>
          )}
        </div>
      )}

      {!loading && !error && contributions.length > 0 && (
        <div>
          {contributions.map((contribution, index) => {
            const completion = COMPLETION_LABELS[contribution.completionSource] || null;
            const CompletionIcon = completion?.icon;

            return (
              <div
                key={contribution.issue?.id ?? index}
                style={{
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "space-between",
                  gap: "12px",
                  padding: "14px 16px",
                  borderTop: index === 0 ? "none" : "0.5px solid var(--cm-border)",
                  flexWrap: "wrap",
                }}
              >
                <div style={{ minWidth: 0 }}>
                  <Link
                    href={`/issues/${contribution.issue?.id}`}
                    style={{ fontSize: "13px", fontWeight: 600, color: "var(--cm-text-primary)" }}
                  >
                    {contribution.issue?.title ?? `Issue #${contribution.issue?.id}`}
                  </Link>
                  <p style={{ fontSize: "12px", color: "var(--cm-text-secondary)", margin: "3px 0 0" }}>
                    {contribution.project?.name}
                    {contribution.completedAt ? ` · ${formatDate(contribution.completedAt)}` : ""}
                  </p>
                </div>

                {completion && (
                  <span
                    style={{
                      display: "inline-flex",
                      alignItems: "center",
                      gap: "4px",
                      fontSize: "11px",
                      fontWeight: 600,
                      padding: "3px 10px",
                      borderRadius: "999px",
                      background: completion.bg,
                      color: completion.text,
                      flexShrink: 0,
                    }}
                  >
                    {CompletionIcon && <CompletionIcon size={11} strokeWidth={2} aria-hidden="true" />}
                    {completion.label}
                  </span>
                )}
              </div>
            );
          })}
        </div>
      )}
    </section>
  );
}
