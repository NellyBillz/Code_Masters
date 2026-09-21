"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { ArrowLeft } from "lucide-react";
import { getProject, listProjectIssues } from "../../../../lib/api";
import IssueRow from "../../../components/IssueRow";
import Button from "../../../components/ui/Button";
import Select from "../../../components/ui/Select";
import Input from "../../../components/ui/Input";
import EmptyState from "../../../components/ui/EmptyState";
import { IssueRowSkeleton } from "../../../components/ui/Skeleton";

const PAGE_SIZE = 20;

const DEFAULT_FILTERS = { difficulty: "", label: "", status: "" };

export default function ProjectIssues() {
  const params = useParams();
  const projectId = params?.projectId;

  const [project, setProject] = useState(null);
  const [filters, setFilters] = useState(DEFAULT_FILTERS);
  const [issues, setIssues] = useState([]);
  const [meta, setMeta] = useState({ page: 0, size: PAGE_SIZE, total: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const updateFilter = (key, value) => {
    setFilters((current) => ({ ...current, [key]: value }));
    setMeta((current) => ({ ...current, page: 0 }));
  };

  const hasActiveFilters = filters.difficulty || filters.label || filters.status;

  useEffect(() => {
    if (!projectId) return;
    let cancelled = false;

    getProject(projectId)
      .then((result) => {
        if (!cancelled) setProject(result);
      })
      .catch(() => {});

    return () => {
      cancelled = true;
    };
  }, [projectId]);

  useEffect(() => {
    if (!projectId) return;
    let cancelled = false;

    async function loadIssues() {
      setLoading(true);
      setError("");

      try {
        const result = await listProjectIssues(projectId, {
          page: meta.page,
          size: PAGE_SIZE,
          difficulty: filters.difficulty || undefined,
          label: filters.label || undefined,
          status: filters.status || undefined,
        });

        if (cancelled) return;
        setIssues(result.items || []);
        setMeta(result.meta || { page: 0, size: PAGE_SIZE, total: 0 });
      } catch (err) {
        if (cancelled) return;
        setIssues([]);
        setError(err.message || "Failed to load issues.");
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    loadIssues();
    return () => {
      cancelled = true;
    };
  }, [projectId, filters.difficulty, filters.label, filters.status, meta.page]);

  const totalPages = Math.max(Math.ceil(meta.total / PAGE_SIZE), 1);

  return (
    <main className="mx-auto max-w-[1280px] px-4 py-10 sm:px-6 lg:px-8">
      <Link
        href={`/projects/${projectId}`}
        className="inline-flex items-center gap-1.5 text-sm font-medium text-foreground-muted transition-colors hover:text-foreground"
      >
        <ArrowLeft size={15} strokeWidth={1.75} aria-hidden="true" />
        Back to project
      </Link>

      <header className="mt-6 max-w-2xl">
        <h1 className="font-display text-[2rem] font-extrabold tracking-tight text-foreground sm:text-4xl">
          Contribution opportunities
        </h1>
        <p className="mt-2 text-[15px] leading-relaxed text-foreground-secondary">
          {project ? (
            <>
              Find a realistic place to start contributing to{" "}
              <Link href={`/projects/${projectId}`} className="font-medium text-primary hover:text-primary-hover">
                {project.name}
              </Link>
              .
            </>
          ) : (
            "Find a realistic place to start contributing."
          )}
        </p>
      </header>

      <div className="mt-8 flex flex-wrap items-end gap-4 border-b border-border pb-6">
        <div className="w-40">
          <label htmlFor="difficulty" className="mb-1.5 block text-[13px] font-medium text-foreground-secondary">
            Difficulty
          </label>
          <Select id="difficulty" value={filters.difficulty} onChange={(e) => updateFilter("difficulty", e.target.value)}>
            <option value="">Any</option>
            <option value="beginner">Beginner</option>
            <option value="intermediate">Intermediate</option>
            <option value="advanced">Advanced</option>
          </Select>
        </div>

        <div className="w-40">
          <label htmlFor="status" className="mb-1.5 block text-[13px] font-medium text-foreground-secondary">
            Status
          </label>
          <Select id="status" value={filters.status} onChange={(e) => updateFilter("status", e.target.value)}>
            <option value="">Any</option>
            <option value="open">Open</option>
            <option value="claimed">Claimed</option>
            <option value="closed">Closed</option>
          </Select>
        </div>

        <div className="w-48">
          <label htmlFor="label" className="mb-1.5 block text-[13px] font-medium text-foreground-secondary">
            Label
          </label>
          <Input
            id="label"
            type="text"
            placeholder="e.g. good-first-issue"
            value={filters.label}
            onChange={(e) => updateFilter("label", e.target.value)}
          />
        </div>

        {hasActiveFilters && (
          <Button variant="ghost" size="sm" onClick={() => setFilters(DEFAULT_FILTERS)}>
            Clear filters
          </Button>
        )}

        {!loading && !error && (
          <p className="ml-auto text-sm text-foreground-muted">
            {meta.total} {meta.total === 1 ? "issue" : "issues"} found
          </p>
        )}
      </div>

      <div className="mx-auto max-w-[820px]">
        {loading && (
          <div>
            {Array.from({ length: 5 }).map((_, i) => (
              <IssueRowSkeleton key={i} />
            ))}
          </div>
        )}

        {!loading && error && (
          <div role="alert" className="mt-6 rounded-[10px] border border-border bg-surface p-8 text-center">
            <h3 className="text-[15px] font-semibold text-foreground">We couldn&apos;t load issues</h3>
            <p className="mx-auto mt-1.5 max-w-sm text-sm text-foreground-muted">{error}</p>
          </div>
        )}

        {!loading && !error && issues.length === 0 && (
          <div className="mt-6">
            <EmptyState
              title="No issues found"
              description="Try a different difficulty, label or status filter."
              action={
                hasActiveFilters && (
                  <Button variant="secondary" size="sm" onClick={() => setFilters(DEFAULT_FILTERS)}>
                    Clear filters
                  </Button>
                )
              }
            />
          </div>
        )}

        {!loading && !error && issues.length > 0 && (
          <div>
            {issues.map((issue) => (
              <IssueRow key={issue.id} issue={issue} />
            ))}
          </div>
        )}

        {!loading && !error && issues.length > 0 && totalPages > 1 && (
          <nav aria-label="Issue pagination" className="mt-10 flex items-center justify-center gap-3">
            <Button
              variant="secondary"
              size="sm"
              disabled={meta.page <= 0}
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
              disabled={meta.page + 1 >= totalPages}
              onClick={() => setMeta((current) => ({ ...current, page: current.page + 1 }))}
            >
              Next
            </Button>
          </nav>
        )}
      </div>
    </main>
  );
}
