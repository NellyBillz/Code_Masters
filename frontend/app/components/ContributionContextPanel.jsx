import { BookOpen, ShieldCheck, Clock, GitMerge, Users } from "lucide-react";

/**
 * Evidence-based "Contribution context" panel (API-04.1) — plain facts and
 * counts only, rendered as-is from GET /issues/{issueId}/contribution-context.
 * Deliberately no weighting, score, or combined judgment: every line here
 * traces directly to one field in the response. A fact that's false/zero is
 * omitted rather than shown as a negative, since absence of a signal isn't
 * itself a negative signal (a project that just never populated
 * hasContributingGuide isn't necessarily worse than one that has it).
 *
 * Not a client component: no interactivity, just a presentational render of
 * data already fetched server-side by the issue page.
 */
export default function ContributionContextPanel({ context }) {
  if (!context) return null;

  const { project = {}, issue = {} } = context;
  const {
    hasContributingGuide,
    hasCodeOfConduct,
    daysSinceLastActivity,
    completedContributionsCount,
  } = project;
  const { ageInDays, inFlightClaimCount } = issue;

  const facts = [];

  facts.push({
    icon: Clock,
    text: ageInDays === 0 ? "Opened today" : `Opened ${ageInDays} day${ageInDays === 1 ? "" : "s"} ago`,
  });

  if (daysSinceLastActivity != null) {
    facts.push({
      icon: Clock,
      text:
        daysSinceLastActivity === 0
          ? "Project active today"
          : `Project last active ${daysSinceLastActivity} day${daysSinceLastActivity === 1 ? "" : "s"} ago`,
    });
  }

  if (hasContributingGuide) {
    facts.push({ icon: BookOpen, text: "Has a contributing guide" });
  }

  if (hasCodeOfConduct) {
    facts.push({ icon: ShieldCheck, text: "Has a code of conduct" });
  }

  if (completedContributionsCount > 0) {
    facts.push({
      icon: GitMerge,
      text: `${completedContributionsCount} verified contribution${completedContributionsCount === 1 ? "" : "s"} on this project`,
    });
  }

  if (inFlightClaimCount > 0) {
    facts.push({
      icon: Users,
      text: `${inFlightClaimCount} other contributor${inFlightClaimCount === 1 ? " has" : "s have"} also claimed this issue — claims are a signal of intent, not exclusive.`,
    });
  }

  return (
    <section className="cm-glass" style={{ borderRadius: "20px", padding: "18px 20px", marginBottom: "20px" }}>
      <h2 style={{ fontSize: "13px", fontWeight: 700, margin: "0 0 12px", color: "var(--cm-text-primary)" }}>
        Contribution context
      </h2>
      <ul style={{ listStyle: "none", margin: 0, padding: 0, display: "flex", flexDirection: "column", gap: "8px" }}>
        {facts.map(({ icon: Icon, text }, index) => (
          <li key={index} style={{ display: "flex", alignItems: "center", gap: "8px", fontSize: "12.5px", color: "var(--cm-text-secondary)" }}>
            <Icon size={13} strokeWidth={2} color="var(--cm-text-muted)" aria-hidden="true" />
            {text}
          </li>
        ))}
      </ul>
    </section>
  );
}
