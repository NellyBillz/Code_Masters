"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams, useSearchParams } from "next/navigation";
import {
  ArrowLeft,
  Star,
  GitFork,
  ArrowUpRight,
  BadgeCheck,
  Check,
  Circle,
  Minus,
  Pencil,
} from "lucide-react";
import { getProject } from "../../../lib/api";
import { useAuth } from "../../context/AuthContext";
import MaintainersPanel from "../../components/MaintainersPanel";
import SyncStatusBanner from "../../components/SyncStatusBanner";
import EditProjectPanel from "../../components/EditProjectPanel";

const TABS = ["Overview", "Issues", "Pull requests", "Contributors", "Discussions"];

export default function ProjectDetail() {
  const params = useParams();
  const searchParams = useSearchParams();
  const projectId = params?.projectId;
  const { user: currentUser } = useAuth();

  const [project, setProject] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [activeTab, setActiveTab] = useState("Overview");
  const [editing, setEditing] = useState(false);

  const isMaintainer = Boolean(
    currentUser &&
      project?.maintainers?.some(
        (maint) => maint.user?.id === currentUser.id || maint.user?.username === currentUser.username
      )
  );

  useEffect(() => {
    if (!projectId) return;
    let cancelled = false;

    async function loadProject() {
      setLoading(true);
      setError("");
      try {
        const result = await getProject(projectId);
        if (!cancelled) setProject(result);
      } catch (err) {
        if (!cancelled) setError(err.message || "Failed to load project.");
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    loadProject();
    return () => {
      cancelled = true;
    };
  }, [projectId]);

  const handleMaintainerAdded = () => {
    if (projectId) getProject(projectId).then(setProject).catch(console.error);
  };

  const handleMaintainerRemoved = (userId) => {
    setProject((prev) => ({
      ...prev,
      maintainers: prev.maintainers.filter((m) => m.user?.id !== userId),
    }));
  };

  if (loading) return <StatusPanel text="Loading project…" />;
  if (error) return <StatusPanel title="Something went wrong" text={error} />;
  if (!project) return <StatusPanel title="Project not found" text="This project may have been removed." />;

  return (
    <div style={{ padding: "8px 4px 40px" }}>
      <Link
        href="/projects"
        style={{ display: "inline-flex", alignItems: "center", gap: "6px", fontSize: "13px", color: "var(--cm-text-secondary)", marginBottom: "18px" }}
      >
        <ArrowLeft size={14} strokeWidth={2} aria-hidden="true" />
        Back to projects
      </Link>

      <SyncStatusBanner
        projectId={project.id}
        initialJobId={searchParams.get("syncJobId")}
        isMaintainer={isMaintainer}
        onSynced={() => getProject(projectId).then(setProject).catch(() => {})}
      />

      {/* Header */}
      <header className="cm-glass" style={{ borderRadius: "28px", padding: "24px 28px", marginBottom: "20px" }}>
        <div style={{ display: "flex", alignItems: "flex-start", justifyContent: "space-between", gap: "16px", flexWrap: "wrap" }}>
          <div style={{ display: "flex", alignItems: "flex-start", gap: "14px", minWidth: 0 }}>
            <span
              aria-hidden="true"
              style={{
                width: "44px",
                height: "44px",
                borderRadius: "12px",
                background: "var(--cm-sidebar)",
                color: "var(--cm-lime)",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                fontWeight: 700,
                fontSize: "15px",
                flexShrink: 0,
              }}
            >
              {project.name?.[0]?.toUpperCase() || "?"}
            </span>

            <div style={{ minWidth: 0 }}>
              <div style={{ display: "flex", alignItems: "center", gap: "8px", flexWrap: "wrap" }}>
                <h1 style={{ fontSize: "22px", fontWeight: 700, margin: 0, color: "var(--cm-text-primary)" }}>
                  {project.name}
                </h1>
                {project.verified && (
                  <span
                    style={{
                      display: "inline-flex",
                      alignItems: "center",
                      gap: "4px",
                      background: "var(--cm-lime-soft)",
                      color: "var(--cm-lime-text)",
                      fontSize: "11px",
                      fontWeight: 600,
                      padding: "3px 9px",
                      borderRadius: "999px",
                    }}
                  >
                    <BadgeCheck size={12} strokeWidth={2} aria-hidden="true" />
                    Verified
                  </span>
                )}
              </div>
              <p style={{ fontSize: "13px", color: "var(--cm-text-secondary)", margin: "6px 0 0", maxWidth: "560px" }}>
                {project.description}
              </p>
            </div>
          </div>

          <div style={{ display: "flex", alignItems: "center", gap: "8px", flexShrink: 0 }}>
            <span
              className="cm-glass"
              style={{ display: "inline-flex", alignItems: "center", gap: "6px", borderRadius: "999px", padding: "8px 14px", fontSize: "13px", fontWeight: 600 }}
            >
              <Star size={14} strokeWidth={0} fill="var(--cm-orange)" aria-hidden="true" />
              {project.stars ?? 0}
            </span>
            <span
              className="cm-glass"
              style={{ display: "inline-flex", alignItems: "center", gap: "6px", borderRadius: "999px", padding: "8px 14px", fontSize: "13px", fontWeight: 600 }}
            >
              <GitFork size={14} strokeWidth={1.8} aria-hidden="true" />
              {project.forks ?? 0}
            </span>

            {isMaintainer && !editing && (
              <button
                type="button"
                onClick={() => setEditing(true)}
                className="cm-glass"
                style={{
                  display: "inline-flex",
                  alignItems: "center",
                  gap: "6px",
                  borderRadius: "999px",
                  padding: "8px 16px",
                  fontSize: "13px",
                  fontWeight: 600,
                  color: "var(--cm-text-primary)",
                  cursor: "pointer",
                }}
              >
                <Pencil size={13} strokeWidth={2} aria-hidden="true" />
                Edit
              </button>
            )}

            {project.githubUrl && (
              <a
                href={project.githubUrl}
                target="_blank"
                rel="noreferrer"
                style={{
                  display: "inline-flex",
                  alignItems: "center",
                  gap: "6px",
                  borderRadius: "999px",
                  padding: "9px 18px",
                  fontSize: "13px",
                  fontWeight: 700,
                  background: "var(--cm-orange)",
                  color: "#2B1108",
                }}
              >
                Contribute
                <ArrowUpRight size={14} strokeWidth={2} aria-hidden="true" />
              </a>
            )}
          </div>
        </div>
      </header>

      {editing && (
        <EditProjectPanel
          project={project}
          onCancel={() => setEditing(false)}
          onSaved={(updated) => {
            setProject((prev) => ({ ...prev, ...updated }));
            setEditing(false);
          }}
        />
      )}

      {/* Tabs */}
      <div style={{ display: "flex", gap: "4px", borderBottom: "0.5px solid var(--cm-border)", marginBottom: "24px", overflowX: "auto" }}>
        {TABS.map((tab) => (
          <button
            key={tab}
            type="button"
            onClick={() => setActiveTab(tab)}
            style={{
              padding: "10px 16px",
              fontSize: "13px",
              fontWeight: 600,
              whiteSpace: "nowrap",
              background: "none",
              border: "none",
              cursor: "pointer",
              color: activeTab === tab ? "var(--cm-text-primary)" : "var(--cm-text-secondary)",
              borderBottom: activeTab === tab ? "2px solid var(--cm-lime)" : "2px solid transparent",
            }}
          >
            {tab}
          </button>
        ))}
      </div>

      {activeTab === "Overview" && <OverviewTab project={project} />}
      {activeTab === "Issues" && <IssuesTab project={project} />}
      {activeTab === "Pull requests" && <ComingSoonTab label="pull requests" />}
      {activeTab === "Contributors" && (
        <ContributorsTab
          project={project}
          isMaintainer={isMaintainer}
          onMaintainerAdded={handleMaintainerAdded}
          onMaintainerRemoved={handleMaintainerRemoved}
        />
      )}
      {activeTab === "Discussions" && <DiscussionsTab project={project} />}
    </div>
  );
}

function OverviewTab({ project }) {
  // Every check here reflects a real project field except README and
  // Tests/CI, which have no backing signal anywhere in the API — those are
  // explicitly "unknown", not silently marked done. hasContributingGuide/
  // hasCodeOfConduct come from GitHub's community-profile endpoint via sync
  // (confirmed present on GET /projects/{id} — this file previously ignored
  // them and hardcoded both to done).
  const checklist = [
    { label: "License", status: project.license ? "yes" : "no" },
    { label: "Contributing guide", status: project.hasContributingGuide ? "yes" : "no" },
    { label: "Code of Conduct", status: project.hasCodeOfConduct ? "yes" : "no" },
    { label: "Active maintainers", status: (project.maintainers?.length ?? 0) > 0 ? "yes" : "no" },
    { label: "Good first issues", status: project.hasBeginnerFriendlyIssues ? "yes" : "no" },
    { label: "README present", status: "unknown" },
    { label: "Tests / CI", status: "unknown" },
  ];
  // Readiness is a percentage of known facts only — an "unknown" isn't a
  // failure, and counting it as one would just trade one kind of made-up
  // number for another.
  const trackedChecks = checklist.filter((c) => c.status !== "unknown");
  const readiness = trackedChecks.length > 0
    ? Math.round((trackedChecks.filter((c) => c.status === "yes").length / trackedChecks.length) * 100)
    : 0;

  const circumference = 2 * Math.PI * 42;
  const dash = (readiness / 100) * circumference;

  return (
    <div style={{ display: "grid", gridTemplateColumns: "1.4fr 1fr", gap: "20px", alignItems: "start" }}>
      <div className="cm-glass" style={{ borderRadius: "24px", padding: "24px" }}>
        <h2 style={{ fontSize: "15px", fontWeight: 700, margin: "0 0 14px", color: "var(--cm-text-primary)" }}>About</h2>
        <p style={{ fontSize: "13px", lineHeight: 1.6, color: "var(--cm-text-secondary)", margin: "0 0 18px" }}>
          {project.description || "No description provided."}
        </p>

        <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
          <InfoRow label="README" status="unknown" />
          <InfoRow label="Contributing Guide" status={project.hasContributingGuide ? "yes" : "no"} />
          <InfoRow label="Code of Conduct" status={project.hasCodeOfConduct ? "yes" : "no"} />
          <InfoRow label={project.license || "License unspecified"} status={project.license ? "yes" : "no"} />
        </div>

        <div style={{ marginTop: "22px", paddingTop: "18px", borderTop: "0.5px solid var(--cm-border)" }}>
          <p style={{ fontSize: "12px", fontWeight: 600, color: "var(--cm-text-primary)", margin: "0 0 10px" }}>
            {project.contributors ?? 0} contributors from across South Africa
          </p>
          <ContributorAvatars maintainers={project.maintainers} count={project.contributors} />
        </div>
      </div>

      <div className="cm-glass" style={{ borderRadius: "24px", padding: "24px" }}>
        <h2 style={{ fontSize: "13px", fontWeight: 700, margin: "0 0 18px", color: "var(--cm-text-primary)" }}>
          Contributor readiness
        </h2>

        <div style={{ display: "flex", alignItems: "center", gap: "20px", marginBottom: "20px" }}>
          <div style={{ position: "relative", width: "96px", height: "96px", flexShrink: 0 }}>
            <svg width="96" height="96" viewBox="0 0 96 96" aria-hidden="true">
              <circle cx="48" cy="48" r="42" fill="none" stroke="var(--cm-surface-alt)" strokeWidth="8" />
              <circle
                cx="48"
                cy="48"
                r="42"
                fill="none"
                stroke="var(--cm-lime)"
                strokeWidth="8"
                strokeLinecap="round"
                strokeDasharray={`${dash} ${circumference}`}
                transform="rotate(-90 48 48)"
              />
            </svg>
            <p
              style={{
                position: "absolute",
                inset: 0,
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                fontSize: "22px",
                fontWeight: 700,
                margin: 0,
                color: "var(--cm-text-primary)",
              }}
            >
              {readiness}%
            </p>
          </div>
          <p style={{ fontSize: "12.5px", color: "var(--cm-text-secondary)", margin: 0, lineHeight: 1.5 }}>
            Based on known signals only — license, contributing guide, code of conduct, active maintainers, and open beginner issues. README and test coverage aren&rsquo;t tracked yet, so they&rsquo;re excluded rather than assumed.
          </p>
        </div>

        <ul style={{ listStyle: "none", margin: 0, padding: 0, display: "flex", flexDirection: "column", gap: "10px" }}>
          {checklist.map((item) => (
            <li key={item.label} style={{ display: "flex", alignItems: "center", gap: "8px", fontSize: "12.5px", color: item.status === "unknown" ? "var(--cm-text-muted)" : "var(--cm-text-secondary)" }}>
              {item.status === "yes" && <Check size={14} strokeWidth={2.2} color="var(--cm-lime-text)" aria-hidden="true" />}
              {item.status === "no" && <Circle size={14} strokeWidth={2} color="var(--cm-text-muted)" aria-hidden="true" />}
              {item.status === "unknown" && <Minus size={14} strokeWidth={2} color="var(--cm-text-muted)" aria-hidden="true" />}
              {item.label}
              {item.status === "unknown" && (
                <span style={{ fontSize: "10.5px" }}>(not tracked yet)</span>
              )}
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}

function InfoRow({ label, status }) {
  return (
    <div style={{ display: "flex", alignItems: "center", gap: "8px", fontSize: "12.5px", color: status === "unknown" ? "var(--cm-text-muted)" : "var(--cm-text-secondary)" }}>
      {status === "yes" && <Check size={13} strokeWidth={2} color="var(--cm-lime-text)" aria-hidden="true" />}
      {status === "no" && <Circle size={13} strokeWidth={2} color="var(--cm-text-muted)" aria-hidden="true" />}
      {status === "unknown" && <Minus size={13} strokeWidth={2} color="var(--cm-text-muted)" aria-hidden="true" />}
      {label}
      {status === "unknown" && <span style={{ fontSize: "10.5px" }}>(not tracked yet)</span>}
    </div>
  );
}

function ContributorAvatars({ maintainers = [], count = 0 }) {
  const shown = maintainers.slice(0, 6);
  const remainder = Math.max(count - shown.length, 0);

  if (shown.length === 0) {
    return <p style={{ fontSize: "12px", color: "var(--cm-text-muted)", margin: 0 }}>No contributors listed yet.</p>;
  }

  return (
    <div style={{ display: "flex", alignItems: "center" }}>
      {shown.map((m, i) => {
        const label = m.user?.username ?? m.user?.name ?? "?";
        const avatarStyle = {
          width: "30px",
          height: "30px",
          borderRadius: "50%",
          background: "var(--cm-sidebar)",
          color: "var(--cm-lime)",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          fontSize: "11px",
          fontWeight: 600,
          border: "2px solid var(--cm-bg)",
          marginLeft: i === 0 ? 0 : "-8px",
        };

        if (m.user?.username) {
          return (
            <Link key={m.user.id ?? i} href={`/users/${m.user.username}`} title={label} style={avatarStyle}>
              {label[0]?.toUpperCase()}
            </Link>
          );
        }

        return (
          <span key={m.user?.id ?? i} title={label} style={avatarStyle}>
            {label[0]?.toUpperCase()}
          </span>
        );
      })}
      {remainder > 0 && (
        <span
          style={{
            width: "30px",
            height: "30px",
            borderRadius: "50%",
            background: "var(--cm-surface-alt)",
            color: "var(--cm-text-secondary)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            fontSize: "10px",
            fontWeight: 600,
            border: "2px solid var(--cm-bg)",
            marginLeft: "-8px",
          }}
        >
          +{remainder}
        </span>
      )}
    </div>
  );
}

function IssuesTab({ project }) {
  const issues = project.featuredIssues || [];

  if (issues.length === 0) {
    return <StatusPanel text="No featured issues on this project yet." />;
  }

  return (
    <div className="cm-glass" style={{ borderRadius: "24px", padding: "8px" }}>
      {issues.map((issue, index) => (
        <Link
          key={issue.id ?? index}
          href={`/issues/${issue.id}`}
          style={{
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            padding: "14px 16px",
            borderBottom: index < issues.length - 1 ? "0.5px solid var(--cm-border)" : "none",
            fontSize: "13px",
            color: "var(--cm-text-primary)",
          }}
        >
          {issue.title ?? `Issue #${issue.id}`}
          <ArrowUpRight size={14} strokeWidth={2} color="var(--cm-text-muted)" aria-hidden="true" />
        </Link>
      ))}
    </div>
  );
}

function ContributorsTab({ project, isMaintainer, onMaintainerAdded, onMaintainerRemoved }) {
  const maintainers = project.maintainers || [];

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
      <div className="cm-glass" style={{ borderRadius: "24px", padding: "20px" }}>
        <h2 style={{ fontSize: "14px", fontWeight: 700, margin: "0 0 14px", color: "var(--cm-text-primary)" }}>
          Maintainers
        </h2>

        {maintainers.length > 0 ? (
          <ul style={{ listStyle: "none", margin: 0, padding: 0, display: "flex", flexDirection: "column", gap: "8px" }}>
            {maintainers.map((maintainer, index) => (
              <li
                key={maintainer.user?.id ?? index}
                style={{
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "space-between",
                  padding: "10px 14px",
                  borderRadius: "12px",
                  background: "var(--cm-surface-alt)",
                  fontSize: "13px",
                }}
              >
                {maintainer.user?.username ? (
                  <Link href={`/users/${maintainer.user.username}`} style={{ color: "var(--cm-text-primary)", fontWeight: 600 }}>
                    {maintainer.user.username}
                  </Link>
                ) : (
                  <span style={{ color: "var(--cm-text-primary)" }}>
                    {maintainer.user?.login ?? maintainer.user?.name ?? "Unknown"}
                  </span>
                )}
                {maintainer.role && (
                  <span
                    style={{
                      fontSize: "11px",
                      fontWeight: 600,
                      color: "var(--cm-lime-text)",
                      background: "var(--cm-lime-soft)",
                      borderRadius: "999px",
                      padding: "3px 10px",
                      textTransform: "capitalize",
                    }}
                  >
                    {maintainer.role}
                  </span>
                )}
              </li>
            ))}
          </ul>
        ) : (
          <p style={{ fontSize: "13px", color: "var(--cm-text-muted)", margin: 0 }}>No maintainers listed.</p>
        )}
      </div>

      <MaintainersPanel
        project={project}
        isMaintainer={isMaintainer}
        onMaintainerAdded={onMaintainerAdded}
        onMaintainerRemoved={onMaintainerRemoved}
      />
    </div>
  );
}

function DiscussionsTab({ project }) {
  const comments = project.recentComments || [];

  if (comments.length === 0) {
    return <StatusPanel text="No discussion yet on this project." />;
  }

  return (
    <div className="cm-glass" style={{ borderRadius: "24px", padding: "20px", display: "flex", flexDirection: "column", gap: "10px" }}>
      {comments.map((comment, index) => (
        <p key={comment.id ?? index} style={{ fontSize: "13px", lineHeight: 1.6, color: "var(--cm-text-secondary)", margin: 0, padding: "10px 14px", borderRadius: "12px", background: "var(--cm-surface-alt)" }}>
          {comment.body ?? comment.content ?? String(comment)}
        </p>
      ))}
    </div>
  );
}

function ComingSoonTab({ label }) {
  return <StatusPanel text={`Pull request tracking isn't wired up yet — this tab is a placeholder for ${label}.`} />;
}

function StatusPanel({ title, text }) {
  return (
    <div className="cm-glass" style={{ borderRadius: "24px", padding: "48px 24px", textAlign: "center" }}>
      {title && (
        <p style={{ fontSize: "15px", fontWeight: 600, margin: "0 0 6px", color: "var(--cm-text-primary)" }}>{title}</p>
      )}
      <p style={{ fontSize: "13px", color: "var(--cm-text-secondary)", margin: 0 }}>{text}</p>
    </div>
  );
}
