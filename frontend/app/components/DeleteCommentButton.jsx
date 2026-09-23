"use client";

import { useState } from "react";
import { Trash2, X } from "lucide-react";
import { deleteComment, ApiError } from "../../lib/api";

/**
 * "Delete" action for a single comment (API-02.4,
 * DELETE /comments/{commentId}). Only ever rendered by the caller for
 * comments the current user owns (see IssueComments.jsx). Two-step: the
 * "Delete" link expands into an explicit "Confirm delete" / "Cancel" pair,
 * so a stray click can't remove a comment (the ticket's confirmation
 * requirement); nothing is deleted until "Confirm delete" is clicked.
 */
export default function DeleteCommentButton({ commentId, onDeleted }) {
  const [confirming, setConfirming] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState("");

  async function handleConfirm() {
    setDeleting(true);
    setError("");

    try {
      await deleteComment(commentId);
      onDeleted(commentId);
    } catch (err) {
      if (err instanceof ApiError && err.status === 403) {
        setError("You can only delete your own comments.");
      } else if (err instanceof ApiError && err.status === 404) {
        // Already gone (e.g. deleted from another tab), reflect that in the UI.
        onDeleted(commentId);
        return;
      } else {
        setError(err.message || "Failed to delete this comment. Please try again.");
      }
      setDeleting(false);
      setConfirming(false);
    }
  }

  if (!confirming) {
    return (
      <button
        type="button"
        onClick={() => setConfirming(true)}
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
        <Trash2 size={11} strokeWidth={2} aria-hidden="true" />
        Delete
      </button>
    );
  }

  return (
    <span style={{ display: "inline-flex", alignItems: "center", gap: "8px", flexWrap: "wrap" }}>
      <span style={{ fontSize: "11.5px", color: "var(--cm-orange-text)", fontWeight: 600 }}>
        Delete this comment?
      </span>
      <button
        type="button"
        onClick={handleConfirm}
        disabled={deleting}
        style={{
          borderRadius: "999px",
          padding: "4px 12px",
          fontSize: "11px",
          fontWeight: 700,
          border: "none",
          cursor: deleting ? "not-allowed" : "pointer",
          opacity: deleting ? 0.6 : 1,
          background: "var(--cm-orange)",
          color: "#FCE9DD",
        }}
      >
        {deleting ? "Deleting…" : "Confirm delete"}
      </button>
      <button
        type="button"
        onClick={() => {
          setConfirming(false);
          setError("");
        }}
        disabled={deleting}
        style={{
          display: "inline-flex",
          alignItems: "center",
          gap: "3px",
          background: "none",
          border: "none",
          padding: 0,
          fontSize: "11px",
          fontWeight: 600,
          color: "var(--cm-text-muted)",
          cursor: deleting ? "not-allowed" : "pointer",
        }}
      >
        <X size={10} strokeWidth={2} aria-hidden="true" />
        Cancel
      </button>
      {error && (
        <span role="alert" style={{ flexBasis: "100%", fontSize: "11px", color: "var(--cm-orange-text)" }}>
          {error}
        </span>
      )}
    </span>
  );
}