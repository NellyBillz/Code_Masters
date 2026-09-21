"use client";

import { useEffect, useState } from "react";
import { getIssueComments, postComment } from "../../lib/api";
import { useAuth } from "../context/AuthContext";
import Button from "./ui/Button";
import Avatar from "./ui/Avatar";
import { formatDateTime } from "../../lib/format";

export default function IssueComments({ issueId }) {
  const { user, loading: authLoading } = useAuth();
  const [comments, setComments] = useState([]);
  const [body, setBody] = useState("");
  const [loading, setLoading] = useState(true);
  const [posting, setPosting] = useState(false);
  const [error, setError] = useState("");

  async function loadComments() {
    try {
      setLoading(true);
      const data = await getIssueComments(issueId, { page: 0, size: 20 });
      setComments(data.items || []);
    } catch (err) {
      setError(err.message || "Failed to load comments.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadComments();
  }, [issueId]);

  async function handleSubmit(event) {
    event.preventDefault();

    const trimmedBody = body.trim();
    if (!trimmedBody || !user) return;

    try {
      setPosting(true);
      setError("");
      await postComment(issueId, trimmedBody);
      setBody("");
      await loadComments();
    } catch (err) {
      setError(err.message || "Failed to post comment.");
    } finally {
      setPosting(false);
    }
  }

  return (
    <section aria-label="Discussion">
      <h2 className="mb-5 text-[13px] font-semibold uppercase tracking-wide text-foreground-muted">
        Discussion{comments.length > 0 ? ` · ${comments.length}` : ""}
      </h2>

      {authLoading ? (
        <p className="text-sm text-foreground-muted" aria-live="polite">
          Checking session…
        </p>
      ) : user ? (
        <form onSubmit={handleSubmit} className="mb-8">
          <label htmlFor="comment-body" className="sr-only">
            Write a comment
          </label>
          <textarea
            id="comment-body"
            value={body}
            onChange={(event) => setBody(event.target.value)}
            placeholder="Write a comment…"
            rows={4}
            maxLength={5000}
            className="w-full resize-y rounded-md border border-border-strong bg-surface px-3 py-2.5 text-sm text-foreground outline-none transition-colors placeholder:text-foreground-disabled focus:border-primary focus:ring-2 focus:ring-primary-subtle"
          />
          <div className="mt-2.5 flex justify-end">
            <Button type="submit" variant="primary" size="sm" disabled={posting || !body.trim()}>
              {posting ? "Posting…" : "Comment"}
            </Button>
          </div>
        </form>
      ) : (
        <p className="mb-8 text-sm text-foreground-muted">
          <a href="/auth/github" className="font-medium text-primary hover:text-primary-hover">
            Sign in with GitHub
          </a>{" "}
          to join the discussion.
        </p>
      )}

      {error && (
        <p role="alert" className="mb-4 text-[13px] text-danger">
          {error}
        </p>
      )}

      {loading ? (
        <p className="text-sm text-foreground-muted">Loading comments…</p>
      ) : comments.length > 0 ? (
        <ul className="flex flex-col">
          {comments.map((comment) => (
            <li key={comment.id} className="flex gap-3 border-b border-border py-5 last:border-b-0">
              <Avatar user={comment.author} size="sm" />
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-baseline gap-x-2">
                  <span className="text-sm font-medium text-foreground">
                    {comment.author?.displayName || comment.author?.username || "User"}
                  </span>
                  {comment.createdAt && (
                    <span className="text-xs text-foreground-disabled">{formatDateTime(comment.createdAt)}</span>
                  )}
                </div>
                <p className="mt-1.5 whitespace-pre-wrap break-words text-sm leading-relaxed text-foreground-secondary">
                  {comment.body}
                </p>
              </div>
            </li>
          ))}
        </ul>
      ) : (
        <p className="text-sm text-foreground-muted">No comments yet.</p>
      )}
    </section>
  );
}
