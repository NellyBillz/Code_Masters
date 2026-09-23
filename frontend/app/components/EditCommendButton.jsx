"use client";

import { useState } from "react";
import { Pencil, X } from "lucide-react";
import { updateComment, ApiError } from "../../lib/api";

/**
 * "Edit" action for a single comment (API-02.4,
 * PATCH /comments/{commentId}). Only ever rendered by the caller for
 * comments the current user owns (see IssueComments.jsx) — the backend
 * enforces the same author-only rule server-side (403 FORBIDDEN otherwise),
 * this component just doesn't expose the trigger to non-owners in the UI.
 *
 * Renders either:
 *  - the comment's normal body text + an "Edit" link, or
 *  - an inline textarea (pre-filled with the current body) + Save/Cancel,
 *    while `editing` is true.
 */
export default function EditCommentButton({ comment, onSaved }) {
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState(comment.body);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const trimmedDraft = draft.trim();
  const draftValid = trimmedDraft.length >= 1 && trimmedDraft.length <= 5000;

  function startEditing() {
    setDraft(comment.body);
    setError("");
    setEditing(true);
  }

  function cancelEditing() {
    setEditing(false);
    setDraft(comment.body);
    setError("");
  }

  async function handleSave(event) {
    event.preventDefault();
    if (!draftValid) {
      setError("Comment must be between 1 and 5000 characters.");
      return;
    }
    if (trimmedDraft === comment.body) {
      setEditing(false);
      return;
    }

    setSaving(true);
    setError("");

    try {
      const updated = await updateComment(comment.id, trimmedDraft);
      setEditing(false);
      onSaved(updated);
    } catch (err) {
      if (err instanceof ApiError && err.status === 403) {
        setError("You can only edit your own comments.");
      } else if (err instanceof ApiError && err.status === 404) {
        setError("This comment no longer exists.");
      } else if (err instanceof ApiError && err.status === 400) {
        setError(err.message || "That comment isn't valid.");
      } else {
        setError(err.message || "Failed to save your changes. Please try again.");
      }
    } finally {
      setSaving(false);
    }
  }

  if (!editing) {
    return (
      <>
        <p style={{ fontSize: "13px", color: "var(--cm-text-primary)", margin: "0 0 6px", lineHeight: 1.5 }}>
          {comment.body}
          {comment.edited && (
            <span style={{ fontSize: "11px", color: "var(--cm-text-muted)", marginLeft: "6px" }}>(edited)</span>
          )}
        </p>
        <button
          type="button"
          onClick={startEditing}
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
          <Pencil size={11} strokeWidth={2} aria-hidden="true" />
          Edit
        </button>
      </>
    );
  }

  return (
    <form onSubmit={handleSave} style={{ flexBasis: "100%" }}>
      <textarea
        value={draft}
        onChange={(e) => setDraft(e.target.value)}
        rows={3}
        maxLength={5000}
        autoFocus
        disabled={saving}
        style={{
          width: "100%",
          borderRadius: "10px",
          border: "0.5px solid var(--cm-border)",
          background: "var(--cm-surface)",
          color: "var(--cm-text-primary)",
          fontSize: "13px",
          padding: "9px 12px",
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
          disabled={saving || !draftValid}
          style={{
            borderRadius: "999px",
            padding: "6px 14px",
            fontSize: "11.5px",
            fontWeight: 700,
            border: "none",
            cursor: saving || !draftValid ? "not-allowed" : "pointer",
            opacity: saving || !draftValid ? 0.5 : 1,
            background: "var(--cm-sidebar)",
            color: "#FFFFFF",
          }}
        >
          {saving ? "Saving…" : "Save"}
        </button>
        <button
          type="button"
          onClick={cancelEditing}
          disabled={saving}
          style={{
            display: "inline-flex",
            alignItems: "center",
            gap: "4px",
            borderRadius: "999px",
            padding: "6px 14px",
            fontSize: "11.5px",
            fontWeight: 600,
            border: "0.5px solid var(--cm-border)",
            background: "transparent",
            color: "var(--cm-text-secondary)",
            cursor: saving ? "not-allowed" : "pointer",
          }}
        >
          <X size={11} strokeWidth={2} aria-hidden="true" />
          Cancel
        </button>
      </div>
    </form>
  );
}