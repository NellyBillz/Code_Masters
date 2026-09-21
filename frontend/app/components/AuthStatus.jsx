"use client";

import { useAuth } from "../context/AuthContext";
import Button from "./ui/Button";
import GitHubMark from "./icons/GitHubMark";

/**
 * Lives on the persistent dark header chrome (see Header.jsx), so every
 * color here is a chrome-* token or the theme-adaptive `primary` — never
 * `foreground`/`surface`, which are tuned for the page body and would lose
 * contrast against the header in dark theme.
 */
export default function AuthStatus() {
  const { user, loading, logout } = useAuth();

  if (loading) {
    return (
      <span className="whitespace-nowrap text-sm text-chrome-fg-muted" aria-live="polite">
        <span className="hidden sm:inline">Checking session…</span>
        <span className="sm:hidden">Checking…</span>
      </span>
    );
  }

  if (!user) {
    return (
      <Button as="a" href="/auth/github" variant="primary" size="sm">
        <GitHubMark size={15} />
        <span className="hidden sm:inline">Sign in with GitHub</span>
        <span className="sm:hidden">Sign in</span>
      </Button>
    );
  }

  return (
    <div className="flex items-center gap-3">
      <div className="flex items-center gap-2">
        {user.avatarUrl ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={user.avatarUrl}
            alt=""
            width={24}
            height={24}
            className="h-6 w-6 rounded-full"
          />
        ) : (
          <span
            aria-hidden="true"
            className="flex h-6 w-6 items-center justify-center rounded-full bg-chrome-hover text-[11px] font-semibold text-chrome-fg"
          >
            {(user.displayName || user.username || "?").charAt(0).toUpperCase()}
          </span>
        )}
        <span className="text-sm font-medium text-chrome-fg">
          {user.displayName || user.username}
        </span>
      </div>

      <button
        type="button"
        onClick={logout}
        className="rounded-md border border-chrome-border px-2.5 py-1 text-[13px] font-medium text-chrome-fg-muted transition-colors hover:bg-chrome-hover hover:text-chrome-fg"
      >
        Sign out
      </button>
    </div>
  );
}
