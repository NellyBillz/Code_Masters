"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { GitMerge, UserCheck, Compass } from "lucide-react";
import { getUserContributions } from "../../lib/api";

const COMPLETION_LABELS = {
  github_verified: { label: "GitHub merge verified", icon: GitMerge, bg: "var(--cm-lime-soft)", text: "var(--cm-lime-text)" },
  maintainer_confirmed: { label: "Maintainer confirmed", icon: UserCheck, bg: "var(--cm-orange-soft)", text: "var(--cm-orange-text)" },
  // Falls back to this if the backend returns Claim.status ("completed")
  // instead of the richer completionSource this component was designed
  // around — see the contract warning in the doc comment below.
  completed: { label: "Completed", icon: GitMerge, bg: "var(--cm-lime-soft)", text: "var(--cm-lime-text)" },
};

function formatDate(isoString) {
  if (!isoString) return "";
  const date = new Date(isoString);
  if (Number.isNaN(date.getTime())) return "";
  return date.toLocaleDateString(undefined, { day: "numeric", month: "short", year: "numeric" });
}

/**
 * A developer's verified contribution history (GET /users/{username}/contributions),
 * most recent first. Used on both /profile (own) and /users/{username) (public).
 *
 * ⚠️ CONTRACT UNVERIFIED — this endpoint does not appear anywhere in
 * codemasters-api-spec.yml (only /users/me and /users/{username} are
 * documented there). The field names this component was originally built
 * against (contribution.issue.title, contribution.project.name,
 * contribution.completionSource, contribution.completedAt) do not match
 * the only related schema that *is* in the spec (Claim: flat issueId, no
 * project, status enum [active, released, completed], no
 * completionSource/completedAt).
 *
 * Either this endpoint returns a richer DTO than Claim (undocumented,
 * same category of drift as SyncJob's id/jobId — see lib/api.js), or this
 * component was built against invented field names before the endpoint
 * existed. Nobody has confirmed which by hitting the live backend yet.
 *
 * The rendering below now falls back to the flat Claim shape (issueId,
 * status, createdAt) when the richer nested fields are absent, so this
 * degrades to something reasonable either way instead of showing
 * "Issue #undefined" with no project name. Once someone pastes the real
 * response JSON, delete this comment (or correct it) and simplify the
 * fallbacks below to match whichever shape is actually real.
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
              ? "No verified contributions yet, claim an issue and see it here once it merges."
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
            // Prefer the richer nested shape this component was designed
            // around; fall back to the flat Claim schema (issueId, status,
            // createdAt) that's actually documented in the spec, so either
            // real response shape renders something sensible.
            const issueId = contribution.issue?.id ?? contribution.issueId;
            const issueTitle = contribution.issue?.title ?? (issueId ? `Issue #${issueId}` : "Untitled issue");
            const projectName = contribution.project?.name ?? null;
            const when = contribution.completedAt ?? contribution.updatedAt ?? contribution.createdAt;

            const completionKey = contribution.completionSource ?? contribution.status;
            const completion = COMPLETION_LABELS[completionKey] || null;
            const CompletionIcon = completion?.icon;

            return (
              <div
                key={contribution.id ?? issueId ?? index}
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
                  {issueId ? (
                    <Link
                      href={`/issues/${issueId}`}
                      style={{ fontSize: "13px", fontWeight: 600, color: "var(--cm-text-primary)" }}
                    >
                      {issueTitle}
                    </Link>
                  ) : (
                    <span style={{ fontSize: "13px", fontWeight: 600, color: "var(--cm-text-primary)" }}>
                      {issueTitle}
                    </span>
                  )}
                  <p style={{ fontSize: "12px", color: "var(--cm-text-secondary)", margin: "3px 0 0" }}>
                    {projectName}
                    {projectName && when ? " · " : ""}
                    {when ? formatDate(when) : ""}
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