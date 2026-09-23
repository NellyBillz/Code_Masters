"use client";

import { useState } from "react";
import { AlertTriangle, Flag, X } from "lucide-react";
import { reportProject, ApiError } from "../../lib/api";

const REASON_MIN_LENGTH = 3;
const REASON_MAX_LENGTH = 500;

/**
 * Confirmation panel for reporting a project listing (API-03.9,
 * POST /projects/{projectId}/reports). Mirrors ReportCommentButton's
 * flow/state handling, laid out as its own panel (like EditProjectPanel)
 * since it's triggered from the project header rather than inline in a list.
 */
export default function ReportProjectPanel({ projectId, onCancel, onReported }) {
  const [reason, setReason] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [status, setStatus] = useState(null); // null | "reported" | "already-reported"

  const trimmedReason = reason.trim();
  const reasonValid =
    trimmedReason.length >= REASON_MIN_LENGTH && trimmedReason.length <= REASON_MAX_LENGTH;

  async function handleSubmit(event) {
    event.preventDefault();
    if (!reasonValid) {
      setError(`Reason must be between ${REASON_MIN_LENGTH} and ${REASON_MAX_LENGTH} characters.`);
      return;
    }

    setSubmitting(true);
    setError("");

    try {
      await reportProject(projectId, trimmedReason);
      setStatus("reported");
      if (onReported) onReported();
    } catch (err) {
      if (err instanceof ApiError && err.status === 409) {
        // Backend's REPORT_ALREADY_EXISTS, not really an error for the
        // caller, treat it the same as a successful report.
        setStatus("already-reported");
        if (onReported) onReported();
        return;
      }
      if (err instanceof ApiError && err.status === 401) {
        setError("Please sign in to report this project.");
      } else if (err instanceof ApiError && err.status === 400) {
        setError(err.message || "That reason isn't valid.");
      } else {
        setError(err.message || "Failed to report this project. Please try again.");
      }
    } finally {
      setSubmitting(false);
    }
  }

  if (status === "reported" || status === "already-reported") {
    return (
      <section className="cm-glass" style={{ borderRadius: "24px", padding: "20px 24px", marginBottom: "20px" }}>
        <p style={{ fontSize: "13px", fontWeight: 600, margin: "0 0 4px", color: "var(--cm-text-primary)" }}>
          {status === "reported" ? "Report submitted" : "Already reported"}
        </p>
        <p style={{ fontSize: "12.5px", color: "var(--cm-text-secondary)", margin: "0 0 14px" }}>
          {status === "reported"
            ? "Thanks — our moderators will take a look at this project."
            : "You've already reported this project. A moderator will review it."}
        </p>
        <button
          type="button"
          onClick={onCancel}
          style={{
            borderRadius: "999px",
            padding: "8px 16px",
            fontSize: "12.5px",
            fontWeight: 600,
            border: "0.5px solid var(--cm-border)",
            background: "transparent",
            color: "var(--cm-text-secondary)",
            cursor: "pointer",
          }}
        >
          Close
        </button>
      </section>
    );
  }

  const fieldStyle = {
    width: "100%",
    borderRadius: "10px",
    border: "0.5px solid var(--cm-border)",
    background: "var(--cm-surface)",
    color: "var(--cm-text-primary)",
    fontSize: "13px",
    padding: "9px 12px",
    outline: "none",
    resize: "vertical",
  };

  return (
    <section className="cm-glass" style={{ borderRadius: "24px", padding: "24px", marginBottom: "20px" }}>
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "14px" }}>
        <h2 style={{ display: "flex", alignItems: "center", gap: "8px", fontSize: "15px", fontWeight: 700, margin: 0, color: "var(--cm-text-primary)" }}>
          <Flag size={15} strokeWidth={2} aria-hidden="true" />
          Report this project
        </h2>
        <button
          type="button"
          onClick={onCancel}
          disabled={submitting}
          aria-label="Cancel report"
          style={{ display: "flex", background: "none", border: "none", color: "var(--cm-text-muted)", cursor: submitting ? "not-allowed" : "pointer" }}
        >
          <X size={16} strokeWidth={2} />
        </button>
      </div>

      <div
        style={{
          display: "flex",
          gap: "10px",
          alignItems: "flex-start",
          background: "var(--cm-orange-soft)",
          borderRadius: "12px",
          padding: "12px 16px",
          marginBottom: "16px",
        }}
      >
        <AlertTriangle size={15} strokeWidth={2} style={{ color: "var(--cm-orange-text)", flexShrink: 0, marginTop: "1px" }} aria-hidden="true" />
        <p style={{ fontSize: "12px", lineHeight: 1.6, color: "var(--cm-orange-text)", margin: 0 }}>
          Let a site admin know why this listing needs a look, e.g. spam, a broken or
          unrelated repo link, abusive content, or a duplicate listing.
        </p>
      </div>

      <form onSubmit={handleSubmit}>
        <label htmlFor="report-project-reason" style={{ display: "block", fontSize: "11px", fontWeight: 600, color: "var(--cm-text-secondary)", marginBottom: "6px" }}>
          Reason
        </label>
        <textarea
          id="report-project-reason"
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          placeholder="What's wrong with this project listing?"
          rows={3}
          maxLength={REASON_MAX_LENGTH}
          disabled={submitting}
          style={fieldStyle}
        />
        <p style={{ fontSize: "11px", color: "var(--cm-text-muted)", margin: "6px 0 0" }}>
          {trimmedReason.length}/{REASON_MAX_LENGTH}
        </p>

        {error && (
          <p role="alert" style={{ fontSize: "12.5px", color: "var(--cm-orange-text)", margin: "12px 0 0" }}>
            {error}
          </p>
        )}

        <div style={{ display: "flex", gap: "10px", marginTop: "18px" }}>
          <button
            type="submit"
            disabled={submitting || !reasonValid}
            style={{
              display: "inline-flex",
              alignItems: "center",
              gap: "8px",
              borderRadius: "999px",
              padding: "10px 20px",
              fontSize: "13px",
              fontWeight: 700,
              border: "none",
              cursor: submitting || !reasonValid ? "not-allowed" : "pointer",
              opacity: submitting || !reasonValid ? 0.5 : 1,
              background: "var(--cm-orange)",
              color: "#FCE9DD",
            }}
          >
            <Flag size={13} strokeWidth={2} aria-hidden="true" />
            {submitting ? "Submitting…" : "Submit report"}
          </button>

          <button
            type="button"
            onClick={onCancel}
            disabled={submitting}
            style={{
              borderRadius: "999px",
              padding: "10px 20px",
              fontSize: "13px",
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
    </section>
  );
}