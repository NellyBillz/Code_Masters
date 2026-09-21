import Link from "next/link";
import { ArrowLeft, ArrowUpRight } from "lucide-react";
import { getIssue } from "../../../lib/api";
import IssueComments from "../../components/IssueComments";
import ClaimPanel from "../../components/ClaimPanel";
import IssueMaintainerOverride from "../../components/IssueMaintainerOverride";
import DifficultyBadge from "../../components/ui/DifficultyBadge";
import Badge from "../../components/ui/Badge";
import Button from "../../components/ui/Button";

export default async function IssueDetailPage({ params }) {
  const { issueId } = await params;
  const issue = await getIssue(issueId);

  return (
    <main className="mx-auto max-w-[1280px] px-4 py-10 sm:px-6 lg:px-8">
      <Link
        href={`/projects/${issue.project.id}`}
        className="inline-flex items-center gap-1.5 text-sm font-medium text-foreground-muted transition-colors hover:text-foreground"
      >
        <ArrowLeft size={15} strokeWidth={1.75} aria-hidden="true" />
        {issue.project.name}
      </Link>

      <div className="mt-8 grid grid-cols-1 gap-10 lg:grid-cols-[1fr_320px] lg:gap-16">
        <div className="min-w-0 max-w-[760px]">
          <div className="mb-3 flex flex-wrap items-center gap-1.5">
            <DifficultyBadge difficulty={issue.difficulty} />
            {issue.labels?.slice(0, 5).map((label) => (
              <Badge key={label} tone="neutral">
                {label}
              </Badge>
            ))}
            <span className="ml-1 text-sm capitalize text-foreground-muted">{issue.status}</span>
          </div>

          <h1 className="font-display text-[1.75rem] font-extrabold leading-tight tracking-tight text-foreground sm:text-[2rem]">
            {issue.title}
          </h1>

          {issue.bodyExcerpt && (
            <p className="mt-4 whitespace-pre-wrap text-[15px] leading-relaxed text-foreground-secondary">
              {issue.bodyExcerpt}
            </p>
          )}

          <div className="mt-12 border-t border-border pt-10">
            <IssueComments issueId={issueId} />
          </div>
        </div>

        <aside className="flex flex-col gap-6 lg:sticky lg:top-24 lg:h-fit">
          <div className="rounded-[10px] border border-border bg-surface p-5">
            <h2 className="mb-3 text-[13px] font-semibold uppercase tracking-wide text-foreground-muted">
              Contribution
            </h2>

            <dl className="flex flex-col gap-2 text-sm">
              <div className="flex items-center justify-between">
                <dt className="text-foreground-muted">Status</dt>
                <dd className="font-medium capitalize text-foreground">{issue.status}</dd>
              </div>
              {issue.isBeginnerFriendly && (
                <div className="text-[13px] font-medium text-success">Beginner-friendly</div>
              )}
            </dl>

            <Button
              href={issue.githubUrl}
              target="_blank"
              rel="noreferrer"
              variant="primary"
              className="mt-5 w-full"
            >
              Contribute on GitHub
              <ArrowUpRight size={15} strokeWidth={1.75} aria-hidden="true" />
            </Button>

            <div className="mt-5 border-t border-border pt-5">
              <ClaimPanel issueId={issueId} initialClaims={issue.claims || []} />
            </div>
          </div>

          <IssueMaintainerOverride issue={issue} />
        </aside>
      </div>
    </main>
  );
}
