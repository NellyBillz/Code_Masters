"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
} from "react";

const STORAGE_KEY = "cm-theme";

const ThemeContext = createContext({
  theme: "light",
  toggleTheme: () => {},
});

function getStoredTheme() {
  const stored = window.localStorage.getItem(STORAGE_KEY);
  if (stored === "dark" || stored === "light") return stored;

  return window.matchMedia("(prefers-color-scheme: dark)").matches
    ? "dark"
    : "light";
}

export function ThemeProvider({ children }) {
  // Always starts as "light" so the server render and the client's first
  // (hydration) render match exactly. The real theme, which depends on
  // localStorage/matchMedia and is only knowable client-side, is applied
  // in the effect below, after hydration has already succeeded.
  const [theme, setTheme] = useState("light");

  useEffect(() => {
    setTheme(getStoredTheme());
  }, []);

  // Apply the class + persist whenever theme changes (including the
  // initial value, so the <html> class matches on first client render).
  useEffect(() => {
    document.documentElement.classList.toggle("dark", theme === "dark");
    window.localStorage.setItem(STORAGE_KEY, theme);
  }, [theme]);

  const toggleTheme = useCallback(() => {
    setTheme((current) => (current === "dark" ? "light" : "dark"));
  }, []);

  return (
    <ThemeContext.Provider value={{ theme, toggleTheme }}>
      {children}
    </ThemeContext.Provider>
  );
}

export function useTheme() {
  return useContext(ThemeContext);
}
