import Link from "next/link";
import { getIssue } from "../../../lib/api";

export default async function IssueDetailPage({ params }) {
    const { issueId } = await params;
    const issue = await getIssue(issueId);

    return (
        <main style={{ padding: "2rem", maxWidth: "1000px", margin: "0 auto" }}>
            <Link href="/projects">← Back to projects</Link>

            <section style={{ marginTop: "2rem" }}>
                <div>
                    <span>{issue.status}</span>
                    {" · "}
                    <span>{issue.difficulty}</span>
                </div>

                <h1>{issue.title}</h1>

                <p>{issue.bodyExcerpt}</p>

                <p>
                    Labels:{" "}
                    {issue.labels?.length
                        ? issue.labels.join(", ")
                        : "No labels"}
                </p>

                <p>
                    Beginner friendly:{" "}
                    {issue.isBeginnerFriendly ? "Yes" : "No"}
                </p>

                <p>
                    Claims: {issue.claimCount}
                </p>

                <p>
                    <a
                        href={issue.githubUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                    >
                        View this issue on GitHub →
                    </a>
                </p>
            </section>

            <section style={{ marginTop: "2rem" }}>
                <h2>Parent Project</h2>

                <Link href={`/projects/${issue.project.id}`}>
                    {issue.project.name}
                </Link>

                <p>{issue.project.description}</p>
            </section>

            <section style={{ marginTop: "2rem" }}>
                <h2>Comments</h2>

                {issue.comments?.length ? (
                    issue.comments.map((comment) => (
                        <article key={comment.id}>
                            <p>{comment.body}</p>
                        </article>
                    ))
                ) : (
                    <p>No comments yet.</p>
                )}
            </section>

            <section style={{ marginTop: "2rem" }}>
                <h2>Claims</h2>

                {issue.claims?.length ? (
                    issue.claims.map((claim) => (
                        <article key={claim.id}>
                            <p>{JSON.stringify(claim)}</p>
                        </article>
                    ))
                ) : (
                    <p>No active claims.</p>
                )}
            </section>
        </main>
    );
}