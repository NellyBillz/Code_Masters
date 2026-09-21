"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import {
  ArrowLeft,
  ArrowRight,
  ArrowUpRight,
  BookOpen,
  CircleDot,
  Clock,
  FileCheck2,
  ScrollText,
  Sparkles,
  Star,
  Users,
} from "lucide-react";
import { getProject } from "../../../lib/api";
import { useAuth } from "../../context/AuthContext";
import MaintainersPanel from "../../components/MaintainersPanel";
import IssueRow from "../../components/IssueRow";
import ConnectionBadge from "../../components/ui/ConnectionBadge";
import Badge from "../../components/ui/Badge";
import Button from "../../components/ui/Button";
import Avatar from "../../components/ui/Avatar";
import EmptyState from "../../components/ui/EmptyState";
import { Skeleton } from "../../components/ui/Skeleton";
import { LANGUAGE_COLORS, formatRelativeTime, formatCount, formatDateTime } from "../../../lib/format";

function ProjectDetailSkeleton() {
  return (
    <main className="mx-auto max-w-[1280px] px-4 py-10 sm:px-6 lg:px-8">
      <Skeleton className="h-4 w-32" />
      <div className="mt-6 border-b border-border pb-8">
        <Skeleton className="h-6 w-40 rounded-full" />
        <Skeleton className="mt-4 h-9 w-2/3" />
        <Skeleton className="mt-3 h-4 w-full max-w-xl" />
        <Skeleton className="mt-2 h-4 w-1/2 max-w-md" />
      </div>
      <div className="mt-10 grid grid-cols-1 gap-10 lg:grid-cols-[1fr_300px]">
        <div className="flex flex-col gap-4">
          <Skeleton className="h-4 w-full" />
          <Skeleton className="h-4 w-5/6" />
          <Skeleton className="h-32 w-full" />
        </div>
        <div className="flex flex-col gap-4">
          <Skeleton className="h-4 w-full" />
          <Skeleton className="h-4 w-2/3" />
        </div>
      </div>
    </main>
  );
}

export default function ProjectDetail() {
  const params = useParams();
  const projectId = params?.projectId;
  const { user: currentUser } = useAuth();

  const [project, setProject] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const isMaintainer = Boolean(
    currentUser &&
      project?.maintainers?.some(
        (maint) =>
          maint.user?.id === currentUser.id || maint.user?.username === currentUser.username
      )
  );

  useEffect(() => {
    if (!projectId) return;

    let cancelled = false;

    async function loadProject() {
      setLoading(true);
      setError("");

      try {
        const projectResult = await getProject(projectId);
        if (!cancelled) setProject(projectResult);
      } catch (err) {
        if (!cancelled) setError(err.message || "Failed to load project.");
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    loadProject();
    return () => {
      cancelled = true;
    };
  }, [projectId]);

  const handleMaintainerAdded = () => {
    if (projectId && typeof projectId === "string") {
      getProject(projectId).then(setProject).catch(() => {});
    }
  };

  const handleMaintainerRemoved = (userId) => {
    setProject((prev) => ({
      ...prev,
      maintainers: prev.maintainers.filter((m) => m.user?.id !== userId),
    }));
  };

  if (loading) {
    return <ProjectDetailSkeleton />;
  }

  if (error || !project) {
    return (
      <main className="mx-auto max-w-[720px] px-4 py-16 text-center sm:px-6 lg:px-8">
        <h1 className="text-xl font-semibold text-foreground">We couldn&apos;t load this project</h1>
        <p className="mx-auto mt-2 max-w-sm text-sm text-foreground-muted">
          {error || "The project may have been removed or is temporarily unavailable."}
        </p>
        <Button href="/projects" variant="secondary" size="sm" className="mt-6">
          Back to projects
        </Button>
      </main>
    );
  }

  const languages = [project.primaryLanguage, ...(project.languages || []).filter((l) => l !== project.primaryLanguage)].filter(
    Boolean
  );
  const activity = formatRelativeTime(project.lastActivityAt);

  return (
    <main className="mx-auto max-w-[1280px] px-4 pt-8 sm:px-6 lg:px-8">
      <Link
        href="/projects"
        className="inline-flex items-center gap-1.5 text-sm font-medium text-foreground-muted transition-colors hover:text-foreground"
      >
        <ArrowLeft size={15} strokeWidth={1.75} aria-hidden="true" />
        Back to projects
      </Link>

      <div className="mt-6 flex flex-col gap-6 border-b border-border pb-8 lg:flex-row lg:items-start lg:justify-between lg:gap-10">
        <div className="min-w-0 max-w-[720px]">
          <div className="mb-3 flex flex-wrap items-center gap-2">
            <ConnectionBadge connection={project.connection} />
            {!project.acceptingContributions && (
              <Badge tone="warning">Not accepting new contributors</Badge>
            )}
          </div>

          <h1 className="font-display text-[2rem] font-extrabold leading-tight tracking-tight text-foreground sm:text-4xl">
            {project.name}
          </h1>

          {project.description && (
            <p className="mt-3 text-[15px] leading-relaxed text-foreground-secondary">
              {project.description}
            </p>
          )}

          <div className="mt-4 flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-foreground-muted">
            {project.primaryLanguage && (
              <span className="inline-flex items-center gap-1.5">
                <span
                  className="h-2 w-2 rounded-full"
                  style={{ backgroundColor: LANGUAGE_COLORS[project.primaryLanguage] || "#94a3b8" }}
                  aria-hidden="true"
                />
                {project.primaryLanguage}
              </span>
            )}
            {project.category && (
              <>
                <span aria-hidden="true">·</span>
                <span>{project.category}</span>
              </>
            )}
            {project.license && (
              <>
                <span aria-hidden="true">·</span>
                <span>{project.license}</span>
              </>
            )}
          </div>
        </div>

        <div className="flex flex-shrink-0 gap-3 lg:flex-col">
          <Button href={project.githubUrl} target="_blank" rel="noreferrer" variant="primary" className="flex-1 lg:flex-none">
            View on GitHub
            <ArrowUpRight size={15} strokeWidth={1.75} aria-hidden="true" />
          </Button>
          <Button href={`/projects/${project.id}/issues`} variant="secondary" className="flex-1 lg:flex-none">
            View issues
          </Button>
        </div>
      </div>

      <dl className="flex flex-wrap gap-x-8 gap-y-3 border-b border-border py-6 text-sm tabular-nums text-foreground-muted">
        <div className="flex items-center gap-1.5">
          <Star size={15} strokeWidth={1.75} aria-hidden="true" />
          <dd className="font-semibold text-foreground">{formatCount(project.stars)}</dd>
          <dt>Stars</dt>
        </div>
        <div className="flex items-center gap-1.5">
          <Users size={15} strokeWidth={1.75} aria-hidden="true" />
          <dd className="font-semibold text-foreground">{formatCount(project.contributors)}</dd>
          <dt>Contributors</dt>
        </div>
        <div className="flex items-center gap-1.5">
          <CircleDot size={15} strokeWidth={1.75} aria-hidden="true" />
          <dd className="font-semibold text-foreground">{formatCount(project.openIssues)}</dd>
          <dt>Open issues</dt>
        </div>
        {activity && (
          <div className="flex items-center gap-1.5">
            <Clock size={15} strokeWidth={1.75} aria-hidden="true" />
            {activity}
          </div>
        )}
      </dl>

      <div className="grid grid-cols-1 gap-10 py-10 lg:grid-cols-[1fr_300px] lg:gap-12">
        <div className="flex min-w-0 flex-col gap-10">
          <nav aria-label="Project sections" className="flex items-center gap-6 border-b border-border text-sm font-medium">
            <a href="#overview" className="border-b-2 border-primary pb-3 text-foreground">
              Overview
            </a>
            <Link
              href={`/projects/${project.id}/issues`}
              className="border-b-2 border-transparent pb-3 text-foreground-muted transition-colors hover:text-foreground"
            >
              Issues
            </Link>
            <a
              href="#discussion"
              className="border-b-2 border-transparent pb-3 text-foreground-muted transition-colors hover:text-foreground"
            >
              Discussion
            </a>
          </nav>

          <section id="overview" className="flex flex-col gap-10">
            {(languages.length > 0 || project.tags?.length > 0) && (
              <div>
                <h2 className="mb-3 text-[13px] font-semibold uppercase tracking-wide text-foreground-muted">
                  Technology
                </h2>
                <div className="flex flex-wrap gap-2">
                  {languages.map((lang) => (
                    <Badge key={lang} tone="neutral">
                      {lang}
                    </Badge>
                  ))}
                  {project.tags?.map((tag) => (
                    <Badge key={tag} tone="neutral">
                      {tag}
                    </Badge>
                  ))}
                </div>
              </div>
            )}

            <div>
              <h2 className="mb-3 text-[13px] font-semibold uppercase tracking-wide text-foreground-muted">
                Contribution readiness
              </h2>
              <ul className="flex flex-col gap-2.5 text-sm text-foreground-secondary">
                <li className="flex items-center gap-2">
                  <ScrollText size={15} strokeWidth={1.75} className="text-foreground-disabled" aria-hidden="true" />
                  {project.hasContributingGuide ? "Has a contributing guide" : "No contributing guide found"}
                </li>
                <li className="flex items-center gap-2">
                  <FileCheck2 size={15} strokeWidth={1.75} className="text-foreground-disabled" aria-hidden="true" />
                  {project.hasCodeOfConduct ? "Has a code of conduct" : "No code of conduct found"}
                </li>
                {project.hasBeginnerFriendlyIssues && (
                  <li className="flex items-center gap-2 text-success">
                    <Sparkles size={15} strokeWidth={1.75} aria-hidden="true" />
                    Has beginner-friendly issues open
                  </li>
                )}
              </ul>
            </div>

            <div>
              <div className="mb-3 flex items-end justify-between gap-4">
                <h2 className="text-[13px] font-semibold uppercase tracking-wide text-foreground-muted">
                  Contribution opportunities
                </h2>
                <Link
                  href={`/projects/${project.id}/issues`}
                  className="inline-flex items-center gap-1 text-sm font-medium text-primary hover:text-primary-hover"
                >
                  View all issues
                  <ArrowRight size={14} strokeWidth={1.75} aria-hidden="true" />
                </Link>
              </div>

              {project.featuredIssues?.length > 0 ? (
                <div>
                  {project.featuredIssues.map((issue) => (
                    <IssueRow key={issue.id} issue={issue} />
                  ))}
                </div>
              ) : (
                <EmptyState
                  title="No open issues right now"
                  description="Check back soon, or browse this project's full issue list."
                />
              )}
            </div>
          </section>

          <section id="discussion">
            <h2 className="mb-4 text-[13px] font-semibold uppercase tracking-wide text-foreground-muted">Discussion</h2>
            {project.recentComments?.length > 0 ? (
              <ul className="flex flex-col">
                {project.recentComments.map((comment) => (
                  <li key={comment.id} className="flex gap-3 border-b border-border py-4 last:border-b-0">
                    <Avatar user={comment.author} size="sm" />
                    <div className="min-w-0 flex-1">
                      <div className="flex flex-wrap items-baseline gap-x-2">
                        <span className="text-sm font-medium text-foreground">
                          {comment.author?.displayName || comment.author?.username || "User"}
                        </span>
                        {comment.createdAt && (
                          <span className="text-xs text-foreground-disabled">{formatDateTime(comment.createdAt)}</span>
                        )}
                      </div>
                      <p className="mt-1 whitespace-pre-wrap break-words text-sm leading-relaxed text-foreground-secondary">
                        {comment.body}
                      </p>
                    </div>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="text-sm text-foreground-muted">No comments yet.</p>
            )}
          </section>
        </div>

        <aside className="flex flex-col gap-10">
          <div>
            <h2 className="mb-3 text-[13px] font-semibold uppercase tracking-wide text-foreground-muted">Maintainers</h2>
            {project.maintainers?.length > 0 ? (
              <ul className="flex flex-col gap-3">
                {project.maintainers.map((maintainer, index) => (
                  <li key={maintainer.user?.id ?? index} className="flex items-center gap-2.5">
                    <Avatar user={maintainer.user} size="sm" />
                    <span className="truncate text-sm text-foreground-secondary">
                      {maintainer.user?.displayName ?? maintainer.user?.username ?? "Unknown"}
                    </span>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="text-sm text-foreground-muted">No maintainers listed.</p>
            )}
          </div>

          <div>
            <h2 className="mb-3 text-[13px] font-semibold uppercase tracking-wide text-foreground-muted">
              Project links
            </h2>
            <a
              href={project.githubUrl}
              target="_blank"
              rel="noreferrer"
              className="inline-flex items-center gap-1.5 text-sm font-medium text-primary hover:text-primary-hover"
            >
              <BookOpen size={14} strokeWidth={1.75} aria-hidden="true" />
              GitHub repository
            </a>
          </div>

          {(project.license || project.category) && (
            <div>
              <h2 className="mb-3 text-[13px] font-semibold uppercase tracking-wide text-foreground-muted">Details</h2>
              <dl className="flex flex-col gap-1.5 text-sm">
                {project.license && (
                  <div className="flex justify-between gap-4">
                    <dt className="text-foreground-muted">License</dt>
                    <dd className="text-foreground-secondary">{project.license}</dd>
                  </div>
                )}
                {project.category && (
                  <div className="flex justify-between gap-4">
                    <dt className="text-foreground-muted">Category</dt>
                    <dd className="text-foreground-secondary">{project.category}</dd>
                  </div>
                )}
              </dl>
            </div>
          )}

          {project.connection && (
            <div>
              <h2 className="mb-3 text-[13px] font-semibold uppercase tracking-wide text-foreground-muted">
                South African context
              </h2>
              <p className="text-sm leading-relaxed text-foreground-secondary">
                {project.connection === "south_african"
                  ? "This project is based in South Africa or maintained by a South African team."
                  : "This project's South African connection has been verified by the Code Masters community."}
              </p>
            </div>
          )}

          {isMaintainer && (
            <MaintainersPanel
              project={project}
              isMaintainer={isMaintainer}
              onMaintainerAdded={handleMaintainerAdded}
              onMaintainerRemoved={handleMaintainerRemoved}
            />
          )}
        </aside>
      </div>
    </main>
  );
}
