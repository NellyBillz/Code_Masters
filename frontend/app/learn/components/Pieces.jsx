"use client";

// Small shared UI pieces, ported near-verbatim from the original
// Learn section "SMALL UI PIECES" section. Only the palette source changed
// (T now resolves to Code Masters cm-tokens, see ../theme.js).

import { T } from "../theme";
import { levelFromXp, xpForLevel, titleForLevel } from "../data";

export function XPBar({ xp }) {
  const lvl = levelFromXp(xp);
  const lo = xpForLevel(lvl),
    hi = xpForLevel(lvl + 1);
  const pct = Math.min(100, Math.round(((xp - lo) / (hi - lo)) * 100));
  return (
    <div>
      <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 5 }}>
        <span className="px" style={{ fontSize: 11, color: T.amber }}>
          LV {lvl} · {titleForLevel(lvl)}
        </span>
        <span className="mono" style={{ fontSize: 11, color: T.dim }}>
          {xp - lo} / {hi - lo} XP
        </span>
      </div>
      <div style={{ height: 12, background: T.ink, border: `1px solid ${T.line}`, borderRadius: 6, overflow: "hidden" }}>
        <div
          className="xpfill"
          style={{
            width: pct + "%",
            height: "100%",
            borderRadius: 5,
            background: `repeating-linear-gradient(90deg, ${T.amber} 0 8px, color-mix(in srgb, ${T.amber} 55%, white) 8px 16px)`,
          }}
        />
      </div>
    </div>
  );
}

export function Pill({ children, color = T.dim, bg = "transparent" }) {
  return (
    <span
      className="mono"
      style={{
        fontSize: 11,
        color,
        background: bg,
        border: `1px solid color-mix(in srgb, ${color} 30%, transparent)`,
        borderRadius: 999,
        padding: "3px 10px",
        display: "inline-block",
      }}
    >
      {children}
    </span>
  );
}

export function Toast({ toast }) {
  if (!toast) return null;
  return (
    <div
      className="pop"
      style={{
        position: "fixed",
        bottom: 24,
        left: "50%",
        transform: "translateX(-50%)",
        zIndex: 60,
        background: T.panel,
        border: `1px solid ${toast.color || T.amber}`,
        borderRadius: 16,
        padding: "12px 20px",
        boxShadow: "0 8px 30px rgba(0,0,0,.35)",
        display: "flex",
        gap: 10,
        alignItems: "center",
        backdropFilter: "var(--cm-card-blur)",
      }}
    >
      <span style={{ fontSize: 20 }}>{toast.icon}</span>
      <div>
        <div className="px" style={{ fontSize: 11, color: toast.color || T.amber }}>
          {toast.title}
        </div>
        {toast.sub && <div style={{ fontSize: 13, color: T.dim }}>{toast.sub}</div>}
      </div>
    </div>
  );
}

export function CodeBlock({ v }) {
  return (
    <pre
      className="mono"
      style={{
        background: "#070A14",
        border: `1px solid ${T.line}`,
        borderRadius: 12,
        padding: 14,
        fontSize: 13,
        lineHeight: 1.6,
        overflowX: "auto",
        color: "#3DE8D6",
        whiteSpace: "pre-wrap",
      }}
    >
      {v}
    </pre>
  );
}

export function Ring({ pct, color, size = 44 }) {
  const r = 17,
    c = 2 * Math.PI * r;
  return (
    <svg width={size} height={size} viewBox="0 0 44 44" aria-label={pct + "% complete"}>
      <circle cx="22" cy="22" r={r} fill="none" stroke={T.line} strokeWidth="4" />
      <circle
        cx="22"
        cy="22"
        r={r}
        fill="none"
        stroke={color}
        strokeWidth="4"
        strokeLinecap="round"
        strokeDasharray={`${(pct / 100) * c} ${c}`}
        transform="rotate(-90 22 22)"
      />
      <text x="22" y="26" textAnchor="middle" fill={T.text} fontSize="11" fontFamily="JetBrains Mono">
        {pct}%
      </text>
    </svg>
  );
}
