"use client";

import { Target } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import RecommendedIssues from "../components/RecommendedIssues";

/**
 * The full "Recommended for you" list — Skill-Matching Recommendation Engine
 * (wow-feature, 2026-09-24). The homepage only teases the top 3 (see
 * RecommendedIssues' `limit` prop) so it doesn't push "Featured projects"
 * down the page; this is where the rest lives.
 */
export default function RecommendedPage() {
  const { user, loading: authLoading } = useAuth();

  if (authLoading) {
    return (
      <div style={{ padding: "8px 4px 40px", maxWidth: "760px", margin: "0 auto" }}>
        <p style={{ fontSize: "13px", color: "var(--cm-text-muted)" }}>Checking session…</p>
      </div>
    );
  }

  if (!user) {
    return (
      <div style={{ padding: "8px 4px 40px", maxWidth: "760px", margin: "0 auto" }}>
        <div className="cm-glass" style={{ borderRadius: "24px", padding: "48px 24px", textAlign: "center" }}>
          <Target size={22} strokeWidth={1.8} color="var(--cm-text-muted)" aria-hidden="true" style={{ marginBottom: "10px" }} />
          <p style={{ fontSize: "15px", fontWeight: 600, margin: "0 0 6px", color: "var(--cm-text-primary)" }}>
            Sign in to see your recommendations
          </p>
          <p style={{ fontSize: "13px", color: "var(--cm-text-secondary)", margin: "0 0 14px" }}>
            Issues are ranked against the skills on your profile.
          </p>
          <a
            href="/auth/github"
            style={{
              display: "inline-flex",
              alignItems: "center",
              gap: "8px",
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
        </div>
      </div>
    );
  }

  return (
    <div style={{ padding: "8px 4px 40px", maxWidth: "760px", margin: "0 auto" }}>
      <RecommendedIssues signedIn showEmptyState />
    </div>
  );
}
