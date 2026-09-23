"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Check, X, ShieldCheck, ArrowUpRight } from "lucide-react";
import { listPendingProjects, moderateProject } from "../../../lib/api";
import { useAuth } from "../../context/AuthContext";

const PAGE_SIZE = 20;

export default function AdminProjectsPage() {
  const router = useRouter();
  const { user, loading: authLoading } = useAuth();
  const isAdmin = Boolean(user?.isSiteAdmin);

  const [pending, setPending] = useState([]);
  const [meta, setMeta] = useState({ page: 0, size: PAGE_SIZE, total: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [reasons, setReasons] = useState({});
  const [busyId, setBusyId] = useState(null);

  // Non-admins (and the not-yet-resolved auth state) never trigger the
  // pending-projects fetch at all, this view is absent, not disabled, for
  // anyone who isn't a confirmed site admin. Redirecting away rather than
  // rendering a "you don't have access" message here means a non-admin
  // never sees so much as the shape of this page.
  useEffect(() => {
    if (!authLoading && !isAdmin) {
      router.replace("/");
    }
  }, [authLoading, isAdmin, router]);

  useEffect(() => {
    if (!isAdmin) return;

    let cancelled = false;

    async function load() {
      setLoading(true);
      setError("");
      try {
        const result = await listPendingProjects({ page: meta.page, size: PAGE_SIZE });
        if (cancelled) return;
        setPending(result.items || []);
        setMeta(result.meta || { page: 0, size: PAGE_SIZE, total: 0 });
      } catch (err) {
        if (cancelled) return;
        setError(err.message || "Failed to load the moderation queue.");
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, [isAdmin, meta.page]);

  async function handleDecide(projectId, decision) {
    setBusyId(projectId);
    setError("");

    try {
      await moderateProject(projectId, decision, decision === "reject" ? reasons[projectId] : undefined);
      // Approved/rejected projects leave the pending queue immediately,
      // approving flips listingStatus to published server-side, which is
      // exactly what makes it show up on /projects right away; this page
      // just needs to stop showing it here.
      setPending((current) => current.filter((p) => p.id !== projectId));
      setMeta((current) => ({ ...current, total: Math.max(current.total - 1, 0) }));
    } catch (err) {
      setError(err.message || "That decision didn't go through. Please try again.");
    } finally {
      setBusyId(null);
    }
  }

  // While auth is resolving, or once it's resolved to "not an admin", render
  // nothing resembling this page, no queue, no counts, no layout hints.
  if (authLoading || !isAdmin) {
    return null;
  }

  return (
    <div style={{ padding: "8px 4px 40px", maxWidth: "820px", margin: "0 auto" }}>
      <header style={{ display: "flex", alignItems: "center", gap: "10px", marginBottom: "20px" }}>
        <span
          aria-hidden="true"
          style={{
            width: "38px",
            height: "38px",
            borderRadius: "12px",
            background: "var(--cm-sidebar)",
            color: "var(--cm-lime)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            flexShrink: 0,
          }}
        >
          <ShieldCheck size={18} strokeWidth={2} />
        </span>
        <div>
          <h1 style={{ fontSize: "20px", fontWeight: 700, margin: 0, color: "var(--cm-text-primary)" }}>
            Moderation queue
          </h1>
          <p style={{ fontSize: "13px", color: "var(--cm-text-secondary)", margin: "2px 0 0" }}>
            {meta.total} project{meta.total === 1 ? "" : "s"} awaiting review, site admins only.
          </p>
        </div>
      </header>

      {error && (
        <p
          role="alert"
          className="cm-glass"
          style={{ borderRadius: "14px", padding: "12px 16px", fontSize: "13px", color: "var(--cm-orange-text)", marginBottom: "16px" }}
        >
          {error}
        </p>
      )}

      {loading ? (
        <StatusPanel text="Loading the queue…" />
      ) : pending.length === 0 ? (
        <StatusPanel title="Nothing pending" text="Every submission has been reviewed." />
      ) : (
        <div style={{ display: "flex", flexDirection: "column", gap: "14px" }}>
          {pending.map((project) => (
            <PendingProjectCard
              key={project.id}
              project={project}
              busy={busyId === project.id}
              reason={reasons[project.id] || ""}
              onReasonChange={(value) => setReasons((current) => ({ ...current, [project.id]: value }))}
              onDecide={(decision) => handleDecide(project.id, decision)}
            />
          ))}
        </div>
      )}
    </div>
  );
}

function PendingProjectCard({ project, busy, reason, onReasonChange, onDecide }) {
  return (
    <div className="cm-glass" style={{ borderRadius: "20px", padding: "18px 20px" }}>
      <div style={{ display: "flex", alignItems: "flex-start", justifyContent: "space-between", gap: "12px", flexWrap: "wrap" }}>
        <div style={{ minWidth: 0 }}>
          <div style={{ display: "flex", alignItems: "center", gap: "8px", flexWrap: "wrap" }}>
            <h2 style={{ fontSize: "15px", fontWeight: 700, margin: 0, color: "var(--cm-text-primary)" }}>
              {project.name}
            </h2>
            {project.owner && (
              <span style={{ fontSize: "12px", color: "var(--cm-text-muted)" }}>by {project.owner}</span>
            )}
          </div>
          {project.description && (
            <p style={{ fontSize: "13px", color: "var(--cm-text-secondary)", margin: "6px 0 0", lineHeight: 1.5 }}>
              {project.description}
            </p>
          )}
        </div>

        {project.githubUrl && (
          <a
            href={project.githubUrl}
            target="_blank"
            rel="noopener noreferrer"
            style={{ display: "inline-flex", alignItems: "center", gap: "4px", fontSize: "12.5px", fontWeight: 600, color: "var(--cm-text-primary)", flexShrink: 0 }}
          >
            View on GitHub
            <ArrowUpRight size={13} strokeWidth={2} aria-hidden="true" />
          </a>
        )}
      </div>

      <div style={{ display: "flex", flexWrap: "wrap", gap: "6px", margin: "12px 0" }}>
        {project.primaryLanguage && <Tag>{project.primaryLanguage}</Tag>}
        {project.category && <Tag>{project.category}</Tag>}
        {project.connection && <Tag>{project.connection.replace(/_/g, " ")}</Tag>}
        {project.createdAt && <Tag>submitted {new Date(project.createdAt).toLocaleDateString()}</Tag>}
      </div>

      <textarea
        value={reason}
        onChange={(event) => onReasonChange(event.target.value)}
        placeholder="Reason for rejecting (optional, shown to the submitter)"
        rows={2}
        maxLength={1000}
        disabled={busy}
        style={{
          width: "100%",
          borderRadius: "12px",
          border: "0.5px solid var(--cm-border)",
          background: "var(--cm-surface)",
          color: "var(--cm-text-primary)",
          fontSize: "12.5px",
          padding: "10px 12px",
          marginBottom: "12px",
          resize: "vertical",
        }}
      />

      <div style={{ display: "flex", gap: "8px" }}>
        <button
          type="button"
          onClick={() => onDecide("approve")}
          disabled={busy}
          style={{
            display: "inline-flex",
            alignItems: "center",
            gap: "6px",
            borderRadius: "999px",
            padding: "9px 18px",
            fontSize: "13px",
            fontWeight: 700,
            border: "none",
            cursor: busy ? "not-allowed" : "pointer",
            opacity: busy ? 0.6 : 1,
            background: "var(--cm-lime)",
            color: "#0A0A0A",
          }}
        >
          <Check size={14} strokeWidth={2.4} aria-hidden="true" />
          {busy ? "Working…" : "Approve"}
        </button>

        <button
          type="button"
          onClick={() => onDecide("reject")}
          disabled={busy}
          style={{
            display: "inline-flex",
            alignItems: "center",
            gap: "6px",
            borderRadius: "999px",
            padding: "9px 18px",
            fontSize: "13px",
            fontWeight: 700,
            border: "0.5px solid var(--cm-border)",
            cursor: busy ? "not-allowed" : "pointer",
            opacity: busy ? 0.6 : 1,
            background: "var(--cm-surface)",
            color: "var(--cm-orange-text)",
          }}
        >
          <X size={14} strokeWidth={2.4} aria-hidden="true" />
          Reject
        </button>
      </div>
    </div>
  );
}

function Tag({ children }) {
  return (
    <span
      style={{
        fontSize: "11px",
        color: "var(--cm-text-secondary)",
        background: "var(--cm-surface-alt)",
        padding: "3px 10px",
        borderRadius: "999px",
        textTransform: "capitalize",
      }}
    >
      {children}
    </span>
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
