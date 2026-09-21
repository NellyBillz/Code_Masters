"use client";

import { useTheme } from "../context/ThemeContext";

/**
 * Four chevrons arranged to form an X — the core Code Masters mark.
 * `tone` controls the chevron color independent of the surrounding
 * treatment, so the same paths work inside either variant below.
 */
function ChevronX({ size = 22, tone = "#FFFFFF" }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path d="M2 3l7.5 8L2 19" stroke={tone} strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M22 3l-7.5 8L22 19" stroke={tone} strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

export default function Logo({ withWordmark = true, size = 34 }) {
  const { theme } = useTheme();
  const isDark = theme === "dark";

  return (
    <span style={{ display: "inline-flex", alignItems: "center", gap: "10px" }}>
      {isDark ? (
        // Dark mode: holographic glass tile — layered borders + a soft
        // multi-color glow standing in for the "glass cube" render.
        <span
          aria-hidden="true"
          style={{
            width: size,
            height: size,
            borderRadius: "9px",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            position: "relative",
            background: "rgba(255,255,255,0.06)",
            border: "1px solid rgba(255,255,255,0.18)",
            boxShadow:
              "0 0 14px rgba(200,255,100,0.25), 0 0 22px rgba(244,140,60,0.15), inset 0 0 10px rgba(45,212,191,0.15)",
          }}
        >
          <ChevronX size={size * 0.55} tone="#F5F5F5" />
        </span>
      ) : (
        // Light mode: solid black circle, white chevrons.
        <span
          aria-hidden="true"
          style={{
            width: size,
            height: size,
            borderRadius: "50%",
            background: "#0A0A0A",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
          }}
        >
          <ChevronX size={size * 0.55} tone="#FFFFFF" />
        </span>
      )}

      {withWordmark && (
        <span
          style={{
            display: "flex",
            flexDirection: "column",
            lineHeight: 1.05,
            fontWeight: 700,
            fontSize: "13px",
            letterSpacing: "0.14em",
            color: "var(--cm-text-primary)",
          }}
        >
          <span>CODE</span>
          <span>MASTERS</span>
        </span>
      )}
    </span>
  );
}
