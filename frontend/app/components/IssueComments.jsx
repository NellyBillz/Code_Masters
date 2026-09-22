"use client";

import { useEffect, useState } from "react";
import { AlertTriangle } from "lucide-react";
import { getIssueComments, postComment } from "../../lib/api";
import { useAuth } from "../context/AuthContext";

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
    <section className="cm-glass" style={{ borderRadius: "24px", padding: "20px 24px" }}>
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: "10px", marginBottom: "16px" }}>
        <h2 style={{ fontSize: "15px", fontWeight: 700, margin: 0, color: "var(--cm-text-primary)" }}>Comments</h2>

        {/* comments endpoints aren't on the confirmed-working list yet */}
        <span
          title="This endpoint hasn't been confirmed working end-to-end yet"
          style={{ display: "inline-flex", alignItems: "center", gap: "4px", fontSize: "10.5px", fontWeight: 600, color: "var(--cm-orange-text)", background: "var(--cm-orange-soft)", padding: "3px 9px", borderRadius: "999px" }}
        >
          <AlertTriangle size={11} strokeWidth={2} aria-hidden="true" />
          Rolling out
        </span>
      </div>

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
          {comments.map((comment) => (
            <article key={comment.id} style={{ background: "var(--cm-surface-alt)", borderRadius: "14px", padding: "12px 16px" }}>
              <p style={{ fontSize: "13px", color: "var(--cm-text-primary)", margin: "0 0 6px", lineHeight: 1.5 }}>{comment.body}</p>
              <p style={{ fontSize: "11.5px", color: "var(--cm-text-muted)", margin: 0 }}>
                {comment.author?.username || "User"}
                {comment.createdAt ? ` · ${new Date(comment.createdAt).toLocaleString()}` : ""}
              </p>
            </article>
          ))}
        </div>
      ) : (
        <p style={{ fontSize: "12.5px", color: "var(--cm-text-muted)" }}>No comments yet.</p>
      )}
    </section>
  );
}
