"use client";

import { useEffect, useState } from "react";
import { getIssueComments, postComment } from "../../lib/api";

export default function IssueComments({ issueId }) {
    const [comments, setComments] = useState([]);
    const [body, setBody] = useState("");
    const [loading, setLoading] = useState(true);
    const [posting, setPosting] = useState(false);
    const [error, setError] = useState("");
    const [loggedIn, setLoggedIn] = useState(false);

    async function loadComments() {
        try {
            setLoading(true);
            const data = await getIssueComments(issueId, {
                page: 0,
                size: 20,
            });
            setComments(data.items || []);
        } catch (err) {
            setError(err.message || "Failed to load comments.");
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        setLoggedIn(
            document.cookie
                .split("; ")
                .some((cookie) => cookie.startsWith("CODEMASTERS_CSRF="))
        );

        loadComments();
    }, [issueId]);

    async function handleSubmit(event) {
        event.preventDefault();

        const trimmedBody = body.trim();

        if (!trimmedBody || !loggedIn) {
            return;
        }

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
        <section style={{ marginTop: "2rem" }}>
            <h2>Comments</h2>

            {loggedIn ? (
                <form
                    onSubmit={handleSubmit}
                    style={{ marginBottom: "2rem" }}
                >
                    <textarea
                        value={body}
                        onChange={(event) => setBody(event.target.value)}
                        placeholder="Write a comment..."
                        rows={4}
                        maxLength={5000}
                        style={{
                            width: "100%",
                            padding: "0.75rem",
                            marginBottom: "0.75rem",
                        }}
                    />

                    <button
                        type="submit"
                        disabled={posting || !body.trim()}
                    >
                        {posting ? "Posting..." : "Post comment"}
                    </button>
                </form>
            ) : (
                <p>Log in to post a comment.</p>
            )}

            {error && <p>{error}</p>}

            {loading ? (
                <p>Loading comments...</p>
            ) : comments.length ? (
                comments.map((comment) => (
                    <article
                        key={comment.id}
                        style={{
                            padding: "1rem 0",
                            borderBottom: "1px solid #ddd",
                        }}
                    >
                        <p>{comment.body}</p>

                        <small>
                            {comment.author?.username || "User"}
                            {" · "}
                            {comment.createdAt
                                ? new Date(
                                      comment.createdAt
                                  ).toLocaleString()
                                : ""}
                        </small>
                    </article>
                ))
            ) : (
                <p>No comments yet.</p>
            )}
        </section>
    );
}
