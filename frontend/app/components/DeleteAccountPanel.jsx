"use client";

import { useState } from "react";
import { AlertTriangle, Trash2, X } from "lucide-react";
import { deleteCurrentUser } from "../../lib/api";

/**
 * Confirmation panel for permanently deleting the caller's own account
 * (API-03.11 / DELETE /users/me). Requires the caller to type their exact
 * username before the delete button becomes active, this is the "cannot be
 * deleted accidentally" safeguard, a single click is never enough.
 */
export default function DeleteAccountPanel({ user, onCancel, onDeleted }) {
  const [confirmText, setConfirmText] = useState("");
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState("");

  const expected = user.username || "";
  const canDelete = confirmText.trim() === expected && !deleting;

  async function handleDelete() {
    if (!canDelete) return;

    setDeleting(true);
    setError("");

    try {
      await deleteCurrentUser();
      // The backend clears the session/CSRF cookies as part of this call,
      // so there's nothing further to await here, just hand off to the
      // caller to update local auth state and redirect.
      onDeleted();
    } catch (err) {
      if (err.status === 401) {
        // Session was already gone server-side, treat this the same as a
        // successful deletion rather than showing a confusing error.
        onDeleted();
        return;
      }
      setError(err.message || "Failed to delete your account. Please try again.");
      setDeleting(false);
    }
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
  };

  return (
    <section className="cm-glass" style={{ borderRadius: "24px", padding: "24px", marginBottom: "20px" }}>
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "14px" }}>
        <h2 style={{ fontSize: "15px", fontWeight: 700, margin: 0, color: "var(--cm-text-primary)" }}>
          Delete account
        </h2>
        <button
          type="button"
          onClick={onCancel}
          disabled={deleting}
          aria-label="Cancel account deletion"
          style={{ display: "flex", alignItems: "center", background: "none", border: "none", color: "var(--cm-text-muted)", cursor: deleting ? "not-allowed" : "pointer" }}
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
          padding: "14px 16px",
          marginBottom: "18px",
        }}
      >
        <AlertTriangle size={16} strokeWidth={2} style={{ color: "var(--cm-orange-text)", flexShrink: 0, marginTop: "1px" }} aria-hidden="true" />
        <p style={{ fontSize: "12.5px", lineHeight: 1.6, color: "var(--cm-orange-text)", margin: 0 }}>
          This permanently deletes your Code Masters account. Your profile, claims and
          comments will be anonymized and you&rsquo;ll be signed out immediately.{" "}
          <strong>This action cannot be undone.</strong>
        </p>
      </div>

      <label htmlFor="delete-account-confirm" style={{ display: "block", fontSize: "11px", fontWeight: 600, color: "var(--cm-text-secondary)", marginBottom: "6px" }}>
        Type <strong>{expected}</strong> to confirm
      </label>
      <input
        id="delete-account-confirm"
        type="text"
        value={confirmText}
        onChange={(e) => setConfirmText(e.target.value)}
        placeholder={expected}
        autoComplete="off"
        disabled={deleting}
        style={fieldStyle}
      />

      {error && (
        <p role="alert" style={{ fontSize: "12.5px", color: "var(--cm-orange-text)", margin: "14px 0 0" }}>
          {error}
        </p>
      )}

      <div style={{ display: "flex", gap: "10px", marginTop: "20px" }}>
        <button
          type="button"
          onClick={handleDelete}
          disabled={!canDelete}
          style={{
            display: "inline-flex",
            alignItems: "center",
            gap: "8px",
            borderRadius: "999px",
            padding: "10px 20px",
            fontSize: "13px",
            fontWeight: 700,
            border: "none",
            cursor: canDelete ? "pointer" : "not-allowed",
            opacity: canDelete ? 1 : 0.5,
            background: "var(--cm-orange)",
            color: "#FCE9DD",
          }}
        >
          <Trash2 size={14} strokeWidth={2} aria-hidden="true" />
          {deleting ? "Deleting…" : "Permanently delete account"}
        </button>

        <button
          type="button"
          onClick={onCancel}
          disabled={deleting}
          style={{
            borderRadius: "999px",
            padding: "10px 20px",
            fontSize: "13px",
            fontWeight: 600,
            border: "0.5px solid var(--cm-border)",
            background: "transparent",
            color: "var(--cm-text-secondary)",
            cursor: deleting ? "not-allowed" : "pointer",
          }}
        >
          Cancel
        </button>
      </div>
    </section>
  );
}
