"use client";

import { useState } from "react";
import { Save, X } from "lucide-react";
import { updateCurrentUser } from "../../lib/api";

const BIO_MAX_LENGTH = 1000;

/**
 * Edit form for the caller's own profile, displayName, bio, location,
 * skills. These are the only fields PATCH /users/me accepts; username,
 * avatarUrl, reputation and email are GitHub-derived/system-managed and
 * aren't editable here.
 */
export default function EditProfilePanel({ user, onCancel, onSaved }) {
  const [displayName, setDisplayName] = useState(user.displayName || "");
  const [bio, setBio] = useState(user.bio || "");
  const [location, setLocation] = useState(user.location || "");
  const [skillsInput, setSkillsInput] = useState((user.skills || []).join(", "));
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  const bioTooLong = bio.length > BIO_MAX_LENGTH;

  async function handleSubmit(event) {
    event.preventDefault();

    if (bioTooLong) {
      setError(`Bio must be at most ${BIO_MAX_LENGTH} characters (currently ${bio.length}).`);
      return;
    }

    setSaving(true);
    setError("");

    try {
      const updated = await updateCurrentUser({
        displayName: displayName.trim(),
        bio: bio.trim(),
        location: location.trim(),
        skills: skillsInput
          .split(",")
          .map((s) => s.trim())
          .filter(Boolean),
      });
      onSaved(updated);
    } catch (err) {
      if (err.status === 401) {
        setError("Your session has expired. Please sign in again.");
      } else if (err.status === 400) {
        setError(err.message || "Some of these values aren't valid.");
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
          Edit profile
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
        Your username and avatar come from GitHub and can&rsquo;t be changed here.
      </p>

      <form onSubmit={handleSubmit}>
        <div style={{ marginBottom: "14px" }}>
          <label htmlFor="edit-display-name" style={labelStyle}>Display name</label>
          <input
            id="edit-display-name"
            type="text"
            value={displayName}
            onChange={(e) => setDisplayName(e.target.value)}
            placeholder={user.username}
            style={fieldStyle}
          />
        </div>

        <div style={{ marginBottom: "14px" }}>
          <div style={{ display: "flex", justifyContent: "space-between" }}>
            <label htmlFor="edit-bio" style={labelStyle}>Bio</label>
            <span style={{ fontSize: "11px", color: bioTooLong ? "var(--cm-orange-text)" : "var(--cm-text-muted)" }}>
              {bio.length}/{BIO_MAX_LENGTH}
            </span>
          </div>
          <textarea
            id="edit-bio"
            value={bio}
            onChange={(e) => setBio(e.target.value)}
            rows={4}
            placeholder="Tell other developers a bit about yourself…"
            style={{ ...fieldStyle, resize: "vertical" }}
          />
        </div>

        <div style={{ marginBottom: "14px" }}>
          <label htmlFor="edit-location" style={labelStyle}>Location</label>
          <input
            id="edit-location"
            type="text"
            value={location}
            onChange={(e) => setLocation(e.target.value)}
            placeholder="e.g. Cape Town, ZA"
            style={fieldStyle}
          />
        </div>

        <div style={{ marginBottom: "20px" }}>
          <label htmlFor="edit-skills" style={labelStyle}>Skills (comma-separated)</label>
          <input
            id="edit-skills"
            type="text"
            value={skillsInput}
            onChange={(e) => setSkillsInput(e.target.value)}
            placeholder="e.g. Java, Spring Boot, PostgreSQL"
            style={fieldStyle}
          />
        </div>

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
