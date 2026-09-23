"use client";

import { useState } from "react";
import { CheckCircle2, XCircle } from "lucide-react";
import { moderateProject, ApiError } from "../../lib/api";

const REASON_MAX_LENGTH = 500;

/**
 * Approve/reject controls for a single pending project submission
 * (POST /admin/projects/{id}/moderation). Only ever rendered by
 * AdminProjectsPage for projects whose listingStatus is "pending". Picking
 * an action expands into an optional reason plus an explicit confirm,
 * mirroring AdminReportActions' two-step pattern so a stray click can't
 * approve/reject a submission.
 */
export default function AdminProjectActions({ project, onUpdated }) {
  const [pendingAction, setPendingAction] = useState(null); // null | "approve" | "reject"
  const [reason, setReason] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  const trimmedReason = reason.trim();

  function startAction(action) {
    setPendingAction(action);
    setReason("");
    setError("");
  }

  function cancel() {
    setPendingAction(null);
    setReason("");
    setError("");
  }

  async function handleConfirm() {
    setSubmitting(true);
    setError("");

    try {
      const updated = await moderateProject(project.id, pendingAction, trimmedReason || undefined);
      onUpdated(updated);
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        setError("Please sign in again to review submissions.");
      } else if (err instanceof ApiError && err.status === 403) {
        setError("You don't have permission to review submissions.");
      } else if (err instanceof ApiError && err.status === 404) {
        setError("This submission no longer exists.");
      } else {
        setError(err.message || "Failed to update this submission. Please try again.");
      }
      setSubmitting(false);
    }
  }

  if (!pendingAction) {
    return (
      <div style={{ display: "inline-flex", gap: "14px" }}>
        <button
          type="button"
          onClick={() => startAction("approve")}
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
          Approve
        </button>
        <button
          type="button"
          onClick={() => startAction("reject")}
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
          <XCircle size={12} strokeWidth={2} aria-hidden="true" />
          Reject
        </button>
      </div>
    );
  }

  return (
    <div style={{ textAlign: "left", width: "220px" }}>
      <p style={{ fontSize: "11.5px", fontWeight: 600, margin: "0 0 6px", color: "var(--cm-text-primary)" }}>
        {pendingAction === "approve" ? "Approve this submission?" : "Reject this submission?"}
      </p>
      <textarea
        value={reason}
        onChange={(e) => setReason(e.target.value)}
        placeholder={pendingAction === "reject" ? "Reason (recommended)" : "Note (optional)"}
        rows={2}
        maxLength={REASON_MAX_LENGTH}
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

      {error && (
        <p role="alert" style={{ fontSize: "11px", color: "var(--cm-orange-text)", margin: "6px 0 0" }}>
          {error}
        </p>
      )}

      <div style={{ display: "flex", gap: "8px", marginTop: "8px" }}>
        <button
          type="button"
          onClick={handleConfirm}
          disabled={submitting}
          style={{
            borderRadius: "999px",
            padding: "5px 14px",
            fontSize: "11px",
            fontWeight: 700,
            border: "none",
            cursor: submitting ? "not-allowed" : "pointer",
            opacity: submitting ? 0.6 : 1,
            background: pendingAction === "approve" ? "var(--cm-lime)" : "var(--cm-orange)",
            color: pendingAction === "approve" ? "#0A0A0A" : "#FCE9DD",
          }}
        >
          {submitting ? "Saving…" : `Confirm ${pendingAction}`}
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
