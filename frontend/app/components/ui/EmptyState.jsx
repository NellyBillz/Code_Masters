export default function EmptyState({ title, description, action }) {
  return (
    <div className="rounded-[10px] border border-dashed border-border-strong bg-surface-subtle/40 p-8 text-center">
      <h3 className="text-[15px] font-semibold text-foreground">{title}</h3>
      {description && (
        <p className="mx-auto mt-1.5 max-w-sm text-sm text-foreground-muted">{description}</p>
      )}
      {action && <div className="mt-4 flex justify-center">{action}</div>}
    </div>
  );
}
