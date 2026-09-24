"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { HandHeart, GitPullRequest, CheckCircle2, MessageSquareWarning, Users } from "lucide-react";
import {
  getIssueClaims,
  postClaim,
  deleteClaim,
  attachPullRequest,
  requestCollaboration,
  listCollaborationRequests,
  respondToCollaborationRequest,
  cancelCollaborationRequest,
  ApiError,
} from "../../lib/api";
import { useAuth } from "../context/AuthContext";

const JOINABLE_STATUSES = ["active", "changes_requested"];

// "released" is a retracted signal, nothing left to show for it. Every other
// status (active, changes_requested, completed) stays visible so a claim's
// resolution is still visible instead of quietly disappearing once it's no
// longer "active".
function isVisible(claim) {
  const status = claim.status ? String(claim.status).toLowerCase() : "active";
  return status !== "released";
}

function statusOf(claim) {
  return claim.status ? String(claim.status).toLowerCase() : "active";
}

const COMPLETION_LABELS = {
  github_verified: "Verified by GitHub",
  maintainer_confirmed: "Confirmed by a maintainer",
};

export default function ClaimPanel({ issueId, initialClaims = [] }) {
  const { user: me, loading: authLoading } = useAuth();
  const [claims, setClaims] = useState(initialClaims.filter(isVisible));
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  async function refresh() {
    const list = await getIssueClaims(issueId);
    setClaims((Array.isArray(list) ? list : []).filter(isVisible));
  }

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- fetch-on-mount pattern; no derived-state alternative for reading server data
    refresh().catch(() => {});
    // eslint-disable-next-line react-hooks/exhaustive-deps -- refresh is stable for this component's lifetime
  }, [issueId]);

  const myClaim = me ? claims.find((c) => c.user?.id === me.id) : null;
  const myClaimStatus = myClaim ? statusOf(myClaim) : null;
  const activeCount = claims.filter((c) => statusOf(c) === "active").length;

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
      <h2 style={{ fontSize: "15px", fontWeight: 700, margin: "0 0 10px", color: "var(--cm-text-primary)" }}>
        Contributors working on this
      </h2>

      <p style={{ fontSize: "13px", color: "var(--cm-text-secondary)", margin: "0 0 4px" }}>
        {activeCount === 0
          ? "No one has said they're working on this yet."
          : `${activeCount} contributor${activeCount === 1 ? " has" : "s have"} said they're working on this.`}
      </p>
      <p style={{ fontSize: "12px", color: "var(--cm-text-muted)", margin: "0 0 16px" }}>
        Claims are a signal of intent. Several people can claim the same issue.
      </p>

      {authLoading ? (
        <p style={{ fontSize: "12.5px", color: "var(--cm-text-muted)" }}>Checking session…</p>
      ) : me ? (
        myClaimStatus !== "completed" && (
          <button
            type="button"
            onClick={handleToggle}
            disabled={busy || myClaimStatus === "changes_requested"}
            title={myClaimStatus === "changes_requested" ? "Attach an updated pull request instead of releasing" : undefined}
            style={{
              display: "inline-flex",
              alignItems: "center",
              gap: "8px",
              borderRadius: "999px",
              padding: "10px 20px",
              fontSize: "13px",
              fontWeight: 700,
              border: "none",
              cursor: busy || myClaimStatus === "changes_requested" ? "not-allowed" : "pointer",
              opacity: busy || myClaimStatus === "changes_requested" ? 0.6 : 1,
              background: myClaim ? "var(--cm-surface-alt)" : "var(--cm-lime)",
              color: myClaim ? "var(--cm-text-primary)" : "#0A0A0A",
            }}
          >
            <HandHeart size={15} strokeWidth={2} aria-hidden="true" />
            {busy ? "Working…" : myClaim ? "Release my claim" : "Claim this issue"}
          </button>
        )
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

      {myClaim && (myClaimStatus === "active" || myClaimStatus === "changes_requested") && (
        <PullRequestForm
          issueId={issueId}
          claim={myClaim}
          onAttached={() => refresh().catch(() => {})}
        />
      )}

      {claims.length > 0 && (
        <ul style={{ listStyle: "none", margin: "16px 0 0", padding: 0, display: "flex", flexDirection: "column", gap: "8px" }}>
          {claims.map((claim) => (
            <ClaimRow
              key={claim.id}
              issueId={issueId}
              claim={claim}
              me={me}
              isMe={Boolean(me && claim.user?.id === me.id)}
            />
          ))}
        </ul>
      )}
    </section>
  );
}

function ClaimRow({ issueId, claim, me, isMe }) {
  const status = statusOf(claim);
  const collaborators = claim.collaborators || [];

  return (
    <li
      style={{
        fontSize: "12.5px",
        color: "var(--cm-text-secondary)",
        background: "var(--cm-surface-alt)",
        borderRadius: "12px",
        padding: "10px 14px",
      }}
    >
      <div style={{ display: "flex", alignItems: "center", gap: "6px", flexWrap: "wrap" }}>
        <strong style={{ color: "var(--cm-text-primary)" }}>
          {claim.user?.username ? (
            <Link href={`/users/${claim.user.username}`}>
              {claim.user?.displayName || claim.user.username}
            </Link>
          ) : (
            claim.user?.displayName || "User"
          )}
        </strong>
        {isMe ? " (you)" : ""}
        {claim.createdAt ? ` · ${new Date(claim.createdAt).toLocaleDateString()}` : ""}

        {status === "completed" && (
          <span
            style={{
              display: "inline-flex",
              alignItems: "center",
              gap: "4px",
              fontSize: "10.5px",
              fontWeight: 700,
              color: "var(--cm-lime-text)",
              background: "var(--cm-lime-soft)",
              padding: "2px 8px",
              borderRadius: "999px",
            }}
          >
            <CheckCircle2 size={11} strokeWidth={2} aria-hidden="true" />
            Completed{claim.completionSource ? ` · ${COMPLETION_LABELS[claim.completionSource] || claim.completionSource}` : ""}
          </span>
        )}

        {status === "changes_requested" && (
          <span
            style={{
              display: "inline-flex",
              alignItems: "center",
              gap: "4px",
              fontSize: "10.5px",
              fontWeight: 700,
              color: "var(--cm-orange-text)",
              background: "var(--cm-orange-soft)",
              padding: "2px 8px",
              borderRadius: "999px",
            }}
          >
            <MessageSquareWarning size={11} strokeWidth={2} aria-hidden="true" />
            Changes requested
          </span>
        )}
      </div>

      {claim.note ? <div style={{ marginTop: "4px" }}>{claim.note}</div> : null}

      {claim.maintainerFeedback && (
        <p style={{ margin: "6px 0 0", padding: "8px 10px", borderRadius: "8px", background: "var(--cm-orange-soft)", color: "var(--cm-orange-text)" }}>
          {claim.maintainerFeedback}
        </p>
      )}

      {claim.pullRequestUrl && (
        <a
          href={claim.pullRequestUrl}
          target="_blank"
          rel="noreferrer"
          style={{ display: "inline-flex", alignItems: "center", gap: "4px", marginTop: "6px", color: "var(--cm-text-primary)", fontWeight: 600 }}
        >
          <GitPullRequest size={12} strokeWidth={2} aria-hidden="true" />
          View pull request
        </a>
      )}

      {collaborators.length > 0 && (
        <p style={{ display: "flex", alignItems: "center", gap: "4px", flexWrap: "wrap", margin: "6px 0 0", fontSize: "11.5px", color: "var(--cm-text-muted)" }}>
          <Users size={11} strokeWidth={2} aria-hidden="true" />
          with{" "}
          {collaborators.map((collaborator, index) => (
            <span key={collaborator.id ?? index}>
              {collaborator.username ? (
                <Link href={`/users/${collaborator.username}`} style={{ fontWeight: 600, color: "var(--cm-text-secondary)" }}>
                  {collaborator.displayName || collaborator.username}
                </Link>
              ) : (
                collaborator.displayName || "a contributor"
              )}
              {me && collaborator.id === me.id ? " (you)" : ""}
              {index < collaborators.length - 1 ? ", " : ""}
            </span>
          ))}
        </p>
      )}

      {me && JOINABLE_STATUSES.includes(status) && (
        <ClaimCollaborationActions issueId={issueId} claim={claim} me={me} isOwner={isMe} />
      )}
    </li>
  );
}

function ClaimCollaborationActions({ issueId, claim, me, isOwner }) {
  const [requests, setRequests] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  async function load() {
    try {
      const list = await listCollaborationRequests(issueId, claim.id);
      setRequests(Array.isArray(list) ? list : []);
    } catch {
      setRequests([]);
    }
  }

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- fetch-on-mount pattern; no derived-state alternative for reading server data
    load().catch(() => {});
    // eslint-disable-next-line react-hooks/exhaustive-deps -- load is stable for this component's lifetime
  }, [issueId, claim.id]);

  if (requests === null) return null;

  const myPendingRequest = requests.find((r) => r.status === "pending" && r.requester?.id === me.id);
  const incomingPendingRequests = isOwner ? requests.filter((r) => r.status === "pending") : [];

  async function handleRequest() {
    setBusy(true);
    setError("");
    try {
      await requestCollaboration(issueId, claim.id);
      await load();
    } catch (err) {
      setError(err.message || "Failed to send request. Please try again.");
    } finally {
      setBusy(false);
    }
  }

  async function handleCancel(requestId) {
    setBusy(true);
    setError("");
    try {
      await cancelCollaborationRequest(issueId, claim.id, requestId);
      await load();
    } catch (err) {
      setError(err.message || "Failed to cancel request. Please try again.");
    } finally {
      setBusy(false);
    }
  }

  async function handleRespond(requestId, decision) {
    setBusy(true);
    setError("");
    try {
      await respondToCollaborationRequest(issueId, claim.id, requestId, decision);
      await load();
    } catch (err) {
      setError(err.message || "Failed to respond. Please try again.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div style={{ marginTop: "8px" }}>
      {isOwner &&
        incomingPendingRequests.map((request) => (
          <div
            key={request.id}
            style={{ display: "flex", alignItems: "center", gap: "8px", flexWrap: "wrap", fontSize: "11.5px", color: "var(--cm-text-secondary)", marginBottom: "4px" }}
          >
            <span>
              {request.requester?.username ? (
                <Link href={`/users/${request.requester.username}`} style={{ fontWeight: 600, color: "var(--cm-text-primary)" }}>
                  {request.requester.displayName || request.requester.username}
                </Link>
              ) : (
                "A contributor"
              )}
              {" wants to collaborate"}
            </span>
            <button
              type="button"
              onClick={() => handleRespond(request.id, "accept")}
              disabled={busy}
              style={{ background: "none", border: "none", padding: 0, fontWeight: 700, color: "var(--cm-lime-text)", cursor: busy ? "not-allowed" : "pointer" }}
            >
              Accept
            </button>
            <button
              type="button"
              onClick={() => handleRespond(request.id, "decline")}
              disabled={busy}
              style={{ background: "none", border: "none", padding: 0, fontWeight: 700, color: "var(--cm-text-muted)", cursor: busy ? "not-allowed" : "pointer" }}
            >
              Decline
            </button>
          </div>
        ))}

      {!isOwner &&
        (myPendingRequest ? (
          <span style={{ display: "inline-flex", alignItems: "center", gap: "8px", fontSize: "11.5px", color: "var(--cm-text-muted)" }}>
            Collaboration request sent
            <button
              type="button"
              onClick={() => handleCancel(myPendingRequest.id)}
              disabled={busy}
              style={{ background: "none", border: "none", padding: 0, fontWeight: 700, color: "var(--cm-text-muted)", textDecoration: "underline", cursor: busy ? "not-allowed" : "pointer" }}
            >
              Cancel
            </button>
          </span>
        ) : (
          <button
            type="button"
            onClick={handleRequest}
            disabled={busy}
            style={{ display: "inline-flex", alignItems: "center", gap: "4px", background: "none", border: "none", padding: 0, fontSize: "11.5px", fontWeight: 600, color: "var(--cm-lime-text)", cursor: busy ? "not-allowed" : "pointer" }}
          >
            <Users size={11} strokeWidth={2} aria-hidden="true" />
            Request to collaborate
          </button>
        ))}

      {error && (
        <p role="alert" style={{ fontSize: "11px", color: "var(--cm-orange-text)", margin: "4px 0 0" }}>
          {error}
        </p>
      )}
    </div>
  );
}

function PullRequestForm({ issueId, claim, onAttached }) {
  const [url, setUrl] = useState(claim.pullRequestUrl || "");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  async function handleSubmit(event) {
    event.preventDefault();
    const trimmedUrl = url.trim();
    if (!trimmedUrl) return;

    setSubmitting(true);
    setError("");
    try {
      await attachPullRequest(issueId, claim.id, trimmedUrl);
      onAttached();
    } catch (err) {
      if (err instanceof ApiError && err.status === 403) {
        setError("Only the claim's owner can attach a pull request.");
      } else {
        setError(err.message || "Failed to attach pull request. Please try again.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} style={{ marginTop: "14px", display: "flex", gap: "8px", flexWrap: "wrap" }}>
      <input
        type="url"
        value={url}
        onChange={(e) => setUrl(e.target.value)}
        placeholder="https://github.com/owner/repo/pull/123"
        disabled={submitting}
        required
        style={{
          flex: "1 1 260px",
          borderRadius: "10px",
          border: "0.5px solid var(--cm-border)",
          background: "var(--cm-surface)",
          color: "var(--cm-text-primary)",
          fontSize: "12.5px",
          padding: "9px 12px",
          outline: "none",
        }}
      />
      <button
        type="submit"
        disabled={submitting}
        style={{
          borderRadius: "999px",
          padding: "9px 16px",
          fontSize: "12.5px",
          fontWeight: 700,
          border: "none",
          cursor: submitting ? "not-allowed" : "pointer",
          opacity: submitting ? 0.6 : 1,
          background: "var(--cm-orange)",
          color: "#2B1108",
          whiteSpace: "nowrap",
        }}
      >
        {submitting ? "Saving…" : claim.pullRequestUrl ? "Update pull request" : "Attach pull request"}
      </button>
      {error && (
        <p role="alert" style={{ flexBasis: "100%", fontSize: "12px", color: "var(--cm-orange-text)", margin: 0 }}>
          {error}
        </p>
      )}
    </form>
  );
}
