"use client";

import { useEffect, useState } from "react";
import {
  MapPin,
  ArrowUpRight,
  ShieldCheck,
  AlertTriangle,
} from "lucide-react";
import Link from "next/link";
import { getCurrentUser, getMaintainerActivity, ApiError } from "../../lib/api";
import GitHubStats from "../components/GitHubStats";
import StatTile from "../components/StatTile";
import RecentContributions from "../components/RecentContributions";

export default function ProfilePage() {
  const [user, setUser] = useState(null);
  const [maintainedProjects, setMaintainedProjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;

    async function loadProfile() {
      setLoading(true);
      setError(null);
      try {
        const result = await getCurrentUser();
        if (cancelled) return;
        setUser(result);

        // Maintainer activity is a bonus section, not core to the profile —
        // if it fails (e.g. the caller maintains nothing yet), the page
        // still shows the rest of the profile rather than erroring out.
        try {
          const activity = await getMaintainerActivity();
          if (!cancelled) setMaintainedProjects(activity.projects || []);
        } catch {
          if (!cancelled) setMaintainedProjects([]);
        }
      } catch (err) {
        if (cancelled) return;
        if (err instanceof ApiError && err.status === 401) {
          setError("unauthenticated");
        } else {
          setError(err.message || "Failed to load your profile.");
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    loadProfile();
    return () => {
      cancelled = true;
    };
  }, []);

  if (loading) return <StatusPanel text="Loading your profile…" />;

  if (error === "unauthenticated") {
    return (
      <StatusPanel title="Sign in to view your profile">
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
  if (!user) return null;

  const initials = (user.displayName || user.username || "?")[0]?.toUpperCase();
  const skills = user.skills || [];

  return (
    <div style={{ padding: "8px 4px 40px", maxWidth: "760px", margin: "0 auto" }}>
      {/* Header */}
      <header className="cm-glass" style={{ borderRadius: "28px", padding: "28px", marginBottom: "20px" }}>
        <div style={{ display: "flex", alignItems: "flex-start", gap: "18px", flexWrap: "wrap" }}>
          {user.avatarUrl ? (
            // eslint-disable-next-line @next/next/no-img-element -- external, size-variable avatar URL
            <img
              src={user.avatarUrl}
              alt=""
              width={72}
              height={72}
              style={{ borderRadius: "50%", flexShrink: 0 }}
            />
          ) : (
            <span
              aria-hidden="true"
              style={{
                width: "72px",
                height: "72px",
                borderRadius: "50%",
                background: "var(--cm-sidebar)",
                color: "var(--cm-lime)",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                fontWeight: 700,
                fontSize: "26px",
                flexShrink: 0,
              }}
            >
              {initials}
            </span>
          )}

          <div style={{ minWidth: 0, flex: 1 }}>
            <div style={{ display: "flex", alignItems: "center", gap: "8px", flexWrap: "wrap" }}>
              <h1 style={{ fontSize: "22px", fontWeight: 700, margin: 0, color: "var(--cm-text-primary)" }}>
                {user.displayName || user.username}
              </h1>
              {user.isSiteAdmin && (
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
                  <ShieldCheck size={12} strokeWidth={2} aria-hidden="true" />
                  Site admin
                </span>
              )}
            </div>

            <a
              href={`https://github.com/${user.username}`}
              target="_blank"
              rel="noreferrer"
              style={{
                display: "inline-flex",
                alignItems: "center",
                gap: "4px",
                fontSize: "13px",
                fontWeight: 600,
                color: "var(--cm-text-secondary)",
                margin: "4px 0 0",
              }}
            >
              @{user.username}
              <ArrowUpRight size={13} strokeWidth={2} aria-hidden="true" />
            </a>

            {user.bio && (
              <p style={{ fontSize: "13px", lineHeight: 1.6, color: "var(--cm-text-secondary)", margin: "10px 0 0", maxWidth: "520px" }}>
                {user.bio}
              </p>
            )}

            {user.location && (
              <p style={{ display: "flex", alignItems: "center", gap: "5px", fontSize: "12.5px", color: "var(--cm-text-muted)", margin: "10px 0 0" }}>
                <MapPin size={13} strokeWidth={1.8} aria-hidden="true" />
                {user.location}
              </p>
            )}

            <GitHubStats username={user.username} />
          </div>
        </div>

        {skills.length > 0 && (
          <div style={{ display: "flex", gap: "6px", flexWrap: "wrap", marginTop: "18px" }}>
            {skills.map((skill) => (
              <span
                key={skill}
                style={{
                  fontSize: "11.5px",
                  fontWeight: 600,
                  color: "var(--cm-text-secondary)",
                  background: "var(--cm-surface-alt)",
                  padding: "4px 11px",
                  borderRadius: "999px",
                }}
              >
                {skill}
              </span>
            ))}
          </div>
        )}
      </header>

      {/* Stats */}
      <div style={{ display: "grid", gridTemplateColumns: "repeat(3, 1fr)", gap: "12px", marginBottom: "20px" }}>
        <StatTile label="Verified contributions" value={user.contributionsCount ?? 0} />
        <StatTile label="Projects maintained" value={maintainedProjects.length} />
        <StatTile label="Reputation" value={user.reputation ?? 0} />
      </div>

      {maintainedProjects.length > 0 && <MaintainedProjects projects={maintainedProjects} />}

      <div style={{ marginTop: maintainedProjects.length > 0 ? "20px" : 0 }}>
        <RecentContributions username={user.username} ownProfile />
      </div>
    </div>
  );
}

function MaintainedProjects({ projects }) {
  return (
    <section className="cm-glass" style={{ borderRadius: "24px", padding: "8px", marginBottom: "20px" }}>
      <h2 style={{ fontSize: "15px", fontWeight: 700, margin: "12px 16px", color: "var(--cm-text-primary)" }}>
        Projects you maintain
      </h2>

      {projects.map((entry, index) => {
        const activeCount = entry.activeClaims?.length ?? 0;
        const needsReviewCount = entry.claimsAwaitingReview?.length ?? 0;

        return (
          <Link
            key={entry.project?.id ?? index}
            href={`/projects/${entry.project?.id}`}
            style={{
              display: "flex",
              alignItems: "center",
              justifyContent: "space-between",
              gap: "12px",
              padding: "14px 16px",
              borderTop: index === 0 ? "none" : "0.5px solid var(--cm-border)",
              flexWrap: "wrap",
            }}
          >
            <div style={{ minWidth: 0 }}>
              <p style={{ fontSize: "13px", fontWeight: 600, margin: 0, color: "var(--cm-text-primary)" }}>
                {entry.project?.name}
              </p>
              <p style={{ fontSize: "12px", color: "var(--cm-text-secondary)", margin: "3px 0 0" }}>
                {activeCount} active claim{activeCount === 1 ? "" : "s"}
              </p>
            </div>

            {needsReviewCount > 0 && (
              <span
                style={{
                  display: "inline-flex",
                  alignItems: "center",
                  gap: "4px",
                  fontSize: "11px",
                  fontWeight: 600,
                  padding: "3px 10px",
                  borderRadius: "999px",
                  background: "var(--cm-orange-soft)",
                  color: "var(--cm-orange-text)",
                  flexShrink: 0,
                }}
              >
                <AlertTriangle size={11} strokeWidth={2} aria-hidden="true" />
                {needsReviewCount} needs review
              </span>
            )}
          </Link>
        );
      })}
    </section>
  );
}

function StatusPanel({ title, text, children }) {
  return (
    <div className="cm-glass" style={{ borderRadius: "24px", padding: "48px 24px", textAlign: "center" }}>
      {title && (
        <p style={{ fontSize: "15px", fontWeight: 600, margin: "0 0 6px", color: "var(--cm-text-primary)" }}>{title}</p>
      )}
      {text && <p style={{ fontSize: "13px", color: "var(--cm-text-secondary)", margin: 0 }}>{text}</p>}
      {children}
    </div>
  );
}
