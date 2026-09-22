"use client";

import { useState } from "react";
import { Save, X } from "lucide-react";
import { updateProject } from "../../lib/api";

const CONNECTIONS = [
  { value: "south_african", label: "South African" },
  { value: "community_verified", label: "Community verified" },
];

/**
 * Edit form for a project's Code Masters-owned metadata (category, tags,
 * connection, country codes, accepting-contributions toggle). Only rendered
 * for a confirmed maintainer — the backend re-checks this on submit
 * regardless (403 FORBIDDEN), so this component surfaces that error rather
 * than assuming the caller is always authorized.
 */
export default function EditProjectPanel({ project, onCancel, onSaved }) {
  const [category, setCategory] = useState(project.category || "");
  const [tagsInput, setTagsInput] = useState((project.tags || []).join(", "));
  const [connection, setConnection] = useState(project.connection || "south_african");
  const [countryCodesInput, setCountryCodesInput] = useState((project.countryCodes || []).join(", "));
  const [acceptingContributions, setAcceptingContributions] = useState(project.acceptingContributions ?? true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  function parseList(input) {
    return input
      .split(",")
      .map((s) => s.trim())
      .filter(Boolean);
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setSaving(true);
    setError("");

    try {
      const updated = await updateProject(project.id, {
        category: category.trim(),
        tags: parseList(tagsInput),
        connection,
        countryCodes: parseList(countryCodesInput).map((c) => c.toUpperCase()),
        acceptingContributions,
      });
      onSaved(updated);
    } catch (err) {
      if (err.status === 403) {
        setError("Only a maintainer of this project can save changes. You may have lost maintainer access.");
      } else if (err.status === 401) {
        setError("Your session has expired. Please sign in again.");
      } else {
        setError(err.message || "Failed to save changes.");
      }
    } finally {
      setSaving(false);
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

  const labelStyle = {
    display: "block",
    fontSize: "11px",
    fontWeight: 600,
    color: "var(--cm-text-secondary)",
    marginBottom: "6px",
  };

  return (
    <section className="cm-glass" style={{ borderRadius: "24px", padding: "24px", marginBottom: "20px" }}>
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "18px" }}>
        <h2 style={{ fontSize: "15px", fontWeight: 700, margin: 0, color: "var(--cm-text-primary)" }}>
          Edit project
        </h2>
        <button
          type="button"
          onClick={onCancel}
          aria-label="Cancel editing"
          style={{ display: "flex", alignItems: "center", background: "none", border: "none", color: "var(--cm-text-muted)", cursor: "pointer" }}
        >
          <X size={16} strokeWidth={2} />
        </button>
      </div>

      <p style={{ fontSize: "12px", color: "var(--cm-text-muted)", margin: "0 0 18px" }}>
        Only Code Masters-owned fields are editable here — name, description, and stats come from GitHub sync.
      </p>

      <form onSubmit={handleSubmit}>
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "14px", marginBottom: "14px" }}>
          <div>
            <label htmlFor="edit-category" style={labelStyle}>Category</label>
            <input
              id="edit-category"
              type="text"
              value={category}
              onChange={(e) => setCategory(e.target.value)}
              placeholder="e.g. Developer Tools"
              style={fieldStyle}
            />
          </div>

          <div>
            <label htmlFor="edit-connection" style={labelStyle}>Connection</label>
            <select
              id="edit-connection"
              value={connection}
              onChange={(e) => setConnection(e.target.value)}
              style={{ ...fieldStyle, cursor: "pointer" }}
            >
              {CONNECTIONS.map((c) => (
                <option key={c.value} value={c.value}>{c.label}</option>
              ))}
            </select>
          </div>
        </div>

        <div style={{ marginBottom: "14px" }}>
          <label htmlFor="edit-tags" style={labelStyle}>Tags (comma-separated)</label>
          <input
            id="edit-tags"
            type="text"
            value={tagsInput}
            onChange={(e) => setTagsInput(e.target.value)}
            placeholder="e.g. spring-boot, postgres"
            style={fieldStyle}
          />
        </div>

        <div style={{ marginBottom: "18px" }}>
          <label htmlFor="edit-countries" style={labelStyle}>Country codes (comma-separated)</label>
          <input
            id="edit-countries"
            type="text"
            value={countryCodesInput}
            onChange={(e) => setCountryCodesInput(e.target.value)}
            placeholder="e.g. ZA"
            style={fieldStyle}
          />
        </div>

        <label style={{ display: "flex", alignItems: "center", gap: "8px", fontSize: "12.5px", color: "var(--cm-text-secondary)", cursor: "pointer", marginBottom: "20px" }}>
          <input
            type="checkbox"
            checked={acceptingContributions}
            onChange={(e) => setAcceptingContributions(e.target.checked)}
            style={{ width: "15px", height: "15px", accentColor: "var(--cm-lime)" }}
          />
          Currently accepting new contributors
        </label>

        {error && (
          <p role="alert" style={{ fontSize: "12.5px", color: "var(--cm-orange-text)", marginBottom: "14px" }}>
            {error}
          </p>
        )}

        <div style={{ display: "flex", gap: "10px" }}>
          <button
            type="submit"
            disabled={saving}
            style={{
              display: "inline-flex",
              alignItems: "center",
              gap: "8px",
              borderRadius: "999px",
              padding: "10px 20px",
              fontSize: "13px",
              fontWeight: 700,
              border: "none",
              cursor: saving ? "not-allowed" : "pointer",
              opacity: saving ? 0.6 : 1,
              background: "var(--cm-lime)",
              color: "#0A0A0A",
            }}
          >
            <Save size={14} strokeWidth={2} aria-hidden="true" />
            {saving ? "Saving…" : "Save changes"}
          </button>

          <button
            type="button"
            onClick={onCancel}
            disabled={saving}
            style={{
              borderRadius: "999px",
              padding: "10px 20px",
              fontSize: "13px",
              fontWeight: 600,
              border: "0.5px solid var(--cm-border)",
              background: "transparent",
              color: "var(--cm-text-secondary)",
              cursor: saving ? "not-allowed" : "pointer",
            }}
          >
            Cancel
          </button>
        </div>
      </form>
    </section>
  );
}
