"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { AlertTriangle, ArrowUpRight, HandHeart, Inbox, MessageCircle } from "lucide-react";
import { getMaintainerActivity } from "../../lib/api";
import { useAuth } from "../context/AuthContext";
import ClaimReviewActions from "../components/ClaimReviewActions";

function formatDate(isoString) {
  if (!isoString) return "";
  const date = new Date(isoString);
  if (Number.isNaN(date.getTime())) return "";
  return date.toLocaleDateString(undefined, { day: "numeric", month: "short", year: "numeric" });
}

export default function MaintainerDashboardPage() {
  const { user, loading: authLoading } = useAuth();
  const [projects, setProjects] = useState(null);
  const [error, setError] = useState("");

  async function loadActivity() {
    try {
      const activity = await getMaintainerActivity();
      setProjects(activity.projects || []);
    } catch (err) {
      setError(err.message || "Failed to load your maintainer activity.");
    }
  }

  useEffect(() => {
    if (!user) return;
    let cancelled = false;

    async function load() {
      setError("");
      try {
        const activity = await getMaintainerActivity();
        if (!cancelled) setProjects(activity.projects || []);
      } catch (err) {
        if (!cancelled) setError(err.message || "Failed to load your maintainer activity.");
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, [user]);

  if (authLoading) return <StatusPanel text="Checking session…" />;

  if (!user) {
    return (
      <StatusPanel title="Sign in to see your maintainer dashboard">
        <a
          href="/auth/github"
          style={{
            display: "inline-flex",
            alignItems: "center",
            gap: "8px",
            marginTop: "14px",
            background: "var(--cm-orange)",
            color: "#FCE9DD",
            fontSize: "13px",
            fontWeight: 600,
            borderRadius: "8px",
            padding: "10px 18px",
          }}
        >
          Sign in with GitHub
        </a>
      </StatusPanel>
    );
  }

  if (error) return <StatusPanel title="Something went wrong" text={error} />;
  if (projects === null) return <StatusPanel text="Loading your maintainer activity…" />;

  if (projects.length === 0) {
    return (
      <StatusPanel title="You don't maintain any projects yet">
        <Link
          href="/projects/new"
          style={{
            display: "inline-flex",
            alignItems: "center",
            gap: "8px",
            marginTop: "14px",
            background: "var(--cm-lime)",
            color: "#0A0A0A",
            fontSize: "13px",
            fontWeight: 700,
            borderRadius: "999px",
            padding: "10px 20px",
          }}
        >
          Submit a project
        </Link>
      </StatusPanel>
    );
  }

  const needsReview = projects.flatMap((entry) =>
    (entry.claimsAwaitingReview || []).map((claim) => ({ claim, project: entry.project }))
  );

  return (
    <div style={{ padding: "8px 4px 40px", maxWidth: "760px", margin: "0 auto" }}>
      <DashboardHeader />

      {needsReview.length > 0 && (
        <section
          className="cm-glass"
          style={{ borderRadius: "24px", padding: "20px", marginBottom: "24px", background: "var(--cm-orange-soft)" }}
        >
          <h2 style={{ display: "flex", alignItems: "center", gap: "8px", fontSize: "14px", fontWeight: 700, margin: "0 0 14px", color: "var(--cm-orange-text)" }}>
            <AlertTriangle size={16} strokeWidth={2} aria-hidden="true" />
            Needs your review ({needsReview.length})
          </h2>
          <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
            {needsReview.map(({ claim, project }, index) => (
              <ClaimRow key={claim.id ?? index} claim={claim} project={project} highlight onReviewed={loadActivity} />
            ))}
          </div>
        </section>
      )}

      <div style={{ display: "flex", flexDirection: "column", gap: "20px" }}>
        {projects.map((entry) => (
          <ProjectActivityCard key={entry.project?.id} entry={entry} />
        ))}
      </div>
    </div>
  );
}

function DashboardHeader() {
  return (
    <header style={{ marginBottom: "24px" }}>
      <p style={{ fontSize: "12px", fontWeight: 600, letterSpacing: "0.1em", color: "var(--cm-orange-text)", margin: "0 0 8px" }}>
        MAINTAINER DASHBOARD
      </p>
      <h1 style={{ fontSize: "28px", fontWeight: 700, margin: 0, color: "var(--cm-text-primary)" }}>
        Your activity
      </h1>
      <p style={{ fontSize: "13px", color: "var(--cm-text-secondary)", margin: "8px 0 0" }}>
        Active claims, claims awaiting your review, and recent comments across every project you maintain.
      </p>
    </header>
  );
}

function ProjectActivityCard({ entry }) {
  const project = entry.project || {};
  const activeClaims = entry.activeClaims || [];
  const claimsAwaitingReview = entry.claimsAwaitingReview || [];
  const recentComments = entry.recentComments || [];

  return (
    <section className="cm-glass" style={{ borderRadius: "24px", padding: "20px" }}>
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "16px", flexWrap: "wrap", gap: "8px" }}>
        <Link
          href={`/projects/${project.id}`}
          style={{ display: "inline-flex", alignItems: "center", gap: "6px", fontSize: "15px", fontWeight: 700, color: "var(--cm-text-primary)" }}
        >
          {project.name || `Project #${project.id}`}
          <ArrowUpRight size={14} strokeWidth={2} color="var(--cm-text-muted)" aria-hidden="true" />
        </Link>
        <span style={{ fontSize: "11.5px", color: "var(--cm-text-muted)" }}>
          {activeClaims.length} active claim{activeClaims.length === 1 ? "" : "s"}
          {claimsAwaitingReview.length > 0 ? ` · ${claimsAwaitingReview.length} awaiting review` : ""}
        </span>
      </div>

      <ActivitySubsection
        icon={HandHeart}
        title="Active claims"
        items={activeClaims}
        emptyText="No active claims on this project right now."
        renderItem={(claim, index) => <ClaimRow key={claim.id ?? index} claim={claim} project={project} />}
      />

      <ActivitySubsection
        icon={MessageCircle}
        title="Recent comments"
        items={recentComments}
        emptyText="No recent comments on this project."
        renderItem={(comment, index) => <CommentRow key={comment.id ?? index} comment={comment} />}
        last
      />
    </section>
  );
}

function ActivitySubsection({ icon: Icon, title, items, emptyText, renderItem, last }) {
  return (
    <div style={{ marginTop: "16px", paddingTop: "16px", borderTop: last ? "none" : "0.5px solid var(--cm-border)" }}>
      <h3 style={{ display: "flex", alignItems: "center", gap: "6px", fontSize: "12px", fontWeight: 700, margin: "0 0 10px", color: "var(--cm-text-secondary)" }}>
        <Icon size={13} strokeWidth={2} aria-hidden="true" />
        {title}
      </h3>
      {items.length === 0 ? (
        <p style={{ fontSize: "12px", color: "var(--cm-text-muted)", margin: 0 }}>{emptyText}</p>
      ) : (
        <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
          {items.map(renderItem)}
        </div>
      )}
    </div>
  );
}

function ClaimRow({ claim, project, highlight, onReviewed }) {
  return (
    <div
      style={{
        background: highlight ? "var(--cm-surface)" : "var(--cm-surface-alt)",
        borderRadius: "12px",
        padding: "10px 14px",
        fontSize: "12.5px",
      }}
    >
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: "8px", flexWrap: "wrap" }}>
        <span style={{ color: "var(--cm-text-primary)" }}>
          {claim.user?.username ? (
            <Link href={`/users/${claim.user.username}`} style={{ fontWeight: 600 }}>
              {claim.user?.displayName || claim.user.username}
            </Link>
          ) : (
            <strong>{claim.user?.displayName || "A contributor"}</strong>
          )}
          {" on "}
          <Link href={`/issues/${claim.issueId}`} style={{ fontWeight: 600, color: "var(--cm-lime-text)" }}>
            issue #{claim.issueId}
          </Link>
          {highlight && project?.name ? ` (${project.name})` : ""}
        </span>
        {claim.createdAt && (
          <span style={{ color: "var(--cm-text-muted)", fontSize: "11px" }}>{formatDate(claim.createdAt)}</span>
        )}
      </div>
      {claim.note && (
        <p style={{ margin: "6px 0 0", color: "var(--cm-text-secondary)" }}>{claim.note}</p>
      )}
      {claim.pullRequestUrl && (
        <a
          href={claim.pullRequestUrl}
          target="_blank"
          rel="noreferrer"
          style={{ display: "inline-flex", alignItems: "center", gap: "4px", marginTop: "6px", color: "var(--cm-orange-text)", fontWeight: 600 }}
        >
          View pull request
          <ArrowUpRight size={12} strokeWidth={2} aria-hidden="true" />
        </a>
      )}
      {highlight && (
        <ClaimReviewActions issueId={claim.issueId} claimId={claim.id} onReviewed={onReviewed} />
      )}
    </div>
  );
}

function CommentRow({ comment }) {
  return (
    <div style={{ background: "var(--cm-surface-alt)", borderRadius: "12px", padding: "10px 14px", fontSize: "12.5px" }}>
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: "8px" }}>
        {comment.author?.username ? (
          <Link href={`/users/${comment.author.username}`} style={{ fontWeight: 600, color: "var(--cm-text-primary)" }}>
            {comment.author?.displayName || comment.author.username}
          </Link>
        ) : (
          <strong style={{ color: "var(--cm-text-primary)" }}>{comment.author?.displayName || "User"}</strong>
        )}
        {comment.createdAt && (
          <span style={{ color: "var(--cm-text-muted)", fontSize: "11px" }}>{formatDate(comment.createdAt)}</span>
        )}
      </div>
      <p style={{ margin: "6px 0 0", color: "var(--cm-text-secondary)" }}>{comment.body}</p>
    </div>
  );
}

function StatusPanel({ title, text, children }) {
  return (
    <div style={{ padding: "8px 4px 40px", maxWidth: "760px", margin: "0 auto" }}>
      <div className="cm-glass" style={{ borderRadius: "24px", padding: "48px 24px", textAlign: "center" }}>
        <Inbox size={22} strokeWidth={1.8} color="var(--cm-text-muted)" aria-hidden="true" style={{ marginBottom: "10px" }} />
        {title && (
          <p style={{ fontSize: "15px", fontWeight: 600, margin: "0 0 6px", color: "var(--cm-text-primary)" }}>{title}</p>
        )}
        {text && <p style={{ fontSize: "13px", color: "var(--cm-text-secondary)", margin: 0 }}>{text}</p>}
        {children}
      </div>
    </div>
  );
}
