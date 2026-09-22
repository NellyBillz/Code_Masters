import Link from "next/link";
import { ArrowLeft, ArrowUpRight, Tag, Sparkles } from "lucide-react";
import { getIssue } from "../../../lib/api";
import IssueComments from "../../components/IssueComments";
import ClaimPanel from "../../components/ClaimPanel";
import IssueMaintainerOverride from "../../components/IssueMaintainerOverride";

const STATUS_STYLES = {
  open: { bg: "var(--cm-lime-soft)", text: "var(--cm-lime-text)" },
  closed: { bg: "var(--cm-surface-alt)", text: "var(--cm-text-secondary)" },
};

export default async function IssueDetailPage({ params }) {
  const { issueId } = await params;

  let issue;
  try {
    issue = await getIssue(issueId);
  } catch (err) {
    return (
      <div style={{ padding: "8px 4px 40px", maxWidth: "760px", margin: "0 auto" }}>
        <Link
          href="/projects"
          style={{ display: "inline-flex", alignItems: "center", gap: "6px", fontSize: "13px", color: "var(--cm-text-secondary)", marginBottom: "18px" }}
        >
          <ArrowLeft size={14} strokeWidth={2} aria-hidden="true" />
          Back to projects
        </Link>
        <div className="cm-glass" style={{ borderRadius: "24px", padding: "48px 24px", textAlign: "center" }}>
          <p style={{ fontSize: "15px", fontWeight: 600, margin: "0 0 6px", color: "var(--cm-text-primary)" }}>
            We couldn&apos;t load this issue
          </p>
          <p style={{ fontSize: "13px", color: "var(--cm-text-secondary)", margin: 0 }}>
            {err.status === 404 ? "This issue may not exist or has been removed." : err.message || "The platform may be temporarily unavailable."}
          </p>
        </div>
      </div>
    );
  }

  const statusStyle = STATUS_STYLES[String(issue.status).toLowerCase()] || STATUS_STYLES.closed;

  return (
    <div style={{ padding: "8px 4px 40px", maxWidth: "760px", margin: "0 auto" }}>
      <Link
        href="/projects"
        style={{ display: "inline-flex", alignItems: "center", gap: "6px", fontSize: "13px", color: "var(--cm-text-secondary)", marginBottom: "18px" }}
      >
        <ArrowLeft size={14} strokeWidth={2} aria-hidden="true" />
        Back to projects
      </Link>

      <section className="cm-glass" style={{ borderRadius: "28px", padding: "24px 28px", marginBottom: "20px" }}>
        <div style={{ display: "flex", alignItems: "center", gap: "8px", marginBottom: "12px", flexWrap: "wrap" }}>
          <span
            style={{
              fontSize: "11px",
              fontWeight: 600,
              textTransform: "capitalize",
              padding: "3px 10px",
              borderRadius: "999px",
              background: statusStyle.bg,
              color: statusStyle.text,
            }}
          >
            {issue.status || "unknown"}
          </span>
          {issue.difficulty && (
            <span
              style={{
                fontSize: "11px",
                fontWeight: 600,
                textTransform: "capitalize",
                padding: "3px 10px",
                borderRadius: "999px",
                background: "var(--cm-orange-soft)",
                color: "var(--cm-orange-text)",
              }}
            >
              {issue.difficulty}
            </span>
          )}
          {issue.isBeginnerFriendly && (
            <span
              style={{
                display: "inline-flex",
                alignItems: "center",
                gap: "4px",
                fontSize: "11px",
                fontWeight: 600,
                padding: "3px 10px",
                borderRadius: "999px",
                background: "var(--cm-lime-soft)",
                color: "var(--cm-lime-text)",
              }}
            >
              <Sparkles size={11} strokeWidth={2} aria-hidden="true" />
              Beginner friendly
            </span>
          )}
        </div>

        <h1 style={{ fontSize: "24px", fontWeight: 700, margin: "0 0 12px", color: "var(--cm-text-primary)" }}>
          {issue.title}
        </h1>

        {issue.bodyExcerpt && (
          <p style={{ fontSize: "14px", lineHeight: 1.6, color: "var(--cm-text-secondary)", margin: "0 0 16px" }}>
            {issue.bodyExcerpt}
          </p>
        )}

        {issue.labels?.length > 0 && (
          <div style={{ display: "flex", alignItems: "center", gap: "6px", flexWrap: "wrap", marginBottom: "18px" }}>
            <Tag size={12} strokeWidth={1.8} color="var(--cm-text-muted)" aria-hidden="true" />
            {issue.labels.map((label) => (
              <span
                key={label}
                style={{ fontSize: "11px", color: "var(--cm-text-secondary)", background: "var(--cm-surface-alt)", padding: "3px 9px", borderRadius: "999px" }}
              >
                {label}
              </span>
            ))}
          </div>
        )}

        {issue.githubUrl && (
          <a
            href={issue.githubUrl}
            target="_blank"
            rel="noopener noreferrer"
            style={{
              display: "inline-flex",
              alignItems: "center",
              gap: "6px",
              fontSize: "13px",
              fontWeight: 600,
              color: "var(--cm-text-primary)",
            }}
          >
            View this issue on GitHub
            <ArrowUpRight size={14} strokeWidth={2} aria-hidden="true" />
          </a>
        )}
      </section>

      {issue.project && (
        <Link
          href={`/projects/${issue.project.id}`}
          className="cm-glass"
          style={{ display: "block", borderRadius: "20px", padding: "16px 20px", marginBottom: "20px" }}
        >
          <p style={{ fontSize: "11px", color: "var(--cm-text-muted)", margin: "0 0 4px" }}>Parent project</p>
          <p style={{ fontSize: "14px", fontWeight: 600, margin: "0 0 4px", color: "var(--cm-text-primary)" }}>{issue.project.name}</p>
          <p style={{ fontSize: "12.5px", color: "var(--cm-text-secondary)", margin: 0 }}>{issue.project.description}</p>
        </Link>
      )}

      <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
        <ClaimPanel issueId={issueId} initialClaims={issue.claims || []} />
        <IssueMaintainerOverride issue={issue} />
        <IssueComments issueId={issueId} />
      </div>
    </div>
  );
}
