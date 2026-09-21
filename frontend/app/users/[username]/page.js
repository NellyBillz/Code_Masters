"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import { getContributions } from "../../../lib/api";

const PAGE_SIZE = 20;

function CompletionBadge({ completionSource }) {
    let label = "Completed";
    if (completionSource === "github_verified") label = "Verified via GitHub";
    else if (completionSource === "maintainer_confirmed")
        label = "Confirmed by maintainer";

    return (
        <span className="inline-flex shrink-0 items-center gap-1 rounded-full bg-green-50 px-3 py-1 text-xs font-semibold text-green-700">
            ✓ {label}
        </span>
    );
}

export default function UserProfilePage() {
    const params = useParams();
    const username = params?.username;

    const [contributions, setContributions] = useState([]);
    const [meta, setMeta] = useState({ page: 0, size: PAGE_SIZE, total: 0 });
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        if (!username) return;

        let cancelled = false;

        async function loadContributions() {
            setLoading(true);
            setError("");

            try {
                const result = await getContributions(username, {
                    page: meta.page,
                    size: PAGE_SIZE
                });

                if (!cancelled) {
                    setContributions(result.items);
                    setMeta(result.meta);
                }
            } catch (err) {
                if (!cancelled) {
                    setError(err.message || "Failed to load contributions.");
                }
            } finally {
                if (!cancelled) {
                    setLoading(false);
                }
            }
        }

        loadContributions();

        return () => {
            cancelled = true;
        };
    }, [username, meta.page]);

    const totalPages = Math.ceil(meta.total / PAGE_SIZE);
    const canGoPrevious = meta.page > 0;
    const canGoNext = meta.page + 1 < totalPages;

    return (
        <main className="mx-auto w-full max-w-4xl px-4 py-8 sm:px-6 lg:px-8">
            <header className="mb-8">
                <p className="mb-2 text-sm font-medium text-blue-600">
                    Developer profile
                </p>

                <h1 className="text-3xl font-bold tracking-tight text-slate-900 sm:text-4xl">
                    {username}
                </h1>
            </header>

            <section aria-labelledby="contributions-heading">
                <h2
                    id="contributions-heading"
                    className="mb-4 text-xl font-bold text-slate-900"
                >
                    Contributions
                </h2>

                {loading && (
                    <div className="rounded-2xl border border-slate-200 bg-white p-10 text-center shadow-sm">
                        <p className="text-sm font-medium text-slate-600">
                            Loading contributions...
                        </p>
                    </div>
                )}

                {!loading && error && (
                    <div
                        role="alert"
                        className="rounded-2xl border border-red-200 bg-red-50 p-5 text-sm text-red-700"
                    >
                        {error}
                    </div>
                )}

                {!loading && !error && contributions.length === 0 && (
                    <div className="rounded-2xl border border-dashed border-slate-300 bg-slate-50 p-12 text-center">
                        <h3 className="text-lg font-semibold text-slate-900">
                            No contributions yet
                        </h3>

                        <p className="mt-2 text-sm text-slate-500">
                            Verified contributions show up here once an issue
                            this person claimed is merged on GitHub or
                            confirmed by a maintainer.
                        </p>
                    </div>
                )}

                {!loading && !error && contributions.length > 0 && (
                    <ul className="space-y-4">
                        {contributions.map((contribution, index) => (
                            <li
                                key={`${contribution.issue?.id ?? "issue"}-${index}`}
                                className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"
                            >
                                <div className="flex flex-wrap items-start justify-between gap-3">
                                    <div>
                                        <Link
                                            href={`/issues/${contribution.issue?.id}`}
                                            className="font-semibold text-slate-900 hover:text-blue-700"
                                        >
                                            {contribution.issue?.title ??
                                                `Issue #${contribution.issue?.id}`}
                                        </Link>

                                        <p className="mt-1 text-sm text-slate-500">
                                            on{" "}
                                            <Link
                                                href={`/projects/${contribution.project?.id}`}
                                                className="font-medium text-blue-600 hover:text-blue-800"
                                            >
                                                {contribution.project?.name}
                                            </Link>
                                        </p>
                                    </div>

                                    <CompletionBadge
                                        completionSource={
                                            contribution.completionSource
                                        }
                                    />
                                </div>

                                <div className="mt-3 flex flex-wrap items-center gap-3 text-sm text-slate-500">
                                    {contribution.completedAt && (
                                        <span>
                                            Completed{" "}
                                            {new Date(
                                                contribution.completedAt
                                            ).toLocaleDateString()}
                                        </span>
                                    )}

                                    {contribution.pullRequestUrl && (
                                        <a
                                            href={contribution.pullRequestUrl}
                                            target="_blank"
                                            rel="noreferrer"
                                            className="font-medium text-blue-600 hover:text-blue-800"
                                        >
                                            View pull request →
                                        </a>
                                    )}
                                </div>
                            </li>
                        ))}
                    </ul>
                )}

                {totalPages > 1 && (
                    <nav
                        aria-label="Contribution pagination"
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
            </section>
        </main>
    );
}
