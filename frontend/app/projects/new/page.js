"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { ChevronDown } from "lucide-react";
import { createProject, syncProject, getSyncJob, ApiError } from "../../../lib/api";
import { useAuth } from "../../context/AuthContext";
import { CATEGORIES, OTHER_CATEGORY as OTHER } from "../../../lib/projectCategories";

// Mirrors CreateProjectRequest's own @Pattern regex exactly (backend:
// dto/project/CreateProjectRequest.java) so an obviously-wrong URL is
// caught before an API round-trip, not because the frontend re-derives
// its own idea of what's valid.
const GITHUB_URL_PATTERN = /^https:\/\/github\.com\/[\w.-]+\/[\w.-]+\/?$/;

// No automatic South-African-connection detection exists anywhere in the
// platform yet (no algorithm in the sync pipeline, nothing in the product
// spec defines one) — asking a submitter to self-declare "South African"
// vs "community verified" would just be asking them to answer a question
// the platform itself hasn't defined criteria for. Every new submission
// is honestly "not yet verified" until a maintainer/curator corrects it
// via the project's Edit panel, which is where that judgment call belongs.
const DEFAULT_CONNECTION = "community_verified";

// The platform is scoped to South Africa (product definition, throughout) —
// there's nothing for a submitter to choose here, so it isn't asked.
const DEFAULT_COUNTRY_CODES = ["ZA"];

export default function SubmitProjectPage() {
  const router = useRouter();
  const { user, loading: authLoading } = useAuth();

  const [githubUrl, setGithubUrl] = useState("");
  const [category, setCategory] = useState(CATEGORIES[0]);
  const [customCategory, setCustomCategory] = useState("");
  const [tagsInput, setTagsInput] = useState("");

  const [fieldErrors, setFieldErrors] = useState({});
  const [generalError, setGeneralError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [submitLabel, setSubmitLabel] = useState("Submitting…");

  if (authLoading) {
    return <StatusPanel text="Checking session…" />;
  }

  if (!user) {
    return (
      <StatusPanel title="Sign in to submit a project">
        <a
          href="/auth/github"
          style={{
            display: "inline-flex",
            alignItems: "center",
            gap: "8px",
            marginTop: "14px",
            background: "var(--cm-orange)",
            color: "#FCE9DD",
            fontSize: "13px",
            fontWeight: 600,
            borderRadius: "8px",
            padding: "10px 18px",
          }}
        >
          Sign in with GitHub
        </a>
      </StatusPanel>
    );
  }

  const effectiveCategory = category === OTHER ? customCategory.trim() : category;

  function parseList(input) {
    return input
      .split(",")
      .map((s) => s.trim())
      .filter(Boolean);
  }

  function validate() {
    const errors = {};
    const trimmedUrl = githubUrl.trim();

    if (!trimmedUrl) {
      errors.githubUrl = "githubUrl must not be blank";
    } else if (!GITHUB_URL_PATTERN.test(trimmedUrl)) {
      errors.githubUrl = "githubUrl must be a GitHub repository URL, e.g. https://github.com/owner/repo";
    }

    if (!effectiveCategory) {
      errors.category = "category is required";
    }

    return errors;
  }

  // Waits for the sync job to finish so the page we redirect to already has
  // real GitHub data instead of a bare stub — but never for long, and never
  // fatally. A slow or rate-limited GitHub call must not trap someone who
  // already successfully submitted; per api-design.md Rule 5, a sync
  // failure never breaks anything else, so on timeout/error we just move on
  // and let the project page show whatever it has (a maintainer can always
  // retry the sync from there once that UI exists).
  async function waitForSync(jobId, { intervalMs = 1200, maxAttempts = 8 } = {}) {
    for (let attempt = 0; attempt < maxAttempts; attempt += 1) {
      let job;
      try {
        job = await getSyncJob(jobId);
      } catch {
        return;
      }
      if (job.status === "completed" || job.status === "failed") return;
      await new Promise((resolve) => setTimeout(resolve, intervalMs));
    }
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setGeneralError("");

    const clientErrors = validate();
    if (Object.keys(clientErrors).length > 0) {
      setFieldErrors(clientErrors);
      return;
    }
    setFieldErrors({});

    setSubmitting(true);
    setSubmitLabel("Submitting…");
    let created;
    try {
      created = await createProject({
        githubUrl: githubUrl.trim(),
        connection: DEFAULT_CONNECTION,
        category: effectiveCategory,
        tags: parseList(tagsInput),
        countryCodes: DEFAULT_COUNTRY_CODES,
      });
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        setGeneralError("Your session has expired. Please sign in again.");
      } else if (err instanceof ApiError && err.code === "VALIDATION_ERROR" && err.body?.details) {
        setFieldErrors(err.body.details);
      } else if (err instanceof ApiError && err.code === "PROJECT_ALREADY_EXISTS") {
        setFieldErrors({ githubUrl: err.message });
      } else {
        setGeneralError(err.message || "Failed to submit project.");
      }
      setSubmitting(false);
      return;
    }

    // The project itself is already saved at this point — anything past
    // here is best-effort enrichment, not something that should ever make
    // submission look like it failed.
    let jobId = null;
    try {
      setSubmitLabel("Fetching project details from GitHub…");
      const job = await syncProject(created.id);
      jobId = job.id;
      await waitForSync(jobId);
    } catch {
      // Sync couldn't even start (e.g. a transient error) — the project
      // still exists and redirecting to it is still correct. No jobId to
      // pass through means the project page just won't show a sync banner.
    }

    // syncJobId lets the project page show what happened (synced fine,
    // failed with why, or still running) instead of the redirect landing
    // on a page that gives no sign anything happened at all.
    router.push(jobId ? `/projects/${created.id}?syncJobId=${jobId}` : `/projects/${created.id}`);
  }

  const fieldStyle = {
    width: "100%",
    borderRadius: "10px",
    border: "0.5px solid var(--cm-border)",
    background: "var(--cm-surface)",
    color: "var(--cm-text-primary)",
    fontSize: "13px",
    padding: "10px 12px",
    outline: "none",
  };

  const errorFieldStyle = { ...fieldStyle, border: "0.5px solid var(--cm-orange)" };

  const labelStyle = {
    display: "block",
    fontSize: "11px",
    fontWeight: 600,
    color: "var(--cm-text-secondary)",
    marginBottom: "6px",
  };

  return (
    <div style={{ padding: "8px 4px 40px", maxWidth: "620px", margin: "0 auto" }}>
      <header style={{ marginBottom: "24px" }}>
        <p style={{ fontSize: "12px", fontWeight: 600, letterSpacing: "0.1em", color: "var(--cm-orange-text)", margin: "0 0 8px" }}>
          PROJECT SUBMISSION
        </p>
        <h1 style={{ fontSize: "28px", fontWeight: 700, margin: 0, color: "var(--cm-text-primary)" }}>
          Submit a project
        </h1>
        <p style={{ fontSize: "13px", color: "var(--cm-text-secondary)", margin: "8px 0 0" }}>
          You&rsquo;ll be listed as the owner. GitHub details like name, description
          and stars are filled in automatically once the project syncs. New
          submissions are reviewed before they appear publicly.
        </p>
      </header>

      <form onSubmit={handleSubmit} className="cm-glass" style={{ borderRadius: "24px", padding: "24px" }}>
        <div style={{ marginBottom: "16px" }}>
          <label htmlFor="github-url" style={labelStyle}>GitHub repository URL *</label>
          <input
            id="github-url"
            type="text"
            value={githubUrl}
            onChange={(e) => setGithubUrl(e.target.value)}
            placeholder="https://github.com/owner/repo"
            style={fieldErrors.githubUrl ? errorFieldStyle : fieldStyle}
            aria-invalid={Boolean(fieldErrors.githubUrl)}
            aria-describedby={fieldErrors.githubUrl ? "github-url-error" : undefined}
          />
          {fieldErrors.githubUrl && (
            <p id="github-url-error" role="alert" style={{ fontSize: "12px", color: "var(--cm-orange-text)", margin: "6px 0 0" }}>
              {fieldErrors.githubUrl}
            </p>
          )}
        </div>

        <div style={{ marginBottom: category === OTHER ? "10px" : "16px" }}>
          <label htmlFor="category" style={labelStyle}>Category *</label>
          <div style={{ position: "relative" }}>
            <select
              id="category"
              value={category}
              onChange={(e) => setCategory(e.target.value)}
              style={{
                ...(fieldErrors.category ? errorFieldStyle : fieldStyle),
                appearance: "none",
                WebkitAppearance: "none",
                MozAppearance: "none",
                padding: "10px 34px 10px 12px",
                cursor: "pointer",
              }}
            >
              {CATEGORIES.map((c) => (
                <option key={c} value={c}>{c}</option>
              ))}
              <option value={OTHER}>Other…</option>
            </select>
            <ChevronDown
              size={15}
              strokeWidth={2}
              aria-hidden="true"
              style={{ position: "absolute", right: "10px", top: "50%", transform: "translateY(-50%)", color: "var(--cm-text-secondary)", pointerEvents: "none" }}
            />
          </div>
          {fieldErrors.category && (
            <p role="alert" style={{ fontSize: "12px", color: "var(--cm-orange-text)", margin: "6px 0 0" }}>
              {fieldErrors.category}
            </p>
          )}
        </div>

        {category === OTHER && (
          <div style={{ marginBottom: "16px" }}>
            <label htmlFor="custom-category" style={labelStyle}>Custom category</label>
            <input
              id="custom-category"
              type="text"
              value={customCategory}
              onChange={(e) => setCustomCategory(e.target.value)}
              placeholder="e.g. Developer Tools"
              style={fieldStyle}
            />
          </div>
        )}

        <div style={{ marginBottom: "20px" }}>
          <label htmlFor="tags" style={labelStyle}>Tags (comma-separated, optional)</label>
          <input
            id="tags"
            type="text"
            value={tagsInput}
            onChange={(e) => setTagsInput(e.target.value)}
            placeholder="e.g. spring-boot, postgres"
            style={fieldErrors.tags ? errorFieldStyle : fieldStyle}
          />
          {fieldErrors.tags && (
            <p role="alert" style={{ fontSize: "12px", color: "var(--cm-orange-text)", margin: "6px 0 0" }}>
              {fieldErrors.tags}
            </p>
          )}
        </div>

        {generalError && (
          <p role="alert" style={{ fontSize: "12.5px", color: "var(--cm-orange-text)", marginBottom: "16px" }}>
            {generalError}
          </p>
        )}

        <button
          type="submit"
          disabled={submitting}
          style={{
            borderRadius: "999px",
            padding: "11px 24px",
            fontSize: "13px",
            fontWeight: 700,
            border: "none",
            cursor: submitting ? "not-allowed" : "pointer",
            opacity: submitting ? 0.6 : 1,
            background: "var(--cm-lime)",
            color: "#0A0A0A",
          }}
        >
          {submitting ? submitLabel : "Submit project"}
        </button>
      </form>
    </div>
  );
}

function StatusPanel({ title, text, children }) {
  return (
    <div style={{ padding: "8px 4px 40px", maxWidth: "620px", margin: "0 auto" }}>
      <div className="cm-glass" style={{ borderRadius: "24px", padding: "48px 24px", textAlign: "center" }}>
        {title && (
          <p style={{ fontSize: "15px", fontWeight: 600, margin: "0 0 6px", color: "var(--cm-text-primary)" }}>{title}</p>
        )}
        {text && <p style={{ fontSize: "13px", color: "var(--cm-text-secondary)", margin: 0 }}>{text}</p>}
        {children}
      </div>
    </div>
  );
}
