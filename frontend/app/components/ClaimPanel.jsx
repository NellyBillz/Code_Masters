"use client";

import { useEffect, useState } from "react";
import { getIssueClaims, postClaim, deleteClaim } from "../../lib/api";
import { useAuth } from "../context/AuthContext";
import Button from "./ui/Button";
import Avatar from "./ui/Avatar";

function isActive(claim) {
  return !claim.status || String(claim.status).toLowerCase() === "active";
}

/**
 * A claim is a signal of intent, not a GitHub assignment — several people
 * can claim the same issue (design brief §16). Keep the language and
 * visual weight below the "Contribute on GitHub" action above it.
 */
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
    refresh().catch(() => {});
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
    <div>
      <p className="text-sm text-foreground-secondary">
        {count === 0
          ? "No one has said they're working on this yet."
          : `${count} ${count === 1 ? "person has" : "people have"} said they're working on this.`}
      </p>

      {authLoading ? (
        <p className="mt-3 text-sm text-foreground-muted" aria-live="polite">
          Checking session…
        </p>
      ) : me ? (
        <Button
          variant={myClaim ? "secondary" : "primary"}
          size="sm"
          onClick={handleToggle}
          disabled={busy}
          className="mt-3 w-full"
        >
          {busy ? "Working…" : myClaim ? "Release my claim" : "Claim this issue"}
        </Button>
      ) : (
        <Button href="/auth/github" variant="secondary" size="sm" className="mt-3 w-full">
          Sign in to claim
        </Button>
      )}

      {error && (
        <p role="alert" className="mt-2 text-[13px] text-danger">
          {error}
        </p>
      )}

      {count > 0 && (
        <ul className="mt-4 flex flex-col gap-2.5 border-t border-border pt-4">
          {claims.map((claim) => (
            <li key={claim.id} className="flex items-start gap-2">
              <Avatar user={claim.user} size="sm" />
              <div className="min-w-0">
                <p className="truncate text-[13px] text-foreground-secondary">
                  {claim.user?.displayName || claim.user?.username || "User"}
                  {me && claim.user?.id === me.id ? " (you)" : ""}
                </p>
                {claim.note && (
                  <p className="mt-0.5 text-[13px] text-foreground-muted">{claim.note}</p>
                )}
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
