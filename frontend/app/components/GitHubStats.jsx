"use client";

import { useEffect, useState } from "react";
import { FolderGit2, Users, UserPlus, CalendarDays } from "lucide-react";

/**
 * Live public GitHub numbers for a username, repos, followers, following,
 * account age. Public, unauthenticated GitHub REST endpoint; no backend
 * involvement. Best-effort: GitHub's anonymous rate limit is low, so a
 * failure here just means this row doesn't render, never a page-level error.
 */
export default function GitHubStats({ username }) {
  const [stats, setStats] = useState(null);

  useEffect(() => {
    let cancelled = false;

    fetch(`https://api.github.com/users/${username}`)
      .then((res) => (res.ok ? res.json() : null))
      .then((data) => {
        if (!cancelled && data) setStats(data);
      })
      .catch(() => {});

    return () => {
      cancelled = true;
    };
  }, [username]);

  if (!stats) return null;

  const memberSince = stats.created_at ? new Date(stats.created_at).getFullYear() : null;

  return (
    <div style={{ display: "flex", alignItems: "center", gap: "14px", flexWrap: "wrap", fontSize: "12px", color: "var(--cm-text-muted)", margin: "10px 0 0" }}>
      <span style={{ display: "inline-flex", alignItems: "center", gap: "5px" }}>
        <FolderGit2 size={13} strokeWidth={1.8} aria-hidden="true" />
        {stats.public_repos ?? 0} repositories
      </span>
      <span style={{ display: "inline-flex", alignItems: "center", gap: "5px" }}>
        <Users size={13} strokeWidth={1.8} aria-hidden="true" />
        {stats.followers ?? 0} followers
      </span>
      <span style={{ display: "inline-flex", alignItems: "center", gap: "5px" }}>
        <UserPlus size={13} strokeWidth={1.8} aria-hidden="true" />
        {stats.following ?? 0} following
      </span>
      {memberSince && (
        <span style={{ display: "inline-flex", alignItems: "center", gap: "5px" }}>
          <CalendarDays size={13} strokeWidth={1.8} aria-hidden="true" />
          On GitHub since {memberSince}
        </span>
      )}
    </div>
  );
}
