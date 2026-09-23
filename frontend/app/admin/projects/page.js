"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { ShieldCheck, ArrowUpRight } from "lucide-react";
import { listPendingProjects } from "../../../lib/api";
import { useAuth } from "../../context/AuthContext";
import AdminProjectActions from "../../components/AdminProjectActions";

const PAGE_SIZE = 20;

const CONNECTION_LABELS = {
  south_african: "South African",
  community_verified: "Community verified",
};

export default function AdminProjectsPage() {
  const router = useRouter();
  const { user, loading: authLoading } = useAuth();
  const isAdmin = Boolean(user?.isSiteAdmin);

  const [projects, setProjects] = useState([]);
  const [meta, setMeta] = useState({ page: 0, size: PAGE_SIZE, total: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  // Same pattern as /admin/reports: a non-admin (or auth still resolving)
  // never triggers the fetch and never sees this page's shape at all, this
  // view is absent, not disabled, for anyone who isn't a confirmed site admin.
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
        const result = await listPendingProjects({ page: 0, size: PAGE_SIZE });
        if (cancelled) return;
        setProjects(result.items || []);
        setMeta(result.meta || { page: 0, size: PAGE_SIZE, total: 0 });
      } catch (err) {
        if (cancelled) return;
        setError(err.message || "Failed to load pending submissions.");
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, [isAdmin]);

  if (authLoading || !isAdmin) {
    return null;
  }

  function handleReviewed(projectId) {
    setProjects((prev) => prev.filter((p) => p.id !== projectId));
    setMeta((prev) => ({ ...prev, total: Math.max(prev.total - 1, 0) }));
  }

  return (
    <div style={{ padding: "8px 4px 40px", maxWidth: "880px", margin: "0 auto" }}>
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
            Pending submissions
          </h1>
          <p style={{ fontSize: "13px", color: "var(--cm-text-secondary)", margin: "2px 0 0" }}>
            {meta.total} project{meta.total === 1 ? "" : "s"} awaiting review, site admins only.
          </p>
        </div>
      </header>

      {loading ? (
        <StatusPanel text="Loading pending submissions…" />
      ) : error ? (
        <StatusPanel title="Something went wrong" text={error} />
      ) : projects.length === 0 ? (
        <StatusPanel title="Nothing here" text="No projects awaiting review right now." />
      ) : (
        <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
          {projects.map((project) => (
            <ProjectRow
              key={project.id}
              project={project}
              onUpdated={(updated) => handleReviewed(updated.id ?? project.id)}
            />
          ))}
        </div>
      )}
    </div>
  );
}

function ProjectRow({ project, onUpdated }) {
  const submitter = project.maintainers?.[0]?.user;

  return (
    <div className="cm-glass" style={{ borderRadius: "20px", padding: "18px 20px" }}>
      <div style={{ display: "flex", alignItems: "flex-start", justifyContent: "space-between", gap: "12px", flexWrap: "wrap" }}>
        <div style={{ minWidth: 0 }}>
          <div style={{ display: "flex", alignItems: "center", gap: "8px", flexWrap: "wrap", marginBottom: "8px" }}>
            <a
              href={project.githubUrl}
              target="_blank"
              rel="noreferrer"
              style={{ display: "inline-flex", alignItems: "center", gap: "4px", fontSize: "14px", fontWeight: 700, color: "var(--cm-text-primary)" }}
            >
              {project.name}
              <ArrowUpRight size={12} strokeWidth={2} aria-hidden="true" color="var(--cm-text-muted)" />
            </a>
            {project.connection && (
              <span
                style={{
                  fontSize: "11px",
                  fontWeight: 600,
                  padding: "3px 9px",
                  borderRadius: "999px",
                  background: "var(--cm-surface-alt)",
                  color: "var(--cm-text-secondary)",
                }}
              >
                {CONNECTION_LABELS[project.connection] || project.connection}
              </span>
            )}
            {project.category && (
              <span
                style={{
                  fontSize: "11px",
                  fontWeight: 600,
                  padding: "3px 9px",
                  borderRadius: "999px",
                  background: "var(--cm-surface-alt)",
                  color: "var(--cm-text-secondary)",
                }}
              >
                {project.category}
              </span>
            )}
          </div>

          <p style={{ fontSize: "13px", color: "var(--cm-text-secondary)", margin: "0 0 6px", lineHeight: 1.5, maxWidth: "560px" }}>
            {project.description || "No description provided."}
          </p>

          {project.tags?.length > 0 && (
            <p style={{ fontSize: "11.5px", color: "var(--cm-text-muted)", margin: "0 0 6px" }}>
              {project.tags.join(" · ")}
            </p>
          )}

          <p style={{ fontSize: "12px", color: "var(--cm-text-muted)", margin: 0 }}>
            Submitted by{" "}
            {submitter?.username ? (
              <span style={{ fontWeight: 600, color: "var(--cm-text-secondary)" }}>{submitter.displayName || submitter.username}</span>
            ) : (
              "a user"
            )}
            {project.createdAt ? ` · ${new Date(project.createdAt).toLocaleDateString()}` : ""}
          </p>
        </div>

        <div style={{ textAlign: "right", flexShrink: 0 }}>
          <AdminProjectActions project={project} onUpdated={onUpdated} />
        </div>
      </div>
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
