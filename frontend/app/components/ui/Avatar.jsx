const SIZE_CLASSES = {
  sm: "h-6 w-6 text-[11px]",
  md: "h-9 w-9 text-[13px]",
  lg: "h-14 w-14 text-lg",
};

export default function Avatar({ user, size = "md", className = "" }) {
  const label = user?.displayName || user?.username || "?";
  const sizeClass = SIZE_CLASSES[size] || SIZE_CLASSES.md;

  if (user?.avatarUrl) {
    return (
      // eslint-disable-next-line @next/next/no-img-element
      <img
        src={user.avatarUrl}
        alt=""
        className={`flex-shrink-0 rounded-full ${sizeClass} ${className}`}
      />
    );
  }

  return (
    <span
      aria-hidden="true"
      className={`flex flex-shrink-0 items-center justify-center rounded-full bg-surface-subtle font-semibold text-foreground-muted ${sizeClass} ${className}`}
    >
      {label.charAt(0).toUpperCase()}
    </span>
  );
}
