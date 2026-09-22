"use client";

import { useState } from "react";
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
      if (onMaintainerAdded) {
        onMaintainerAdded(result);
      }
    } catch (err) {
      setError(
        err.message || "Failed to add maintainer. Check the username and try again."
      );
    } finally {
      setLoading(false);
    }
  };

  const handleRemove = async (userId) => {
    if (!confirm("Are you sure you want to remove this maintainer?")) {
      return;
    }

    setError("");
    setSuccess("");
    setLoading(true);

    try {
      await removeMaintainer(project.id, userId);
      setSuccess("Maintainer removed.");
      if (onMaintainerRemoved) {
        onMaintainerRemoved(userId);
      }
    } catch (err) {
      setError(err.message || "Failed to remove maintainer.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <section style={{ marginTop: "2rem", padding: "1rem", border: "1px solid #ccc" }}>
      <h2>Maintainers (Admin)</h2>

      {/* Current maintainers list */}
      <div style={{ marginBottom: "1.5rem" }}>
        <h3>Current maintainers</h3>
        {project.maintainers?.length > 0 ? (
          <ul>
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
                    padding: "0.5rem 0",
                  }}
                >
                  <span>{username}</span>
                  <button
                    onClick={() => handleRemove(userId)}
                    disabled={loading}
                    style={{ color: "#d9534f", cursor: "pointer" }}
                  >
                    Remove
                  </button>
                </li>
              );
            })}
          </ul>
        ) : (
          <p>No maintainers yet.</p>
        )}
      </div>

      {/* Invite form */}
      <form onSubmit={handleInvite} style={{ marginBottom: "1rem" }}>
        <h3>Invite a maintainer</h3>
        <div style={{ marginBottom: "0.5rem" }}>
          <label htmlFor="username-input">GitHub username:</label>
          <br />
          <input
            id="username-input"
            type="text"
            value={usernameInput}
            onChange={(e) => setUsernameInput(e.target.value)}
            placeholder="e.g., octocat"
            disabled={loading}
            style={{
              padding: "0.5rem",
              marginTop: "0.25rem",
              width: "100%",
              maxWidth: "300px",
            }}
          />
        </div>

        <button
          type="submit"
          disabled={loading}
          style={{
            padding: "0.5rem 1rem",
            backgroundColor: "#5cb85c",
            color: "white",
            border: "none",
            cursor: loading ? "not-allowed" : "pointer",
            opacity: loading ? 0.6 : 1,
          }}
        >
          {loading ? "Adding..." : "Add maintainer"}
        </button>
      </form>

      {/* Messages */}
      {error && (
        <div
          style={{
            padding: "0.75rem",
            backgroundColor: "#f2dede",
            color: "#a94442",
            marginBottom: "0.5rem",
            borderRadius: "4px",
          }}
          role="alert"
        >
          {error}
        </div>
      )}

      {success && (
        <div
          style={{
            padding: "0.75rem",
            backgroundColor: "#dff0d8",
            color: "#3c763d",
            marginBottom: "0.5rem",
            borderRadius: "4px",
          }}
          role="status"
        >
          {success}
        </div>
      )}
    </section>
  );
}