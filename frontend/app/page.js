import Link from "next/link";
import { ArrowRight } from "lucide-react";
import Button from "./components/ui/Button";
import ProjectCard from "./components/ProjectCard";
import IssueRow from "./components/IssueRow";
import EmptyState from "./components/ui/EmptyState";
import Footer from "./components/Footer";
import { listProjects, listProjectIssues, getStats } from "../lib/api";

/**
 * The product's own stated core loop (product definition §4) — used as the
 * homepage's only "features" section, in place of a generic icon-grid
 * feature list that carries no information specific to this product.
 */
const CORE_LOOP = [
  {
    index: "01",
    title: "Discover",
    description: "Search the South African open-source ecosystem by language, category and tag.",
  },
  {
    index: "02",
    title: "Understand",
    description: "See a project's activity, maintainers and contribution opportunities.",
  },
  {
    index: "03",
    title: "Discuss",
    description: "Ask questions on the project or issue before you commit any time.",
  },
  {
    index: "04",
    title: "Contribute",
    description: "Move to the real GitHub repository and open a pull request.",
  },
];

const FEATURED_PROJECTS_SIZE = 6;
const ISSUE_SOURCE_PROJECTS = 3;
const CONTRIBUTION_OPPORTUNITIES_SIZE = 4;

async function getHomepageData() {
  const statsPromise = getStats().catch(() => null);

  let projects = [];
  let error = null;
  try {
    const featured = await listProjects({ sort: "stars", size: FEATURED_PROJECTS_SIZE });
    projects = featured.items || [];
  } catch (err) {
    error = err.message || "Failed to load projects.";
  }

  const issueSourceProjects = projects.slice(0, ISSUE_SOURCE_PROJECTS);
  const issueLists = await Promise.all(
    issueSourceProjects.map((project) =>
      listProjectIssues(project.id, { difficulty: "beginner", status: "open", size: 2 })
        .then((result) => (result.items || []).map((issue) => ({ issue, project })))
        .catch(() => [])
    )
  );
  const opportunities = issueLists.flat().slice(0, CONTRIBUTION_OPPORTUNITIES_SIZE);

  const stats = await statsPromise;

  return { projects, opportunities, stats, error };
}

export default async function Home() {
  const { projects, opportunities, stats, error } = await getHomepageData();

  return (
    <main>
      {/* Hero */}
      <section className="mx-auto max-w-[1280px] px-4 pb-12 pt-16 sm:px-6 sm:pt-20 lg:px-8">
        <p className="mb-3 text-sm font-medium text-accent">South African Open Source</p>
        <h1 className="max-w-2xl font-display text-[3rem] font-extrabold leading-[1.05] tracking-tight text-foreground sm:text-[3.75rem]">
          Find the projects worth contributing to.
        </h1>
        <p className="mt-5 max-w-xl text-base leading-relaxed text-foreground-secondary">
          Discover South African open-source projects, understand where help is
          needed, and connect with the people building them.
        </p>

        <div className="mt-8 flex flex-wrap items-center gap-x-6 gap-y-3">
          <Button href="/projects" variant="primary">
            Explore projects
          </Button>
          <Link
            href="/projects?hasBeginnerIssues=true"
            className="inline-flex items-center gap-1.5 text-sm font-medium text-foreground-secondary transition-colors hover:text-primary"
          >
            Browse beginner-friendly issues
            <ArrowRight size={15} strokeWidth={1.75} aria-hidden="true" />
          </Link>
        </div>

        {stats && (
          <dl className="mt-10 flex flex-wrap gap-x-8 gap-y-3 border-t border-border pt-6 text-sm tabular-nums">
            <div className="flex items-baseline gap-1.5">
              <dt className="text-foreground-muted">Published projects</dt>
              <dd className="font-semibold text-foreground">{stats.publishedProjects}</dd>
            </div>
            <div className="flex items-baseline gap-1.5">
              <dt className="text-foreground-muted">Contributors engaged</dt>
              <dd className="font-semibold text-foreground">{stats.totalContributorsEngaged}</dd>
            </div>
            <div className="flex items-baseline gap-1.5">
              <dt className="text-foreground-muted">Contributions verified</dt>
              <dd className="font-semibold text-foreground">{stats.totalContributionsCompleted}</dd>
            </div>
          </dl>
        )}
      </section>

      {/* Core loop */}
      <section className="border-y border-border bg-surface-subtle/40">
        <div className="mx-auto grid max-w-[1280px] grid-cols-1 gap-8 px-4 py-10 sm:grid-cols-2 sm:px-6 lg:grid-cols-4 lg:gap-0 lg:px-8 lg:py-12">
          {CORE_LOOP.map((step, i) => (
            <div
              key={step.index}
              className={`pr-6 ${i > 0 ? "lg:border-l lg:border-border lg:pl-6" : ""}`}
            >
              <span className="font-mono text-xs text-foreground-disabled">{step.index}</span>
              <h3 className="mt-1.5 text-[15px] font-semibold text-foreground">{step.title}</h3>
              <p className="mt-1 text-sm leading-relaxed text-foreground-muted">
                {step.description}
              </p>
            </div>
          ))}
        </div>
      </section>

      {/* Featured projects */}
      <section className="mx-auto max-w-[1280px] px-4 py-14 sm:px-6 lg:px-8">
        <div className="mb-6 flex items-end justify-between gap-4">
          <div>
            <h2 className="font-display text-2xl font-bold tracking-tight text-foreground">
              Featured projects
            </h2>
            <p className="mt-1 text-sm text-foreground-muted">
              Active South African open-source projects on Code Masters.
            </p>
          </div>
          <Button href="/projects" variant="ghost" size="sm" className="hidden sm:inline-flex">
            View all →
          </Button>
        </div>

        {error && (
          <EmptyState
            title="We couldn't load projects"
            description="The platform may be temporarily unavailable. Please try again shortly."
          />
        )}

        {!error && projects.length === 0 && (
          <EmptyState
            title="No published projects yet"
            description="Check back soon — approved South African projects will appear here."
          />
        )}

        {!error && projects.length > 0 && (
          <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {projects.map((project) => (
              <ProjectCard key={project.id} project={project} />
            ))}
          </div>
        )}

        <Button href="/projects" variant="secondary" size="sm" className="mt-6 w-full sm:hidden">
          View all projects
        </Button>
      </section>

      {/* Contribution opportunities */}
      {opportunities.length > 0 && (
        <section className="border-t border-border">
          <div className="mx-auto max-w-[1280px] px-4 py-14 sm:px-6 lg:px-8">
            <div className="mb-2">
              <h2 className="font-display text-2xl font-bold tracking-tight text-foreground">
                Contribution opportunities
              </h2>
              <p className="mt-1 text-sm text-foreground-muted">
                Real, beginner-friendly issues open right now.
              </p>
            </div>

            <div className="mx-auto max-w-[760px]">
              {opportunities.map(({ issue, project }) => (
                <IssueRow key={issue.id} issue={issue} projectName={project.name} />
              ))}
            </div>
          </div>
        </section>
      )}

      <Footer />
    </main>
  );
}
