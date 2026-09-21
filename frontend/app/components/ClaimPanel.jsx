"use client";

import { useEffect, useState } from "react";
import {
    getCurrentUser,
    getIssueClaims,
    postClaim,
    deleteClaim,
} from "../../lib/api";

function isActive(claim) {
    return !claim.status || String(claim.status).toLowerCase() === "active";
}

export default function ClaimPanel({ issueId, initialClaims = [] }) {
    const [claims, setClaims] = useState(initialClaims.filter(isActive));
    const [me, setMe] = useState(null);
    const [hasSession, setHasSession] = useState(false);
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState("");

    async function refresh() {
        const list = await getIssueClaims(issueId);
        setClaims((Array.isArray(list) ? list : []).filter(isActive));
    }

    useEffect(() => {
        let cancelled = false;

        const session = document.cookie
            .split("; ")
            .some((cookie) => cookie.startsWith("CODEMASTERS_CSRF="));
        setHasSession(session);

        refresh().catch(() => {});

        if (session) {
            getCurrentUser()
                .then((user) => {
                    if (!cancelled) setMe(user);
                })
                .catch(() => {});
        }

        return () => {
            cancelled = true;
        };
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
            // Always re-read from the server so the list shows what is true.
            await refresh().catch(() => {});
            setBusy(false);
        }
    }

    return (
        <section style={{ marginTop: "2rem" }}>
            <h2>Contributors working on this</h2>

            <p>
                {count === 0
                    ? "No one has said they're working on this yet."
                    : `${count} contributor${count === 1 ? " has" : "s have"} said they're working on this.`}
            </p>
            <p style={{ fontSize: "0.9rem", color: "#555" }}>
                Claims are a signal of intent. Several people can claim the
                same issue.
            </p>

            {hasSession ? (
                <button
                    type="button"
                    onClick={handleToggle}
                    disabled={busy || !me}
                >
                    {busy
                        ? "Working..."
                        : myClaim
                          ? "Release my claim"
                          : "Claim this issue"}
                </button>
            ) : (
                <p>
                    <a href="/auth/github">Log in with GitHub</a> to claim this
                    issue.
                </p>
            )}

            {error && <p role="alert">{error}</p>}

            {count > 0 && (
                <ul style={{ marginTop: "1rem", paddingLeft: "1.25rem" }}>
                    {claims.map((claim) => (
                        <li key={claim.id} style={{ marginBottom: "0.5rem" }}>
                            <strong>
                                {claim.user?.displayName ||
                                    claim.user?.username ||
                                    "User"}
                            </strong>
                            {me && claim.user?.id === me.id ? " (you)" : ""}
                            {claim.createdAt
                                ? ` · ${new Date(claim.createdAt).toLocaleDateString()}`
                                : ""}
                            {claim.note ? (
                                <div style={{ color: "#555" }}>{claim.note}</div>
                            ) : null}
                        </li>
                    ))}
                </ul>
            )}
        </section>
    );
}