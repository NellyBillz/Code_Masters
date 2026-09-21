"use client";

import { useEffect, useState } from "react";
import { getStats } from "../../lib/api";

function formatNumber(n) {
    if (typeof n !== "number" || Number.isNaN(n)) return "—";
    return n.toLocaleString("en-ZA");
}

function StatTile({ label, value }) {
    return (
        <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
            <p className="text-sm font-medium text-slate-500">{label}</p>
            <p className="mt-2 text-3xl font-semibold text-slate-900">
                {formatNumber(value)}
            </p>
        </div>
    );
}

export default function ImpactPage() {
    const [stats, setStats] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        let cancelled = false;

        async function loadStats() {
            setLoading(true);
            setError("");

            try {
                const result = await getStats();

                if (!cancelled) {
                    setStats(result);
                }
            } catch (err) {
                if (!cancelled) {
                    setError(err.message || "Failed to load impact stats.");
                }
            } finally {
                if (!cancelled) {
                    setLoading(false);
                }
            }
        }

        loadStats();

        return () => {
            cancelled = true;
        };
    }, []);

    return (
        <main className="mx-auto w-full max-w-5xl px-4 py-8 sm:px-6 lg:px-8">
            <header className="mb-8">
                <p className="mb-2 text-sm font-medium text-blue-600">
                    Measurable impact
                </p>

                <h1 className="text-3xl font-bold tracking-tight text-slate-900 sm:text-4xl">
                    Platform impact
                </h1>

                <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-600 sm:text-base">
                    Real, live numbers — not a claim in a pitch deck. Every
                    figure here comes straight from the same public{" "}
                    <code className="rounded bg-slate-100 px-1.5 py-0.5 text-xs">
                        GET /stats
                    </code>{" "}
                    endpoint anyone can call.
                </p>
            </header>

            {loading && (
                <div className="rounded-2xl border border-slate-200 bg-white p-10 text-center shadow-sm">
                    <p className="text-sm font-medium text-slate-600">
                        Loading...
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

            {!loading && !error && stats && (
                <>
                    <div className="rounded-2xl border border-green-200 bg-green-50 p-8 text-center shadow-sm">
                        <p className="text-sm font-semibold uppercase tracking-wide text-green-700">
                            Verified contributions completed
                        </p>

                        <p className="mt-2 text-6xl font-semibold text-green-800">
                            {formatNumber(stats.totalContributionsCompleted)}
                        </p>

                        <p className="mt-2 text-sm text-green-700">
                            Only counted once a claim resolves to a real,
                            evidenced GitHub merge or a maintainer&apos;s
                            confirmation — not just claim volume.
                        </p>
                    </div>

                    <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
                        <StatTile
                            label="Published projects"
                            value={stats.publishedProjects}
                        />
                        <StatTile
                            label="Accepting contributions"
                            value={stats.activeProjectsAcceptingContributions}
                        />
                        <StatTile
                            label="Contributors engaged"
                            value={stats.totalContributorsEngaged}
                        />
                        <StatTile
                            label="Active claims"
                            value={stats.totalActiveClaims}
                        />
                    </div>

                    {stats.generatedAt && (
                        <p className="mt-6 text-xs text-slate-400">
                            Generated{" "}
                            {new Date(stats.generatedAt).toLocaleString()}
                        </p>
                    )}
                </>
            )}
        </main>
    );
}
