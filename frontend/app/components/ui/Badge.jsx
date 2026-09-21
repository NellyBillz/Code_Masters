const TONE_CLASSES = {
  neutral: "bg-surface-subtle text-foreground-secondary",
  primary: "bg-primary-subtle text-primary",
  accent: "bg-accent-subtle text-accent",
  success: "bg-accent-subtle text-success",
  warning: "bg-surface-subtle text-warning",
  danger: "bg-surface-subtle text-danger",
};

/**
 * Compact metadata label. Reserved for tags, technology labels, difficulty,
 * status and connection/verification — not a general-purpose container
 * (design brief §36).
 */
export default function Badge({ tone = "neutral", icon: Icon, className = "", children, ...rest }) {
  return (
    <span
      className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-medium ${TONE_CLASSES[tone]} ${className}`}
      {...rest}
    >
      {Icon && <Icon size={12} strokeWidth={2} aria-hidden="true" />}
      {children}
    </span>
  );
}
