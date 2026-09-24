"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { HelpCircle, CheckCircle2 } from "lucide-react";
import { getIssueComments, postComment, resolveQuestion, getProject } from "../../lib/api";
import { useAuth } from "../context/AuthContext";
import ReportCommentButton from "./ReportCommentButton";
import EditCommentButton from "./EditCommentButton";
import DeleteCommentButton from "./DeleteCommentButton";

export default function IssueComments({ issueId, projectId }) {
  const { user, loading: authLoading } = useAuth();
  const [comments, setComments] = useState([]);
  const [body, setBody] = useState("");
  const [isQuestion, setIsQuestion] = useState(false);
  const [loading, setLoading] = useState(true);
  const [posting, setPosting] = useState(false);
  const [error, setError] = useState("");
  const [project, setProject] = useState(null);

  useEffect(() => {
    if (!projectId) return;
    getProject(projectId)
      .then(setProject)
      .catch(() => setProject(null));
  }, [projectId]);

  const isMaintainer = Boolean(
    user && project?.maintainers?.some(
      (maintainer) => maintainer.user?.id === user.id || maintainer.user?.username === user.username
    )
  );

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
    // eslint-disable-next-line react-hooks/set-state-in-effect -- fetch-on-mount pattern; no derived-state alternative for reading server data
    loadComments();
    // eslint-disable-next-line react-hooks/exhaustive-deps -- loadComments is stable for this component's lifetime
  }, [issueId]);

  async function handleSubmit(event) {
    event.preventDefault();
    const trimmedBody = body.trim();
    if (!trimmedBody || !user) return;

    try {
      setPosting(true);
      setError("");
      await postComment(issueId, trimmedBody, isQuestion);
      setBody("");
      setIsQuestion(false);
      await loadComments();
    } catch (err) {
      setError(err.message || "Failed to post comment.");
    } finally {
      setPosting(false);
    }
  }

  async function handleResolve(commentId) {
    try {
      const updated = await resolveQuestion(commentId);
      setComments((prev) => prev.map((c) => (c.id === commentId ? updated : c)));
    } catch (err) {
      setError(err.message || "Failed to mark this question answered.");
    }
  }

  return (
    <section className="cm-glass" style={{ borderRadius: "24px", padding: "20px 24px" }}>
      <h2 style={{ fontSize: "15px", fontWeight: 700, margin: "0 0 16px", color: "var(--cm-text-primary)" }}>Comments</h2>

      {authLoading ? (
        <p style={{ fontSize: "12.5px", color: "var(--cm-text-muted)" }}>Checking session…</p>
      ) : user ? (
        <form onSubmit={handleSubmit} style={{ marginBottom: "20px" }}>
          <textarea
            value={body}
            onChange={(event) => setBody(event.target.value)}
            placeholder="Write a comment…"
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

          <label style={{ display: "flex", alignItems: "center", gap: "7px", fontSize: "12px", color: "var(--cm-text-secondary)", marginBottom: "12px", cursor: "pointer" }}>
            <input
              type="checkbox"
              checked={isQuestion}
              onChange={(event) => setIsQuestion(event.target.checked)}
              style={{ width: "14px", height: "14px", accentColor: "var(--cm-orange)" }}
            />
            This is a blocking question — I need this answered before I can start
          </label>

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
            {posting ? "Posting…" : isQuestion ? "Post question" : "Post comment"}
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
                {comment.isQuestion && (
                  <span
                    style={{
                      display: "inline-flex",
                      alignItems: "center",
                      gap: "4px",
                      fontSize: "10.5px",
                      fontWeight: 700,
                      padding: "2px 8px",
                      borderRadius: "999px",
                      marginBottom: "6px",
                      background: comment.resolved ? "var(--cm-lime-soft)" : "var(--cm-orange-soft)",
                      color: comment.resolved ? "var(--cm-lime-text)" : "var(--cm-orange-text)",
                    }}
                  >
                    {comment.resolved ? (
                      <>
                        <CheckCircle2 size={11} strokeWidth={2} aria-hidden="true" />
                        Question answered
                      </>
                    ) : (
                      <>
                        <HelpCircle size={11} strokeWidth={2} aria-hidden="true" />
                        Blocking question
                      </>
                    )}
                  </span>
                )}
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
                  {isMaintainer && comment.isQuestion && !comment.resolved && (
                    <button
                      type="button"
                      onClick={() => handleResolve(comment.id)}
                      style={{ display: "inline-flex", alignItems: "center", gap: "4px", background: "none", border: "none", padding: 0, fontSize: "11.5px", fontWeight: 700, color: "var(--cm-lime-text)", cursor: "pointer" }}
                    >
                      <CheckCircle2 size={11} strokeWidth={2} aria-hidden="true" />
                      Mark answered
                    </button>
                  )}
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
        <p style={{ fontSize: "12.5px", color: "var(--cm-text-muted)" }}>No comments yet.</p>
      )}
    </section>
  );
}