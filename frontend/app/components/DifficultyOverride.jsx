"use client";

import { useState } from "react";
import { SlidersHorizontal } from "lucide-react";
import { updateIssueClassification } from "../../lib/api";

const selectStyle = {
  borderRadius: "10px",
  border: "0.5px solid var(--cm-border)",
  background: "var(--cm-surface)",
  color: "var(--cm-text-primary)",
  fontSize: "13px",
  padding: "9px 12px",
  outline: "none",
};

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

  if (!isMaintainer) return null;

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
      onUpdate?.(result);
    } catch (err) {
      setError(err.message || "Failed to update issue classification.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="cm-glass" style={{ borderRadius: "24px", padding: "20px 24px" }}>
      <div style={{ display: "flex", alignItems: "center", gap: "8px", marginBottom: "16px" }}>
        <SlidersHorizontal size={15} strokeWidth={1.8} color="var(--cm-text-secondary)" aria-hidden="true" />
        <h2 style={{ fontSize: "15px", fontWeight: 700, margin: 0, color: "var(--cm-text-primary)" }}>
          Difficulty override
        </h2>
        <span style={{ fontSize: "10.5px", fontWeight: 600, color: "var(--cm-text-muted)", marginLeft: "auto" }}>
          Maintainer only
        </span>
      </div>

      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))", gap: "14px", marginBottom: "16px" }}>
        <div>
          <label htmlFor="difficulty-select" style={{ display: "block", fontSize: "11px", color: "var(--cm-text-secondary)", marginBottom: "6px" }}>
            Difficulty
          </label>
          <select
            id="difficulty-select"
            value={difficulty}
            onChange={(e) => setDifficulty(e.target.value)}
            disabled={loading}
            style={{ ...selectStyle, width: "100%" }}
          >
            <option value="">Unknown</option>
            <option value="beginner">Beginner</option>
            <option value="intermediate">Intermediate</option>
            <option value="advanced">Advanced</option>
          </select>
        </div>

        <label style={{ display: "flex", alignItems: "center", gap: "8px", fontSize: "12.5px", color: "var(--cm-text-secondary)", cursor: "pointer", alignSelf: "end", paddingBottom: "9px" }}>
          <input
            id="beginner-friendly-checkbox"
            type="checkbox"
            checked={isBeginnerFriendly}
            onChange={(e) => setIsBeginnerFriendly(e.target.checked)}
            disabled={loading}
            style={{ width: "15px", height: "15px", accentColor: "var(--cm-lime)" }}
          />
          Mark as beginner-friendly
        </label>
      </div>

      <button
        type="button"
        onClick={handleApply}
        disabled={loading}
        style={{
          borderRadius: "999px",
          padding: "9px 20px",
          fontSize: "13px",
          fontWeight: 700,
          border: "none",
          cursor: loading ? "not-allowed" : "pointer",
          opacity: loading ? 0.6 : 1,
          background: "var(--cm-orange)",
          color: "#2B1108",
        }}
      >
        {loading ? "Updating…" : "Apply changes"}
      </button>

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
