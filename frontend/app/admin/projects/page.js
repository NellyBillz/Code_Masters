"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { getPendingProjects, moderateProject } from "../../../lib/api";
import { useAuth } from "../../context/AuthContext";

const PAGE_SIZE = 20;

function PendingProjectRow({ project, onDecision }) {
    const [reason, setReason] = useState("");
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState("");

    async function handleDecision(decision) {
        setBusy(true);
        setError("");

        try {
            await moderateProject(project.id, {
                decision,
                reason: reason.trim() || undefined
            });
            onDecision(project.id);
        } catch (err) {
            setError(err.message || "Failed to record decision.");
            setBusy(false);
        }
    }

    return (
        <li className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
            <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                    <h3 className="text-lg font-bold text-slate-900">
                        {project.name}
                    </h3>

                    <a
                        href={project.githubUrl}
                        target="_blank"
                        rel="noreferrer"
                        className="text-sm font-medium text-blue-600 hover:text-blue-800"
                    >
                        {project.githubUrl}
                    </a>
                </div>

                <div className="flex flex-wrap gap-2">
                    {project.connection && (
                        <span className="rounded-full bg-green-50 px-3 py-1 text-xs font-semibold text-green-700">
                            {project.connection}
                        </span>
                    )}

                    {project.category && (
                        <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-700">
                            {project.category}
                        </span>
                    )}
                </div>
            </div>

            {project.description && (
                <p className="mt-3 text-sm leading-6 text-slate-600">
                    {project.description}
                </p>
            )}

            {project.tags?.length > 0 && (
                <div className="mt-3 flex flex-wrap gap-2">
                    {project.tags.map((tag) => (
                        <span
                            key={tag}
                            className="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-700"
                        >
                            {tag}
                        </span>
                    ))}
                </div>
            )}

            <div className="mt-5 flex flex-wrap items-center gap-3">
                <input
                    type="text"
                    placeholder="Reason (optional, shown on reject)"
                    value={reason}
                    onChange={(e) => setReason(e.target.value)}
                    disabled={busy}
                    className="min-w-[16rem] flex-1 rounded-xl border border-slate-300 bg-white px-4 py-2 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                />

                <button
                    type="button"
                    onClick={() => handleDecision("approve")}
                    disabled={busy}
                    className="rounded-xl bg-green-600 px-4 py-2 text-sm font-semibold text-white transition hover:bg-green-700 disabled:cursor-not-allowed disabled:opacity-50"
                >
                    Approve
                </button>

                <button
                    type="button"
                    onClick={() => handleDecision("reject")}
                    disabled={busy}
                    className="rounded-xl border border-red-300 bg-white px-4 py-2 text-sm font-semibold text-red-700 transition hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-50"
                >
                    Reject
                </button>
            </div>

            {error && (
                <p role="alert" className="mt-3 text-sm text-red-700">
                    {error}
                </p>
            )}
        </li>
    );
}

export default function AdminPendingProjectsPage() {
    const { user, loading: authLoading } = useAuth();

    const [projects, setProjects] = useState([]);
    const [meta, setMeta] = useState({ page: 0, size: PAGE_SIZE, total: 0 });
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    const isSiteAdmin = Boolean(user?.isSiteAdmin);

    useEffect(() => {
        if (!isSiteAdmin) {
            return;
        }

        let cancelled = false;

        async function loadPending() {
            setLoading(true);
            setError("");

            try {
                const result = await getPendingProjects({
                    page: meta.page,
                    size: PAGE_SIZE
                });

                if (!cancelled) {
                    setProjects(result.items);
                    setMeta(result.meta);
                }
            } catch (err) {
                if (!cancelled) {
                    setError(err.message || "Failed to load pending projects.");
                }
            } finally {
                if (!cancelled) {
                    setLoading(false);
                }
            }
        }

        loadPending();

        return () => {
            cancelled = true;
        };
    }, [isSiteAdmin, meta.page]);

    function handleDecision(projectId) {
        setProjects((current) => current.filter((p) => p.id !== projectId));
        setMeta((current) => ({
            ...current,
            total: Math.max(current.total - 1, 0)
        }));
    }

    if (authLoading || (isSiteAdmin && loading)) {
        return (
            <main className="mx-auto w-full max-w-4xl px-4 py-8 sm:px-6 lg:px-8">
                <div className="rounded-2xl border border-slate-200 bg-white p-10 text-center shadow-sm">
                    <p className="text-sm font-medium text-slate-600">
                        Loading...
                    </p>
                </div>
            </main>
        );
    }

    if (!isSiteAdmin) {
        return (
            <main className="mx-auto w-full max-w-4xl px-4 py-8 sm:px-6 lg:px-8">
                <div className="rounded-2xl border border-slate-200 bg-white p-10 text-center shadow-sm">
                    <h1 className="text-xl font-bold text-slate-900">
                        Page not found
                    </h1>
                </div>
            </main>
        );
    }

    const totalPages = Math.ceil(meta.total / PAGE_SIZE);
    const canGoPrevious = meta.page > 0;
    const canGoNext = meta.page + 1 < totalPages;

    return (
        <main className="mx-auto w-full max-w-4xl px-4 py-8 sm:px-6 lg:px-8">
            <div className="mb-8">
                <Link
                    href="/projects"
                    className="inline-flex items-center text-sm font-medium text-blue-600 transition hover:text-blue-800"
                >
                    ← Back to projects
                </Link>
            </div>

            <header className="mb-8">
                <p className="mb-2 text-sm font-medium text-blue-600">
                    Site admin
                </p>

                <h1 className="text-3xl font-bold tracking-tight text-slate-900 sm:text-4xl">
                    Moderation queue
                </h1>

                <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-600 sm:text-base">
                    Projects submitted for review. Approving publishes a
                    project immediately; rejecting keeps it hidden from
                    public discovery.
                </p>
            </header>

            {error && (
                <div
                    role="alert"
                    className="mb-6 rounded-2xl border border-red-200 bg-red-50 p-5 text-sm text-red-700"
                >
                    {error}
                </div>
            )}

            {!error && projects.length === 0 && (
                <div className="rounded-2xl border border-dashed border-slate-300 bg-slate-50 p-12 text-center">
                    <h3 className="text-lg font-semibold text-slate-900">
                        Nothing waiting on review
                    </h3>

                    <p className="mt-2 text-sm text-slate-500">
                        New submissions will show up here.
                    </p>
                </div>
            )}

            {projects.length > 0 && (
                <ul className="space-y-4">
                    {projects.map((project) => (
                        <PendingProjectRow
                            key={project.id}
                            project={project}
                            onDecision={handleDecision}
                        />
                    ))}
                </ul>
            )}

            {totalPages > 1 && (
                <nav
                    aria-label="Pending project pagination"
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
                        Page {meta.page + 1} of {Math.max(totalPages, 1)}
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
            )}
        </main>
    );
}
