"use client";

import { useState } from "react";
import { Copy, Check } from "lucide-react";

/**
 * Copies the current page's URL — the "shareable proof-of-work link" the
 * Contributor Passport wow-feature is built around (2026-09-24). A separate
 * client component since the profile page itself is a Server Component and
 * clipboard access needs the browser.
 */
export default function ShareProfileButton() {
  const [copied, setCopied] = useState(false);

  async function handleClick() {
    try {
      await navigator.clipboard.writeText(window.location.href);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // Clipboard access can be denied/unavailable; failing silently is
      // fine here, there's nothing else useful to do about it.
    }
  }

  return (
    <button
      type="button"
      onClick={handleClick}
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
          Copy link
        </>
      )}
    </button>
  );
}
