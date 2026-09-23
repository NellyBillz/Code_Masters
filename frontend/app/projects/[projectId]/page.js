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
  Clock,
  XCircle,
  GitPullRequest,
  GitMerge,
  Flag,
} from "lucide-react";
import { getProject, getProjectIssues, getIssueClaims } from "../../../lib/api";
import { useAuth } from "../../context/AuthContext";
import MaintainersPanel from "../../components/MaintainersPanel";
import SyncStatusBanner from "../../components/SyncStatusBanner";
import EditProjectPanel from "../../components/EditProjectPanel";
import ReportProjectPanel from "../../components/ReportProjectPanel";
import ProjectComments from "../../components/ProjectComments";

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
  const [reporting, setReporting] = useState(false);

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

      {project.listingStatus !== "published" && (
        <div
          className="cm-glass"
          style={{
            display: "flex",
            alignItems: "center",
            gap: "10px",
            borderRadius: "16px",
            padding: "12px 18px",
            marginBottom: "20px",
            background: project.listingStatus === "rejected" ? "var(--cm-orange-soft)" : "var(--cm-surface-alt)",
            color: project.listingStatus === "rejected" ? "var(--cm-orange-text)" : "var(--cm-text-secondary)",
            fontSize: "13px",
            fontWeight: 600,
          }}
        >
          {project.listingStatus === "rejected" ? (
            <XCircle size={16} strokeWidth={2} aria-hidden="true" />
          ) : (
            <Clock size={16} strokeWidth={2} aria-hidden="true" />
          )}
          {project.listingStatus === "rejected"
            ? "This submission was rejected by a site admin and isn't visible to anyone else on Code Masters."
            : "Pending review, only you and this project's maintainers can see this. It won't appear in search, the projects list, or platform stats until a site admin approves it."}
        </div>
      )}

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

            {currentUser && !isMaintainer && !reporting && (
              <button
                type="button"
                onClick={() => setReporting(true)}
                className="cm-glass"
                aria-label="Report this project"
                title="Report this project"
                style={{
                  display: "inline-flex",
                  alignItems: "center",
                  justifyContent: "center",
                  width: "34px",
                  height: "34px",
                  borderRadius: "999px",
                  color: "var(--cm-text-muted)",
                  cursor: "pointer",
                }}
              >
                <Flag size={14} strokeWidth={2} aria-hidden="true" />
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

      {reporting && (
        <ReportProjectPanel
          projectId={project.id}
          onCancel={() => setReporting(false)}
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
      {activeTab === "Pull requests" && <PullRequestsTab project={project} />}
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
  // Tests/CI, which have no backing signal anywhere in the API, those are
  // explicitly "unknown", not silently marked done. hasContributingGuide/
  // hasCodeOfConduct come from GitHub's community-profile endpoint via sync
  // (confirmed present on GET /projects/{id}, this file previously ignored
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
  // Readiness is a percentage of known facts only, an "unknown" isn't a
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
            Based on known signals only, license, contributing guide, code of conduct, active maintainers, and open beginner issues. README and test coverage aren&rsquo;t tracked yet, so they&rsquo;re excluded rather than assumed.
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

const ISSUE_FILTER_SELECT_STYLE = {
  borderRadius: "10px",
  border: "0.5px solid var(--cm-border)",
  background: "var(--cm-surface)",
  color: "var(--cm-text-primary)",
  fontSize: "12.5px",
  padding: "8px 12px",
  outline: "none",
};

function IssuesTab({ project }) {
  const [filters, setFilters] = useState({ difficulty: "", status: "", label: "" });
  const [issues, setIssues] = useState(null);
  const [error, setError] = useState("");

  function updateFilter(key, value) {
    setFilters((current) => ({ ...current, [key]: value }));
  }

  useEffect(() => {
    let cancelled = false;

    // eslint-disable-next-line react-hooks/set-state-in-effect -- fetch-on-mount pattern; no derived-state alternative for reading server data
    setIssues(null);
    setError("");

    getProjectIssues(project.id, {
      size: 50,
      difficulty: filters.difficulty || undefined,
      status: filters.status || undefined,
      label: filters.label || undefined,
    })
      .then((result) => {
        if (!cancelled) setIssues(result.items || []);
      })
      .catch((err) => {
        if (!cancelled) setError(err.message || "Failed to load issues.");
      });

    return () => {
      cancelled = true;
    };
  }, [project.id, filters.difficulty, filters.status, filters.label]);

  const hasActiveFilters = Boolean(filters.difficulty || filters.status || filters.label);

  const filterBar = (
    <div style={{ display: "flex", gap: "10px", flexWrap: "wrap", marginBottom: "14px" }}>
      <select
        aria-label="Filter by difficulty"
        value={filters.difficulty}
        onChange={(e) => updateFilter("difficulty", e.target.value)}
        style={ISSUE_FILTER_SELECT_STYLE}
      >
        <option value="">All difficulties</option>
        <option value="beginner">Beginner</option>
        <option value="intermediate">Intermediate</option>
        <option value="advanced">Advanced</option>
        <option value="unknown">Unknown</option>
      </select>

      <select
        aria-label="Filter by status"
        value={filters.status}
        onChange={(e) => updateFilter("status", e.target.value)}
        style={ISSUE_FILTER_SELECT_STYLE}
      >
        <option value="">All statuses</option>
        <option value="open">Open</option>
        <option value="closed">Closed</option>
        <option value="claimed">Claimed</option>
      </select>

      <input
        type="text"
        aria-label="Filter by label"
        placeholder="Filter by label…"
        value={filters.label}
        onChange={(e) => updateFilter("label", e.target.value)}
        style={{ ...ISSUE_FILTER_SELECT_STYLE, flex: "1 1 160px" }}
      />
    </div>
  );

  if (error) {
    return (
      <div>
        {filterBar}
        <StatusPanel title="Something went wrong" text={error} />
      </div>
    );
  }

  if (issues === null) {
    return (
      <div>
        {filterBar}
        <StatusPanel text="Loading issues…" />
      </div>
    );
  }

  if (issues.length === 0) {
    return (
      <div>
        {filterBar}
        <StatusPanel
          text={hasActiveFilters ? "No issues match these filters." : "No issues found for this project yet."}
        />
      </div>
    );
  }

  return (
    <div>
      {filterBar}
      <div className="cm-glass" style={{ borderRadius: "24px", padding: "8px" }}>
      {issues.map((issue, index) => (
        <Link
          key={issue.id ?? index}
          href={`/issues/${issue.id}`}
          style={{
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            gap: "12px",
            padding: "14px 16px",
            borderBottom: index < issues.length - 1 ? "0.5px solid var(--cm-border)" : "none",
            fontSize: "13px",
            color: "var(--cm-text-primary)",
          }}
        >
          <span style={{ display: "flex", alignItems: "center", gap: "8px", minWidth: 0 }}>
            <span
              style={{
                fontSize: "10.5px",
                fontWeight: 600,
                textTransform: "capitalize",
                padding: "2px 8px",
                borderRadius: "999px",
                background: issue.status === "open" ? "var(--cm-lime-soft)" : "var(--cm-surface-alt)",
                color: issue.status === "open" ? "var(--cm-lime-text)" : "var(--cm-text-secondary)",
                flexShrink: 0,
              }}
            >
              {issue.status || "unknown"}
            </span>
            {issue.title ?? `Issue #${issue.id}`}
          </span>
          <ArrowUpRight size={14} strokeWidth={2} color="var(--cm-text-muted)" aria-hidden="true" style={{ flexShrink: 0 }} />
        </Link>
      ))}
      </div>
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
  return <ProjectComments projectId={project.id} />;
}

// There's no dedicated pull-request-listing endpoint on this backend — PR
// info (pullRequestUrl, pullRequestState) lives on individual Claims
// (API-03.3/GH-03.2), one issue at a time via GET /issues/{issueId}/claims.
// So this tab gets the project's issues (already-loaded pattern, same as
// IssuesTab above), fetches each issue's claims, and keeps only the ones
// with a pull request actually attached. If one issue's claims fail to
// load, that issue just contributes no PRs, it doesn't fail the whole tab,
// same "bonus section degrades gracefully" approach used for maintainer
// activity on /profile.
const PR_STATE_STYLES = {
  open: { label: "Open", bg: "var(--cm-lime-soft)", text: "var(--cm-lime-text)", Icon: GitPullRequest },
  merged: { label: "Merged", bg: "var(--cm-orange-soft)", text: "var(--cm-orange-text)", Icon: GitMerge },
  closed_unmerged: { label: "Closed", bg: "var(--cm-surface-alt)", text: "var(--cm-text-secondary)", Icon: XCircle },
};

function PullRequestsTab({ project }) {
  const [pullRequests, setPullRequests] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;

    // eslint-disable-next-line react-hooks/set-state-in-effect -- fetch-on-mount pattern; no derived-state alternative for reading server data
    setPullRequests(null);
    setError("");

    async function load() {
      let issues;
      try {
        const result = await getProjectIssues(project.id, { size: 50 });
        issues = result.items || [];
      } catch (err) {
        if (!cancelled) setError(err.message || "Failed to load pull requests.");
        return;
      }

      const claimsPerIssue = await Promise.allSettled(
        issues.map((issue) => getIssueClaims(issue.id).then((claims) => ({ issue, claims })))
      );

      if (cancelled) return;

      const prs = [];
      for (const settled of claimsPerIssue) {
        if (settled.status !== "fulfilled") continue;
        const { issue, claims } = settled.value;
        for (const claim of claims) {
          if (claim.pullRequestUrl) prs.push({ issue, claim });
        }
      }

      prs.sort((a, b) => new Date(b.claim.updatedAt || 0) - new Date(a.claim.updatedAt || 0));
      setPullRequests(prs);
    }

    load();
    return () => {
      cancelled = true;
    };
  }, [project.id]);

  if (error) return <StatusPanel title="Something went wrong" text={error} />;
  if (pullRequests === null) return <StatusPanel text="Loading pull requests…" />;
  if (pullRequests.length === 0) {
    return <StatusPanel text="No pull requests linked to this project's issues yet." />;
  }

  return (
    <div className="cm-glass" style={{ borderRadius: "24px", padding: "8px" }}>
      {pullRequests.map(({ issue, claim }, index) => {
        const style = PR_STATE_STYLES[claim.pullRequestState] || PR_STATE_STYLES.open;
        const StateIcon = style.Icon;

        return (
          <a
            key={claim.id ?? index}
            href={claim.pullRequestUrl}
            target="_blank"
            rel="noopener noreferrer"
            style={{
              display: "flex",
              alignItems: "center",
              justifyContent: "space-between",
              gap: "12px",
              padding: "14px 16px",
              borderBottom: index < pullRequests.length - 1 ? "0.5px solid var(--cm-border)" : "none",
              fontSize: "13px",
              color: "var(--cm-text-primary)",
            }}
          >
            <span style={{ display: "flex", alignItems: "center", gap: "8px", minWidth: 0 }}>
              <span
                style={{
                  display: "inline-flex",
                  alignItems: "center",
                  gap: "4px",
                  fontSize: "10.5px",
                  fontWeight: 600,
                  padding: "2px 8px",
                  borderRadius: "999px",
                  background: style.bg,
                  color: style.text,
                  flexShrink: 0,
                }}
              >
                <StateIcon size={11} strokeWidth={2} aria-hidden="true" />
                {style.label}
              </span>
              <span style={{ overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                {issue.title ?? `Issue #${issue.id}`}
                {claim.user?.username && (
                  <span style={{ color: "var(--cm-text-muted)" }}> · {claim.user.username}</span>
                )}
              </span>
            </span>
            <ArrowUpRight size={14} strokeWidth={2} color="var(--cm-text-muted)" aria-hidden="true" style={{ flexShrink: 0 }} />
          </a>
        );
      })}
    </div>
  );
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