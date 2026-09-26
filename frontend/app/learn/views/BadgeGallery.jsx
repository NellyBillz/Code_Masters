"use client";

import { T } from "../theme";
import { BADGES } from "../data";

export default function BadgeGallery({ profile }) {
  const owned = new Set(profile.badges || []);
  return (
    <div style={{ maxWidth: 720, margin: "0 auto" }}>
      <h1 className="px" style={{ fontSize: 20, color: T.violet }}>
        🎖️ BADGES
      </h1>
      <p style={{ color: T.dim, fontSize: 13 }}>
        {owned.size}/{BADGES.length} unlocked
      </p>
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(200px, 1fr))", gap: 12 }}>
        {BADGES.map((b) => (
          <div
            key={b.id}
            className="card"
            style={{ padding: 16, textAlign: "center", opacity: owned.has(b.id) ? 1 : 0.4, borderColor: owned.has(b.id) ? T.violet : T.line }}
          >
            <div style={{ fontSize: 34, filter: owned.has(b.id) ? "none" : "grayscale(1)" }}>{b.icon}</div>
            <div style={{ fontWeight: 700, marginTop: 6 }}>{b.name}</div>
            <div style={{ fontSize: 12, color: T.dim, marginTop: 4 }}>{b.desc}</div>
            {owned.has(b.id) && (
              <div className="mono" style={{ fontSize: 11, color: T.violet, marginTop: 6 }}>
                UNLOCKED
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
