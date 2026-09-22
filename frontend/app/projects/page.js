"use client";

import { useEffect, useState } from "react";
import { listProjects } from "../../lib/api";
import ProjectCard from "../components/ProjectCard";

const PAGE_SIZE = 20;

export default function Projects() {
    const [filters, setFilters] = useState({
        q: "",
        language: "",
        category: "",
        hasBeginnerIssues: false,
        sort: "relevance"
    });

    const [projects, setProjects] = useState([]);
    const [meta, setMeta] = useState({
        page: 0,
        size: PAGE_SIZE,
        total: 0
    });

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    const updateFilter = (key, value) => {
        setFilters((current) => ({
            ...current,
            [key]: value
        }));

        setMeta((current) => ({
            ...current,
            page: 0
        }));
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
                    hasBeginnerIssues: filters.hasBeginnerIssues
                        ? true
                        : undefined,
                    sort: filters.sort
                });

                if (cancelled) {
                    return;
                }

                setProjects(result.items);
                setMeta(result.meta);
            } catch (err) {
                if (cancelled) {
                    return;
                }

                setProjects([]);
                setError(err.message || "Failed to load projects.");
            } finally {
                if (!cancelled) {
                    setLoading(false);
                }
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
        filters.sort,
        meta.page
    ]);

    const totalPages = Math.ceil(meta.total / PAGE_SIZE);
    const canGoPrevious = meta.page > 0;
    const canGoNext = meta.page + 1 < totalPages;

    return (
        <main className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
            <header className="mb-8">
                <p className="mb-2 text-sm font-medium text-blue-600">
                    Open-source discovery
                </p>

                <h1 className="text-3xl font-bold tracking-tight text-slate-900 sm:text-4xl">
                    Projects
                </h1>

                <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-600 sm:text-base">
                    Discover South African open-source projects and find
                    opportunities to contribute.
                </p>
            </header>

            <section
                aria-label="Project filters"
                className="mb-8 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm sm:p-6"
            >
                <div className="mb-5">
                    <label
                        htmlFor="search"
                        className="mb-2 block text-sm font-semibold text-slate-800"
                    >
                        Search projects
                    </label>

                    <input
                        id="search"
                        type="text"
                        placeholder="Search by project name or description..."
                        value={filters.q}
                        onChange={(e) =>
                            updateFilter("q", e.target.value)
                        }
                        className="w-full rounded-xl border border-slate-300 bg-white px-4 py-3 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                    />
                </div>

                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
                    <div>
                        <label
                            htmlFor="language"
                            className="mb-2 block text-sm font-medium text-slate-700"
                        >
                            Language
                        </label>

                        <select
                            id="language"
                            value={filters.language}
                            onChange={(e) =>
                                updateFilter(
                                    "language",
                                    e.target.value
                                )
                            }
                            className="w-full rounded-xl border border-slate-300 bg-white px-3 py-3 text-sm text-slate-800 outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                        >
                            <option value="">All languages</option>
                            <option value="Java">Java</option>
                            <option value="Python">Python</option>
                            <option value="JavaScript">
                                JavaScript
                            </option>
                            <option value="TypeScript">
                                TypeScript
                            </option>
                            <option value="C++">C++</option>
                        </select>
                    </div>

                    <div>
                        <label
                            htmlFor="category"
                            className="mb-2 block text-sm font-medium text-slate-700"
                        >
                            Category
                        </label>

                        <select
                            id="category"
                            value={filters.category}
                            onChange={(e) =>
                                updateFilter(
                                    "category",
                                    e.target.value
                                )
                            }
                            className="w-full rounded-xl border border-slate-300 bg-white px-3 py-3 text-sm text-slate-800 outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                        >
                            <option value="">All categories</option>
                            <option value="Web">Web</option>
                            <option value="Mobile">Mobile</option>
                            <option value="AI">AI</option>
                            <option value="Backend">Backend</option>
                            <option value="Game">Game</option>
                        </select>
                    </div>

                    <div>
                        <label
                            htmlFor="sort"
                            className="mb-2 block text-sm font-medium text-slate-700"
                        >
                            Sort by
                        </label>

                        <select
                            id="sort"
                            value={filters.sort}
                            onChange={(e) =>
                                updateFilter(
                                    "sort",
                                    e.target.value
                                )
                            }
                            className="w-full rounded-xl border border-slate-300 bg-white px-3 py-3 text-sm text-slate-800 outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                        >
                            <option value="relevance">
                                Relevance
                            </option>
                            <option value="recent">Recent</option>
                            <option value="stars">Stars</option>
                            <option value="contributors">
                                Contributors
                            </option>
                        </select>
                    </div>
                </div>

                <label className="mt-5 flex cursor-pointer items-center gap-3 rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm font-medium text-slate-700 transition hover:bg-slate-100">
                    <input
                        type="checkbox"
                        checked={filters.hasBeginnerIssues}
                        onChange={(e) =>
                            updateFilter(
                                "hasBeginnerIssues",
                                e.target.checked
                            )
                        }
                        className="h-4 w-4 rounded border-slate-300 text-blue-600 focus:ring-blue-500"
                    />

                    <span>Show projects with beginner-friendly issues</span>
                </label>
            </section>

            <section aria-labelledby="projects-heading">
                <div className="mb-5 flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
                    <div>
                        <h2
                            id="projects-heading"
                            className="text-xl font-bold text-slate-900"
                        >
                            Explore projects
                        </h2>

                        {!loading && !error && (
                            <p className="mt-1 text-sm text-slate-500">
                                {meta.total}{" "}
                                {meta.total === 1
                                    ? "project"
                                    : "projects"}{" "}
                                found
                            </p>
                        )}
                    </div>
                </div>

                {loading && (
                    <div className="rounded-2xl border border-slate-200 bg-white p-10 text-center shadow-sm">
                        <p className="text-sm font-medium text-slate-600">
                            Loading projects...
                        </p>
                    </div>
                )}

                {error && (
                    <div
                        role="alert"
                        className="rounded-2xl border border-red-200 bg-red-50 p-5 text-sm text-red-700"
                    >
                        {error}
                    </div>
                )}

                {!loading && !error && projects.length === 0 && (
                    <div className="rounded-2xl border border-dashed border-slate-300 bg-slate-50 p-12 text-center">
                        <h3 className="text-lg font-semibold text-slate-900">
                            No projects found
                        </h3>

                        <p className="mt-2 text-sm text-slate-500">
                            Try changing your search or filters.
                        </p>
                    </div>
                )}

                {!loading && !error && projects.length > 0 && (
                    <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">
                        {projects.map((project) => (
                            <ProjectCard
                                key={project.id}
                                project={project}
                            />
                        ))}
                    </div>
                )}
            </section>

            <nav
                aria-label="Project pagination"
                className="mt-8 flex items-center justify-center gap-4"
            >
                <button
                    type="button"
                    disabled={!canGoPrevious || loading}
                    onClick={() =>
                        setMeta((current) => ({
                            ...current,
                            page: current.page - 1
                        }))
                    }
                    className="rounded-xl border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-40"
                >
                    Previous
                </button>

                <span className="text-sm font-medium text-slate-600">
                    Page {meta.page + 1} of{" "}
                    {Math.max(totalPages, 1)}
                </span>

                <button
                    type="button"
                    disabled={!canGoNext || loading}
                    onClick={() =>
                        setMeta((current) => ({
                            ...current,
                            page: current.page + 1
                        }))
                    }
                    className="rounded-xl bg-slate-900 px-4 py-2 text-sm font-medium text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-40"
                >
                    Next
                </button>
            </nav>
        </main>
    );
}
