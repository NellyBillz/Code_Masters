"use client";

import { useState } from "react";
import Link from "next/link";
import { UserPlus, X } from "lucide-react";
import { inviteMaintainer, removeMaintainer } from "../../lib/api";

/**
 * MaintainersPanel: Invite and remove maintainers for a project.
 *
 * Only visible to project owners/maintainers. Shows:
 * - List of current maintainers with remove buttons
 * - Form to invite new maintainers by username
 *
 * @param {Object} props
 * @param {Object} project - The project object (with id and maintainers)
 * @param {boolean} isMaintainer - Whether current user is a maintainer
 * @param {Function} onMaintainerAdded - Callback when a maintainer is added
 * @param {Function} onMaintainerRemoved - Callback when a maintainer is removed
 */
export default function MaintainersPanel({
  project,
  isMaintainer,
  onMaintainerAdded,
  onMaintainerRemoved,
}) {
  const [usernameInput, setUsernameInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  // Only show this to maintainers
  if (!isMaintainer || !project) {
    return null;
  }

  const handleInvite = async (e) => {
    e.preventDefault();
    setError("");
    setSuccess("");

    if (!usernameInput.trim()) {
      setError("Please enter a username.");
      return;
    }

    setLoading(true);
    try {
      const result = await inviteMaintainer(project.id, usernameInput);
      setSuccess(`Added ${usernameInput} as maintainer!`);
      setUsernameInput("");
      onMaintainerAdded?.(result);
    } catch (err) {
      setError(err.message || "Failed to add maintainer. Check the username and try again.");
    } finally {
      setLoading(false);
    }
  };

  const handleRemove = async (userId) => {
    if (!confirm("Are you sure you want to remove this maintainer?")) return;

    setError("");
    setSuccess("");
    setLoading(true);

    try {
      await removeMaintainer(project.id, userId);
      setSuccess("Maintainer removed.");
      onMaintainerRemoved?.(userId);
    } catch (err) {
      setError(err.message || "Failed to remove maintainer.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="cm-glass" style={{ borderRadius: "24px", padding: "20px 24px" }}>
      <div style={{ display: "flex", alignItems: "center", gap: "8px", marginBottom: "16px" }}>
        <h2 style={{ fontSize: "15px", fontWeight: 700, margin: 0, color: "var(--cm-text-primary)" }}>
          Manage maintainers
        </h2>
        <span style={{ fontSize: "10.5px", fontWeight: 600, color: "var(--cm-text-muted)", marginLeft: "auto" }}>
          Owner only
        </span>
      </div>

      {project.maintainers?.length > 0 ? (
        <ul style={{ listStyle: "none", margin: "0 0 18px", padding: 0, display: "flex", flexDirection: "column", gap: "6px" }}>
          {project.maintainers.map((maint) => {
            const username = maint.user?.username || maint.user?.login || "Unknown";
            const userId = maint.user?.id;

            return (
              <li
                key={userId}
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                  padding: "9px 14px",
                  borderRadius: "12px",
                  background: "var(--cm-surface-alt)",
                  fontSize: "13px",
                }}
              >
                {maint.user?.username ? (
                  <Link href={`/users/${maint.user.username}`} style={{ color: "var(--cm-text-primary)", fontWeight: 600 }}>
                    {username}
                  </Link>
                ) : (
                  <span style={{ color: "var(--cm-text-primary)" }}>{username}</span>
                )}
                <button
                  type="button"
                  onClick={() => handleRemove(userId)}
                  disabled={loading}
                  aria-label={`Remove ${username}`}
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: "4px",
                    background: "none",
                    border: "none",
                    color: "var(--cm-orange-text)",
                    fontSize: "12px",
                    fontWeight: 600,
                    cursor: loading ? "not-allowed" : "pointer",
                    opacity: loading ? 0.5 : 1,
                  }}
                >
                  <X size={13} strokeWidth={2} aria-hidden="true" />
                  Remove
                </button>
              </li>
            );
          })}
        </ul>
      ) : (
        <p style={{ fontSize: "12.5px", color: "var(--cm-text-muted)", marginBottom: "18px" }}>No maintainers yet.</p>
      )}

      <form onSubmit={handleInvite}>
        <label htmlFor="username-input" style={{ display: "block", fontSize: "11px", color: "var(--cm-text-secondary)", marginBottom: "6px" }}>
          Invite by GitHub username
        </label>
        <div style={{ display: "flex", gap: "8px" }}>
          <input
            id="username-input"
            type="text"
            value={usernameInput}
            onChange={(e) => setUsernameInput(e.target.value)}
            placeholder="e.g. octocat"
            disabled={loading}
            style={{
              flex: 1,
              borderRadius: "10px",
              border: "0.5px solid var(--cm-border)",
              background: "var(--cm-surface)",
              color: "var(--cm-text-primary)",
              fontSize: "13px",
              padding: "9px 12px",
              outline: "none",
            }}
          />
          <button
            type="submit"
            disabled={loading}
            style={{
              display: "inline-flex",
              alignItems: "center",
              gap: "6px",
              borderRadius: "10px",
              padding: "9px 16px",
              fontSize: "13px",
              fontWeight: 700,
              border: "none",
              cursor: loading ? "not-allowed" : "pointer",
              opacity: loading ? 0.6 : 1,
              background: "var(--cm-lime)",
              color: "#0A0A0A",
              whiteSpace: "nowrap",
            }}
          >
            <UserPlus size={14} strokeWidth={2} aria-hidden="true" />
            {loading ? "Adding…" : "Add"}
          </button>
        </div>
      </form>

      {error && (
        <p role="alert" style={{ fontSize: "12.5px", color: "var(--cm-orange-text)", marginTop: "12px" }}>
          {error}
        </p>
      )}
      {success && (
        <p role="status" style={{ fontSize: "12.5px", color: "var(--cm-lime-text)", marginTop: "12px" }}>
          {success}
        </p>
      )}
    </section>
  );
}
