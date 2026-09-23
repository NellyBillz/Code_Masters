"use client";

import Link from "next/link";
import { Sparkles, Zap, Flame, HelpCircle, GitBranch, GitPullRequest, GitMerge, UserCheck } from "lucide-react";

const DIFFICULTIES = [
  {
    Icon: Sparkles,
    label: "Beginner",
    body: "Small, well-scoped, usually documented. Good first issue if you're new to the codebase or to open source.",
    bg: "var(--cm-lime-soft)",
    text: "var(--cm-lime-text)",
  },
  {
    Icon: Zap,
    label: "Intermediate",
    body: "Needs some familiarity with the project's stack or codebase, but isn't architecturally risky.",
    bg: "var(--cm-surface-alt)",
    text: "var(--cm-text-primary)",
  },
  {
    Icon: Flame,
    label: "Advanced",
    body: "Touches core logic, performance, or security. Expect back-and-forth with the maintainer.",
    bg: "var(--cm-orange-soft)",
    text: "var(--cm-orange-text)",
  },
  {
    Icon: HelpCircle,
    label: "Unknown",
    body: "The maintainer hasn't labeled it yet. Ask in the issue before starting serious work.",
    bg: "var(--cm-surface-alt)",
    text: "var(--cm-text-secondary)",
  },
];

const CLAIM_STATES = [
  { label: "Active", body: "You've claimed it and you're working on it. Several people can claim the same issue, claiming is a signal of intent, not a lock." },
  { label: "Completed", body: "Marked done, either because GitHub shows the linked PR merged, or a maintainer confirmed it by hand." },
  { label: "Released", body: "You (or the maintainer) freed up the claim. No penalty, this happens all the time." },
];

const WORKFLOW_STEPS = [
  { Icon: GitBranch, title: "Fork and branch", body: "Fork the upstream repo (not this platform), then branch off its default branch for your change." },
  { Icon: GitPullRequest, title: "Open a PR", body: "Push your branch and open a pull request against the upstream repo, referencing the issue number." },
  { Icon: GitMerge, title: "Address review", body: "Maintainers may ask for changes. That's normal, most PRs go through at least one round." },
  { Icon: UserCheck, title: "Get verified", body: "Once merged, it's picked up automatically or a maintainer confirms it, either way it lands on your profile." },
];

function SectionHeading({ children }) {
  return (
    <h2 style={{ fontSize: "16px", fontWeight: 700, margin: "0 0 14px", color: "var(--cm-text-primary)" }}>
      {children}
    </h2>
  );
}

export default function LearnPage() {
  return (
    <div>
      <section className="cm-glass" style={{ borderRadius: "24px", padding: "36px 28px", marginBottom: "24px" }}>
        <h1 style={{ fontSize: "26px", fontWeight: 700, margin: 0, color: "var(--cm-text-primary)" }}>
          Learn how Code Masters works
        </h1>
        <p style={{ fontSize: "14px", color: "var(--cm-text-secondary)", margin: "10px 0 0", maxWidth: "560px" }}>
          A quick reference for difficulty labels, claim states, and the actual PR workflow, so
          nothing here is a surprise once you've found an issue.
        </p>
      </section>

      <section style={{ marginBottom: "24px" }}>
        <SectionHeading>Difficulty labels</SectionHeading>
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))", gap: "14px" }}>
          {DIFFICULTIES.map(({ Icon, label, body, bg, text }) => (
            <div key={label} className="cm-glass" style={{ borderRadius: "18px", padding: "16px" }}>
              <span
                style={{
                  display: "inline-flex",
                  alignItems: "center",
                  gap: "6px",
                  fontSize: "11.5px",
                  fontWeight: 700,
                  padding: "3px 10px",
                  borderRadius: "999px",
                  background: bg,
                  color: text,
                  marginBottom: "10px",
                }}
              >
                <Icon size={12} strokeWidth={2} aria-hidden="true" />
                {label}
              </span>
              <p style={{ fontSize: "12.5px", color: "var(--cm-text-secondary)", margin: 0, lineHeight: 1.5 }}>{body}</p>
            </div>
          ))}
        </div>
      </section>

      <section style={{ marginBottom: "24px" }}>
        <SectionHeading>Claim states</SectionHeading>
        <div className="cm-glass" style={{ borderRadius: "18px", padding: "6px" }}>
          {CLAIM_STATES.map(({ label, body }, index) => (
            <div
              key={label}
              style={{ padding: "14px 16px", borderTop: index === 0 ? "none" : "0.5px solid var(--cm-border)" }}
            >
              <p style={{ fontSize: "13px", fontWeight: 700, margin: 0, color: "var(--cm-text-primary)" }}>{label}</p>
              <p style={{ fontSize: "12.5px", color: "var(--cm-text-secondary)", margin: "3px 0 0", lineHeight: 1.5 }}>{body}</p>
            </div>
          ))}
        </div>
      </section>

      <section style={{ marginBottom: "8px" }}>
        <SectionHeading>The actual workflow</SectionHeading>
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))", gap: "14px" }}>
          {WORKFLOW_STEPS.map(({ Icon, title, body }, index) => (
            <div key={title} className="cm-glass" style={{ borderRadius: "18px", padding: "16px" }}>
              <div
                style={{
                  display: "inline-flex",
                  alignItems: "center",
                  justifyContent: "center",
                  width: "30px",
                  height: "30px",
                  borderRadius: "999px",
                  background: "var(--cm-surface-alt)",
                  color: "var(--cm-text-primary)",
                  marginBottom: "10px",
                  fontSize: "12px",
                  fontWeight: 700,
                }}
              >
                {index + 1}
              </div>
              <p style={{ fontSize: "13px", fontWeight: 700, margin: 0, color: "var(--cm-text-primary)", display: "flex", alignItems: "center", gap: "6px" }}>
                <Icon size={13} strokeWidth={2} aria-hidden="true" />
                {title}
              </p>
              <p style={{ fontSize: "12.5px", color: "var(--cm-text-secondary)", margin: "6px 0 0", lineHeight: 1.5 }}>{body}</p>
            </div>
          ))}
        </div>
      </section>

      <p style={{ fontSize: "13px", color: "var(--cm-text-secondary)", margin: "24px 0 0" }}>
        Ready to find something?{" "}
        <Link href="/contribute" style={{ fontWeight: 700, color: "var(--cm-lime-text)" }}>
          Head to Contribute
        </Link>
        .
      </p>
    </div>
  );
}