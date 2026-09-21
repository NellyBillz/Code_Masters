"use client";

import { useState } from "react";
import { X } from "lucide-react";
import { inviteMaintainer, removeMaintainer } from "../../lib/api";
import Button from "./ui/Button";
import Input from "./ui/Input";
import Avatar from "./ui/Avatar";

/**
 * Maintainer management — only ever rendered to authorized maintainers
 * (design brief §14: "show them separately and only to authorized users"),
 * so it carries its own bordered card rather than blending into the
 * read-only maintainer list next to it.
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
      setSuccess(`Added ${usernameInput} as maintainer.`);
      setUsernameInput("");
      if (onMaintainerAdded) onMaintainerAdded(result);
    } catch (err) {
      setError(err.message || "Failed to add maintainer. Check the username and try again.");
    } finally {
      setLoading(false);
    }
  };

  const handleRemove = async (userId) => {
    if (!confirm("Remove this maintainer?")) return;

    setError("");
    setSuccess("");
    setLoading(true);

    try {
      await removeMaintainer(project.id, userId);
      setSuccess("Maintainer removed.");
      if (onMaintainerRemoved) onMaintainerRemoved(userId);
    } catch (err) {
      setError(err.message || "Failed to remove maintainer.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="rounded-[10px] border border-border bg-surface p-5">
      <h2 className="mb-4 text-[13px] font-semibold uppercase tracking-wide text-foreground-muted">
        Manage maintainers
      </h2>

      {project.maintainers?.length > 0 && (
        <ul className="mb-4 flex flex-col gap-2.5">
          {project.maintainers.map((maint) => {
            const username = maint.user?.username || maint.user?.login || "Unknown";
            const userId = maint.user?.id;

            return (
              <li key={userId} className="flex items-center justify-between gap-2">
                <span className="flex min-w-0 items-center gap-2">
                  <Avatar user={maint.user} size="sm" />
                  <span className="truncate text-sm text-foreground-secondary">{username}</span>
                </span>
                <button
                  type="button"
                  onClick={() => handleRemove(userId)}
                  disabled={loading}
                  aria-label={`Remove ${username} as maintainer`}
                  className="flex-shrink-0 rounded-md p-1 text-foreground-disabled transition-colors hover:bg-surface-subtle hover:text-danger disabled:cursor-not-allowed disabled:opacity-50"
                >
                  <X size={14} strokeWidth={1.75} />
                </button>
              </li>
            );
          })}
        </ul>
      )}

      <form onSubmit={handleInvite} className="flex flex-col gap-2 border-t border-border pt-4">
        <label htmlFor="username-input" className="text-[13px] font-medium text-foreground-secondary">
          Invite by GitHub username
        </label>
        <div className="flex gap-2">
          <Input
            id="username-input"
            type="text"
            value={usernameInput}
            onChange={(e) => setUsernameInput(e.target.value)}
            placeholder="octocat"
            disabled={loading}
          />
          <Button type="submit" variant="secondary" size="sm" disabled={loading} className="flex-shrink-0">
            {loading ? "Adding…" : "Add"}
          </Button>
        </div>
      </form>

      {error && (
        <p role="alert" className="mt-3 text-[13px] text-danger">
          {error}
        </p>
      )}
      {success && (
        <p role="status" className="mt-3 text-[13px] text-success">
          {success}
        </p>
      )}
    </div>
  );
}
