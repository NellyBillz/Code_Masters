"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { ArrowRight } from "lucide-react";
import HeroVisual from "./components/HeroVisual";
import ProjectCard from "./components/ProjectCard";
import { listProjects } from "../lib/api";

// Placeholder headline stats — there's no aggregate stats endpoint in the
// API client yet (lib/api.js only exposes paged project listings), so
// these mirror the reference design rather than a live count. Swap in a
// real endpoint here once one exists.
const HERO_STATS = [
  { value: "126", label: "Projects" },
  { value: "2,431", label: "Builders" },
  { value: "684", label: "Open issues" },
];

export default function Home() {
  const [projects, setProjects] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  useEffect(() => {
    let cancelled = false;

    async function loadFeatured() {
      setLoading(true);
      setError(false);

      try {
        const result = await listProjects({ size: 6, sort: "stars" });
        if (cancelled) return;
        setProjects(result.items);
      } catch {
        if (cancelled) return;
        setError(true);
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    loadFeatured();
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div>
      {/* Hero */}
      <section
        className="cm-glass"
        style={{
          position: "relative",
          overflow: "hidden",
          borderRadius: "32px",
          padding: "56px 40px",
          display: "grid",
          gridTemplateColumns: "1.3fr 1fr",
          gap: "40px",
          alignItems: "center",
        }}
      >
        {/* Light-mode-only organic blob accents */}
        <span className="cm-blob" style={{ width: 220, height: 220, top: -60, left: -60, background: "var(--cm-blob)" }} aria-hidden="true" />
        <span className="cm-blob" style={{ width: 160, height: 160, bottom: -40, left: "30%", background: "var(--cm-blob)" }} aria-hidden="true" />

        <div style={{ position: "relative" }}>
          <span
            style={{
              fontSize: "11px",
              fontWeight: 600,
              letterSpacing: "0.16em",
              color: "var(--cm-text-secondary)",
            }}
          >
            SOUTH AFRICAN OPEN SOURCE
          </span>

          <h1
            style={{
              fontSize: "clamp(38px, 5vw, 64px)",
              fontWeight: 700,
              lineHeight: 1.05,
              letterSpacing: "-0.02em",
              margin: "14px 0 18px",
              color: "var(--cm-text-primary)",
            }}
          >
            Build what
            <br />
            <span
              style={{
                background: "linear-gradient(90deg, #F48C3C, #C8FF64, #2DD4BF)",
                WebkitBackgroundClip: "text",
                WebkitTextFillColor: "transparent",
                backgroundClip: "text",
              }}
            >
              matters.
            </span>
          </h1>

          <p
            style={{
              fontSize: "15px",
              lineHeight: 1.6,
              color: "var(--cm-text-secondary)",
              margin: "0 0 28px",
              maxWidth: "380px",
            }}
          >
            Real projects. Real people.
            <br />
            A stronger South Africa.
          </p>

          <Link
            href="/projects"
            style={{
              display: "inline-flex",
              alignItems: "center",
              gap: "8px",
              borderRadius: "999px",
              padding: "13px 22px",
              fontSize: "14px",
              fontWeight: 600,
              background: "var(--cm-sidebar)",
              color: "#FFFFFF",
              boxShadow: "0 0 0 rgba(0,0,0,0)",
            }}
          >
            Explore the ecosystem
            <ArrowRight size={15} strokeWidth={2} aria-hidden="true" />
          </Link>

          <div style={{ display: "flex", gap: "32px", marginTop: "36px" }}>
            {HERO_STATS.map((stat) => (
              <div key={stat.label}>
                <p style={{ fontSize: "22px", fontWeight: 700, margin: 0, color: "var(--cm-text-primary)" }}>
                  {stat.value}
                </p>
                <p style={{ fontSize: "12px", color: "var(--cm-text-secondary)", margin: "2px 0 0" }}>
                  {stat.label}
                </p>
              </div>
            ))}
          </div>

          <p
            style={{
              fontSize: "12px",
              color: "var(--cm-text-muted)",
              marginTop: "24px",
              fontStyle: "italic",
            }}
          >
            &ldquo;A stronger South Africa builds when we build together.&rdquo;
          </p>
        </div>

        <HeroVisual />
      </section>

      {/* Featured projects */}
      <section style={{ padding: "48px 4px 24px" }}>
        <div
          style={{
            display: "flex",
            alignItems: "flex-end",
            justifyContent: "space-between",
            flexWrap: "wrap",
            gap: "10px",
            marginBottom: "20px",
          }}
        >
          <div>
            <h2 style={{ fontSize: "22px", fontWeight: 700, margin: "0 0 4px", color: "var(--cm-text-primary)" }}>
              Featured projects
            </h2>
            <p style={{ fontSize: "13px", color: "var(--cm-text-secondary)", margin: 0 }}>
              Active South African open-source projects on Code Masters.
            </p>
          </div>

          <Link
            href="/projects"
            style={{ fontSize: "13px", fontWeight: 600, color: "var(--cm-lime-text)", display: "inline-flex", alignItems: "center", gap: "4px" }}
          >
            View all
            <ArrowRight size={14} strokeWidth={2} aria-hidden="true" />
          </Link>
        </div>

        {loading && (
          <StatusPanel text="Loading projects…" />
        )}

        {!loading && error && (
          <StatusPanel
            title="We couldn't load projects"
            text="The platform may be temporarily unavailable. Please try again shortly."
          />
        )}

        {!loading && !error && projects.length === 0 && (
          <StatusPanel text="No featured projects yet — check back soon." />
        )}

        {!loading && !error && projects.length > 0 && (
          <div
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(auto-fit, minmax(240px, 1fr))",
              gap: "16px",
            }}
          >
            {projects.map((project) => (
              <ProjectCard key={project.id} project={project} />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

function StatusPanel({ title, text }) {
  return (
    <div
      className="cm-glass"
      style={{
        borderRadius: "24px",
        padding: "48px 24px",
        textAlign: "center",
      }}
    >
      {title && (
        <p style={{ fontSize: "15px", fontWeight: 600, margin: "0 0 6px", color: "var(--cm-text-primary)" }}>
          {title}
        </p>
      )}
      <p style={{ fontSize: "13px", color: "var(--cm-text-secondary)", margin: 0 }}>{text}</p>
    </div>
  );
}
