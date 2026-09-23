"use client";

import { useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { Search, ArrowUpRight, Sparkles } from "lucide-react";
import { search } from "../../lib/api";
import ProjectCard from "../components/ProjectCard";

const PAGE_SIZE = 20;

const TYPES = [
  { value: "all", label: "All" },
  { value: "projects", label: "Projects" },
  { value: "issues", label: "Issues" },
];

const STATUS_STYLES = {
  open: { bg: "var(--cm-lime-soft)", text: "var(--cm-lime-text)" },
  closed: { bg: "var(--cm-surface-alt)", text: "var(--cm-text-secondary)" },
};

const SEARCH_FILTER_SELECT_STYLE = {
  borderRadius: "10px",
  border: "0.5px solid var(--cm-border)",
  background: "var(--cm-surface)",
  color: "var(--cm-text-primary)",
  fontSize: "12.5px",
  padding: "8px 12px",
  outline: "none",
};

export default function SearchPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const q = searchParams.get("q") || "";
  const type = searchParams.get("type") || "all";

  const [queryInput, setQueryInput] = useState(q);
  const [language, setLanguage] = useState("");
  const [difficulty, setDifficulty] = useState("");
  const [page, setPage] = useState(0);
  const [results, setResults] = useState([]);
  const [meta, setMeta] = useState({ page: 0, size: PAGE_SIZE, total: 0 });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  // Keep the input box in sync when the URL's q changes from elsewhere
  // (e.g. the header search form navigating here again with a new term).
  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- syncing local editable state from the URL param it's derived from
    setQueryInput(q);
  }, [q]);

  // A new search term, type, or filter always starts back at page 0.
  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- resetting pagination in response to a q/type/filter change, not derivable inline
    setPage(0);
  }, [q, type, language, difficulty]);

  useEffect(() => {
    if (q.trim().length < 2) {
      // eslint-disable-next-line react-hooks/set-state-in-effect -- clearing stale results when the query drops below the API's minLength, not a fetch
      setResults([]);
      setMeta({ page: 0, size: PAGE_SIZE, total: 0 });
      setError("");
      setLoading(false);
      return;
    }

    let cancelled = false;

    async function runSearch() {
      setLoading(true);
      setError("");
      try {
        const result = await search({
          q,
          type,
          language: language || undefined,
          difficulty: difficulty || undefined,
          size: PAGE_SIZE,
          page,
        });
        if (cancelled) return;
        setResults(result.items || []);
        setMeta(result.meta || { page: 0, size: PAGE_SIZE, total: 0 });
      } catch (err) {
        if (cancelled) return;
        setResults([]);
        setError(err.message || "Search failed.");
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    runSearch();
    return () => {
      cancelled = true;
    };
  }, [q, type, language, difficulty, page]);

  function goTo(nextQ, nextType) {
    const params = new URLSearchParams();
    if (nextQ.trim()) params.set("q", nextQ.trim());
    if (nextType && nextType !== "all") params.set("type", nextType);
    router.push(`/search${params.toString() ? `?${params.toString()}` : ""}`);
  }

  function handleSubmit(event) {
    event.preventDefault();
    goTo(queryInput, type);
  }

  const totalPages = Math.ceil(meta.total / PAGE_SIZE);
  const canGoPrevious = page > 0;
  const canGoNext = page + 1 < totalPages;

  return (
    <div style={{ padding: "8px 4px 40px", maxWidth: "760px", margin: "0 auto" }}>
      <header style={{ marginBottom: "24px" }}>
        <p style={{ fontSize: "12px", fontWeight: 600, letterSpacing: "0.1em", color: "var(--cm-orange-text)", margin: "0 0 8px" }}>
          SEARCH
        </p>
        <h1 style={{ fontSize: "28px", fontWeight: 700, margin: 0, color: "var(--cm-text-primary)" }}>
          {q ? `Results for "${q}"` : "Search Code Masters"}
        </h1>
      </header>

      <form
        onSubmit={handleSubmit}
        className="cm-glass"
        style={{ display: "flex", alignItems: "center", gap: "10px", borderRadius: "999px", padding: "10px 18px", marginBottom: "16px" }}
      >
        <Search size={16} strokeWidth={1.8} color="var(--cm-text-muted)" aria-hidden="true" />
        <input
          type="search"
          value={queryInput}
          onChange={(e) => setQueryInput(e.target.value)}
          placeholder="Search projects, issues, or technologies..."
          aria-label="Search projects, issues, or technologies"
          style={{ flex: 1, border: "none", outline: "none", background: "transparent", fontSize: "14px", color: "var(--cm-text-primary)" }}
        />
        <button
          type="submit"
          style={{
            borderRadius: "999px",
            padding: "8px 16px",
            fontSize: "13px",
            fontWeight: 700,
            border: "none",
            cursor: "pointer",
            background: "var(--cm-lime)",
            color: "#0A0A0A",
          }}
        >
          Search
        </button>
      </form>

      <div style={{ display: "flex", gap: "8px", marginBottom: "16px", flexWrap: "wrap" }}>
        {TYPES.map((t) => {
          const active = type === t.value;
          return (
            <button
              key={t.value}
              type="button"
              onClick={() => {
                setPage(0);
                goTo(q, t.value);
              }}
              style={{
                padding: "7px 16px",
                borderRadius: "999px",
                fontSize: "12.5px",
                fontWeight: 600,
                cursor: "pointer",
                background: active ? "var(--cm-lime)" : "transparent",
                color: active ? "#0A0A0A" : "var(--cm-text-secondary)",
                border: active ? "none" : "0.5px solid var(--cm-border)",
              }}
            >
              {t.label}
            </button>
          );
        })}
      </div>

      <div style={{ display: "flex", gap: "10px", flexWrap: "wrap", marginBottom: "24px" }}>
        {(type === "all" || type === "projects") && (
          <select
            aria-label="Filter by language"
            value={language}
            onChange={(e) => setLanguage(e.target.value)}
            style={SEARCH_FILTER_SELECT_STYLE}
          >
            <option value="">All languages</option>
            <option value="Java">Java</option>
            <option value="Python">Python</option>
            <option value="JavaScript">JavaScript</option>
            <option value="TypeScript">TypeScript</option>
            <option value="C++">C++</option>
          </select>
        )}

        {(type === "all" || type === "issues") && (
          <select
            aria-label="Filter by difficulty"
            value={difficulty}
            onChange={(e) => setDifficulty(e.target.value)}
            style={SEARCH_FILTER_SELECT_STYLE}
          >
            <option value="">All difficulties</option>
            <option value="beginner">Beginner</option>
            <option value="intermediate">Intermediate</option>
            <option value="advanced">Advanced</option>
            <option value="unknown">Unknown</option>
          </select>
        )}
      </div>

      {q.trim().length > 0 && q.trim().length < 2 && (
        <StatusPanel title="Keep typing" text="Search terms need at least 2 characters." />
      )}

      {q.trim().length === 0 && (
        <StatusPanel text="Enter a search term above to find projects and issues." />
      )}

      {q.trim().length >= 2 && loading && <StatusPanel text="Searching…" />}

      {q.trim().length >= 2 && !loading && error && (
        <StatusPanel title="Something went wrong" text={error} />
      )}

      {q.trim().length >= 2 && !loading && !error && results.length === 0 && (
        <StatusPanel title="No results" text={`Nothing matched "${q}". Try a different term or a broader filter.`} />
      )}

      {q.trim().length >= 2 && !loading && !error && results.length > 0 && (
        <>
          <p style={{ fontSize: "12.5px", color: "var(--cm-text-secondary)", margin: "0 0 16px" }}>
            {meta.total} {meta.total === 1 ? "result" : "results"}
          </p>

          <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
            {results.map((item) =>
              item.resultType === "project" ? (
                <ProjectCard key={`project-${item.id}`} project={item} />
              ) : (
                <IssueResultRow key={`issue-${item.id}`} issue={item} />
              )
            )}
          </div>

          <nav aria-label="Search result pagination" style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: "16px", marginTop: "32px" }}>
            <button
              type="button"
              disabled={!canGoPrevious}
              onClick={() => setPage(page - 1)}
              style={{
                borderRadius: "10px",
                border: "0.5px solid var(--cm-border)",
                background: "var(--cm-surface)",
                color: "var(--cm-text-primary)",
                padding: "9px 18px",
                fontSize: "13px",
                fontWeight: 600,
                cursor: canGoPrevious ? "pointer" : "not-allowed",
                opacity: canGoPrevious ? 1 : 0.4,
              }}
            >
              Previous
            </button>

            <span style={{ fontSize: "13px", color: "var(--cm-text-secondary)" }}>
              Page {page + 1} of {Math.max(totalPages, 1)}
            </span>

            <button
              type="button"
              disabled={!canGoNext}
              onClick={() => setPage(page + 1)}
              style={{
                borderRadius: "10px",
                border: "none",
                padding: "9px 18px",
                fontSize: "13px",
                fontWeight: 600,
                background: "var(--cm-sidebar)",
                color: "#FFFFFF",
                cursor: canGoNext ? "pointer" : "not-allowed",
                opacity: canGoNext ? 1 : 0.4,
              }}
            >
              Next
            </button>
          </nav>
        </>
      )}
    </div>
  );
}

function IssueResultRow({ issue }) {
  const statusStyle = STATUS_STYLES[String(issue.status).toLowerCase()] || STATUS_STYLES.closed;

  return (
    <Link href={`/issues/${issue.id}`} className="cm-glass" style={{ display: "block", borderRadius: "20px", padding: "16px 20px" }}>
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: "12px" }}>
        <div style={{ minWidth: 0 }}>
          <div style={{ display: "flex", alignItems: "center", gap: "8px", marginBottom: "6px", flexWrap: "wrap" }}>
            <span
              style={{
                fontSize: "10.5px",
                fontWeight: 600,
                textTransform: "capitalize",
                padding: "2px 9px",
                borderRadius: "999px",
                background: statusStyle.bg,
                color: statusStyle.text,
              }}
            >
              {issue.status || "unknown"}
            </span>
            {issue.isBeginnerFriendly && (
              <span
                style={{
                  display: "inline-flex",
                  alignItems: "center",
                  gap: "4px",
                  fontSize: "10.5px",
                  fontWeight: 600,
                  padding: "2px 9px",
                  borderRadius: "999px",
                  background: "var(--cm-lime-soft)",
                  color: "var(--cm-lime-text)",
                }}
              >
                <Sparkles size={10} strokeWidth={2} aria-hidden="true" />
                Beginner friendly
              </span>
            )}
          </div>
          <p style={{ fontSize: "14px", fontWeight: 600, margin: 0, color: "var(--cm-text-primary)" }}>
            {issue.title}
          </p>
          {issue.bodyExcerpt && (
            <p style={{ fontSize: "12.5px", color: "var(--cm-text-secondary)", margin: "4px 0 0" }}>
              {issue.bodyExcerpt}
            </p>
          )}
        </div>
        <ArrowUpRight size={16} strokeWidth={2} color="var(--cm-text-muted)" aria-hidden="true" style={{ flexShrink: 0 }} />
      </div>
    </Link>
  );
}

function StatusPanel({ title, text }) {
  return (
    <div className="cm-glass" style={{ borderRadius: "24px", padding: "48px 24px", textAlign: "center" }}>
      {title && (
        <p style={{ fontSize: "15px", fontWeight: 600, margin: "0 0 6px", color: "var(--cm-text-primary)" }}>{title}</p>
      )}
      <p style={{ fontSize: "13px", color: "var(--cm-text-secondary)", margin: 0 }}>{text}</p>
    </div>
  );
}
