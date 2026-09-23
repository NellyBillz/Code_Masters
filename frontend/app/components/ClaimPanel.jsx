"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { HandHeart, AlertTriangle } from "lucide-react";
import { getIssueClaims, postClaim, deleteClaim } from "../../lib/api";
import { useAuth } from "../context/AuthContext";

function isActive(claim) {
  return !claim.status || String(claim.status).toLowerCase() === "active";
}

export default function ClaimPanel({ issueId, initialClaims = [] }) {
  const { user: me, loading: authLoading } = useAuth();
  const [claims, setClaims] = useState(initialClaims.filter(isActive));
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  async function refresh() {
    const list = await getIssueClaims(issueId);
    setClaims((Array.isArray(list) ? list : []).filter(isActive));
  }

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- fetch-on-mount pattern; no derived-state alternative for reading server data
    refresh().catch(() => {});
    // eslint-disable-next-line react-hooks/exhaustive-deps -- refresh is stable for this component's lifetime
  }, [issueId]);

  const myClaim = me ? claims.find((c) => c.user?.id === me.id) : null;
  const count = claims.length;

  async function handleToggle() {
    setBusy(true);
    setError("");
    try {
      if (myClaim) {
        await deleteClaim(issueId);
      } else {
        await postClaim(issueId);
      }
    } catch (err) {
      setError(err.message || "Something went wrong. Please try again.");
    } finally {
      await refresh().catch(() => {});
      setBusy(false);
    }
  }

  return (
    <section className="cm-glass" style={{ borderRadius: "24px", padding: "20px 24px" }}>
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: "10px", marginBottom: "10px" }}>
        <h2 style={{ fontSize: "15px", fontWeight: 700, margin: 0, color: "var(--cm-text-primary)" }}>
          Contributors working on this
        </h2>

        {/* claim endpoints aren't on the confirmed-working list yet, say so rather than let a failure look like a bug */}
        <span
          title="This endpoint hasn't been confirmed working end-to-end yet"
          style={{ display: "inline-flex", alignItems: "center", gap: "4px", fontSize: "10.5px", fontWeight: 600, color: "var(--cm-orange-text)", background: "var(--cm-orange-soft)", padding: "3px 9px", borderRadius: "999px" }}
        >
          <AlertTriangle size={11} strokeWidth={2} aria-hidden="true" />
          Rolling out
        </span>
      </div>

      <p style={{ fontSize: "13px", color: "var(--cm-text-secondary)", margin: "0 0 4px" }}>
        {count === 0
          ? "No one has said they're working on this yet."
          : `${count} contributor${count === 1 ? " has" : "s have"} said they're working on this.`}
      </p>
      <p style={{ fontSize: "12px", color: "var(--cm-text-muted)", margin: "0 0 16px" }}>
        Claims are a signal of intent. Several people can claim the same issue.
      </p>

      {authLoading ? (
        <p style={{ fontSize: "12.5px", color: "var(--cm-text-muted)" }}>Checking session…</p>
      ) : me ? (
        <button
          type="button"
          onClick={handleToggle}
          disabled={busy}
          style={{
            display: "inline-flex",
            alignItems: "center",
            gap: "8px",
            borderRadius: "999px",
            padding: "10px 20px",
            fontSize: "13px",
            fontWeight: 700,
            border: "none",
            cursor: busy ? "not-allowed" : "pointer",
            opacity: busy ? 0.6 : 1,
            background: myClaim ? "var(--cm-surface-alt)" : "var(--cm-lime)",
            color: myClaim ? "var(--cm-text-primary)" : "#0A0A0A",
          }}
        >
          <HandHeart size={15} strokeWidth={2} aria-hidden="true" />
          {busy ? "Working…" : myClaim ? "Release my claim" : "Claim this issue"}
        </button>
      ) : (
        <a href="/auth/github" style={{ fontSize: "13px", fontWeight: 600, color: "var(--cm-lime-text)" }}>
          Log in with GitHub to claim this issue
        </a>
      )}

      {error && (
        <p role="alert" style={{ fontSize: "12.5px", color: "var(--cm-orange-text)", marginTop: "12px" }}>
          {error}
        </p>
      )}

      {count > 0 && (
        <ul style={{ listStyle: "none", margin: "16px 0 0", padding: 0, display: "flex", flexDirection: "column", gap: "8px" }}>
          {claims.map((claim) => (
            <li
              key={claim.id}
              style={{
                fontSize: "12.5px",
                color: "var(--cm-text-secondary)",
                background: "var(--cm-surface-alt)",
                borderRadius: "12px",
                padding: "10px 14px",
              }}
            >
              <strong style={{ color: "var(--cm-text-primary)" }}>
                {claim.user?.username ? (
                  <Link href={`/users/${claim.user.username}`}>
                    {claim.user?.displayName || claim.user.username}
                  </Link>
                ) : (
                  claim.user?.displayName || "User"
                )}
              </strong>
              {me && claim.user?.id === me.id ? " (you)" : ""}
              {claim.createdAt ? ` · ${new Date(claim.createdAt).toLocaleDateString()}` : ""}
              {claim.note ? <div style={{ marginTop: "4px" }}>{claim.note}</div> : null}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
