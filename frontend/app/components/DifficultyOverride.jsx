"use client";

import { useState } from "react";
import { updateIssueClassification } from "../../lib/api";
import Button from "./ui/Button";
import Select from "./ui/Select";
import Checkbox from "./ui/Checkbox";

export default function DifficultyOverride({
  issueId,
  currentDifficulty,
  currentIsBeginnerFriendly,
  isMaintainer,
  onUpdate,
}) {
  const [difficulty, setDifficulty] = useState(currentDifficulty?.toLowerCase() || "");
  const [isBeginnerFriendly, setIsBeginnerFriendly] = useState(currentIsBeginnerFriendly ?? false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  if (!isMaintainer) {
    return null;
  }

  const handleApply = async () => {
    setError("");
    setSuccess("");
    setLoading(true);

    try {
      const result = await updateIssueClassification(issueId, {
        difficulty: difficulty || null,
        isBeginnerFriendly,
      });
      setSuccess("Classification updated.");
      if (onUpdate) onUpdate(result);
    } catch (err) {
      setError(err.message || "Failed to update issue classification.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="rounded-[10px] border border-border bg-surface p-5">
      <h2 className="mb-4 text-[13px] font-semibold uppercase tracking-wide text-foreground-muted">
        Maintainer controls
      </h2>

      <label htmlFor="difficulty-select" className="mb-1.5 block text-[13px] font-medium text-foreground-secondary">
        Difficulty
      </label>
      <Select
        id="difficulty-select"
        value={difficulty}
        onChange={(e) => setDifficulty(e.target.value)}
        disabled={loading}
      >
        <option value="">Unclassified</option>
        <option value="beginner">Beginner</option>
        <option value="intermediate">Intermediate</option>
        <option value="advanced">Advanced</option>
      </Select>

      <Checkbox
        label="Beginner-friendly"
        checked={isBeginnerFriendly}
        onChange={(e) => setIsBeginnerFriendly(e.target.checked)}
        disabled={loading}
        className="mt-3.5"
      />

      <Button variant="secondary" size="sm" onClick={handleApply} disabled={loading} className="mt-4 w-full">
        {loading ? "Updating…" : "Apply changes"}
      </Button>

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
