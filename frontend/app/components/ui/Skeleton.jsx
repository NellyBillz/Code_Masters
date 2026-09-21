/**
 * Bare pulsing block — compose into layout-shaped skeletons (design brief
 * §22: skeletons mirror the final layout, not a generic spinner).
 */
export function Skeleton({ className = "" }) {
  return <div className={`animate-pulse rounded-md bg-surface-subtle ${className}`} />;
}

export function ProjectCardSkeleton() {
  return (
    <div className="flex h-full flex-col gap-3 rounded-[10px] border border-border bg-surface p-5">
      <div className="flex items-start justify-between gap-3">
        <Skeleton className="h-5 w-2/3" />
        <Skeleton className="h-5 w-20 rounded-full" />
      </div>
      <Skeleton className="h-4 w-full" />
      <Skeleton className="h-4 w-4/5" />
      <div className="flex gap-3">
        <Skeleton className="h-4 w-16" />
        <Skeleton className="h-4 w-20" />
      </div>
      <div className="mt-auto flex justify-between border-t border-border pt-3">
        <Skeleton className="h-4 w-32" />
        <Skeleton className="h-4 w-20" />
      </div>
    </div>
  );
}

export function IssueRowSkeleton() {
  return (
    <div className="flex items-start justify-between gap-4 border-b border-border py-4 last:border-b-0">
      <div className="min-w-0 flex-1">
        <Skeleton className="h-5 w-16 rounded-full" />
        <Skeleton className="mt-2 h-4 w-3/4" />
        <Skeleton className="mt-2 h-3.5 w-1/3" />
      </div>
    </div>
  );
}
