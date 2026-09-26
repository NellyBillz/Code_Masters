"use client";

import { useEffect, useState } from "react";
import { T } from "../theme";
import { fetchLocalLeaderboard } from "../engine";
import { Pill } from "../components/Pieces";

/**
 * The original TechAway prototype's leaderboard read/wrote
 * window.storage.list("lb:", true) — a "public" key namespace that only
 * exists inside the Anthropic Artifacts sandbox, and its copy claimed
 * "shared publicly ... visible to everyone." There is no Code Masters
 * backend endpoint that aggregates XP across real users, so shipping that
 * claim here would be a lie. This reads localStorage on the current
 * browser only, and says so plainly.
 */
export default function Leaderboard({ profile }) {
  const [rows, setRows] = useState(null);
  useEffect(() => {
    fetchLocalLeaderboard().then(setRows);
  }, []);
  return (
    <div style={{ maxWidth: 620, margin: "0 auto" }}>
      <h1 className="px" style={{ fontSize: 20, color: T.amber }}>
        🏆 LEADERBOARD
      </h1>
      <p style={{ color: T.dim, fontSize: 13 }}>
        Local to this device and browser only — there&apos;s no shared backend for this yet, so this shows profiles that have used Learn on this browser, not a
        global ranking.
      </p>
      {rows === null ? (
        <div className="mono cursor" style={{ color: T.cyan }}>
          loading standings ▮
        </div>
      ) : rows.length === 0 ? (
        <div className="card" style={{ padding: 24, textAlign: "center", color: T.dim }}>
          No other players on this device yet.
        </div>
      ) : (
        rows.map((r, i) => (
          <div
            key={i}
            className="card"
            style={{
              padding: "12px 16px",
              marginBottom: 8,
              display: "flex",
              alignItems: "center",
              gap: 12,
              borderColor: r.name === profile.name ? T.amber : T.line,
            }}
          >
            <span className="px" style={{ fontSize: 14, width: 34, color: i === 0 ? T.amber : i === 1 ? T.dim : i === 2 ? T.coral : T.dim }}>
              {i < 3 ? ["🥇", "🥈", "🥉"][i] : "#" + (i + 1)}
            </span>
            <span style={{ flex: 1, fontWeight: 600 }}>
              {r.name}
              {r.name === profile.name && <span style={{ color: T.amber }}> · you</span>}
            </span>
            <span className="mono" style={{ color: T.amber, fontSize: 13 }}>
              {r.xp} XP
            </span>
          </div>
        ))
      )}
    </div>
  );
}
