"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { Target, ArrowUpRight, ArrowRight } from "lucide-react";
import { getRecommendedIssues } from "../../lib/api";

/**
 * "Recommended for you" — Skill-Matching Recommendation Engine (wow-feature,
 * 2026-09-24). Every card shows the plain-language reasons it matched
 * (never a combined score) — the deliberate, explainable design choice this
 * feature is built around. Renders nothing at all (not even an empty state)
 * when the caller isn't signed in or has no recommendations yet, so it
 * never competes for space with the rest of the homepage on a cold profile.
 *
 * @param {boolean} signedIn
 * @param {number} [limit] Caps how many cards render, with a "See all" link
 *   to /recommended for the rest — the homepage passes 3 so this doesn't
 *   push "Featured projects" down the page; the dedicated /recommended page
 *   passes nothing and shows the full list.
 * @param {boolean} [showEmptyState] When true, an empty/error result shows a
 *   real message instead of rendering nothing — for the dedicated
 *   /recommended page, where a blank result would be confusing for someone
 *   who navigated there on purpose. The homepage (a secondary, capped
 *   teaser) leaves this false so it never competes for space on a cold profile.
 */
export default function RecommendedIssues({ signedIn, limit, showEmptyState = false }) {
  const [recommendations, setRecommendations] = useState(null);
  const [error, setError] = useState(false);

  useEffect(() => {
    if (!signedIn) return;
    let cancelled = false;

    getRecommendedIssues()
      .then((result) => !cancelled && setRecommendations(result || []))
      .catch(() => !cancelled && setError(true));

    return () => {
      cancelled = true;
    };
  }, [signedIn]);

  if (!signedIn) return null;

  const isEmpty = recommendations !== null && recommendations.length === 0;
  if (!showEmptyState && (error || isEmpty)) return null;

  const shown = limit ? recommendations?.slice(0, limit) : recommendations;
  const hasMore = limit && recommendations && recommendations.length > limit;

  return (
    <section style={{ marginBottom: "28px" }}>
      <h2
        style={{
          display: "flex",
          alignItems: "center",
          gap: "8px",
          fontSize: "16px",
          fontWeight: 700,
          margin: "0 0 14px",
          color: "var(--cm-text-primary)",
        }}
      >
        <Target size={16} strokeWidth={2} color="var(--cm-lime-text)" aria-hidden="true" />
        Recommended for you
      </h2>

      {recommendations === null && !error ? (
        <p style={{ fontSize: "12.5px", color: "var(--cm-text-muted)" }}>Loading…</p>
      ) : error ? (
        <p style={{ fontSize: "12.5px", color: "var(--cm-text-muted)" }}>
          Couldn&apos;t load recommendations right now.
        </p>
      ) : isEmpty ? (
        <p style={{ fontSize: "12.5px", color: "var(--cm-text-muted)" }}>
          No matches yet — add skills to your{" "}
          <Link href="/profile" style={{ color: "var(--cm-lime-text)", fontWeight: 600 }}>
            profile
          </Link>{" "}
          or check back once more issues match what you know.
        </p>
      ) : (
        <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
          {shown.map((rec) => (
            <RecommendationCard key={rec.issue.id} recommendation={rec} />
          ))}

          {hasMore && (
            <Link
              href="/recommended"
              style={{
                display: "inline-flex",
                alignItems: "center",
                gap: "6px",
                alignSelf: "flex-start",
                fontSize: "12.5px",
                fontWeight: 600,
                color: "var(--cm-lime-text)",
                marginTop: "2px",
              }}
            >
              See all {recommendations.length} recommendations
              <ArrowRight size={13} strokeWidth={2} aria-hidden="true" />
            </Link>
          )}
        </div>
      )}
    </section>
  );
}

function RecommendationCard({ recommendation }) {
  const { issue, project, reasons } = recommendation;

  return (
    <Link
      href={`/issues/${issue.id}`}
      className="cm-glass"
      style={{ display: "block", borderRadius: "20px", padding: "16px 20px" }}
    >
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: "12px" }}>
        <div style={{ minWidth: 0 }}>
          <p style={{ fontSize: "11.5px", fontWeight: 600, color: "var(--cm-text-muted)", margin: "0 0 4px" }}>
            {project?.name}
          </p>
          <p style={{ fontSize: "14px", fontWeight: 600, margin: 0, color: "var(--cm-text-primary)" }}>
            {issue.title}
          </p>

          {reasons?.length > 0 && (
            <div style={{ display: "flex", flexWrap: "wrap", gap: "6px", marginTop: "8px" }}>
              {reasons.map((reason) => (
                <span
                  key={reason}
                  style={{
                    fontSize: "10.5px",
                    fontWeight: 600,
                    padding: "3px 9px",
                    borderRadius: "999px",
                    background: "var(--cm-lime-soft)",
                    color: "var(--cm-lime-text)",
                  }}
                >
                  {reason}
                </span>
              ))}
            </div>
          )}
        </div>
        <ArrowUpRight size={16} strokeWidth={2} color="var(--cm-text-muted)" aria-hidden="true" style={{ flexShrink: 0 }} />
      </div>
    </Link>
  );
}
