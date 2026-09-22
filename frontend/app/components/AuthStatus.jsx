"use client";

import { useAuth } from "../context/AuthContext";

function GithubMark() {
  return (
    <svg width="14" height="14" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
      <path d="M8 0C3.58 0 0 3.58 0 8a8 8 0 0 0 5.47 7.59c.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82a7.5 7.5 0 0 1 4 0c1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8 8 0 0 0 16 8c0-4.42-3.58-8-8-8z" />
    </svg>
  );
}

export default function AuthStatus() {
  const { user, loading, logout } = useAuth();

  if (loading) {
    return <span style={{ fontSize: "13px", color: "var(--cm-text-muted)" }}>Loading…</span>;
  }

  if (!user) {
    return (
      <a
        href="/auth/github"
        style={{
          display: "inline-flex",
          alignItems: "center",
          gap: "8px",
          background: "var(--cm-orange)",
          color: "#FCE9DD",
          fontSize: "13px",
          fontWeight: 500,
          borderRadius: "8px",
          padding: "8px 14px",
        }}
      >
        <GithubMark />
        Sign in with GitHub
      </a>
    );
  }

  return (
    <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
      {user.avatarUrl && (
        // eslint-disable-next-line @next/next/no-img-element -- external, size-variable avatar URLs; not worth next/image's config here
        <img
          src={user.avatarUrl}
          alt=""
          width={26}
          height={26}
          style={{ borderRadius: "50%" }}
        />
      )}

      <span style={{ fontSize: "13px", color: "var(--cm-text-primary)" }}>
        {user.displayName || user.username}
      </span>

      <button
        type="button"
        onClick={logout}
        style={{
          fontSize: "12px",
          background: "var(--cm-surface)",
          border: "0.5px solid var(--cm-border)",
          borderRadius: "8px",
          padding: "6px 12px",
          color: "var(--cm-text-secondary)",
          cursor: "pointer",
        }}
      >
        Log out
      </button>
    </div>
  );
}
