import Link from "next/link";
import { Search } from "lucide-react";
import Logo from "./Logo";
import AuthStatus from "./AuthStatus";
import ThemeToggle from "./ThemeToggle";

const NAV_LINKS = [
  { href: "/projects", label: "Projects" },
];

/**
 * Persistent dark "chrome" header — the AWS Console / GitHub convention of
 * a structural dark bar that stays dark regardless of the site's light/dark
 * theme, rather than a navbar that just inverts with the page.
 */
export default function Header() {
  return (
    <header className="sticky top-0 z-30 h-16 border-b border-chrome-border bg-chrome-bg">
      <div className="mx-auto flex h-full max-w-[1280px] items-center gap-6 px-4 sm:px-6 lg:px-8">
        <div className="flex items-center gap-6">
          <Logo variant="chrome" />
          <nav aria-label="Primary" className="hidden items-center gap-1 sm:flex">
            {NAV_LINKS.map((link) => (
              <Link
                key={link.href}
                href={link.href}
                className="rounded-md px-3 py-1.5 text-sm font-medium text-chrome-fg-muted transition-colors hover:bg-chrome-hover hover:text-chrome-fg"
              >
                {link.label}
              </Link>
            ))}
          </nav>
        </div>

        <Link
          href="/projects"
          className="ml-auto hidden min-w-0 flex-1 max-w-sm items-center gap-2 rounded-md border border-chrome-border bg-chrome-hover/60 px-3 py-1.5 text-sm text-chrome-fg-muted transition-colors hover:border-chrome-fg-muted hover:text-chrome-fg md:flex"
        >
          <Search size={15} strokeWidth={1.75} aria-hidden="true" />
          Search projects and issues…
        </Link>

        <div className="ml-auto flex items-center gap-2 sm:ml-0">
          <Link
            href="/projects"
            className="rounded-md p-2 text-chrome-fg-muted transition-colors hover:bg-chrome-hover hover:text-chrome-fg md:hidden"
            aria-label="Search"
          >
            <Search size={18} strokeWidth={1.75} aria-hidden="true" />
          </Link>
          <ThemeToggle />
          <AuthStatus />
        </div>
      </div>
    </header>
  );
}
