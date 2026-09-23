"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Home, Compass, GitPullRequest, BookOpen, Users, Sparkles, ShieldCheck, ShieldAlert } from "lucide-react";
import { useAuth } from "../context/AuthContext";

// All primary nav items now map to real routes.
const NAV_ITEMS = [
  { href: "/", label: "Home", icon: Home, enabled: true },
  { href: "/projects", label: "Explore", icon: Compass, enabled: true },
  { href: "/contribute", label: "Contribute", icon: GitPullRequest, enabled: true },
  { href: "/learn", label: "Learn", icon: BookOpen, enabled: true },
  { href: "/community", label: "Community", icon: Users, enabled: true },
  { href: "/impact", label: "Impact", icon: Sparkles, enabled: true },
];

export default function Sidebar() {
  const pathname = usePathname();
  const { user } = useAuth();

  // The moderation queue link only exists in this array for a confirmed
  // site admin (user.isSiteAdmin, from GET /users/me), it's appended here,
  // not rendered-but-disabled like the placeholders above, so it's genuinely
  // absent from the DOM for everyone else, not just visually hidden.
  const navItems = user?.isSiteAdmin
    ? [
        ...NAV_ITEMS,
        { href: "/admin/projects", label: "Admin queue", icon: ShieldCheck, enabled: true },
        { href: "/admin/reports", label: "Abuse reports", icon: ShieldAlert, enabled: true },
      ]
    : NAV_ITEMS;

  return (
    <nav
      aria-label="Primary"
      className="hidden md:flex"
      style={{
        flexDirection: "column",
        alignItems: "center",
        gap: "8px",
        width: "68px",
        flexShrink: 0,
        padding: "16px 10px",
        background: "var(--cm-sidebar)",
        borderRadius: "999px",
        position: "sticky",
        top: "14px",
        height: "fit-content",
        maxHeight: "calc(100vh - 28px)",
      }}
    >
      {navItems.map(({ href, label, icon: Icon, enabled }) => {
        const active = enabled && (href === "/" ? pathname === "/" : pathname.startsWith(href));

        const itemStyle = {
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          width: "44px",
          height: "44px",
          borderRadius: "999px",
          background: active ? "#FFFFFF" : "transparent",
          color: active ? "#0A0A0A" : enabled ? "#FFFFFF" : "rgba(255,255,255,0.35)",
        };

        if (!enabled) {
          return (
            <span key={href} title={`${label}, coming soon`} style={itemStyle}>
              <Icon size={19} strokeWidth={1.9} />
            </span>
          );
        }

        return (
          <Link key={href} href={href} aria-label={label} title={label} style={itemStyle}>
            <Icon size={19} strokeWidth={1.9} />
          </Link>
        );
      })}

      <div style={{ flex: 1 }} />

      <Link
        href={user ? "/profile" : "/auth/github"}
        aria-label={user ? "Your profile" : "Sign in"}
        title={user ? user.displayName || user.username : "Sign in"}
        style={{ marginTop: "8px" }}
      >
        {user?.avatarUrl ? (
          // eslint-disable-next-line @next/next/no-img-element -- external, size-variable avatar URL
          <img
            src={user.avatarUrl}
            alt=""
            width={36}
            height={36}
            style={{ borderRadius: "50%", display: "block" }}
          />
        ) : (
          <span
            aria-hidden="true"
            style={{
              width: "36px",
              height: "36px",
              borderRadius: "50%",
              background: "rgba(255,255,255,0.12)",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              color: "#FFFFFF",
              fontSize: "13px",
              fontWeight: 500,
            }}
          >
            {user?.displayName?.[0]?.toUpperCase() || user?.username?.[0]?.toUpperCase() || "?"}
          </span>
        )}
      </Link>
    </nav>
  );
}