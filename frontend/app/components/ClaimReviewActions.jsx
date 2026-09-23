"use client";

import { useState } from "react";
import { CheckCircle2, MessageSquareWarning } from "lucide-react";
import { reviewClaim, ApiError } from "../../lib/api";

const FEEDBACK_MAX_LENGTH = 2000;

/**
 * A maintainer's review controls for a single claim with a pull request
 * attached (API-03.4, POST /issues/{issueId}/claims/{claimId}/review). Only
 * ever rendered for claims already surfaced as "awaiting review" by
 * GET /users/me/maintainer-activity, so the caller is already known to
 * maintain this claim's project. Two decisions: request changes (feedback
 * required) or confirm completed (the manual fallback for cases GitHub
 * sync can't verify itself) — mirrors AdminReportActions'/
 * AdminProjectActions' two-step confirm pattern.
 */
export default function ClaimReviewActions({ issueId, claimId, onReviewed }) {
  const [pendingAction, setPendingAction] = useState(null); // null | "request_changes" | "confirm_completed"
  const [feedback, setFeedback] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  const trimmedFeedback = feedback.trim();

  function startAction(action) {
    setPendingAction(action);
    setFeedback("");
    setError("");
  }

  function cancel() {
    setPendingAction(null);
    setFeedback("");
    setError("");
  }

  async function handleConfirm() {
    if (pendingAction === "request_changes" && !trimmedFeedback) {
      setError("Feedback is required when requesting changes.");
      return;
    }

    setSubmitting(true);
    setError("");

    try {
      const updated = await reviewClaim(issueId, claimId, pendingAction, trimmedFeedback || undefined);
      onReviewed(updated);
    } catch (err) {
      if (err instanceof ApiError && err.status === 403) {
        setError("Only a maintainer of this project can review this claim.");
      } else if (err instanceof ApiError && err.status === 409) {
        setError(err.message || "This claim can no longer be reviewed.");
      } else if (err instanceof ApiError && err.status === 400) {
        setError(err.message || "Feedback is required when requesting changes.");
      } else {
        setError(err.message || "Failed to review this claim. Please try again.");
      }
      setSubmitting(false);
    }
  }

  if (!pendingAction) {
    return (
      <div style={{ display: "inline-flex", gap: "14px", marginTop: "8px" }}>
        <button
          type="button"
          onClick={() => startAction("confirm_completed")}
          style={{
            display: "inline-flex",
            alignItems: "center",
            gap: "4px",
            background: "none",
            border: "none",
            padding: 0,
            fontSize: "11.5px",
            fontWeight: 600,
            color: "var(--cm-lime-text)",
            cursor: "pointer",
          }}
        >
          <CheckCircle2 size={12} strokeWidth={2} aria-hidden="true" />
          Confirm completed
        </button>
        <button
          type="button"
          onClick={() => startAction("request_changes")}
          style={{
            display: "inline-flex",
            alignItems: "center",
            gap: "4px",
            background: "none",
            border: "none",
            padding: 0,
            fontSize: "11.5px",
            fontWeight: 600,
            color: "var(--cm-orange-text)",
            cursor: "pointer",
          }}
        >
          <MessageSquareWarning size={12} strokeWidth={2} aria-hidden="true" />
          Request changes
        </button>
      </div>
    );
  }

  return (
    <div style={{ marginTop: "10px" }}>
      <p style={{ fontSize: "11.5px", fontWeight: 600, margin: "0 0 6px", color: "var(--cm-text-primary)" }}>
        {pendingAction === "confirm_completed" ? "Confirm this claim as completed?" : "What needs to change?"}
      </p>

      {pendingAction === "request_changes" && (
        <textarea
          value={feedback}
          onChange={(e) => setFeedback(e.target.value)}
          placeholder="e.g. Tests are failing on CI, please also update the docs…"
          rows={2}
          maxLength={FEEDBACK_MAX_LENGTH}
          disabled={submitting}
          style={{
            width: "100%",
            borderRadius: "10px",
            border: "0.5px solid var(--cm-border)",
            background: "var(--cm-surface)",
            color: "var(--cm-text-primary)",
            fontSize: "12px",
            padding: "8px 10px",
            outline: "none",
            resize: "vertical",
          }}
        />
      )}

      {error && (
        <p role="alert" style={{ fontSize: "11px", color: "var(--cm-orange-text)", margin: "6px 0 0" }}>
          {error}
        </p>
      )}

      <div style={{ display: "flex", gap: "8px", marginTop: "8px" }}>
        <button
          type="button"
          onClick={handleConfirm}
          disabled={submitting || (pendingAction === "request_changes" && !trimmedFeedback)}
          style={{
            borderRadius: "999px",
            padding: "5px 14px",
            fontSize: "11px",
            fontWeight: 700,
            border: "none",
            cursor: submitting ? "not-allowed" : "pointer",
            opacity: submitting || (pendingAction === "request_changes" && !trimmedFeedback) ? 0.6 : 1,
            background: pendingAction === "confirm_completed" ? "var(--cm-lime)" : "var(--cm-orange)",
            color: pendingAction === "confirm_completed" ? "#0A0A0A" : "#FCE9DD",
          }}
        >
          {submitting ? "Saving…" : pendingAction === "confirm_completed" ? "Confirm completed" : "Send feedback"}
        </button>
        <button
          type="button"
          onClick={cancel}
          disabled={submitting}
          style={{
            borderRadius: "999px",
            padding: "5px 14px",
            fontSize: "11px",
            fontWeight: 600,
            border: "0.5px solid var(--cm-border)",
            background: "transparent",
            color: "var(--cm-text-secondary)",
            cursor: submitting ? "not-allowed" : "pointer",
          }}
        >
          Cancel
        </button>
      </div>
    </div>
  );
}
