import Link from "next/link";
import { Search } from "lucide-react";
import Logo from "./Logo";
import AuthStatus from "./AuthStatus";
import ThemeToggle from "./ThemeToggle";

const NAV_LINKS = [
  { href: "/projects", label: "Projects" },
];

/**
 * Header — plain, theme-following chrome (light in light mode, dark in dark
 * mode) matching main's actual header (`layout.js`: a bare bordered bar),
 * rather than a bar that stays permanently dark regardless of theme.
 */
export default function Header() {
  return (
    <header className="sticky top-0 z-30 h-16 border-b border-border bg-surface">
      <div className="mx-auto flex h-full max-w-[1280px] items-center gap-6 px-4 sm:px-6 lg:px-8">
        <div className="flex items-center gap-6">
          <Logo />
          <nav aria-label="Primary" className="hidden items-center gap-1 sm:flex">
            {NAV_LINKS.map((link) => (
              <Link
                key={link.href}
                href={link.href}
                className="rounded-md px-3 py-1.5 text-sm font-medium text-foreground-muted transition-colors hover:bg-surface-subtle hover:text-foreground"
              >
                {link.label}
              </Link>
            ))}
          </nav>
        </div>

        <Link
          href="/projects"
          className="ml-auto hidden min-w-0 flex-1 max-w-sm items-center gap-2 rounded-md border border-border bg-surface-subtle/60 px-3 py-1.5 text-sm text-foreground-muted transition-colors hover:border-border-strong hover:text-foreground md:flex"
        >
          <Search size={15} strokeWidth={1.75} aria-hidden="true" />
          Search projects and issues…
        </Link>

        <div className="ml-auto flex items-center gap-2 sm:ml-0">
          <Link
            href="/projects"
            className="rounded-md p-2 text-foreground-muted transition-colors hover:bg-surface-subtle hover:text-foreground md:hidden"
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
