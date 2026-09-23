"use client";

import Link from "next/link";
import { Compass, GitMerge, ArrowRight, UserCircle } from "lucide-react";
import { useAuth } from "../context/AuthContext";

const STEPS = [
  {
    Icon: Compass,
    title: "Find an issue",
    body: "Browse projects and filter for beginner-friendly issues, or search for a language or category you know.",
  },
  {
    Icon: GitMerge,
    title: "Claim it",
    body: "Claiming an issue is a signal of intent, it doesn't lock others out, but it tells the maintainer you're on it.",
  },
  {
    Icon: UserCircle,
    title: "Ship it",
    body: "Open a PR against the upstream repo as normal. Once a maintainer or GitHub confirms it, it shows up on your profile.",
  },
];

const ctaStyle = {
  display: "inline-flex",
  alignItems: "center",
  gap: "8px",
  fontSize: "13.5px",
  fontWeight: 700,
  padding: "11px 20px",
  borderRadius: "999px",
  textDecoration: "none",
};

export default function ContributePage() {
  const { user, loading } = useAuth();

  return (
    <div>
      <section
        className="cm-glass"
        style={{ borderRadius: "24px", padding: "36px 28px", marginBottom: "24px" }}
      >
        <h1 style={{ fontSize: "26px", fontWeight: 700, margin: 0, color: "var(--cm-text-primary)" }}>
          Start contributing
        </h1>
        <p style={{ fontSize: "14px", color: "var(--cm-text-secondary)", margin: "10px 0 22px", maxWidth: "560px" }}>
          Every project here is a real, open repo looking for help. Here's how contributing on
          Code Masters actually works, and where to go next.
        </p>

        <div style={{ display: "flex", flexWrap: "wrap", gap: "12px" }}>
          <Link
            href="/projects?hasBeginnerIssues=true"
            style={{ ...ctaStyle, background: "var(--cm-lime)", color: "#0A0A0A" }}
          >
            <Compass size={15} strokeWidth={2} aria-hidden="true" />
            Browse beginner-friendly issues
            <ArrowRight size={14} strokeWidth={2} aria-hidden="true" />
          </Link>

          <Link
            href="/projects"
            style={{ ...ctaStyle, background: "var(--cm-surface-alt)", color: "var(--cm-text-primary)" }}
          >
            Browse all projects
          </Link>

          {!loading && (
            <Link
              href={user ? "/profile" : "/auth/github"}
              style={{ ...ctaStyle, background: "var(--cm-surface-alt)", color: "var(--cm-text-primary)" }}
            >
              {user ? "View your claims" : "Sign in with GitHub"}
            </Link>
          )}
        </div>
      </section>

      <section style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))", gap: "16px" }}>
        {STEPS.map(({ Icon, title, body }, index) => (
          <div key={title} className="cm-glass" style={{ borderRadius: "20px", padding: "20px" }}>
            <div
              style={{
                display: "inline-flex",
                alignItems: "center",
                justifyContent: "center",
                width: "34px",
                height: "34px",
                borderRadius: "999px",
                background: "var(--cm-lime-soft)",
                color: "var(--cm-lime-text)",
                marginBottom: "12px",
              }}
            >
              <Icon size={16} strokeWidth={2} aria-hidden="true" />
            </div>
            <h2 style={{ fontSize: "14px", fontWeight: 700, margin: 0, color: "var(--cm-text-primary)" }}>
              {index + 1}. {title}
            </h2>
            <p style={{ fontSize: "12.5px", color: "var(--cm-text-secondary)", margin: "6px 0 0", lineHeight: 1.5 }}>
              {body}
            </p>
          </div>
        ))}
      </section>
    </div>
  );
}