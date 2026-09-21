"use client";

import { useEffect, useState } from "react";
import { Monitor, Moon, Sun } from "lucide-react";
import { THEME_STORAGE_KEY } from "../theme/theme-script";

const OPTIONS = [
  { value: "light", label: "Light", icon: Sun },
  { value: "dark", label: "Dark", icon: Moon },
  { value: "system", label: "System", icon: Monitor },
];

function applyTheme(value) {
  const root = document.documentElement;
  if (value === "system") {
    root.removeAttribute("data-theme");
  } else {
    root.setAttribute("data-theme", value);
  }
}

export default function ThemeToggle() {
  // Always starts as "system" so the first client render matches the
  // server-rendered markup exactly — the server has no access to
  // localStorage, so reading it during the initial render (even
  // client-side, before mount) causes a hydration mismatch whenever a
  // theme was previously chosen. The <html> element itself already gets
  // the correct theme pre-paint via theme-script.js; this only syncs the
  // toggle's own icon/label after mount.
  const [theme, setTheme] = useState("system");
  const [open, setOpen] = useState(false);

  useEffect(() => {
    try {
      const stored = window.localStorage.getItem(THEME_STORAGE_KEY);
      if (stored === "light" || stored === "dark") {
        setTheme(stored);
      }
    } catch {
      // Private browsing / blocked storage — stay on "system".
    }
  }, []);

  function choose(value) {
    setTheme(value);
    setOpen(false);
    applyTheme(value);
    try {
      if (value === "system") {
        localStorage.removeItem(THEME_STORAGE_KEY);
      } else {
        localStorage.setItem(THEME_STORAGE_KEY, value);
      }
    } catch {
      // Private browsing / blocked storage — theme still applies for this load.
    }
  }

  const current = OPTIONS.find((option) => option.value === theme) || OPTIONS[2];
  const CurrentIcon = current.icon;

  return (
    <div className="relative">
      <button
        type="button"
        onClick={() => setOpen((value) => !value)}
        aria-haspopup="menu"
        aria-expanded={open}
        aria-label={`Theme: ${current.label}. Change theme`}
        className="flex h-9 w-9 items-center justify-center rounded-md border border-transparent text-chrome-fg-muted transition-colors hover:border-chrome-border hover:bg-chrome-hover hover:text-chrome-fg"
      >
        <CurrentIcon size={18} strokeWidth={1.75} aria-hidden="true" />
      </button>

      {open && (
        <>
          <button
            type="button"
            aria-hidden="true"
            tabIndex={-1}
            className="fixed inset-0 z-40 cursor-default"
            onClick={() => setOpen(false)}
          />
          <div
            role="menu"
            aria-label="Theme"
            className="absolute right-0 z-50 mt-2 w-36 overflow-hidden rounded-lg border border-border bg-surface-raised py-1 shadow-lg"
          >
            {OPTIONS.map((option) => {
              const Icon = option.icon;
              const active = option.value === theme;
              return (
                <button
                  key={option.value}
                  type="button"
                  role="menuitemradio"
                  aria-checked={active}
                  onClick={() => choose(option.value)}
                  className={`flex w-full items-center gap-2.5 px-3 py-2 text-sm transition-colors ${
                    active
                      ? "bg-primary-subtle text-primary"
                      : "text-foreground-secondary hover:bg-surface-subtle hover:text-foreground"
                  }`}
                >
                  <Icon size={16} strokeWidth={1.75} aria-hidden="true" />
                  {option.label}
                </button>
              );
            })}
          </div>
        </>
      )}
    </div>
  );
}
