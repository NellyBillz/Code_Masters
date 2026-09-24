"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { Trophy, Medal } from "lucide-react";
import { getLeaderboard, getMyLeaderboardRank } from "../../lib/api";
import { useAuth } from "../context/AuthContext";

const RANK_MEDAL_COLORS = {
  1: "#F4C430",
  2: "#C0C0C0",
  3: "#CD7F32",
};

function formatCount(n) {
  return `${n} contribution${n === 1 ? "" : "s"}`;
}

/**
 * Recognition Leaderboard (wow-feature, 2026-09-24) — public "Top SA Open
 * Source Contributors," ranked by GitHub-verified/maintainer-confirmed
 * contributions. If signed in, a personalized "Your rank" card shows even
 * when the caller isn't in the visible top 20.
 */
export default function LeaderboardPage() {
  const { user } = useAuth();
  const [leaderboard, setLeaderboard] = useState(null);
  const [myRank, setMyRank] = useState(null);
  const [error, setError] = useState(false);

  useEffect(() => {
    let cancelled = false;
    getLeaderboard()
      .then((result) => !cancelled && setLeaderboard(result || []))
      .catch(() => !cancelled && setError(true));
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (!user) return;
    let cancelled = false;
    getMyLeaderboardRank()
      .then((result) => !cancelled && setMyRank(result))
      .catch(() => {});
    return () => {
      cancelled = true;
    };
  }, [user]);

  const myRankIsInVisibleList = Boolean(
    myRank && leaderboard?.some((entry) => entry.userId === myRank.userId)
  );

  return (
    <div style={{ padding: "8px 4px 40px", maxWidth: "760px", margin: "0 auto" }}>
      <header style={{ marginBottom: "20px" }}>
        <p style={{ fontSize: "12px", fontWeight: 600, letterSpacing: "0.1em", color: "var(--cm-orange-text)", margin: "0 0 8px" }}>
          RECOGNITION
        </p>
        <h1 style={{ display: "flex", alignItems: "center", gap: "10px", fontSize: "26px", fontWeight: 700, margin: 0, color: "var(--cm-text-primary)" }}>
          <Trophy size={24} strokeWidth={2} color="var(--cm-lime-text)" aria-hidden="true" />
          Top SA open-source contributors
        </h1>
        <p style={{ fontSize: "13px", color: "var(--cm-text-secondary)", margin: "8px 0 0" }}>
          Ranked by GitHub-verified and maintainer-confirmed contributions — not raw claim activity.
        </p>
      </header>

      {user && myRank && !myRankIsInVisibleList && (
        <section
          className="cm-glass"
          style={{ borderRadius: "20px", padding: "16px 20px", marginBottom: "20px", background: "var(--cm-lime-soft)" }}
        >
          <p style={{ fontSize: "11.5px", fontWeight: 600, color: "var(--cm-lime-text)", margin: "0 0 4px" }}>
            YOUR RANK
          </p>
          <p style={{ fontSize: "14px", fontWeight: 700, color: "var(--cm-text-primary)", margin: 0 }}>
            {myRank.rank ? `#${myRank.rank}` : "Not yet ranked"}
            <span style={{ fontWeight: 500, color: "var(--cm-text-secondary)" }}>
              {" "}
              — {formatCount(myRank.contributionCount)}
              {myRank.rank === null && ", make your first verified contribution to get on the board"}
            </span>
          </p>
        </section>
      )}

      {error && (
        <p style={{ fontSize: "13px", color: "var(--cm-text-muted)" }}>Couldn&apos;t load the leaderboard right now.</p>
      )}

      {!error && leaderboard === null && (
        <p style={{ fontSize: "13px", color: "var(--cm-text-muted)" }}>Loading…</p>
      )}

      {!error && leaderboard?.length === 0 && (
        <div className="cm-glass" style={{ borderRadius: "24px", padding: "40px 24px", textAlign: "center" }}>
          <p style={{ fontSize: "14px", color: "var(--cm-text-secondary)", margin: 0 }}>
            No verified contributions yet — be the first on the board.
          </p>
        </div>
      )}

      {!error && leaderboard?.length > 0 && (
        <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
          {leaderboard.map((entry) => (
            <LeaderboardRow key={entry.userId} entry={entry} isYou={entry.userId === user?.id} />
          ))}
        </div>
      )}
    </div>
  );
}

function LeaderboardRow({ entry, isYou }) {
  const medalColor = RANK_MEDAL_COLORS[entry.rank];

  return (
    <Link
      href={`/users/${entry.username}`}
      className="cm-glass"
      style={{
        display: "flex",
        alignItems: "center",
        gap: "14px",
        borderRadius: "16px",
        padding: "12px 18px",
        textDecoration: "none",
        outline: isYou ? "2px solid var(--cm-lime)" : "none",
        outlineOffset: "-2px",
      }}
    >
      <span
        style={{
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          width: "28px",
          height: "28px",
          borderRadius: "50%",
          flexShrink: 0,
          fontSize: "12.5px",
          fontWeight: 700,
          background: medalColor || "var(--cm-surface-alt)",
          color: medalColor ? "#1A1A1A" : "var(--cm-text-secondary)",
        }}
      >
        {medalColor ? <Medal size={14} strokeWidth={2} aria-hidden="true" /> : entry.rank}
      </span>

      {entry.avatarUrl ? (
        // eslint-disable-next-line @next/next/no-img-element -- external, size-variable avatar URL
        <img src={entry.avatarUrl} alt="" width={32} height={32} style={{ borderRadius: "50%", flexShrink: 0 }} />
      ) : (
        <span
          aria-hidden="true"
          style={{
            width: "32px",
            height: "32px",
            borderRadius: "50%",
            background: "var(--cm-sidebar)",
            color: "var(--cm-lime)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            fontWeight: 700,
            fontSize: "12px",
            flexShrink: 0,
          }}
        >
          {(entry.displayName || entry.username || "?")[0]?.toUpperCase()}
        </span>
      )}

      <span style={{ flex: 1, minWidth: 0 }}>
        <p style={{ fontSize: "13.5px", fontWeight: 600, margin: 0, color: "var(--cm-text-primary)" }}>
          {entry.displayName || entry.username}
          {isYou && <span style={{ color: "var(--cm-lime-text)", fontWeight: 700 }}> (you)</span>}
        </p>
        <p style={{ fontSize: "11.5px", color: "var(--cm-text-muted)", margin: "2px 0 0" }}>@{entry.username}</p>
      </span>

      <span style={{ fontSize: "13px", fontWeight: 700, color: "var(--cm-text-primary)", flexShrink: 0 }}>
        {entry.contributionCount}
      </span>
    </Link>
  );
}
