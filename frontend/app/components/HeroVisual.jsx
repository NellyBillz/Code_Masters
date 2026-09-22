"use client";

import { motion } from "framer-motion";
import { useTheme } from "../context/ThemeContext";

export default function HeroVisual() {
  const { theme } = useTheme();
  const isDark = theme === "dark";

  return (
    <div
      style={{
        position: "relative",
        width: "100%",
        maxWidth: "320px",
        aspectRatio: "1 / 1",
        marginLeft: "auto",
      }}
    >
      {/* Dark-mode-only dusk skyline, built from CSS shapes — a stand-in for
          the reference photo, since no real Cape Town asset is available
          here and hot-linking a stock photo into product code isn't safe
          to do without a license. */}
      {isDark && (
        <svg
          aria-hidden="true"
          viewBox="0 0 320 320"
          style={{ position: "absolute", inset: 0, opacity: 0.5 }}
        >
          <polygon points="0,320 40,180 70,230 110,140 150,220 320,320" fill="#1A2430" />
          <polygon points="180,320 210,200 240,260 270,190 320,260 320,320" fill="#20303E" />
        </svg>
      )}

      <motion.div
        animate={{ y: [0, -10, 0], rotate: [0, 1.5, 0] }}
        transition={{ duration: 6, repeat: Infinity, ease: "easeInOut" }}
        style={{
          position: "absolute",
          inset: "12%",
          borderRadius: "28px",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          background: isDark
            ? "linear-gradient(160deg, rgba(255,255,255,0.09), rgba(255,255,255,0.02))"
            : "linear-gradient(160deg, #FFFFFF, #F5F5F0)",
          border: isDark ? "1px solid rgba(255,255,255,0.18)" : "1px solid var(--cm-border)",
          backdropFilter: isDark ? "blur(16px)" : "none",
          boxShadow: isDark
            ? "0 0 40px rgba(200,255,100,0.18), 0 0 70px rgba(45,212,191,0.12), 0 20px 60px rgba(0,0,0,0.4)"
            : "0 20px 40px rgba(10,10,10,0.08)",
        }}
      >
        <svg width="46%" height="46%" viewBox="0 0 24 24" fill="none" aria-hidden="true">
          <path
            d="M2 3l7.5 8L2 19"
            stroke={isDark ? "#F5F5F5" : "#0A0A0A"}
            strokeWidth="2.4"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
          <path
            d="M22 3l-7.5 8L22 19"
            stroke={isDark ? "#F5F5F5" : "#0A0A0A"}
            strokeWidth="2.4"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      </motion.div>

      {isDark && (
        <div
          style={{
            position: "absolute",
            bottom: "10%",
            right: "6%",
            display: "flex",
            alignItems: "center",
            gap: "6px",
            fontSize: "11px",
            color: "rgba(255,255,255,0.75)",
          }}
        >
          <span
            aria-hidden="true"
            style={{ width: "6px", height: "6px", borderRadius: "50%", background: "var(--cm-orange)" }}
          />
          Cape Town
        </div>
      )}
    </div>
  );
}
