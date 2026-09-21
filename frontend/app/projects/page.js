"use client";

import { useEffect, useState } from "react";
import { Search, SlidersHorizontal, X } from "lucide-react";
import { listProjects } from "../../lib/api";
import ProjectCard from "../components/ProjectCard";
import Button from "../components/ui/Button";
import Input from "../components/ui/Input";
import Select from "../components/ui/Select";
import Checkbox from "../components/ui/Checkbox";
import EmptyState from "../components/ui/EmptyState";
import { ProjectCardSkeleton } from "../components/ui/Skeleton";

const PAGE_SIZE = 20;

const LANGUAGES = ["Java", "Python", "JavaScript", "TypeScript", "Go", "C++"];
const CATEGORIES = ["Web", "Mobile", "AI", "Backend", "Developer Tools", "Game"];

const DEFAULT_FILTERS = {
  q: "",
  language: "",
  category: "",
  hasBeginnerIssues: false,
  hasContributingGuide: false,
  sort: "relevance",
};

function FilterFields({ filters, updateFilter }) {
  return (
    <div className="flex flex-col gap-6">
      <div>
        <label htmlFor="language" className="mb-1.5 block text-[13px] font-medium text-foreground-secondary">
          Language
        </label>
        <Select id="language" value={filters.language} onChange={(e) => updateFilter("language", e.target.value)}>
          <option value="">All languages</option>
          {LANGUAGES.map((lang) => (
            <option key={lang} value={lang}>
              {lang}
            </option>
          ))}
        </Select>
      </div>

      <div>
        <label htmlFor="category" className="mb-1.5 block text-[13px] font-medium text-foreground-secondary">
          Category
        </label>
        <Select id="category" value={filters.category} onChange={(e) => updateFilter("category", e.target.value)}>
          <option value="">All categories</option>
          {CATEGORIES.map((cat) => (
            <option key={cat} value={cat}>
              {cat}
            </option>
          ))}
        </Select>
      </div>

      <div>
        <p className="mb-2 text-[13px] font-medium text-foreground-secondary">Contribution</p>
        <div className="flex flex-col gap-2.5">
          <Checkbox
            label="Has beginner-friendly issues"
            checked={filters.hasBeginnerIssues}
            onChange={(e) => updateFilter("hasBeginnerIssues", e.target.checked)}
          />
          <Checkbox
            label="Has contributing guide"
            checked={filters.hasContributingGuide}
            onChange={(e) => updateFilter("hasContributingGuide", e.target.checked)}
          />
        </div>
      </div>

      <div>
        <label htmlFor="sort" className="mb-1.5 block text-[13px] font-medium text-foreground-secondary">
          Sort
        </label>
        <Select id="sort" value={filters.sort} onChange={(e) => updateFilter("sort", e.target.value)}>
          <option value="relevance">Relevance</option>
          <option value="recent">Recently active</option>
          <option value="stars">Stars</option>
          <option value="contributors">Contributors</option>
        </Select>
      </div>
    </div>
  );
}

export default function Projects() {
  const [filters, setFilters] = useState(DEFAULT_FILTERS);
  const [projects, setProjects] = useState([]);
  const [meta, setMeta] = useState({ page: 0, size: PAGE_SIZE, total: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [retryNonce, setRetryNonce] = useState(0);
  const [mobileFiltersOpen, setMobileFiltersOpen] = useState(false);

  const updateFilter = (key, value) => {
    setFilters((current) => ({ ...current, [key]: value }));
    setMeta((current) => ({ ...current, page: 0 }));
  };

  const clearFilters = () => {
    setFilters(DEFAULT_FILTERS);
    setMeta((current) => ({ ...current, page: 0 }));
  };

  const hasActiveFilters =
    filters.q ||
    filters.language ||
    filters.category ||
    filters.hasBeginnerIssues ||
    filters.hasContributingGuide ||
    filters.sort !== "relevance";

  useEffect(() => {
    let cancelled = false;

    async function loadProjects() {
      setLoading(true);
      setError("");

      try {
        const result = await listProjects({
          page: meta.page,
          size: PAGE_SIZE,
          q: filters.q || undefined,
          language: filters.language || undefined,
          category: filters.category || undefined,
          hasBeginnerIssues: filters.hasBeginnerIssues ? true : undefined,
          hasContributingGuide: filters.hasContributingGuide ? true : undefined,
          sort: filters.sort,
        });

        if (cancelled) return;
        setProjects(result.items || []);
        setMeta(result.meta || { page: 0, size: PAGE_SIZE, total: 0 });
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
  }, [
    filters.q,
    filters.language,
    filters.category,
    filters.hasBeginnerIssues,
    filters.hasContributingGuide,
    filters.sort,
    meta.page,
    retryNonce,
  ]);

  const totalPages = Math.max(Math.ceil(meta.total / PAGE_SIZE), 1);
  const canGoPrevious = meta.page > 0;
  const canGoNext = meta.page + 1 < totalPages;

  return (
    <main className="mx-auto w-full max-w-[1280px] px-4 py-10 sm:px-6 sm:py-12 lg:px-8">
      <header className="max-w-2xl">
        <h1 className="font-display text-[2rem] font-extrabold tracking-tight text-foreground sm:text-[2.25rem]">Projects</h1>
        <p className="mt-2 text-[15px] leading-relaxed text-foreground-secondary">
          Discover South African open-source projects and find opportunities to contribute.
        </p>
      </header>

      <div className="relative mt-8 max-w-2xl">
        <Search
          size={16}
          strokeWidth={1.75}
          aria-hidden="true"
          className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-foreground-disabled"
        />
        <Input
          type="text"
          placeholder="Search projects by name, description, owner or topic"
          value={filters.q}
          onChange={(e) => updateFilter("q", e.target.value)}
          className="pl-9"
          aria-label="Search projects"
        />
      </div>

      <button
        type="button"
        onClick={() => setMobileFiltersOpen((v) => !v)}
        className="mt-4 inline-flex items-center gap-2 rounded-md border border-border-strong bg-surface px-3 py-2 text-sm font-medium text-foreground-secondary lg:hidden"
        aria-expanded={mobileFiltersOpen}
      >
        <SlidersHorizontal size={15} strokeWidth={1.75} aria-hidden="true" />
        Filters
        {hasActiveFilters && <span className="h-1.5 w-1.5 rounded-full bg-primary" aria-hidden="true" />}
      </button>

      {mobileFiltersOpen && (
        <div className="mt-4 rounded-[10px] border border-border bg-surface p-5 lg:hidden">
          <div className="mb-4 flex items-center justify-between">
            <p className="text-[13px] font-semibold uppercase tracking-wide text-foreground-muted">Filters</p>
            <button
              type="button"
              onClick={() => setMobileFiltersOpen(false)}
              aria-label="Close filters"
              className="text-foreground-muted hover:text-foreground"
            >
              <X size={16} strokeWidth={1.75} />
            </button>
          </div>
          <FilterFields filters={filters} updateFilter={updateFilter} />
        </div>
      )}

      <div className="mt-8 grid grid-cols-1 gap-10 lg:grid-cols-[240px_1fr] lg:gap-8">
        <aside className="hidden lg:block">
          <div className="sticky top-24">
            <p className="mb-4 text-[13px] font-semibold uppercase tracking-wide text-foreground-muted">Filters</p>
            <FilterFields filters={filters} updateFilter={updateFilter} />
            {hasActiveFilters && (
              <Button variant="ghost" size="sm" onClick={clearFilters} className="mt-6 w-full">
                Clear filters
              </Button>
            )}
          </div>
        </aside>

        <section aria-labelledby="projects-heading">
          <div className="mb-5 flex items-center justify-between gap-4">
            <p id="projects-heading" className="text-sm text-foreground-muted" aria-live="polite">
              {!loading && !error && (
                <>
                  {meta.total} {meta.total === 1 ? "project" : "projects"} found
                </>
              )}
            </p>
          </div>

          {loading && (
            <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 xl:grid-cols-3">
              {Array.from({ length: 6 }).map((_, i) => (
                <ProjectCardSkeleton key={i} />
              ))}
            </div>
          )}

          {!loading && error && (
            <div role="alert" className="rounded-[10px] border border-border bg-surface p-8 text-center">
              <h3 className="text-[15px] font-semibold text-foreground">We couldn&apos;t load projects</h3>
              <p className="mx-auto mt-1.5 max-w-sm text-sm text-foreground-muted">{error}</p>
              <Button
                variant="secondary"
                size="sm"
                onClick={() => setRetryNonce((n) => n + 1)}
                className="mt-4"
              >
                Try again
              </Button>
            </div>
          )}

          {!loading && !error && projects.length === 0 && (
            <EmptyState
              title="No projects found"
              description="Try a different search term or remove one of your filters."
              action={
                hasActiveFilters && (
                  <Button variant="secondary" size="sm" onClick={clearFilters}>
                    Clear filters
                  </Button>
                )
              }
            />
          )}

          {!loading && !error && projects.length > 0 && (
            <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 xl:grid-cols-3">
              {projects.map((project) => (
                <ProjectCard key={project.id} project={project} />
              ))}
            </div>
          )}

          {!loading && !error && projects.length > 0 && totalPages > 1 && (
            <nav aria-label="Project pagination" className="mt-10 flex items-center justify-center gap-3">
              <Button
                variant="secondary"
                size="sm"
                disabled={!canGoPrevious}
                onClick={() => setMeta((current) => ({ ...current, page: current.page - 1 }))}
              >
                Previous
              </Button>
              <span className="text-sm tabular-nums text-foreground-muted">
                Page {meta.page + 1} of {totalPages}
              </span>
              <Button
                variant="secondary"
                size="sm"
                disabled={!canGoNext}
                onClick={() => setMeta((current) => ({ ...current, page: current.page + 1 }))}
              >
                Next
              </Button>
            </nav>
          )}
        </section>
      </div>
    </main>
  );
}
