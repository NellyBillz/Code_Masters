"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { ChevronDown, Plus } from "lucide-react";
import { listProjects } from "../../lib/api";
import ProjectCard from "../components/ProjectCard";

const PAGE_SIZE = 20;

const CATEGORIES = ["All", "Web", "Mobile", "AI", "Backend", "Game"];

export default function Projects() {
  const [filters, setFilters] = useState({
    q: "",
    language: "",
    category: "",
    tag: "",
    hasBeginnerIssues: false,
    sort: "relevance",
  });

  const [projects, setProjects] = useState([]);
  const [meta, setMeta] = useState({ page: 0, size: PAGE_SIZE, total: 0 });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const updateFilter = (key, value) => {
    setFilters((current) => ({ ...current, [key]: value }));
    setMeta((current) => ({ ...current, page: 0 }));
  };

  useEffect(() => {
    let cancelled = false;

    async function loadProjects() {
      setLoading(true);
      setError("");

      try {
        const result = await listProjects({
          page: meta.page,
          size: PAGE_SIZE,
          q: filters.q,
          language: filters.language,
          category: filters.category,
          tag: filters.tag,
          hasBeginnerIssues: filters.hasBeginnerIssues ? true : undefined,
          sort: filters.sort,
        });

        if (cancelled) return;
        setProjects(result.items);
        setMeta(result.meta);
      } catch (err) {
        if (cancelled) return;
        setProjects([]);
        setError(err.message || "Failed to load projects.");
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    loadProjects();
    return () => {
      cancelled = true;
    };
  }, [filters.q, filters.language, filters.category, filters.tag, filters.hasBeginnerIssues, filters.sort, meta.page]);

  const totalPages = Math.ceil(meta.total / PAGE_SIZE);
  const canGoPrevious = meta.page > 0;
  const canGoNext = meta.page + 1 < totalPages;

  const selectStyle = {
    borderRadius: "10px",
    border: "0.5px solid var(--cm-border)",
    background: "var(--cm-surface)",
    color: "var(--cm-text-primary)",
    fontSize: "13px",
    padding: "9px 12px",
    outline: "none",
  };

  return (
    <div style={{ padding: "8px 4px 40px" }}>
      <header style={{ display: "flex", alignItems: "flex-end", justifyContent: "space-between", gap: "16px", flexWrap: "wrap", marginBottom: "24px" }}>
        <div>
          <p style={{ fontSize: "12px", fontWeight: 600, letterSpacing: "0.1em", color: "var(--cm-orange-text)", margin: "0 0 8px" }}>
            OPEN-SOURCE DISCOVERY
          </p>
          <h1 style={{ fontSize: "32px", fontWeight: 700, margin: 0, color: "var(--cm-text-primary)" }}>
            Projects
          </h1>
          <p style={{ fontSize: "14px", color: "var(--cm-text-secondary)", margin: "8px 0 0", maxWidth: "560px" }}>
            Discover South African open-source projects making an impact.
          </p>
        </div>

        <Link
          href="/projects/new"
          style={{
            display: "inline-flex",
            alignItems: "center",
            gap: "6px",
            borderRadius: "999px",
            padding: "10px 18px",
            fontSize: "13px",
            fontWeight: 700,
            background: "var(--cm-lime)",
            color: "#0A0A0A",
            flexShrink: 0,
          }}
        >
          <Plus size={15} strokeWidth={2.2} aria-hidden="true" />
          Submit a project
        </Link>
      </header>

      {/* Filters */}
      <section
        aria-label="Project filters"
        className="cm-glass"
        style={{ borderRadius: "24px", padding: "20px", marginBottom: "24px" }}
      >
        <label htmlFor="search" style={{ display: "block", fontSize: "12px", fontWeight: 600, marginBottom: "8px", color: "var(--cm-text-primary)" }}>
          Search projects
        </label>
        <input
          id="search"
          type="text"
          placeholder="Search by project name or description..."
          value={filters.q}
          onChange={(e) => updateFilter("q", e.target.value)}
          style={{ ...selectStyle, width: "100%", padding: "11px 14px", marginBottom: "16px" }}
        />

        <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", marginBottom: "16px" }}>
          {CATEGORIES.map((cat) => {
            const value = cat === "All" ? "" : cat;
            const active = filters.category === value;

            return (
              <button
                key={cat}
                type="button"
                onClick={() => updateFilter("category", value)}
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
                {cat}
              </button>
            );
          })}
        </div>

        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(160px, 1fr))", gap: "12px" }}>
          <div>
            <label htmlFor="language" style={{ display: "block", fontSize: "11px", color: "var(--cm-text-secondary)", marginBottom: "6px" }}>
              Language
            </label>
            <Select id="language" value={filters.language} onChange={(e) => updateFilter("language", e.target.value)}>
              <option value="">All languages</option>
              <option value="Java">Java</option>
              <option value="Python">Python</option>
              <option value="JavaScript">JavaScript</option>
              <option value="TypeScript">TypeScript</option>
              <option value="C++">C++</option>
            </Select>
          </div>

          <div>
            <label htmlFor="sort" style={{ display: "block", fontSize: "11px", color: "var(--cm-text-secondary)", marginBottom: "6px" }}>
              Sort by
            </label>
            <Select id="sort" value={filters.sort} onChange={(e) => updateFilter("sort", e.target.value)}>
              <option value="relevance">Most relevant</option>
              <option value="recent">Recent</option>
              <option value="stars">Stars</option>
              <option value="contributors">Contributors</option>
            </Select>
          </div>

          <div>
            <label htmlFor="tag" style={{ display: "block", fontSize: "11px", color: "var(--cm-text-secondary)", marginBottom: "6px" }}>
              Tag
            </label>
            <input
              id="tag"
              type="text"
              placeholder="e.g. react, django"
              value={filters.tag}
              onChange={(e) => updateFilter("tag", e.target.value)}
              style={selectStyle}
            />
          </div>

          <label style={{ display: "flex", alignItems: "center", gap: "8px", fontSize: "12.5px", color: "var(--cm-text-secondary)", cursor: "pointer" }}>
            <input
              type="checkbox"
              checked={filters.hasBeginnerIssues}
              onChange={(e) => updateFilter("hasBeginnerIssues", e.target.checked)}
              style={{ width: "15px", height: "15px", accentColor: "var(--cm-lime)" }}
            />
            Beginner-friendly issues
          </label>
        </div>
      </section>

      {/* Results */}
      <section aria-labelledby="projects-heading">
        <div style={{ display: "flex", alignItems: "flex-end", justifyContent: "space-between", marginBottom: "16px", flexWrap: "wrap", gap: "8px" }}>
          <h2 id="projects-heading" style={{ fontSize: "17px", fontWeight: 700, margin: 0, color: "var(--cm-text-primary)" }}>
            Explore projects
          </h2>

          {!loading && !error && (
            <p style={{ fontSize: "12.5px", color: "var(--cm-text-secondary)", margin: 0 }}>
              {meta.total} {meta.total === 1 ? "project" : "projects"} found
            </p>
          )}
        </div>

        {loading && <StatusPanel text="Loading projects…" />}

        {!loading && error && <StatusPanel title="Something went wrong" text={error} />}

        {!loading && !error && projects.length === 0 && (
          <StatusPanel title="No projects found" text="Try changing your search or filters." />
        )}

        {!loading && !error && projects.length > 0 && (
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(240px, 1fr))", gap: "16px" }}>
            {projects.map((project) => (
              <ProjectCard key={project.id} project={project} />
            ))}
          </div>
        )}
      </section>

      {/* Pagination */}
      <nav aria-label="Project pagination" style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: "16px", marginTop: "32px" }}>
        <button
          type="button"
          disabled={!canGoPrevious || loading}
          onClick={() => setMeta((current) => ({ ...current, page: current.page - 1 }))}
          style={{
            ...selectStyle,
            padding: "9px 18px",
            fontWeight: 600,
            cursor: canGoPrevious ? "pointer" : "not-allowed",
            opacity: canGoPrevious ? 1 : 0.4,
          }}
        >
          Previous
        </button>

        <span style={{ fontSize: "13px", color: "var(--cm-text-secondary)" }}>
          Page {meta.page + 1} of {Math.max(totalPages, 1)}
        </span>

        <button
          type="button"
          disabled={!canGoNext || loading}
          onClick={() => setMeta((current) => ({ ...current, page: current.page + 1 }))}
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
    </div>
  );
}

function StatusPanel({ title, text }) {
  return (
    <div className="cm-glass" style={{ borderRadius: "24px", padding: "48px 24px", textAlign: "center" }}>
      {title && (
        <p style={{ fontSize: "15px", fontWeight: 600, margin: "0 0 6px", color: "var(--cm-text-primary)" }}>
          {title}
        </p>
      )}
      <p style={{ fontSize: "13px", color: "var(--cm-text-secondary)", margin: 0 }}>{text}</p>
    </div>
  );
}

// A plain <select> renders each browser/OS's own dropdown chrome on top of
// whatever styling we give it, on a dark card that shows up as a stray
// light-grey native arrow/box that doesn't match anything else on the page.
// `appearance: none` (with the -webkit/-moz prefixes for older engines)
// strips that native rendering so our own chevron and border are the only
// thing drawn; the <option> list itself still uses OS chrome (no CSS can
// change that cross-browser), but that's a floating native menu the person
// only sees for a moment, not part of the page's persistent look.
function Select({ id, value, onChange, children }) {
  return (
    <div style={{ position: "relative" }}>
      <select
        id={id}
        value={value}
        onChange={onChange}
        style={{
          appearance: "none",
          WebkitAppearance: "none",
          MozAppearance: "none",
          borderRadius: "10px",
          border: "0.5px solid var(--cm-border)",
          background: "var(--cm-surface)",
          color: "var(--cm-text-primary)",
          fontSize: "13px",
          padding: "9px 34px 9px 12px",
          outline: "none",
          width: "100%",
          cursor: "pointer",
        }}
      >
        {children}
      </select>
      <ChevronDown
        size={15}
        strokeWidth={2}
        aria-hidden="true"
        style={{
          position: "absolute",
          right: "10px",
          top: "50%",
          transform: "translateY(-50%)",
          color: "var(--cm-text-secondary)",
          pointerEvents: "none",
        }}
      />
    </div>
  );
}
