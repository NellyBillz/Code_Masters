/**
 * Learn section retro theme, re-skinned onto Code Masters design tokens.
 *
 * The original TechAway module (see techaway.jsx in the design handoff)
 * defined its own hardcoded hex palette (T) and a matching set of CSS
 * classes (.card, .btn, .px, .mono, .opt, ...). Every Learn view reads
 * its colors exclusively through T and those classes — no component
 * hardcodes a hex value for a "themeable" surface.
 *
 * That means we can re-theme the entire gamified experience onto Code
 * Masters' cm-* tokens (including automatic light/dark support, since
 * those tokens already flip under the .dark class from ThemeContext)
 * just by pointing T's slots at CSS var() strings instead of hex, and by
 * rewriting the .card/.btn/.opt rules below to use cm-glass/cm-surface
 * treatment instead of flat panel colors. Every quest card, XP bar,
 * badge, terminal and quiz option updates automatically — none of the
 * ~1,300 lines of ported view logic needed per-line color edits.
 *
 * A few slots (T.coral, T.green) don't have a direct Code Masters
 * equivalent and are kept as their original retro neon hex — they're
 * used for semantic pass/fail states (wrong answer, lab cleared), which
 * reads fine against either light or dark cm surfaces and preserves the
 * "retro gamified" identity the redesign is explicitly asked to keep.
 */

export const T = {
  ink: "var(--cm-bg)",
  panel: "var(--cm-card-bg)",
  panel2: "var(--cm-surface-alt)",
  line: "var(--cm-card-border)",
  text: "var(--cm-text-primary)",
  dim: "var(--cm-text-secondary)",
  // Primary retro accent (was amber) -> Code Masters' lime.
  amber: "var(--cm-lime)",
  // Secondary retro accent (was cyan) -> Code Masters' teal.
  cyan: "var(--cm-teal)",
  // Boss-fight / interview accent (was violet) -> Code Masters' orange.
  violet: "var(--cm-orange)",
  // No direct cm equivalent for these two semantic colors (danger / success) —
  // kept as the original retro neon so pass/fail states stay legible on
  // both light and dark cm surfaces.
  coral: "#FF5C7A",
  green: "#3FAE63",
};

// Text color to pair with a T.amber-colored (lime) button background —
// lime is a light, saturated color in both themes, so it always needs a
// dark foreground, unlike the retro amber the original was built for.
export const AMBER_ON = "#14210A";
export const CYAN_ON = "#04211C";
export const CORAL_ON = "#2A0010";

export const LEARN_STYLES = `
@import url('https://fonts.googleapis.com/css2?family=Silkscreen:wght@400;700&family=JetBrains+Mono:wght@400;600&display=swap');
.ta-root { font-family: inherit; color: ${T.text}; min-height: 100%; }
.ta-root .px { font-family: 'Silkscreen', monospace; letter-spacing: 0.5px; }
.ta-root .mono { font-family: 'JetBrains Mono', monospace; }
.ta-root .card {
  background: ${T.panel};
  border: 0.5px solid ${T.line};
  border-radius: 20px;
  backdrop-filter: var(--cm-card-blur);
  -webkit-backdrop-filter: var(--cm-card-blur);
}
.ta-root .btn {
  font-family: inherit; font-weight: 600; border-radius: 999px; border: 1px solid ${T.line};
  background: ${T.panel2}; color: ${T.text}; padding: 10px 18px; cursor: pointer; font-size: 14px;
  transition: transform .08s ease, filter .15s ease, opacity .15s ease;
}
.ta-root .btn:disabled { opacity: 0.45; cursor: not-allowed; }
.ta-root .btn:hover:not(:disabled) { filter: brightness(1.08); }
.ta-root .btn:active:not(:disabled) { transform: translateY(1px); }
.ta-root .btn:focus-visible, .ta-root input:focus-visible, .ta-root textarea:focus-visible {
  outline: 2px solid ${T.cyan}; outline-offset: 2px;
}
.ta-root .btn-amber { background: ${T.amber}; color: ${AMBER_ON}; border-color: ${T.amber}; }
.ta-root .btn-cyan { background: ${T.cyan}; color: ${CYAN_ON}; border-color: ${T.cyan}; }
.ta-root .btn-coral { background: ${T.coral}; color: ${CORAL_ON}; border-color: ${T.coral}; }
.ta-root .btn-ghost { background: transparent; }
.ta-root .navbtn {
  display: flex; align-items: center; gap: 8px; text-align: left; background: transparent;
  border: 1px solid transparent; color: ${T.dim}; padding: 9px 16px; border-radius: 999px;
  cursor: pointer; font-weight: 600; font-size: 13.5px; white-space: nowrap;
}
.ta-root .navbtn:hover { color: ${T.text}; background: ${T.panel2}; }
.ta-root .navbtn.active { color: ${AMBER_ON}; background: ${T.amber}; }
.ta-root input, .ta-root textarea { font-family: 'JetBrains Mono', monospace; }
.ta-root .xpfill { transition: width .6s cubic-bezier(.2,.9,.3,1.2); }
@keyframes taPop { 0% { transform: scale(.7); opacity: 0 } 60% { transform: scale(1.06) } 100% { transform: scale(1); opacity: 1 } }
@keyframes taRise { from { transform: translateY(14px); opacity: 0 } to { transform: translateY(0); opacity: 1 } }
@keyframes taBlink { 50% { opacity: .25 } }
.ta-root .pop { animation: taPop .35s ease both; }
.ta-root .rise { animation: taRise .3s ease both; }
.ta-root .cursor { animation: taBlink 1s step-end infinite; }
.ta-root .term {
  background: #070A14; border: 1px solid ${T.line}; border-radius: 14px; color: #EAECF8;
  background-image: repeating-linear-gradient(0deg, rgba(255,255,255,0.015) 0 1px, transparent 1px 3px);
}
.ta-root .opt {
  display: block; width: 100%; text-align: left; padding: 12px 14px; border-radius: 14px; border: 1px solid ${T.line};
  background: ${T.panel2}; color: ${T.text}; cursor: pointer; font-family: inherit; font-size: 14px; margin-bottom: 8px;
}
.ta-root .opt:hover:not(:disabled) { border-color: ${T.violet}; }
@media (prefers-reduced-motion: reduce) {
  .ta-root .pop, .ta-root .rise, .ta-root .cursor, .ta-root .xpfill { animation: none !important; transition: none !important; }
}
`;
