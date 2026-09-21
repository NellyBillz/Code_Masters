import Link from "next/link";
import { Star, Users, CircleDot, Sparkles } from "lucide-react";
import ConnectionBadge from "./ui/ConnectionBadge";
import Badge from "./ui/Badge";
import { LANGUAGE_COLORS, formatRelativeTime, formatCount } from "../../lib/format";

/**
 * Project discovery card (design brief §37). Title first, description
 * second, technology third, metrics last — one card definition reused
 * everywhere a project is listed (homepage, /projects), per §34's "do not
 * create separate visually inconsistent versions of the same component."
 */
export default function ProjectCard({ project }) {
  const {
    id,
    name = "Untitled project",
    description = "",
    primaryLanguage,
    languages = [],
    category,
    tags = [],
    stars = 0,
    contributors = 0,
    openIssues = 0,
    lastActivityAt,
    connection,
    hasBeginnerFriendlyIssues = false,
    acceptingContributions = true,
  } = project || {};

  const languageColor = LANGUAGE_COLORS[primaryLanguage] || "#94a3b8";
  const techBadges = [primaryLanguage, ...languages.filter((l) => l !== primaryLanguage)]
    .filter(Boolean)
    .slice(0, 3);
  const activity = formatRelativeTime(lastActivityAt);

  return (
    <Link
      href={`/projects/${id}`}
      className="group flex h-full flex-col gap-3 rounded-[10px] border border-border bg-surface p-5 transition-colors hover:border-border-strong hover:bg-surface-subtle"
    >
      <div className="flex items-start justify-between gap-3">
        <h3 className="text-[17px] font-semibold leading-tight text-foreground group-hover:text-primary">
          {name}
        </h3>
        <ConnectionBadge connection={connection} />
      </div>

      {description && (
        <p className="line-clamp-2 text-sm leading-relaxed text-foreground-secondary">
          {description}
        </p>
      )}

      {(techBadges.length > 0 || category) && (
        <div className="flex flex-wrap items-center gap-x-3 gap-y-1.5 text-[13px] text-foreground-muted">
          {category && <span>{category}</span>}
          {techBadges.map((lang) => (
            <span key={lang} className="inline-flex items-center gap-1.5">
              <span
                className="h-2 w-2 rounded-full"
                style={{ backgroundColor: LANGUAGE_COLORS[lang] || languageColor }}
                aria-hidden="true"
              />
              {lang}
            </span>
          ))}
        </div>
      )}

      {tags.length > 0 && (
        <div className="flex flex-wrap gap-1.5">
          {tags.slice(0, 4).map((tag) => (
            <Badge key={tag} tone="neutral">
              {tag}
            </Badge>
          ))}
        </div>
      )}

      <div className="mt-auto flex flex-col gap-2 border-t border-border pt-3">
        {hasBeginnerFriendlyIssues && (
          <span className="inline-flex w-fit items-center gap-1 text-[13px] font-medium text-success">
            <Sparkles size={13} strokeWidth={2} aria-hidden="true" />
            Beginner-friendly issues open
          </span>
        )}

        <div className="flex flex-wrap items-center justify-between gap-x-4 gap-y-1 text-[13px] text-foreground-muted">
          <div className="flex items-center gap-3.5 tabular-nums">
            <span className="inline-flex items-center gap-1" title="Stars">
              <Star size={14} strokeWidth={1.75} aria-hidden="true" />
              {formatCount(stars)}
            </span>
            <span className="inline-flex items-center gap-1" title="Contributors">
              <Users size={14} strokeWidth={1.75} aria-hidden="true" />
              {formatCount(contributors)}
            </span>
            <span className="inline-flex items-center gap-1" title="Open issues">
              <CircleDot size={14} strokeWidth={1.75} aria-hidden="true" />
              {formatCount(openIssues)} open
            </span>
          </div>
          {activity && <span>{activity}</span>}
        </div>

        {!acceptingContributions && (
          <p className="text-[13px] text-foreground-muted">
            Maintainer has paused new contributors for now.
          </p>
        )}
      </div>
    </Link>
  );
}
