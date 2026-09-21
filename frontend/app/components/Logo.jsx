import Link from "next/link";

const TEXT_CLASSES = {
  default: "text-foreground",
  chrome: "text-chrome-fg",
};

/**
 * Text-first wordmark with a small geometric mark (design brief §11).
 * No mascots, no continent clipart, no generic `</>` icon.
 *
 * `variant="chrome"` is for placement on the persistent dark header bar,
 * where the theme-adaptive `text-foreground` token would be the wrong
 * color (the header stays dark in both light and dark theme).
 */
export default function Logo({ variant = "default", className = "" }) {
  return (
    <Link
      href="/"
      aria-label="Code Masters home"
      className={`flex flex-shrink-0 items-center gap-2 whitespace-nowrap font-display text-base font-bold tracking-tight ${TEXT_CLASSES[variant]} ${className}`}
    >
      <span
        aria-hidden="true"
        className="flex h-6 w-6 items-center justify-center rounded-md bg-primary text-primary-foreground"
      >
        <svg width="13" height="13" viewBox="0 0 13 13" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path
            d="M4.5 2.5 1 6.5l3.5 4M8.5 2.5 12 6.5l-3.5 4"
            stroke="currentColor"
            strokeWidth="1.4"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      </span>
      Code Masters
    </Link>
  );
}
