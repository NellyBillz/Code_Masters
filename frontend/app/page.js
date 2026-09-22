"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { Bell, Search } from "lucide-react";
import Sidebar from "./components/Sidebar";
import ProjectCard from "./components/ProjectCard";
import { useAuth } from "./context/AuthContext";

// Placeholder catalogue until /projects' listProjects() feed is reused here.
// Shape matches ProjectCard's prop contract exactly, so swapping this for a
// real fetch later is a drop-in change — no card changes required.
const FEATURED_PROJECTS = [
  {
    id: 1,
    name: "Code Masters API",
    category: "Platform foundation",
    description:
      "RESTful API powering African open-source discovery. Built with Node.js and PostgreSQL.",
    primaryLanguage: "TypeScript",
    tags: ["API", "Open source"],
    stars: 2300,
    contributors: 34,
    lastActivityAt: new Date().toISOString(),
    connection: "south_african",
    verified: true,
    hasBeginnerFriendlyIssues: true,
  },
  {
    id: 2,
    name: "African Vision",
    category: "Computer vision library",
    description:
      "Open-source ML toolkit for African-specific computer vision applications.",
    primaryLanguage: "Python",
    tags: ["ML", "CV"],
    stars: 1800,
    contributors: 21,
    lastActivityAt: new Date(Date.now() - 2 * 86400000).toISOString(),
    connection: "community_verified",
    verified: true,
    hasBeginnerFriendlyIssues: false,
  },
  {
    id: 3,
    name: "Data Toolkit",
    category: "Analytics framework",
    description:
      "Lightweight data processing and analytics framework for development teams.",
    primaryLanguage: "Python",
    tags: ["Data", "Analytics"],
    stars: 1200,
    contributors: 15,
    lastActivityAt: new Date(Date.now() - 7 * 86400000).toISOString(),
    connection: "south_african",
    verified: false,
    hasBeginnerFriendlyIssues: true,
  },
  {
    id: 4,
    name: "Weave Design",
    category: "UI component library",
    description:
      "Modern React components designed for African tech products and applications.",
    primaryLanguage: "JavaScript",
    tags: ["React", "Design system"],
    stars: 956,
    contributors: 18,
    lastActivityAt: new Date(Date.now() - 3 * 86400000).toISOString(),
    connection: "community_verified",
    verified: true,
    hasBeginnerFriendlyIssues: true,
  },
];

const FILTERS = ["All", "Web", "Mobile", "Data", "Tools"];

export default function CodeMastersHome() {
  const { user } = useAuth();
  const [activeFilter, setActiveFilter] = useState("all");

  const greetingName = user?.displayName || user?.username || "there";

  const visibleProjects = useMemo(() => {
    if (activeFilter === "all") return FEATURED_PROJECTS;

    return FEATURED_PROJECTS.filter((project) =>
      project.tags.some((tag) => tag.toLowerCase() === activeFilter)
    );
  }, [activeFilter]);

  return (
    <div
      style={{
        background: "var(--cm-bg)",
        minHeight: "calc(100vh - 61px)",
        padding: "14px",
        display: "grid",
        gridTemplateColumns: "56px 1fr 220px",
        gap: "14px",
        alignItems: "start",
      }}
    >
      <Sidebar />

      <main>
        <div
          style={{
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            gap: "1rem",
            flexWrap: "wrap",
            marginBottom: "18px",
          }}
        >
          <div>
            <h1 style={{ fontSize: "20px", fontWeight: 500, margin: 0 }}>
              Hi {greetingName}
            </h1>
            <p style={{ fontSize: "13px", color: "var(--cm-text-secondary)", margin: "2px 0 0" }}>
              Welcome back to Code_Masters
            </p>
          </div>

          <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
            <label
              style={{
                display: "flex",
                alignItems: "center",
                gap: "8px",
                border: "0.5px solid var(--cm-border)",
                borderRadius: "8px",
                padding: "6px 12px",
                background: "var(--cm-surface)",
              }}
            >
              <Search size={15} strokeWidth={1.75} color="var(--cm-text-muted)" aria-hidden="true" />
              <input
                type="search"
                placeholder="Search projects"
                aria-label="Search projects"
                style={{
                  border: "none",
                  outline: "none",
                  background: "transparent",
                  fontSize: "13px",
                  color: "var(--cm-text-primary)",
                  width: "160px",
                }}
              />
            </label>

            <button
              type="button"
              aria-label="Notifications"
              style={{
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                width: "32px",
                height: "32px",
                borderRadius: "8px",
                border: "0.5px solid var(--cm-border)",
                background: "var(--cm-surface)",
                color: "var(--cm-text-secondary)",
                cursor: "pointer",
              }}
            >
              <Bell size={15} strokeWidth={1.75} />
            </button>
          </div>
        </div>

        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(3, 1fr)",
            gap: "10px",
            marginBottom: "20px",
          }}
        >
          <StatCard value="6 issues" label="claimed this month" blob="var(--cm-lime)" />
          <StatCard value="3 PRs" label="merged this month" blob="var(--cm-orange)" />

          <Link
            href="/projects"
            style={{
              background: "var(--cm-orange)",
              borderRadius: "14px",
              padding: "14px",
              color: "#2B1108",
              display: "block",
            }}
          >
            <p style={{ fontSize: "13px", fontWeight: 500, margin: "0 0 2px" }}>
              Become a maintainer
            </p>
            <p style={{ fontSize: "11px", margin: 0, opacity: 0.85 }}>
              Unlock repo access
            </p>
          </Link>
        </div>

        <div
          style={{
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            flexWrap: "wrap",
            gap: "10px",
            marginBottom: "12px",
          }}
        >
          <p style={{ fontSize: "14px", fontWeight: 500, margin: 0 }}>
            Open source projects
          </p>

          <div style={{ display: "flex", gap: "6px", flexWrap: "wrap" }}>
            {FILTERS.map((filter) => {
              const value = filter.toLowerCase();
              const active = activeFilter === value;

              return (
                <button
                  key={filter}
                  type="button"
                  onClick={() => setActiveFilter(value)}
                  style={{
                    padding: "6px 14px",
                    borderRadius: "999px",
                    fontSize: "12px",
                    fontWeight: 500,
                    border: active ? "none" : "0.5px solid var(--cm-border)",
                    background: active ? "var(--cm-lime)" : "var(--cm-surface)",
                    color: active ? "#232700" : "var(--cm-text-secondary)",
                    cursor: "pointer",
                  }}
                >
                  {filter}
                </button>
              );
            })}
          </div>
        </div>

        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))",
            gap: "12px",
          }}
        >
          {visibleProjects.map((project) => (
            <ProjectCard key={project.id} project={project} />
          ))}
        </div>
      </main>

      <aside style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
        <div style={{ background: "var(--cm-lime-soft)", borderRadius: "14px", padding: "14px" }}>
          <p style={{ fontSize: "12px", fontWeight: 500, margin: "0 0 10px" }}>Claimed issues</p>
          <ClaimedIssueRow label="Fix auth token refresh" />
          <ClaimedIssueRow label="Add Swahili locale" highlighted />
          <ClaimedIssueRow label="Docs: setup guide" />
        </div>

        <div style={{ background: "var(--cm-orange-soft)", borderRadius: "14px", padding: "14px" }}>
          <p style={{ fontSize: "12px", fontWeight: 500, margin: "0 0 6px" }}>Contributor rank</p>
          <p style={{ fontSize: "11px", color: "var(--cm-text-secondary)", margin: 0 }}>
            Top 12% this quarter across 5 repos.
          </p>
        </div>
      </aside>
    </div>
  );
}

function StatCard({ value, label, blob }) {
  return (
    <div
      style={{
        background: "var(--cm-surface)",
        borderRadius: "14px",
        padding: "14px",
        position: "relative",
        overflow: "hidden",
        border: "0.5px solid var(--cm-border)",
      }}
    >
      <div
        aria-hidden="true"
        style={{
          position: "absolute",
          top: "-14px",
          right: "-14px",
          width: "50px",
          height: "50px",
          borderRadius: "45% 55% 50% 50%",
          background: blob,
          opacity: 0.35,
        }}
      />
      <p style={{ fontSize: "20px", fontWeight: 500, margin: "0 0 2px", position: "relative" }}>
        {value}
      </p>
      <p style={{ fontSize: "11px", color: "var(--cm-text-secondary)", margin: 0, position: "relative" }}>
        {label}
      </p>
    </div>
  );
}

function ClaimedIssueRow({ label, highlighted }) {
  return (
    <div
      style={{
        background: highlighted ? "var(--cm-lime)" : "var(--cm-surface)",
        color: highlighted ? "#232700" : "var(--cm-text-primary)",
        borderRadius: "8px",
        padding: "6px 10px",
        fontSize: "11px",
        marginBottom: "6px",
      }}
    >
      {label}
    </div>
  );
}
