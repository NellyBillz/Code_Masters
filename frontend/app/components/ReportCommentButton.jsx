"use client";

import { useState } from "react";
import { Flag, X } from "lucide-react";
import { reportComment, ApiError } from "../../lib/api";

const REASON_MIN_LENGTH = 3;
const REASON_MAX_LENGTH = 500;

/**
 * "Report" action for a single comment (API-03.9 /
 * POST /comments/{commentId}/reports). Three states:
 *  - idle: just a "Report" button.
 *  - confirming: an inline reason field + Submit/Cancel (the required
 *    confirmation UI, and where the API-required `reason` is collected).
 *  - done: "Reported" if it succeeded, or "Already reported" if the
 *    backend says this caller already flagged this comment (409).
 * All state transitions go through lib/api.js's reportComment(), never a
 * direct fetch() here.
 */
export default function ReportCommentButton({ commentId }) {
  const [state, setState] = useState("idle"); // idle | confirming | submitting | reported | already-reported
  const [reason, setReason] = useState("");
  const [error, setError] = useState("");

  const trimmedReason = reason.trim();
  const reasonValid =
    trimmedReason.length >= REASON_MIN_LENGTH && trimmedReason.length <= REASON_MAX_LENGTH;

  async function handleSubmit(event) {
    event.preventDefault();
    if (!reasonValid) {
      setError(`Reason must be between ${REASON_MIN_LENGTH} and ${REASON_MAX_LENGTH} characters.`);
      return;
    }

    setState("submitting");
    setError("");

    try {
      await reportComment(commentId, trimmedReason);
      setState("reported");
    } catch (err) {
      if (err instanceof ApiError && err.status === 409) {
        // Caller already reported this comment, backend's REPORT_ALREADY_EXISTS.
        // Not really an error from the user's point of view, show it as done.
        setState("already-reported");
        return;
      }
      if (err instanceof ApiError && err.status === 401) {
        setError("Please sign in to report this comment.");
      } else if (err instanceof ApiError && err.status === 400) {
        setError(err.message || "That reason isn't valid.");
      } else {
        setError(err.message || "Failed to report this comment. Please try again.");
      }
      setState("confirming");
    }
  }

  if (state === "reported") {
    return (
      <span style={{ fontSize: "11.5px", fontWeight: 600, color: "var(--cm-text-muted)" }}>
        Reported — thanks, our moderators will take a look.
      </span>
    );
  }

  if (state === "already-reported") {
    return (
      <span style={{ fontSize: "11.5px", fontWeight: 600, color: "var(--cm-text-muted)" }}>
        You&rsquo;ve already reported this comment.
      </span>
    );
  }

  if (state === "idle") {
    return (
      <button
        type="button"
        onClick={() => setState("confirming")}
        style={{
          display: "inline-flex",
          alignItems: "center",
          gap: "4px",
          background: "none",
          border: "none",
          padding: 0,
          fontSize: "11.5px",
          fontWeight: 600,
          color: "var(--cm-text-muted)",
          cursor: "pointer",
        }}
      >
        <Flag size={11} strokeWidth={2} aria-hidden="true" />
        Report
      </button>
    );
  }

  // confirming or submitting
  const submitting = state === "submitting";
  return (
    <form
      onSubmit={handleSubmit}
      style={{
        flexBasis: "100%",
        marginTop: "8px",
        background: "var(--cm-surface)",
        border: "0.5px solid var(--cm-border)",
        borderRadius: "10px",
        padding: "10px 12px",
      }}
    >
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "6px" }}>
        <span style={{ fontSize: "11.5px", fontWeight: 700, color: "var(--cm-text-primary)" }}>
          Why are you reporting this comment?
        </span>
        <button
          type="button"
          onClick={() => {
            setState("idle");
            setReason("");
            setError("");
          }}
          disabled={submitting}
          aria-label="Cancel report"
          style={{ display: "flex", background: "none", border: "none", color: "var(--cm-text-muted)", cursor: submitting ? "not-allowed" : "pointer" }}
        >
          <X size={13} strokeWidth={2} />
        </button>
      </div>

      <textarea
        value={reason}
        onChange={(e) => setReason(e.target.value)}
        placeholder="e.g. Spam, harassment, off-topic…"
        rows={2}
        maxLength={REASON_MAX_LENGTH}
        disabled={submitting}
        style={{
          width: "100%",
          borderRadius: "8px",
          border: "0.5px solid var(--cm-border)",
          background: "var(--cm-surface-alt)",
          color: "var(--cm-text-primary)",
          fontSize: "12px",
          padding: "8px 10px",
          resize: "vertical",
          marginBottom: "8px",
        }}
      />

      {error && (
        <p role="alert" style={{ fontSize: "11.5px", color: "var(--cm-orange-text)", margin: "0 0 8px" }}>
          {error}
        </p>
      )}

      <div style={{ display: "flex", gap: "8px" }}>
        <button
          type="submit"
          disabled={submitting || !reasonValid}
          style={{
            borderRadius: "999px",
            padding: "6px 14px",
            fontSize: "11.5px",
            fontWeight: 700,
            border: "none",
            cursor: submitting || !reasonValid ? "not-allowed" : "pointer",
            opacity: submitting || !reasonValid ? 0.5 : 1,
            background: "var(--cm-orange)",
            color: "#FCE9DD",
          }}
        >
          {submitting ? "Submitting…" : "Submit report"}
        </button>
        <button
          type="button"
          onClick={() => {
            setState("idle");
            setReason("");
            setError("");
          }}
          disabled={submitting}
          style={{
            borderRadius: "999px",
            padding: "6px 14px",
            fontSize: "11.5px",
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
    </form>
  );
}