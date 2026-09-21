"use client";

import { useAuth } from "../context/AuthContext";

export default function AuthStatus() {
  const { user, loading, logout } = useAuth();

  if (loading) {
    return <span style={{ fontSize: "0.9rem", color: "#777" }}>Loading...</span>;
  }

  if (!user) {
    return (
      <a href="/auth/github" style={{ fontSize: "0.9rem" }}>
        Log in with GitHub
      </a>
    );
  }

  return (
    <div style={{ display: "flex", alignItems: "center", gap: "0.75rem" }}>
      {user.avatarUrl && (
        <img
          src={user.avatarUrl}
          alt=""
          width={24}
          height={24}
          style={{ borderRadius: "50%" }}
        />
      )}

      <span style={{ fontSize: "0.9rem" }}>
        {user.displayName || user.username}
      </span>

      <button
        type="button"
        onClick={logout}
        style={{
          fontSize: "0.9rem",
          background: "none",
          border: "1px solid #ccc",
          borderRadius: "0.375rem",
          padding: "0.25rem 0.75rem",
          cursor: "pointer",
        }}
      >
        Log out
      </button>
    </div>
  );
}
