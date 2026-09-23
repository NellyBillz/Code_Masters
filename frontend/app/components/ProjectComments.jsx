"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { getProjectComments, postProjectComment } from "../../lib/api";
import { useAuth } from "../context/AuthContext";
import ReportCommentButton from "./ReportCommentButton";
import EditCommentButton from "./EditCommentButton";
import DeleteCommentButton from "./DeleteCommentButton";

/**
 * Project-level discussion (distinct from an issue's comments) — mirrors
 * IssueComments.jsx exactly (same edit/delete/report components, all
 * already generic on commentId), just pointed at the project-comment
 * endpoints instead of the issue-comment ones.
 */
export default function ProjectComments({ projectId }) {
  const { user, loading: authLoading } = useAuth();
  const [comments, setComments] = useState([]);
  const [body, setBody] = useState("");
  const [loading, setLoading] = useState(true);
  const [posting, setPosting] = useState(false);
  const [error, setError] = useState("");

  async function loadComments() {
    try {
      setLoading(true);
      const data = await getProjectComments(projectId, { page: 0, size: 20 });
      setComments(data.items || []);
    } catch (err) {
      setError(err.message || "Failed to load comments.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- fetch-on-mount pattern; no derived-state alternative for reading server data
    loadComments();
    // eslint-disable-next-line react-hooks/exhaustive-deps -- loadComments is stable for this component's lifetime
  }, [projectId]);

  async function handleSubmit(event) {
    event.preventDefault();
    const trimmedBody = body.trim();
    if (!trimmedBody || !user) return;

    try {
      setPosting(true);
      setError("");
      await postProjectComment(projectId, trimmedBody);
      setBody("");
      await loadComments();
    } catch (err) {
      setError(err.message || "Failed to post comment.");
    } finally {
      setPosting(false);
    }
  }

  return (
    <section className="cm-glass" style={{ borderRadius: "24px", padding: "20px 24px" }}>
      <h2 style={{ fontSize: "15px", fontWeight: 700, margin: "0 0 16px", color: "var(--cm-text-primary)" }}>Discussion</h2>

      {authLoading ? (
        <p style={{ fontSize: "12.5px", color: "var(--cm-text-muted)" }}>Checking session…</p>
      ) : user ? (
        <form onSubmit={handleSubmit} style={{ marginBottom: "20px" }}>
          <textarea
            value={body}
            onChange={(event) => setBody(event.target.value)}
            placeholder="Ask a question or share context about this project…"
            rows={4}
            maxLength={5000}
            style={{
              width: "100%",
              borderRadius: "14px",
              border: "0.5px solid var(--cm-border)",
              background: "var(--cm-surface)",
              color: "var(--cm-text-primary)",
              fontSize: "13px",
              padding: "12px 14px",
              marginBottom: "10px",
              resize: "vertical",
            }}
          />

          <button
            type="submit"
            disabled={posting || !body.trim()}
            style={{
              borderRadius: "999px",
              padding: "9px 18px",
              fontSize: "13px",
              fontWeight: 700,
              border: "none",
              cursor: posting || !body.trim() ? "not-allowed" : "pointer",
              opacity: posting || !body.trim() ? 0.5 : 1,
              background: "var(--cm-sidebar)",
              color: "#FFFFFF",
            }}
          >
            {posting ? "Posting…" : "Post comment"}
          </button>
        </form>
      ) : (
        <p style={{ fontSize: "13px", color: "var(--cm-text-secondary)", marginBottom: "20px" }}>
          <a href="/auth/github" style={{ color: "var(--cm-lime-text)", fontWeight: 600 }}>Log in</a> to post a comment.
        </p>
      )}

      {error && (
        <p role="alert" style={{ fontSize: "12.5px", color: "var(--cm-orange-text)", marginBottom: "12px" }}>
          {error}
        </p>
      )}

      {loading ? (
        <p style={{ fontSize: "12.5px", color: "var(--cm-text-muted)" }}>Loading comments…</p>
      ) : comments.length ? (
        <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
          {comments.map((comment) => {
            const isOwner = Boolean(user && comment.author?.id === user.id);

            return (
              <article key={comment.id} style={{ background: "var(--cm-surface-alt)", borderRadius: "14px", padding: "12px 16px" }}>
                {isOwner ? (
                  <EditCommentButton
                    comment={comment}
                    onSaved={(updated) =>
                      setComments((prev) => prev.map((c) => (c.id === comment.id ? updated : c)))
                    }
                  />
                ) : (
                  <p style={{ fontSize: "13px", color: "var(--cm-text-primary)", margin: "0 0 6px", lineHeight: 1.5 }}>
                    {comment.body}
                    {comment.edited && (
                      <span style={{ fontSize: "11px", color: "var(--cm-text-muted)", marginLeft: "6px" }}>(edited)</span>
                    )}
                  </p>
                )}
                <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: "10px", flexWrap: "wrap" }}>
                  <p style={{ fontSize: "11.5px", color: "var(--cm-text-muted)", margin: 0 }}>
                    {comment.author?.username ? (
                      <Link href={`/users/${comment.author.username}`} style={{ fontWeight: 600 }}>
                        {comment.author.username}
                      </Link>
                    ) : (
                      "User"
                    )}
                    {comment.createdAt ? ` · ${new Date(comment.createdAt).toLocaleString()}` : ""}
                  </p>
                  {user && !isOwner && <ReportCommentButton commentId={comment.id} />}
                  {isOwner && (
                    <DeleteCommentButton
                      commentId={comment.id}
                      onDeleted={(deletedId) =>
                        setComments((prev) => prev.filter((c) => c.id !== deletedId))
                      }
                    />
                  )}
                </div>
              </article>
            );
          })}
        </div>
      ) : (
        <p style={{ fontSize: "12.5px", color: "var(--cm-text-muted)" }}>No discussion yet on this project.</p>
      )}
    </section>
  );
}
