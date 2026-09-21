"use client";

import { useAuth } from "../context/AuthContext";
import Button from "./ui/Button";
import GitHubMark from "./icons/GitHubMark";

/**
 * Lives in the header (see Header.jsx), which now follows the page theme
 * (light in light mode, dark in dark mode) rather than staying permanently
 * dark — so this uses the same theme-adaptive tokens as the rest of the app.
 */
export default function AuthStatus() {
  const { user, loading, logout } = useAuth();

  if (loading) {
    return (
      <span className="whitespace-nowrap text-sm text-foreground-muted" aria-live="polite">
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
            className="flex h-6 w-6 items-center justify-center rounded-full bg-surface-subtle text-[11px] font-semibold text-foreground"
          >
            {(user.displayName || user.username || "?").charAt(0).toUpperCase()}
          </span>
        )}
        <span className="text-sm font-medium text-foreground">
          {user.displayName || user.username}
        </span>
      </div>

      <button
        type="button"
        onClick={logout}
        className="rounded-md border border-border px-2.5 py-1 text-[13px] font-medium text-foreground-muted transition-colors hover:bg-surface-subtle hover:text-foreground"
      >
        Sign out
      </button>
    </div>
  );
}
