"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { Search, Cloud, ChevronDown } from "lucide-react";
import Logo from "./Logo";
import AuthStatus from "./AuthStatus";
import ThemeToggle from "./ThemeToggle";
import LiveClock from "./LiveClock";

export default function SiteHeader() {
  const router = useRouter();
  const [query, setQuery] = useState("");

  function handleSearchSubmit(event) {
    event.preventDefault();

    // Empty search: send them to general discovery rather than a /search
    // page that can't call the API anyway (q has a 2-character minimum).
    router.push(query.trim() ? `/search?q=${encodeURIComponent(query.trim())}` : "/projects");
  }

  return (
    <header
      style={{
        display: "flex",
        alignItems: "center",
        gap: "18px",
        padding: "14px 20px",
      }}
    >
      <Logo size={32} />

      <form
        onSubmit={handleSearchSubmit}
        role="search"
        className="cm-glass"
        style={{
          flex: 1,
          maxWidth: "460px",
          margin: "0 auto",
          display: "flex",
          alignItems: "center",
          gap: "10px",
          borderRadius: "999px",
          padding: "9px 16px",
        }}
      >
        <Search size={15} strokeWidth={1.8} color="var(--cm-text-muted)" aria-hidden="true" />
        <input
          type="search"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="Search projects, issues, or technologies..."
          aria-label="Search projects, issues, or technologies"
          style={{
            border: "none",
            outline: "none",
            background: "transparent",
            fontSize: "13px",
            color: "var(--cm-text-primary)",
            width: "100%",
          }}
        />
        <kbd
          aria-hidden="true"
          style={{
            fontSize: "10px",
            color: "var(--cm-text-muted)",
            border: "0.5px solid var(--cm-border)",
            borderRadius: "5px",
            padding: "1px 5px",
          }}
        >
          ⌘K
        </kbd>
      </form>

      <div style={{ display: "flex", alignItems: "center", gap: "14px", flexShrink: 0 }}>
        <button
          type="button"
          className="cm-glass"
          aria-label="Region: South Africa"
          style={{
            display: "flex",
            alignItems: "center",
            gap: "6px",
            borderRadius: "999px",
            padding: "6px 12px",
            fontSize: "12px",
            color: "var(--cm-text-primary)",
            cursor: "pointer",
          }}
        >
          <span aria-hidden="true">🇿🇦</span>
          ZA
          <ChevronDown size={12} strokeWidth={2} aria-hidden="true" />
        </button>

        {/* Static placeholder, no weather API wired up yet */}
        <span
          style={{
            display: "flex",
            alignItems: "center",
            gap: "6px",
            fontSize: "12px",
            color: "var(--cm-text-secondary)",
          }}
        >
          <Cloud size={14} strokeWidth={1.8} aria-hidden="true" />
          21°C
        </span>

        <LiveClock />

        <ThemeToggle />

        <AuthStatus />
      </div>
    </header>
  );
}
