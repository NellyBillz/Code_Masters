"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Home, FolderKanban, Bug, Users } from "lucide-react";

// Only routes that exist today are real links. The rest render as
// visually-present but disabled icons so the sidebar reads complete
// without shipping dead links — wire these up as the pages land.
const NAV_ITEMS = [
  { href: "/", label: "Home", icon: Home, enabled: true },
  { href: "/projects", label: "Projects", icon: FolderKanban, enabled: true },
  { href: "/issues", label: "Issues", icon: Bug, enabled: false },
  { href: "/contributors", label: "Contributors", icon: Users, enabled: false },
];

export default function Sidebar() {
  const pathname = usePathname();

  return (
    <nav
      aria-label="Primary"
      style={{
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        gap: "18px",
        width: "56px",
        flexShrink: 0,
        padding: "16px 0",
        background: "var(--cm-sidebar)",
        borderRadius: "14px",
      }}
    >
      <Link
        href="/"
        aria-label="Code_Masters home"
        style={{
          width: "28px",
          height: "28px",
          borderRadius: "8px",
          background: "var(--cm-lime)",
          color: "#161611",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          fontWeight: 500,
          fontSize: "13px",
        }}
      >
        C
      </Link>

      {NAV_ITEMS.map(({ href, label, icon: Icon, enabled }) => {
        const active = enabled && (href === "/" ? pathname === "/" : pathname.startsWith(href));

        const iconStyle = {
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          width: "32px",
          height: "32px",
          borderRadius: "8px",
          background: active ? "var(--cm-lime)" : "transparent",
          color: active
            ? "#161611"
            : enabled
              ? "var(--cm-sidebar-icon)"
              : "color-mix(in srgb, var(--cm-sidebar-icon) 45%, transparent)",
        };

        if (!enabled) {
          return (
            <span key={href} title={`${label} — coming soon`} style={iconStyle}>
              <Icon size={18} strokeWidth={1.75} />
            </span>
          );
        }

        return (
          <Link key={href} href={href} aria-label={label} title={label} style={iconStyle}>
            <Icon size={18} strokeWidth={1.75} />
          </Link>
        );
      })}

      <div style={{ flex: 1 }} />
    </nav>
  );
}
