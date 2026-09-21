"use client";

import { useEffect, useState } from "react";
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

            {authLoading ? (
                <p style={{ fontSize: "0.9rem", color: "#777" }}>
                    Checking session...
                </p>
            ) : me ? (
                <button
                    type="button"
                    onClick={handleToggle}
                    disabled={busy}
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