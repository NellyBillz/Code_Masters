import Link from "next/link";
import { ArrowRight } from "lucide-react";
import DifficultyBadge from "./ui/DifficultyBadge";
import Badge from "./ui/Badge";

/**
 * Compact contribution-opportunity row (design brief §12/§15). Reused
 * wherever issues are listed outside their own project's issue tab —
 * homepage, search results.
 */
export default function IssueRow({ issue, projectName }) {
  const { id, title, difficulty, labels = [], bodyExcerpt, status } = issue || {};

  return (
    <Link
      href={`/issues/${id}`}
      className="group flex items-start justify-between gap-4 border-b border-border py-4 transition-colors last:border-b-0 hover:bg-surface-subtle/50 sm:px-2 sm:-mx-2 sm:rounded-md"
    >
      <div className="min-w-0 flex-1">
        <div className="mb-1.5 flex flex-wrap items-center gap-1.5">
          <DifficultyBadge difficulty={difficulty} />
          {labels.slice(0, 2).map((label) => (
            <Badge key={label} tone="neutral">
              {label}
            </Badge>
          ))}
        </div>

        <h3 className="text-[15px] font-medium leading-snug text-foreground group-hover:text-primary">
          {title}
        </h3>

        {projectName && (
          <p className="mt-0.5 text-[13px] text-foreground-muted">{projectName}</p>
        )}

        {bodyExcerpt && (
          <p className="mt-1 line-clamp-1 text-[13px] text-foreground-secondary">
            {bodyExcerpt}
          </p>
        )}

        {status && (
          <p className="mt-1 text-[12px] capitalize text-foreground-disabled">{status}</p>
        )}
      </div>

      <ArrowRight
        size={16}
        strokeWidth={1.75}
        aria-hidden="true"
        className="mt-1 flex-shrink-0 text-foreground-disabled transition-transform group-hover:translate-x-0.5 group-hover:text-primary"
      />
    </Link>
  );
}
