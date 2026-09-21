"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { deleteAccount, friendlyErrorMessage } from "../../lib/api";
import { useAuth } from "../context/AuthContext";

export default function ProfileSettingsPage() {
    const router = useRouter();
    const { user, loading: authLoading, refresh } = useAuth();

    const [confirmed, setConfirmed] = useState(false);
    const [deleting, setDeleting] = useState(false);
    const [error, setError] = useState("");

    async function handleDelete() {
        setDeleting(true);
        setError("");

        try {
            await deleteAccount();
            // The DELETE response already cleared the session/CSRF cookies
            // server-side; refresh() re-reads /users/me, which now 401s, so
            // the shared auth context (and the header it drives) updates to
            // logged-out immediately.
            await refresh();
            router.push("/");
        } catch (err) {
            setError(
                friendlyErrorMessage(
                    err,
                    "Failed to delete your account. Please try again."
                )
            );
            setDeleting(false);
        }
    }

    if (authLoading) {
        return (
            <main className="mx-auto w-full max-w-2xl px-4 py-8 sm:px-6 lg:px-8">
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
            <main className="mx-auto w-full max-w-2xl px-4 py-8 sm:px-6 lg:px-8">
                <div className="rounded-2xl border border-slate-200 bg-white p-10 text-center shadow-sm">
                    <h1 className="text-xl font-bold text-slate-900">
                        Account settings
                    </h1>
                    <p className="mt-3 text-sm text-slate-600">
                        <a
                            href="/auth/github"
                            className="font-medium text-blue-600 hover:text-blue-800"
                        >
                            Log in with GitHub
                        </a>{" "}
                        to manage your account.
                    </p>
                </div>
            </main>
        );
    }

    return (
        <main className="mx-auto w-full max-w-2xl px-4 py-8 sm:px-6 lg:px-8">
            <header className="mb-8">
                <p className="mb-2 text-sm font-medium text-blue-600">
                    {user.displayName || user.username}
                </p>

                <h1 className="text-3xl font-bold tracking-tight text-slate-900 sm:text-4xl">
                    Account settings
                </h1>
            </header>

            <section className="rounded-2xl border border-red-200 bg-white p-6 shadow-sm sm:p-8">
                <h2 className="text-xl font-bold text-red-700">
                    Delete account
                </h2>

                <p className="mt-4 text-sm font-semibold text-slate-800">
                    Deleting your account will:
                </p>
                <ul className="mt-2 list-disc space-y-1 pl-5 text-sm text-slate-600">
                    <li>
                        Clear your username, display name, avatar, bio,
                        location, and skills
                    </li>
                    <li>
                        Clear your email and disconnect your GitHub login
                        from this account
                    </li>
                </ul>

                <p className="mt-4 text-sm font-semibold text-slate-800">
                    It will not:
                </p>
                <ul className="mt-2 list-disc space-y-1 pl-5 text-sm text-slate-600">
                    <li>
                        Remove your past comments or completed
                        contributions — they stay visible, attributed to an
                        anonymized account, so other people&apos;s
                        discussions and the platform&apos;s public stats
                        stay accurate.
                    </li>
                </ul>

                <p className="mt-4 text-sm font-bold text-red-700">
                    This can&apos;t be undone.
                </p>

                <label className="mt-6 flex cursor-pointer items-start gap-3 rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm font-medium text-slate-700">
                    <input
                        type="checkbox"
                        checked={confirmed}
                        onChange={(event) => setConfirmed(event.target.checked)}
                        disabled={deleting}
                        className="mt-0.5 h-4 w-4 rounded border-slate-300 text-red-600 focus:ring-red-500"
                    />
                    <span>I understand this cannot be undone.</span>
                </label>

                <button
                    type="button"
                    onClick={handleDelete}
                    disabled={!confirmed || deleting}
                    className="mt-4 w-full rounded-xl bg-red-700 px-5 py-3 text-sm font-semibold text-white transition hover:bg-red-800 disabled:cursor-not-allowed disabled:opacity-50"
                >
                    {deleting ? "Deleting..." : "Delete my account"}
                </button>

                {error && (
                    <p role="alert" className="mt-3 text-sm text-red-700">
                        {error}
                    </p>
                )}
            </section>
        </main>
    );
}
