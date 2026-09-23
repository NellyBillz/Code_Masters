"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Users, Search, ArrowRight } from "lucide-react";
import { getStats, getPublicProfile, listProjects, ApiError } from "../../lib/api";
import StatTile from "../components/StatTile";
import ProjectCard from "../components/ProjectCard";

export default function CommunityPage() {
  const router = useRouter();

  const [stats, setStats] = useState(null);
  const [statsError, setStatsError] = useState(false);

  const [projects, setProjects] = useState([]);
  const [projectsLoading, setProjectsLoading] = useState(true);

  const [username, setUsername] = useState("");
  const [lookupBusy, setLookupBusy] = useState(false);
  const [lookupError, setLookupError] = useState("");

  useEffect(() => {
    let cancelled = false;

    getStats()
      .then((result) => !cancelled && setStats(result))
      .catch(() => !cancelled && setStatsError(true));

    listProjects({ size: 6, sort: "recent" })
      .then((result) => !cancelled && setProjects(result.items || []))
      .catch(() => !cancelled && setProjects([]))
      .finally(() => !cancelled && setProjectsLoading(false));

    return () => {
      cancelled = true;
    };
  }, []);

  async function handleLookup(event) {
    event.preventDefault();
    const trimmed = username.trim();
    if (!trimmed) return;

    setLookupBusy(true);
    setLookupError("");
    try {
      // Confirms the profile actually exists before navigating, rather than
      // sending the user to a 404 for a typo'd or made-up username.
      await getPublicProfile(trimmed);
      router.push(`/users/${encodeURIComponent(trimmed)}`);
    } catch (err) {
      if (err instanceof ApiError && err.status === 404) {
        setLookupError(`No contributor found with the username "${trimmed}".`);
      } else {
        setLookupError(err.message || "Couldn't look that up right now.");
      }
    } finally {
      setLookupBusy(false);
    }
  }

  return (
    <div>
      <section className="cm-glass" style={{ borderRadius: "24px", padding: "36px 28px", marginBottom: "24px" }}>
        <h1 style={{ fontSize: "26px", fontWeight: 700, margin: 0, color: "var(--cm-text-primary)" }}>
          Community
        </h1>
        <p style={{ fontSize: "14px", color: "var(--cm-text-secondary)", margin: "10px 0 22px", maxWidth: "560px" }}>
          Live numbers from across the platform, and a way to find a specific contributor.
        </p>

        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(140px, 1fr))", gap: "12px" }}>
          {stats ? (
            <>
              <StatTile label="Published projects" value={stats.publishedProjects.toLocaleString()} />
              <StatTile label="Accepting contributions" value={stats.activeProjectsAcceptingContributions.toLocaleString()} />
              <StatTile label="Contributors engaged" value={stats.totalContributorsEngaged.toLocaleString()} />
              <StatTile label="Contributions completed" value={stats.totalContributionsCompleted.toLocaleString()} />
            </>
          ) : statsError ? (
            <p style={{ fontSize: "12.5px", color: "var(--cm-text-muted)", gridColumn: "1 / -1" }}>
              Couldn't load platform stats right now.
            </p>
          ) : (
            <p style={{ fontSize: "12.5px", color: "var(--cm-text-muted)", gridColumn: "1 / -1" }}>Loading stats…</p>
          )}
        </div>
      </section>

      <section className="cm-glass" style={{ borderRadius: "20px", padding: "20px", marginBottom: "24px" }}>
        <h2 style={{ fontSize: "15px", fontWeight: 700, margin: "0 0 12px", color: "var(--cm-text-primary)", display: "flex", alignItems: "center", gap: "8px" }}>
          <Users size={16} strokeWidth={2} aria-hidden="true" />
          Find a contributor
        </h2>
        <form onSubmit={handleLookup} style={{ display: "flex", gap: "10px", flexWrap: "wrap" }}>
          <input
            type="text"
            value={username}
            onChange={(event) => setUsername(event.target.value)}
            placeholder="GitHub username"
            aria-label="GitHub username"
            style={{
              flex: "1 1 220px",
              borderRadius: "10px",
              border: "0.5px solid var(--cm-border)",
              background: "var(--cm-surface)",
              color: "var(--cm-text-primary)",
              fontSize: "13px",
              padding: "10px 14px",
              outline: "none",
            }}
          />
          <button
            type="submit"
            disabled={lookupBusy || !username.trim()}
            style={{
              display: "inline-flex",
              alignItems: "center",
              gap: "6px",
              fontSize: "13px",
              fontWeight: 700,
              padding: "10px 18px",
              borderRadius: "999px",
              border: "none",
              cursor: lookupBusy ? "not-allowed" : "pointer",
              opacity: lookupBusy || !username.trim() ? 0.6 : 1,
              background: "var(--cm-lime)",
              color: "#0A0A0A",
              flexShrink: 0,
            }}
          >
            <Search size={14} strokeWidth={2} aria-hidden="true" />
            {lookupBusy ? "Looking up…" : "View profile"}
          </button>
        </form>
        {lookupError && (
          <p role="alert" style={{ fontSize: "12px", color: "var(--cm-orange-text)", margin: "10px 0 0" }}>
            {lookupError}
          </p>
        )}
      </section>

      <section>
        <h2 style={{ fontSize: "15px", fontWeight: 700, margin: "0 0 14px", color: "var(--cm-text-primary)", display: "flex", alignItems: "center", justifyContent: "space-between" }}>
          Recently active projects
          <Link href="/projects?sort=recent" style={{ fontSize: "12px", fontWeight: 600, color: "var(--cm-lime-text)", display: "inline-flex", alignItems: "center", gap: "4px" }}>
            See all <ArrowRight size={12} strokeWidth={2} aria-hidden="true" />
          </Link>
        </h2>

        {projectsLoading && (
          <p style={{ fontSize: "12.5px", color: "var(--cm-text-muted)" }}>Loading…</p>
        )}

        {!projectsLoading && projects.length === 0 && (
          <p style={{ fontSize: "12.5px", color: "var(--cm-text-muted)" }}>No active projects to show right now.</p>
        )}

        {!projectsLoading && projects.length > 0 && (
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(260px, 1fr))", gap: "16px" }}>
            {projects.map((project) => (
              <ProjectCard key={project.id} project={project} />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}