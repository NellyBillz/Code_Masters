"use client";

import { useEffect, useState } from "react";
import {
    getIssueClaims,
    postClaim,
    deleteClaim,
    attachPullRequest,
    reviewClaim,
    getProject,
} from "../../lib/api";
import { useAuth } from "../context/AuthContext";

function normalizedStatus(claim) {
    return claim.status ? String(claim.status).toLowerCase() : "active";
}

function isInFlight(claim) {
    const status = normalizedStatus(claim);
    return status === "active" || status === "changes_requested";
}

function PullRequestField({ issueId, claim, onUpdated }) {
    const [url, setUrl] = useState(claim.pullRequestUrl || "");
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState("");

    async function handleSubmit(event) {
        event.preventDefault();

        const trimmed = url.trim();
        if (!trimmed) {
            setError("Enter a pull request URL.");
            return;
        }

        setBusy(true);
        setError("");
        try {
            await attachPullRequest(issueId, claim.id, trimmed);
            await onUpdated();
        } catch (err) {
            setError(err.message || "Failed to save pull request link.");
            setBusy(false);
        }
    }

    return (
        <form onSubmit={handleSubmit} style={{ marginTop: "0.75rem" }}>
            <label
                htmlFor={`pr-url-${claim.id}`}
                style={{ display: "block", fontSize: "0.9rem", marginBottom: "0.35rem" }}
            >
                {claim.pullRequestUrl
                    ? "Update your pull request link"
                    : "Attach your pull request"}
            </label>

            <div style={{ display: "flex", gap: "0.5rem" }}>
                <input
                    id={`pr-url-${claim.id}`}
                    type="url"
                    value={url}
                    onChange={(event) => setUrl(event.target.value)}
                    placeholder="https://github.com/owner/repo/pull/123"
                    disabled={busy}
                    style={{ flex: 1, padding: "0.4rem" }}
                />

                <button type="submit" disabled={busy}>
                    {busy ? "Saving..." : claim.pullRequestUrl ? "Update" : "Attach"}
                </button>
            </div>

            {error && (
                <p role="alert" style={{ color: "#b00020", marginTop: "0.35rem" }}>
                    {error}
                </p>
            )}
        </form>
    );
}

function ClaimReviewControls({ issueId, claim, onReviewed }) {
    const [showFeedback, setShowFeedback] = useState(false);
    const [feedback, setFeedback] = useState("");
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState("");

    async function confirmCompleted() {
        setBusy(true);
        setError("");
        try {
            await reviewClaim(issueId, claim.id, { decision: "confirm_completed" });
            await onReviewed();
        } catch (err) {
            setError(err.message || "Failed to confirm completion.");
            setBusy(false);
        }
    }

    async function submitRequestChanges(event) {
        event.preventDefault();

        const trimmed = feedback.trim();
        if (!trimmed) {
            setError("Feedback is required to request changes.");
            return;
        }

        setBusy(true);
        setError("");
        try {
            await reviewClaim(issueId, claim.id, {
                decision: "request_changes",
                feedback: trimmed,
            });
            await onReviewed();
        } catch (err) {
            setError(err.message || "Failed to request changes.");
            setBusy(false);
        }
    }

    return (
        <div
            style={{
                marginTop: "0.75rem",
                paddingTop: "0.75rem",
                borderTop: "1px dashed #ccc",
            }}
        >
            <p style={{ fontSize: "0.85rem", fontWeight: 600, marginBottom: "0.5rem" }}>
                Maintainer review
            </p>

            {!showFeedback ? (
                <div style={{ display: "flex", gap: "0.5rem" }}>
                    <button type="button" onClick={confirmCompleted} disabled={busy}>
                        {busy ? "Working..." : "Confirm complete"}
                    </button>

                    <button
                        type="button"
                        onClick={() => setShowFeedback(true)}
                        disabled={busy}
                    >
                        Request changes
                    </button>
                </div>
            ) : (
                <form onSubmit={submitRequestChanges}>
                    <textarea
                        value={feedback}
                        onChange={(event) => setFeedback(event.target.value)}
                        placeholder="What needs to change before this can be accepted?"
                        rows={3}
                        disabled={busy}
                        style={{ width: "100%", padding: "0.5rem" }}
                    />

                    <div style={{ display: "flex", gap: "0.5rem", marginTop: "0.5rem" }}>
                        <button type="submit" disabled={busy || !feedback.trim()}>
                            {busy ? "Sending..." : "Send request for changes"}
                        </button>

                        <button
                            type="button"
                            onClick={() => {
                                setShowFeedback(false);
                                setFeedback("");
                                setError("");
                            }}
                            disabled={busy}
                        >
                            Cancel
                        </button>
                    </div>
                </form>
            )}

            {error && (
                <p role="alert" style={{ color: "#b00020", marginTop: "0.5rem" }}>
                    {error}
                </p>
            )}
        </div>
    );
}

export default function ClaimPanel({ issueId, projectId, initialClaims = [] }) {
    const { user: me, loading: authLoading } = useAuth();
    const [claims, setClaims] = useState(initialClaims.filter(isInFlight));
    const [project, setProject] = useState(null);
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState("");

    async function refresh() {
        const list = await getIssueClaims(issueId);
        setClaims((Array.isArray(list) ? list : []).filter(isInFlight));
    }

    useEffect(() => {
        refresh().catch(() => {});
    }, [issueId]);

    useEffect(() => {
        if (!projectId) return;

        getProject(projectId)
            .then(setProject)
            .catch(() => setProject(null));
    }, [projectId]);

    const isMaintainer = Boolean(
        me &&
            project?.maintainers?.some(
                (maintainer) =>
                    maintainer.user?.id === me.id ||
                    maintainer.user?.username === me.username
            )
    );

    const myClaim = me ? claims.find((c) => c.user?.id === me.id) : null;
    const myClaimIsActive = myClaim && normalizedStatus(myClaim) === "active";
    // A claim/release toggle only makes sense when there's nothing to
    // toggle from (no claim yet) or the claim is still "active" — the
    // backend only releases active claims, and a changes_requested claim
    // has no toggle action of its own (it just gets a PR field below).
    const showClaimToggle = !myClaim || myClaimIsActive;
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
                showClaimToggle && (
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
                )
            ) : (
                <p>
                    <a href="/auth/github">Log in with GitHub</a> to claim this
                    issue.
                </p>
            )}

            {error && <p role="alert">{error}</p>}

            {count > 0 && (
                <ul style={{ marginTop: "1rem", paddingLeft: "1.25rem" }}>
                    {claims.map((claim) => {
                        const isMe = Boolean(me && claim.user?.id === me.id);
                        const status = normalizedStatus(claim);
                        const canReview =
                            isMaintainer && Boolean(claim.pullRequestUrl);

                        return (
                            <li key={claim.id} style={{ marginBottom: "1rem" }}>
                                <strong>
                                    {claim.user?.displayName ||
                                        claim.user?.username ||
                                        "User"}
                                </strong>
                                {isMe ? " (you)" : ""}
                                {claim.createdAt
                                    ? ` · ${new Date(claim.createdAt).toLocaleDateString()}`
                                    : ""}
                                {status === "changes_requested" && (
                                    <span
                                        style={{
                                            marginLeft: "0.5rem",
                                            fontSize: "0.75rem",
                                            fontWeight: 600,
                                            color: "#8a6100",
                                            background: "#fff3cd",
                                            padding: "0.1rem 0.5rem",
                                            borderRadius: "999px",
                                        }}
                                    >
                                        Changes requested
                                    </span>
                                )}
                                {claim.note ? (
                                    <div style={{ color: "#555" }}>{claim.note}</div>
                                ) : null}

                                {claim.pullRequestUrl && (
                                    <div style={{ marginTop: "0.35rem" }}>
                                        <a
                                            href={claim.pullRequestUrl}
                                            target="_blank"
                                            rel="noreferrer"
                                        >
                                            View pull request →
                                        </a>
                                    </div>
                                )}

                                {claim.maintainerFeedback && (
                                    <div
                                        style={{
                                            marginTop: "0.5rem",
                                            padding: "0.5rem 0.75rem",
                                            background: "#fff8e1",
                                            borderRadius: "0.375rem",
                                        }}
                                    >
                                        <strong>Maintainer feedback:</strong>{" "}
                                        {claim.maintainerFeedback}
                                    </div>
                                )}

                                {isMe && (
                                    <PullRequestField
                                        issueId={issueId}
                                        claim={claim}
                                        onUpdated={refresh}
                                    />
                                )}

                                {canReview && (
                                    <ClaimReviewControls
                                        issueId={issueId}
                                        claim={claim}
                                        onReviewed={refresh}
                                    />
                                )}
                            </li>
                        );
                    })}
                </ul>
            )}
        </section>
    );
}
