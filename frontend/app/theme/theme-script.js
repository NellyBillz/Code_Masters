/**
 * Applies the persisted theme (or system preference) to <html> before
 * hydration, so there is no flash of the wrong theme. Rendered inline in
 * the document <head> — see app/layout.js.
 */
export const THEME_STORAGE_KEY = "codemasters-theme";

export function themeInitScript() {
  return `(function () {
    try {
      var key = ${JSON.stringify(THEME_STORAGE_KEY)};
      var stored = localStorage.getItem(key);
      if (stored === "light" || stored === "dark") {
        document.documentElement.setAttribute("data-theme", stored);
      }
    } catch (e) {}
  })();`;
}
