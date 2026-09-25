"use client";

import { useState } from "react";
import { Copy, Check, BadgeCheck } from "lucide-react";

/**
 * Embeddable README Badge (wow-feature, 2026-09-24) — the maintainer-facing
 * half of it. The backend endpoint (`GET /api/v1/projects/{slug}/badge.svg`)
 * existed with no way for a maintainer to actually find or use it; this
 * panel is that missing affordance, following the same
 * copy-to-clipboard pattern as {@code ShareProfileButton}.
 *
 * The badge URL is built from `window.location.origin` rather than a env
 * var, same reasoning as `ShareProfileButton`'s use of `window.location.href`:
 * whatever domain is currently serving the page is the one GitHub needs to
 * fetch the image from, and `/api/v1/*` is already rewritten to the real
 * backend for every environment (see next.config.js), so this works
 * unchanged in dev and in production. Read straight from `window` during
 * render (guarded, since this also runs once server-side before hydration)
 * rather than mirrored into state — there's no external subscription here,
 * just a value read.
 */
export default function EmbedBadgePanel({ project, isMaintainer }) {
  const [copied, setCopied] = useState(false);

  if (!isMaintainer || !project?.slug) return null;
  if (typeof window === "undefined") return null;

  const origin = window.location.origin;
  const badgeUrl = `${origin}/api/v1/projects/${project.slug}/badge.svg`;
  const markdownSnippet = `[![Code Masters](${badgeUrl})](${origin}/projects/${project.slug})`;

  async function handleCopy() {
    try {
      await navigator.clipboard.writeText(markdownSnippet);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // Clipboard access can be denied/unavailable; failing silently is
      // fine here, the snippet is still visible to copy by hand.
    }
  }

  return (
    <section className="cm-glass" style={{ borderRadius: "24px", padding: "20px 24px" }}>
      <div style={{ display: "flex", alignItems: "center", gap: "8px", marginBottom: "6px" }}>
        <BadgeCheck size={16} strokeWidth={2} color="var(--cm-lime-text)" aria-hidden="true" />
        <h2 style={{ fontSize: "15px", fontWeight: 700, margin: 0, color: "var(--cm-text-primary)" }}>
          Embed on GitHub
        </h2>
      </div>
      <p style={{ fontSize: "12.5px", color: "var(--cm-text-secondary)", margin: "0 0 16px", lineHeight: 1.5 }}>
        Paste this into your repository&apos;s README to show your real, live maintainer count.
      </p>

      <div style={{ display: "flex", alignItems: "center", gap: "12px", marginBottom: "14px" }}>
        {/* eslint-disable-next-line @next/next/no-img-element -- external, dynamically-generated SVG, not a Next-optimizable static asset */}
        <img src={badgeUrl} alt={`${project.name} badge preview`} width={110} height={20} />
      </div>

      <div
        style={{
          display: "flex",
          alignItems: "center",
          gap: "8px",
          borderRadius: "10px",
          border: "0.5px solid var(--cm-border)",
          background: "var(--cm-surface)",
          padding: "9px 12px",
        }}
      >
        <code
          style={{
            flex: 1,
            fontSize: "12px",
            color: "var(--cm-text-secondary)",
            overflowX: "auto",
            whiteSpace: "nowrap",
            fontFamily: "ui-monospace, SFMono-Regular, Menlo, monospace",
          }}
        >
          {markdownSnippet}
        </code>
        <button
          type="button"
          onClick={handleCopy}
          style={{
            display: "inline-flex",
            alignItems: "center",
            gap: "6px",
            fontSize: "12.5px",
            fontWeight: 600,
            color: copied ? "var(--cm-lime-text)" : "var(--cm-text-secondary)",
            background: "var(--cm-surface-alt)",
            border: "none",
            borderRadius: "999px",
            padding: "7px 14px",
            cursor: "pointer",
            flexShrink: 0,
          }}
        >
          {copied ? (
            <>
              <Check size={13} strokeWidth={2} aria-hidden="true" />
              Copied
            </>
          ) : (
            <>
              <Copy size={13} strokeWidth={2} aria-hidden="true" />
              Copy
            </>
          )}
        </button>
      </div>
    </section>
  );
}
