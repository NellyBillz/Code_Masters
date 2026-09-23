"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { ShieldAlert, Flag } from "lucide-react";
import { listReports } from "../../../lib/api";
import { useAuth } from "../../context/AuthContext";

const PAGE_SIZE = 20;

const TABS = [
  { value: "open", label: "Open" },
  { value: "resolved", label: "Resolved" },
  { value: "dismissed", label: "Dismissed" },
];

// What resolving this report would involve, per its current status — text
// only, not buttons. Actually resolving/dismissing (PATCH /admin/reports/{id})
// is FE-GAP-11's job, this page is read-only.
const AVAILABLE_ACTIONS = {
  open: "Resolve or dismiss",
  resolved: "None, already resolved",
  dismissed: "None, already dismissed",
};

const STATUS_STYLES = {
  open: { bg: "var(--cm-orange-soft)", text: "var(--cm-orange-text)" },
  resolved: { bg: "var(--cm-lime-soft)", text: "var(--cm-lime-text)" },
  dismissed: { bg: "var(--cm-surface-alt)", text: "var(--cm-text-secondary)" },
};

export default function AdminReportsPage() {
  const router = useRouter();
  const { user, loading: authLoading } = useAuth();
  const isAdmin = Boolean(user?.isSiteAdmin);

  const [status, setStatus] = useState("open");
  const [reports, setReports] = useState([]);
  const [meta, setMeta] = useState({ page: 0, size: PAGE_SIZE, total: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  // Same pattern as /admin/projects: a non-admin (or auth still resolving)
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
        const result = await listReports({ status, page: 0, size: PAGE_SIZE });
        if (cancelled) return;
        setReports(result.items || []);
        setMeta(result.meta || { page: 0, size: PAGE_SIZE, total: 0 });
      } catch (err) {
        if (cancelled) return;
        setError(err.message || "Failed to load reports.");
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    load();
    return () => {
      cancelled = true;
    };
  }, [isAdmin, status]);

  if (authLoading || !isAdmin) {
    return null;
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
          <ShieldAlert size={18} strokeWidth={2} />
        </span>
        <div>
          <h1 style={{ fontSize: "20px", fontWeight: 700, margin: 0, color: "var(--cm-text-primary)" }}>
            Abuse reports
          </h1>
          <p style={{ fontSize: "13px", color: "var(--cm-text-secondary)", margin: "2px 0 0" }}>
            {meta.total} {status} report{meta.total === 1 ? "" : "s"}, site admins only.
          </p>
        </div>
      </header>

      <div style={{ display: "flex", gap: "8px", marginBottom: "20px" }}>
        {TABS.map((tab) => {
          const active = status === tab.value;
          return (
            <button
              key={tab.value}
              type="button"
              onClick={() => setStatus(tab.value)}
              style={{
                padding: "7px 16px",
                borderRadius: "999px",
                fontSize: "12.5px",
                fontWeight: 600,
                cursor: "pointer",
                background: active ? "var(--cm-lime)" : "transparent",
                color: active ? "#0A0A0A" : "var(--cm-text-secondary)",
                border: active ? "none" : "0.5px solid var(--cm-border)",
              }}
            >
              {tab.label}
            </button>
          );
        })}
      </div>

      {loading ? (
        <StatusPanel text="Loading reports…" />
      ) : error ? (
        <StatusPanel title="Something went wrong" text={error} />
      ) : reports.length === 0 ? (
        <StatusPanel title="Nothing here" text={`No ${status} reports right now.`} />
      ) : (
        <div style={{ display: "flex", flexDirection: "column", gap: "12px" }}>
          {reports.map((report) => (
            <ReportRow key={report.id} report={report} />
          ))}
        </div>
      )}
    </div>
  );
}

function ReportRow({ report }) {
  const statusStyle = STATUS_STYLES[report.status] || STATUS_STYLES.open;

  return (
    <div className="cm-glass" style={{ borderRadius: "20px", padding: "18px 20px" }}>
      <div style={{ display: "flex", alignItems: "flex-start", justifyContent: "space-between", gap: "12px", flexWrap: "wrap" }}>
        <div style={{ minWidth: 0 }}>
          <div style={{ display: "flex", alignItems: "center", gap: "8px", flexWrap: "wrap", marginBottom: "8px" }}>
            <span style={{ fontSize: "11px", color: "var(--cm-text-muted)", fontWeight: 600 }}>
              Report #{report.id}
            </span>
            <span
              style={{
                display: "inline-flex",
                alignItems: "center",
                gap: "4px",
                fontSize: "11px",
                fontWeight: 600,
                textTransform: "capitalize",
                padding: "3px 9px",
                borderRadius: "999px",
                background: "var(--cm-surface-alt)",
                color: "var(--cm-text-secondary)",
              }}
            >
              <Flag size={11} strokeWidth={2} aria-hidden="true" />
              {report.targetType}
            </span>
            <span
              style={{
                fontSize: "11px",
                fontWeight: 600,
                textTransform: "capitalize",
                padding: "3px 9px",
                borderRadius: "999px",
                background: statusStyle.bg,
                color: statusStyle.text,
              }}
            >
              {report.status}
            </span>
          </div>

          <p style={{ fontSize: "13px", fontWeight: 600, margin: "0 0 2px", color: "var(--cm-text-primary)" }}>
            Reported content:{" "}
            {report.targetType === "project" ? (
              <Link href={`/projects/${report.targetId}`} style={{ fontWeight: 700 }}>
                Project #{report.targetId}
              </Link>
            ) : (
              <span>Comment #{report.targetId}</span>
            )}
          </p>

          <p style={{ fontSize: "13px", color: "var(--cm-text-secondary)", margin: "6px 0 0", lineHeight: 1.5 }}>
            {report.reason}
          </p>

          <p style={{ fontSize: "12px", color: "var(--cm-text-muted)", margin: "10px 0 0" }}>
            Reported by{" "}
            {report.reporter?.username ? (
              <Link href={`/users/${report.reporter.username}`} style={{ fontWeight: 600 }}>
                {report.reporter.displayName || report.reporter.username}
              </Link>
            ) : (
              "a user"
            )}
            {report.createdAt ? ` · ${new Date(report.createdAt).toLocaleDateString()}` : ""}
          </p>

          {report.resolution && (
            <p style={{ fontSize: "12px", color: "var(--cm-text-secondary)", margin: "6px 0 0" }}>
              Resolution: {report.resolution}
            </p>
          )}
        </div>

        <div style={{ textAlign: "right", flexShrink: 0 }}>
          <p style={{ fontSize: "10.5px", fontWeight: 600, color: "var(--cm-text-muted)", margin: "0 0 2px", textTransform: "uppercase", letterSpacing: "0.05em" }}>
            Available actions
          </p>
          <p style={{ fontSize: "12px", color: "var(--cm-text-secondary)", margin: 0 }}>
            {AVAILABLE_ACTIONS[report.status] || "None"}
          </p>
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
