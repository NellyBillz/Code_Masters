"use client";

import { Sun, Moon } from "lucide-react";
import { useTheme } from "../context/ThemeContext";

export default function ThemeToggle() {
  const { theme, toggleTheme } = useTheme();
  const isDark = theme === "dark";

  return (
    <button
      type="button"
      onClick={toggleTheme}
      role="switch"
      aria-checked={isDark}
      aria-label={isDark ? "Switch to light mode" : "Switch to dark mode"}
      style={{
        position: "relative",
        display: "inline-flex",
        alignItems: "center",
        width: "52px",
        height: "28px",
        borderRadius: "999px",
        padding: "3px",
        border: "0.5px solid var(--cm-border)",
        background: isDark ? "#141414" : "var(--cm-surface-alt)",
        cursor: "pointer",
        flexShrink: 0,
      }}
    >
      <span
        aria-hidden="true"
        style={{
          width: "22px",
          height: "22px",
          borderRadius: "50%",
          background: isDark ? "var(--cm-lime)" : "#0A0A0A",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          color: isDark ? "#0A0A0A" : "#FFFFFF",
          transform: isDark ? "translateX(24px)" : "translateX(0)",
          transition: "transform 0.18s ease",
        }}
      >
        {isDark ? <Moon size={12} strokeWidth={2} /> : <Sun size={12} strokeWidth={2} />}
      </span>
    </button>
  );
}
