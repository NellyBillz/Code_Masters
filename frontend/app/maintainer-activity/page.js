"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { getMaintainerActivity } from "../../lib/api";
import { useAuth } from "../context/AuthContext";

function ClaimRow({ claim }) {
    return (
        <li className="rounded-xl bg-slate-50 px-4 py-3">
            <div className="flex flex-wrap items-center justify-between gap-2">
                <span className="font-medium text-slate-800">
                    {claim.user?.displayName || claim.user?.username || "User"}
                </span>

                <Link
                    href={`/issues/${claim.issueId}`}
                    className="text-sm font-medium text-blue-600 hover:text-blue-800"
                >
                    View issue →
                </Link>
            </div>

            {claim.note && (
                <p className="mt-1 text-sm text-slate-600">{claim.note}</p>
            )}

            {claim.pullRequestUrl && (
                <a
                    href={claim.pullRequestUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="mt-1 inline-block text-sm font-medium text-blue-600 hover:text-blue-800"
                >
                    View pull request →
                </a>
            )}
        </li>
    );
}

function CommentRow({ comment }) {
    return (
        <li className="rounded-xl bg-slate-50 px-4 py-3">
            <p className="text-sm text-slate-700">{comment.body}</p>
            <p className="mt-1 text-xs text-slate-500">
                {comment.author?.username || "User"}
                {comment.createdAt
                    ? ` · ${new Date(comment.createdAt).toLocaleDateString()}`
                    : ""}
            </p>
        </li>
    );
}

function ProjectActivityCard({ activity }) {
    const { project, activeClaims, claimsAwaitingReview, recentComments } =
        activity;

    return (
        <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
            <h2 className="mb-4 text-xl font-bold text-slate-900">
                <Link
                    href={`/projects/${project.id}`}
                    className="hover:text-blue-700"
                >
                    {project.name}
                </Link>
            </h2>

            <div className="grid grid-cols-1 gap-6 lg:grid-cols-3">
                <div>
                    <h3 className="mb-2 text-sm font-semibold uppercase tracking-wide text-slate-500">
                        Active claims ({activeClaims.length})
                    </h3>

                    {activeClaims.length > 0 ? (
                        <ul className="space-y-2">
                            {activeClaims.map((claim) => (
                                <ClaimRow key={claim.id} claim={claim} />
                            ))}
                        </ul>
                    ) : (
                        <p className="text-sm text-slate-500">
                            No active claims.
                        </p>
                    )}
                </div>

                <div>
                    <h3 className="mb-2 text-sm font-semibold uppercase tracking-wide text-slate-500">
                        Awaiting review ({claimsAwaitingReview.length})
                    </h3>

                    {claimsAwaitingReview.length > 0 ? (
                        <ul className="space-y-2">
                            {claimsAwaitingReview.map((claim) => (
                                <ClaimRow key={claim.id} claim={claim} />
                            ))}
                        </ul>
                    ) : (
                        <p className="text-sm text-slate-500">
                            Nothing waiting on your review.
                        </p>
                    )}
                </div>

                <div>
                    <h3 className="mb-2 text-sm font-semibold uppercase tracking-wide text-slate-500">
                        Recent comments ({recentComments.length})
                    </h3>

                    {recentComments.length > 0 ? (
                        <ul className="space-y-2">
                            {recentComments.map((comment) => (
                                <CommentRow key={comment.id} comment={comment} />
                            ))}
                        </ul>
                    ) : (
                        <p className="text-sm text-slate-500">
                            No recent comments.
                        </p>
                    )}
                </div>
            </div>
        </section>
    );
}

export default function MaintainerActivityPage() {
    const { user, loading: authLoading } = useAuth();

    const [projects, setProjects] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        if (!user) {
            return;
        }

        let cancelled = false;

        async function loadActivity() {
            setLoading(true);
            setError("");

            try {
                const result = await getMaintainerActivity();

                if (!cancelled) {
                    setProjects(result.projects || []);
                }
            } catch (err) {
                if (!cancelled) {
                    setError(err.message || "Failed to load maintainer activity.");
                }
            } finally {
                if (!cancelled) {
                    setLoading(false);
                }
            }
        }

        loadActivity();

        return () => {
            cancelled = true;
        };
    }, [user]);

    if (authLoading) {
        return (
            <main className="mx-auto w-full max-w-5xl px-4 py-8 sm:px-6 lg:px-8">
                <div className="rounded-2xl border border-slate-200 bg-white p-10 text-center shadow-sm">
                    <p className="text-sm font-medium text-slate-600">
                        Checking session...
                    </p>
                </div>
            </main>
        );
    }

    if (!user) {
        return (
            <main className="mx-auto w-full max-w-5xl px-4 py-8 sm:px-6 lg:px-8">
                <div className="rounded-2xl border border-slate-200 bg-white p-10 text-center shadow-sm">
                    <h1 className="text-xl font-bold text-slate-900">
                        Maintainer activity
                    </h1>
                    <p className="mt-3 text-sm text-slate-600">
                        <a
                            href="/auth/github"
                            className="font-medium text-blue-600 hover:text-blue-800"
                        >
                            Log in with GitHub
                        </a>{" "}
                        to see your maintainer activity.
                    </p>
                </div>
            </main>
        );
    }

    return (
        <main className="mx-auto w-full max-w-5xl px-4 py-8 sm:px-6 lg:px-8">
            <header className="mb-8">
                <p className="mb-2 text-sm font-medium text-blue-600">
                    Maintainer sanity
                </p>

                <h1 className="text-3xl font-bold tracking-tight text-slate-900 sm:text-4xl">
                    Maintainer activity
                </h1>

                <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-600 sm:text-base">
                    Active claims, claims awaiting your review, and recent
                    discussion across every project you maintain — in one
                    place.
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

            {!loading && !error && projects.length === 0 && (
                <div className="rounded-2xl border border-dashed border-slate-300 bg-slate-50 p-12 text-center">
                    <h3 className="text-lg font-semibold text-slate-900">
                        You don&apos;t maintain any projects yet
                    </h3>

                    <p className="mt-2 text-sm text-slate-500">
                        Activity across projects you maintain will show up
                        here.
                    </p>
                </div>
            )}

            {!loading && !error && projects.length > 0 && (
                <div className="space-y-6">
                    {projects.map((activity) => (
                        <ProjectActivityCard
                            key={activity.project.id}
                            activity={activity}
                        />
                    ))}
                </div>
            )}
        </main>
    );
}
