/**
 * Learn engine helpers, ported from the original single-file app.
 *
 * runJS / checkChallenge / caesarShift / bumpStreak / todayStr are pure
 * logic and are unchanged from the original.
 *
 * Everything else is new, replacing sandbox-only APIs that don't exist in
 * a real deployment:
 *   - window.storage (Anthropic Artifacts sandbox only) -> localStorage,
 *     namespaced per logged-in Code Masters user (so switching GitHub
 *     accounts on the same browser doesn't mix progress).
 *   - a direct browser fetch() to api.anthropic.com with no key (works
 *     only inside the Artifacts sandbox, which injects it) -> a same-origin
 *     call to /api/learn/ai, a Next.js route that holds the real API key
 *     server-side and degrades honestly when one isn't configured.
 *   - the original "shared leaderboard" (window.storage.list, also sandbox
 *     only) -> an honestly-labeled *local* leaderboard: there is no backend
 *     endpoint for a real cross-user leaderboard, so this only
 *     ever shows profiles that have used Learn in this same browser.
 */

/* ---------- pure logic, unchanged from the original ---------- */

export function runJS(code) {
  const logs = [];
  const fake = {
    log: (...a) => logs.push(a.map((x) => (typeof x === "object" ? JSON.stringify(x) : String(x))).join(" ")),
    error: (...a) => logs.push("ERR " + a.join(" ")),
    warn: (...a) => logs.push("WARN " + a.join(" ")),
  };
  try {
    // eslint-disable-next-line no-new-func
    new Function("console", '"use strict";\n' + code)(fake);
    return { ok: true, logs };
  } catch (e) {
    return { ok: false, logs, error: String(e) };
  }
}

export function checkChallenge(challenge, code) {
  if (challenge.type === "js") {
    const res = runJS(code);
    if (!res.ok) return { pass: false, logs: res.logs, error: res.error };
    const out = res.logs.map((s) => s.trim());
    const want = challenge.expectOutput;
    const pass = want.length === out.length && want.every((w, i) => out[i] === w);
    return { pass, logs: res.logs };
  }
  if (challenge.type === "pattern" || challenge.type === "html") {
    const pass = challenge.mustMatch.every((rx) => new RegExp(rx, challenge.flags || "").test(code));
    return { pass, logs: [] };
  }
  if (challenge.type === "text") {
    const norm = code.trim().toLowerCase().replace(/\s+/g, " ");
    return { pass: challenge.answer.some((a) => a === norm), logs: [] };
  }
  return { pass: false, logs: [] };
}

export function caesarShift(text, shift) {
  return text.replace(/[a-zA-Z]/g, (ch) => {
    const base = ch <= "Z" ? 65 : 97;
    return String.fromCharCode(((ch.charCodeAt(0) - base - shift + 26 * 4) % 26) + base);
  });
}

export const todayStr = () => new Date().toISOString().slice(0, 10);

export function bumpStreak(p) {
  const today = todayStr();
  if (p.lastActive === today) return p;
  const yesterday = new Date(Date.now() - 86400000).toISOString().slice(0, 10);
  const streak = p.lastActive === yesterday ? (p.streak || 0) + 1 : 1;
  return { ...p, streak, lastActive: today };
}

/* ---------- persistence: localStorage, keyed per real Code Masters user ---------- */

function profileKey(username) {
  return `codemasters:profile:${username}`;
}

// Synchronous is fine (localStorage is sync); kept as async functions so
// call sites (ported straight from the original's await-based storage
// calls) didn't need to change shape.
export async function loadProfile(username) {
  if (!username || typeof window === "undefined") return null;
  try {
    const raw = window.localStorage.getItem(profileKey(username));
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export async function saveProfile(username, p) {
  if (!username || typeof window === "undefined") return;
  try {
    window.localStorage.setItem(profileKey(username), JSON.stringify(p));
  } catch {
    /* storage full or unavailable (e.g. private browsing) — non-fatal */
  }
}

/* ---------- local-only leaderboard ----------
 * Honest scope: this reads every Learn profile saved in *this browser's*
 * localStorage. There is no backend endpoint to aggregate XP across real
 * users, so this never claims to be a global/shared ranking — the UI copy
 * in Leaderboard.jsx says "this device" explicitly. */

export async function fetchLocalLeaderboard() {
  if (typeof window === "undefined") return [];
  const rows = [];
  try {
    for (let i = 0; i < window.localStorage.length; i++) {
      const key = window.localStorage.key(i);
      if (!key || !key.startsWith("codemasters:profile:")) continue;
      try {
        const p = JSON.parse(window.localStorage.getItem(key));
        if (p && typeof p.xp === "number") {
          rows.push({ name: p.name, xp: p.xp, level: null, username: key.slice("codemasters:profile:".length) });
        }
      } catch {
        /* skip a corrupted entry */
      }
    }
  } catch {
    return [];
  }
  return rows.sort((a, b) => b.xp - a.xp).slice(0, 20);
}

/* ---------- AI features: server-side proxy, degrades honestly ---------- */

/**
 * Calls the /api/learn/ai proxy instead of the browser hitting
 * api.anthropic.com directly (the original approach only worked inside the
 * Anthropic Artifacts sandbox, which injects a key with no server of its
 * own — that doesn't exist in a real deployment).
 *
 * Throws (matching the original askClaude's contract, so every call site
 * ported from the original's try/catch continues to work unmodified) when
 * the proxy reports the feature isn't configured, or the request fails.
 */
export async function askTutor(prompt) {
  const resp = await fetch("/api/learn/ai", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ prompt }),
  });
  let data;
  try {
    data = await resp.json();
  } catch {
    throw new Error("AI tutor is unavailable right now.");
  }
  if (!resp.ok || data.error) {
    throw new Error(data.error || "AI tutor is unavailable right now.");
  }
  return data.text;
}

// True once at import time in the browser only if we've already learned
// (via a failed/short-circuited call) that no key is configured; views use
// this to show a persistent "AI features are off" notice instead of
// letting every single button silently fail one at a time. Populated by
// LearnApp on mount via a lightweight status check.
export async function checkAiAvailable() {
  try {
    const resp = await fetch("/api/learn/ai", { method: "GET" });
    const data = await resp.json();
    return !!data.available;
  } catch {
    return false;
  }
}
