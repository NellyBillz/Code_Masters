import Link from "next/link";
import { getIssue } from "../../../lib/api";
import IssueComments from "../../components/IssueComments";
  import ClaimPanel from "../../components/ClaimPanel";
import IssueMaintainerOverride from "../../components/IssueMaintainerOverride";

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

            <IssueComments issueId={issueId} />
            <ClaimPanel
                issueId={issueId}
                projectId={issue.project?.id}
                initialClaims={issue.claims || []}
            />
            <IssueMaintainerOverride issue={issue} />
        </main>
    );
}