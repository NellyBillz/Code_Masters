"use client";

import { useEffect, useRef, useState } from "react";
import { useAuth } from "../context/AuthContext";
import { T, LEARN_STYLES } from "./theme";
import { BADGES, allLessons, levelFromXp, titleForLevel } from "./data";
import { loadProfile, saveProfile, bumpStreak, todayStr, checkAiAvailable } from "./engine";
import { XPBar, Toast } from "./components/Pieces";
import TutorDrawer from "./components/TutorDrawer";
import Dashboard from "./views/Dashboard";
import CourseView from "./views/CourseView";
import LessonView from "./views/LessonView";
import QuizView from "./views/QuizView";
import ProjectView from "./views/ProjectView";
import { LabsHome, LabView } from "./views/Labs";
import InterviewGym from "./views/InterviewGym";
import Leaderboard from "./views/Leaderboard";
import BadgeGallery from "./views/BadgeGallery";

const NAV = [
  { id: "dash", icon: "🏠", label: "Command Deck" },
  { id: "labs", icon: "🧪", label: "Cyber Labs" },
  { id: "interview", icon: "🎤", label: "Interview Gym" },
  { id: "leaderboard", icon: "🏆", label: "Leaderboard" },
  { id: "badges", icon: "🎖️", label: "Badges" },
];

function GithubMark() {
  return (
    <svg width="14" height="14" viewBox="0 0 16 16" fill="currentColor" aria-hidden="true">
      <path d="M8 0C3.58 0 0 3.58 0 8a8 8 0 0 0 5.47 7.59c.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82a7.5 7.5 0 0 1 4 0c1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8 8 0 0 0 16 8c0-4.42-3.58-8-8-8z" />
    </svg>
  );
}

/**
 * Code Masters' Learn section, at /learn.
 *
 * This started life as a standalone prototype called "TechAway" and was
 * ported in near-verbatim; that separate identity has since been retired,
 * and everything here now presents itself simply as Learn, part of Code
 * Masters rather than a bolted-on product with its own name. Replaces the
 * original prototype's own onboarding + sidebar app-shell with:
 *  - real Code Masters auth (useAuth()) instead of a name-only local
 *    "pick a hacker alias" step — progress is now tied to your GitHub
 *    account (well, to this browser + your username; see engine.js for
 *    the honest scope of that), not a freeform string anyone could type.
 *  - the existing global Sidebar/SiteHeader chrome for primary nav
 *    (already rendered by app/layout.js), with a horizontal tab row here
 *    for Learn's own sub-sections, matching the tab pattern already
 *    used on /projects/[projectId].
 */
export default function LearnApp() {
  const { user, loading: authLoading } = useAuth();
  const [profile, setProfile] = useState(null);
  const [profileLoading, setProfileLoading] = useState(true);
  const [view, setView] = useState({ page: "dash" });
  const [toast, setToast] = useState(null);
  const [tutorOpen, setTutorOpen] = useState(false);
  const [tutorCtx, setTutorCtx] = useState("");
  const [aiAvailable, setAiAvailable] = useState(null);
  const toastTimer = useRef(null);

  useEffect(() => {
    checkAiAvailable().then(setAiAvailable);
  }, []);

  useEffect(() => {
    if (!user) {
      // eslint-disable-next-line react-hooks/set-state-in-effect -- syncing local profile state to auth state (e.g. sign-out); no derived-state alternative since profile is loaded asynchronously from storage
      setProfile(null);
      setProfileLoading(false);
      return;
    }
    let cancelled = false;
    (async () => {
      setProfileLoading(true);
      const p = await loadProfile(user.username);
      if (cancelled) return;
      if (p) {
        const bumped = bumpStreak(p);
        setProfile(bumped);
        if (bumped !== p) saveProfile(user.username, bumped);
      } else {
        // First visit for this account on this device/browser -- start a
        // fresh run automatically (no separate "pick a name" step; we
        // already know who they are from GitHub auth).
        const fresh = {
          name: user.displayName || user.username,
          xp: 0,
          streak: 1,
          lastActive: todayStr(),
          completed: {},
          badges: [],
          labFlags: [],
        };
        setProfile(fresh);
        saveProfile(user.username, fresh);
      }
      setProfileLoading(false);
    })();
    return () => {
      cancelled = true;
    };
  }, [user]);

  const showToast = (t) => {
    setToast(t);
    clearTimeout(toastTimer.current);
    toastTimer.current = setTimeout(() => setToast(null), 3200);
  };

  const persist = (p) => {
    setProfile(p);
    if (user) saveProfile(user.username, p);
  };

  /* central award: XP + completion + badges + level-up detection.
   * Logic ported verbatim from the original TechAway app shell. */
  const award = (itemId, xp, label, forceBadge = null, flagId = null, lab = null, patch = null) => {
    setProfile((prev) => {
      if (!prev) return prev;
      const p = bumpStreak({ ...prev });
      if (patch) Object.assign(p, patch);
      const before = levelFromXp(p.xp);
      p.xp += xp;
      p.completed = { ...p.completed, [itemId]: true };
      if (flagId) p.labFlags = [...(p.labFlags || []), flagId];
      const badges = new Set(p.badges || []);
      const doneChals = Object.keys(p.completed).filter((k) => allLessons.some((l) => l.id === k)).length;
      if (doneChals >= 1) badges.add("first-quest");
      if (doneChals >= 5) badges.add("combo-5");
      if (flagId) badges.add("flag-bearer");
      if (lab && lab.flags.every((f) => (p.labFlags || []).includes(f.id))) badges.add("lab-rat");
      if ((p.streak || 1) >= 3) badges.add("streak-3");
      if (levelFromXp(p.xp) >= 5) badges.add("level-5");
      const coursesTouched = new Set(allLessons.filter((l) => p.completed[l.id]).map((l) => l.courseId));
      if (coursesTouched.size >= 3) badges.add("polyglot");
      const certs = Object.keys(p.completed).filter((k) => k.startsWith("cert:")).length;
      if (certs >= 1) badges.add("graduate");
      if (forceBadge) badges.add(forceBadge);
      const newBadges = [...badges].filter((b) => !(prev.badges || []).includes(b));
      p.badges = [...badges];
      const after = levelFromXp(p.xp);
      if (after > before) {
        showToast({ icon: "🆙", title: `LEVEL UP! LV ${after} · ${titleForLevel(after)}`, sub: `+${xp} XP · ${label}`, color: T.amber });
      } else if (newBadges.length) {
        const nb = BADGES.find((b) => b.id === newBadges[0]);
        showToast({ icon: nb.icon, title: "BADGE UNLOCKED: " + nb.name.toUpperCase(), sub: `+${xp} XP · ${label}`, color: T.violet });
      } else {
        showToast({ icon: "✨", title: `+${xp} XP`, sub: label, color: T.cyan });
      }
      if (user) saveProfile(user.username, p);
      return p;
    });
  };

  if (authLoading || profileLoading) {
    return (
      <div className="ta-root" style={{ display: "flex", alignItems: "center", justifyContent: "center", minHeight: "50vh" }}>
        <style>{LEARN_STYLES}</style>
        <div className="px cursor" style={{ color: T.amber, fontSize: 16 }}>
          BOOTING TECHAWAY ▮
        </div>
      </div>
    );
  }

  if (!user) {
    return (
      <div className="ta-root">
        <style>{LEARN_STYLES}</style>
        <div className="card" style={{ maxWidth: 460, margin: "40px auto", padding: 32, textAlign: "center" }}>
          <div style={{ fontSize: 44 }}>🕹️</div>
          <h1 className="px" style={{ fontSize: 20, margin: "10px 0 4px", color: T.amber }}>
            Learn
          </h1>
          <p style={{ color: T.dim, fontSize: 14, margin: "0 0 18px" }}>
            Beginner to interview-ready. Learn, hack, level up — sign in to save your progress to your Code Masters account.
          </p>
          <a
            href="/auth/github"
            style={{
              display: "inline-flex",
              alignItems: "center",
              gap: "8px",
              background: "var(--cm-orange)",
              color: "#FCE9DD",
              fontSize: "14px",
              fontWeight: 600,
              borderRadius: "999px",
              padding: "11px 20px",
            }}
          >
            <GithubMark />
            Sign in with GitHub
          </a>
        </div>
      </div>
    );
  }

  if (!profile) return null;

  const activeNav = ["course", "lesson", "quiz", "project"].includes(view.page) ? "dash" : view.page === "lab" ? "labs" : view.page;

  return (
    <div className="ta-root">
      <style>{LEARN_STYLES}</style>

      {aiAvailable === false && (
        <div
          className="card"
          style={{ padding: "10px 16px", marginBottom: 14, fontSize: 12.5, color: T.dim, borderColor: T.violet, display: "flex", gap: 8, alignItems: "center" }}
        >
          ⚠️ AI features (tutor chat, code review, mock interviews, project grading) aren&apos;t configured on this deployment yet — everything else
          works normally.
        </div>
      )}

      <div style={{ display: "flex", flexWrap: "wrap", gap: 10, alignItems: "center", marginBottom: 16 }}>
        <div style={{ display: "flex", gap: 6, flexWrap: "wrap", flex: 1, minWidth: 260 }}>
          {NAV.map((n) => (
            <button key={n.id} className={"navbtn" + (activeNav === n.id ? " active" : "")} onClick={() => setView({ page: n.id })}>
              <span>{n.icon}</span> {n.label}
            </button>
          ))}
        </div>
        <div className="card" style={{ padding: "8px 14px", minWidth: 220, flex: "0 1 260px" }}>
          <XPBar xp={profile.xp} />
        </div>
        <button className="btn btn-cyan" onClick={() => setTutorOpen(true)}>
          🤖 Ask Byte
        </button>
      </div>

      {view.page === "dash" && <Dashboard profile={profile} go={setView} />}
      {view.page === "course" && <CourseView courseId={view.courseId} profile={profile} go={setView} />}
      {view.page === "lesson" && (
        <LessonView courseId={view.courseId} lessonId={view.lessonId} profile={profile} award={award} go={setView} setTutorCtx={setTutorCtx} />
      )}
      {view.page === "quiz" && <QuizView courseId={view.courseId} chapterId={view.chapterId} profile={profile} award={award} go={setView} />}
      {view.page === "project" && <ProjectView courseId={view.courseId} profile={profile} award={award} go={setView} setTutorCtx={setTutorCtx} />}
      {view.page === "labs" && <LabsHome profile={profile} go={setView} />}
      {view.page === "lab" && <LabView labId={view.labId} profile={profile} award={award} go={setView} setTutorCtx={setTutorCtx} />}
      {view.page === "interview" && <InterviewGym profile={profile} award={award} setTutorCtx={setTutorCtx} />}
      {view.page === "leaderboard" && <Leaderboard profile={profile} />}
      {view.page === "badges" && <BadgeGallery profile={profile} />}

      <TutorDrawer open={tutorOpen} onClose={() => setTutorOpen(false)} context={tutorCtx} />
      <Toast toast={toast} />
    </div>
  );
}
