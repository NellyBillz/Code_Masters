"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import {
  ArrowLeft,
  ArrowRight,
  ArrowUpRight,
  Clock,
  FileCheck2,
  ScrollText,
  Sparkles,
} from "lucide-react";
import { getProject } from "../../../lib/api";
import { useAuth } from "../../context/AuthContext";
import MaintainersPanel from "../../components/MaintainersPanel";
import IssueRow from "../../components/IssueRow";
import ConnectionBadge from "../../components/ui/ConnectionBadge";
import Badge from "../../components/ui/Badge";
import Button from "../../components/ui/Button";
import Avatar from "../../components/ui/Avatar";
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

  const statTiles = [
    { label: "Owner", value: project.owner || "Unknown" },
    { label: "License", value: project.license || "Unknown" },
    { label: "Stars", value: formatCount(project.stars) },
    { label: "Forks", value: formatCount(project.forks) },
    { label: "Open issues", value: formatCount(project.openIssues) },
    { label: "Contributors", value: formatCount(project.contributors) },
  ];

  return (
    <main className="mx-auto max-w-[1280px] px-4 py-8 sm:px-6 lg:px-8">
      <Link
        href="/projects"
        className="inline-flex items-center gap-1.5 text-sm font-medium text-foreground-muted transition-colors hover:text-foreground"
      >
        <ArrowLeft size={15} strokeWidth={1.75} aria-hidden="true" />
        Back to projects
      </Link>

      {/* Header card — mirrors main's real project detail page (07932d5):
          a single bordered surface holding badges, title, description and
          the primary actions, instead of a border-bottom-only page header. */}
      <div className="mt-6 rounded-[10px] border border-border bg-surface p-6 sm:p-8">
        <div className="mb-4 flex flex-wrap items-center gap-2">
          {project.primaryLanguage && (
            <Badge tone="primary" className="gap-1.5">
              <span
                className="h-1.5 w-1.5 rounded-full"
                style={{ backgroundColor: LANGUAGE_COLORS[project.primaryLanguage] || "#94a3b8" }}
                aria-hidden="true"
              />
              {project.primaryLanguage}
            </Badge>
          )}
          {project.category && <Badge tone="neutral">{project.category}</Badge>}
          <ConnectionBadge connection={project.connection} />
          {!project.acceptingContributions && (
            <Badge tone="warning">Not accepting new contributors</Badge>
          )}
        </div>

        <h1 className="font-display text-[2rem] font-extrabold leading-tight tracking-tight text-foreground sm:text-4xl">
          {project.name}
        </h1>

        {project.description && (
          <p className="mt-3 max-w-2xl text-[15px] leading-relaxed text-foreground-secondary">
            {project.description}
          </p>
        )}

        {(languages.length > 1 || project.tags?.length > 0) && (
          <div className="mt-4 flex flex-wrap gap-2">
            {languages.slice(1).map((lang) => (
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
        )}

        {activity && (
          <p className="mt-4 flex items-center gap-1.5 text-sm text-foreground-muted">
            <Clock size={14} strokeWidth={1.75} aria-hidden="true" />
            Active {activity}
          </p>
        )}

        <div className="mt-6 flex flex-wrap gap-3">
          <Button href={project.githubUrl} target="_blank" rel="noreferrer" variant="primary">
            View on GitHub
            <ArrowUpRight size={15} strokeWidth={1.75} aria-hidden="true" />
          </Button>
          <Button href={`/projects/${project.id}/issues`} variant="secondary">
            View issues
          </Button>
        </div>
      </div>

      <section className="mt-8">
        <h2 className="mb-4 text-xl font-bold text-foreground">Project information</h2>
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-6">
          {statTiles.map((tile) => (
            <div key={tile.label} className="rounded-[10px] border border-border bg-surface p-4 text-center">
              <p className="text-[11px] font-semibold uppercase tracking-wide text-foreground-muted">
                {tile.label}
              </p>
              <p className="mt-1.5 truncate text-lg font-bold text-foreground">{tile.value}</p>
            </div>
          ))}
        </div>
      </section>

      <div className="mt-8 grid grid-cols-1 gap-6 lg:grid-cols-3">
        <section className="rounded-[10px] border border-border bg-surface p-6">
          <h2 className="mb-4 text-lg font-bold text-foreground">Maintainers</h2>
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
        </section>

        <section className="rounded-[10px] border border-border bg-surface p-6">
          <div className="mb-4 flex items-center justify-between gap-4">
            <h2 className="text-lg font-bold text-foreground">Featured issues</h2>
            <Link
              href={`/projects/${project.id}/issues`}
              className="inline-flex items-center gap-1 text-sm font-medium text-primary hover:text-primary-hover"
            >
              View all
              <ArrowRight size={14} strokeWidth={1.75} aria-hidden="true" />
            </Link>
          </div>
          {project.featuredIssues?.length > 0 ? (
            <div className="-mx-1">
              {project.featuredIssues.map((issue) => (
                <IssueRow key={issue.id} issue={issue} />
              ))}
            </div>
          ) : (
            <p className="text-sm text-foreground-muted">No featured issues.</p>
          )}
        </section>

        <section className="rounded-[10px] border border-border bg-surface p-6">
          <h2 className="mb-4 text-lg font-bold text-foreground">Contribution readiness</h2>
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
          {project.connection && (
            <p className="mt-4 border-t border-border pt-4 text-sm leading-relaxed text-foreground-secondary">
              {project.connection === "south_african"
                ? "This project is based in South Africa or maintained by a South African team."
                : "This project's South African connection has been verified by the Code Masters community."}
            </p>
          )}
        </section>
      </div>

      {isMaintainer && (
        <div className="mt-8">
          <MaintainersPanel
            project={project}
            isMaintainer={isMaintainer}
            onMaintainerAdded={handleMaintainerAdded}
            onMaintainerRemoved={handleMaintainerRemoved}
          />
        </div>
      )}

      <section className="mt-8 rounded-[10px] border border-border bg-surface p-6">
        <h2 className="mb-4 text-lg font-bold text-foreground">Recent comments</h2>
        {project.recentComments?.length > 0 ? (
          <ul className="flex flex-col">
            {project.recentComments.map((comment) => (
              <li key={comment.id} className="flex gap-3 border-b border-border py-4 first:pt-0 last:border-b-0 last:pb-0">
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
    </main>
  );
}
