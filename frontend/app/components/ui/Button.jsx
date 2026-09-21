import Link from "next/link";

/**
 * Shared button primitive. Three levels only (design brief §35) — do not
 * introduce a fourth visual weight; compose with `className` for one-off
 * layout needs (width, margin), not for new color treatments.
 */
const VARIANT_CLASSES = {
  primary:
    "bg-primary text-primary-foreground border border-transparent hover:bg-primary-hover",
  secondary:
    "bg-surface text-foreground border border-border-strong hover:bg-surface-subtle",
  ghost:
    "bg-transparent text-foreground-secondary border border-transparent hover:bg-surface-subtle hover:text-foreground",
};

const SIZE_CLASSES = {
  md: "h-10 px-4 text-sm gap-2",
  sm: "h-8 px-3 text-[13px] gap-1.5",
};

export default function Button({
  as,
  href,
  variant = "primary",
  size = "md",
  className = "",
  children,
  ...rest
}) {
  const classes = `inline-flex items-center justify-center whitespace-nowrap rounded-md font-medium transition-colors disabled:cursor-not-allowed disabled:opacity-50 ${VARIANT_CLASSES[variant]} ${SIZE_CLASSES[size]} ${className}`;

  if (href) {
    return (
      <Link href={href} className={classes} {...rest}>
        {children}
      </Link>
    );
  }

  const Component = as || "button";
  return (
    <Component className={classes} {...rest}>
      {children}
    </Component>
  );
}
