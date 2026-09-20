"use client";

import { useState } from "react";
import { updateIssueClassification } from "../../lib/api";

export default function DifficultyOverride({
  issueId,
  currentDifficulty,
  currentIsBeginnerFriendly,
  isMaintainer,
  onUpdate,
}) {
  const [difficulty, setDifficulty] = useState(
    currentDifficulty?.toLowerCase() || ""
  );
  const [isBeginnerFriendly, setIsBeginnerFriendly] = useState(
    currentIsBeginnerFriendly ?? false
  );
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

      setSuccess("Issue classification updated.");

      if (onUpdate) {
        onUpdate(result);
      }
    } catch (err) {
      setError(err.message || "Failed to update issue classification.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <section style={{ marginTop: "2rem" }}>
      <h2>Difficulty Override</h2>

      <label htmlFor="difficulty-select">Difficulty:</label>
      <select
        id="difficulty-select"
        value={difficulty}
        onChange={(e) => setDifficulty(e.target.value)}
        disabled={loading}
        style={{ display: "block", marginTop: "0.5rem", padding: "0.5rem" }}
      >
        <option value="">Unknown</option>
        <option value="beginner">Beginner</option>
        <option value="intermediate">Intermediate</option>
        <option value="advanced">Advanced</option>
      </select>

      <label
        htmlFor="beginner-friendly-checkbox"
        style={{ display: "block", marginTop: "1rem" }}
      >
        <input
          id="beginner-friendly-checkbox"
          type="checkbox"
          checked={isBeginnerFriendly}
          onChange={(e) => setIsBeginnerFriendly(e.target.checked)}
          disabled={loading}
          style={{ marginRight: "0.5rem" }}
        />
        Mark as beginner-friendly
      </label>

      <button
        type="button"
        onClick={handleApply}
        disabled={loading}
        style={{ marginTop: "1rem", padding: "0.5rem 1rem" }}
      >
        {loading ? "Updating..." : "Apply changes"}
      </button>

      {error && (
        <p role="alert" style={{ marginTop: "1rem" }}>
          {error}
        </p>
      )}

      {success && (
        <p role="status" style={{ marginTop: "1rem" }}>
          {success}
        </p>
      )}
    </section>
  );
}
