"use client";

import { T } from "../theme";
import { COURSES, LABS, allLessons } from "../data";
import { Pill, Ring, XPBar } from "../components/Pieces";

export default function Dashboard({ profile, go }) {
  const done = profile.completed || {};
  const courseStats = COURSES.map((c) => {
    const items = c.chapters.flatMap((ch) => [...ch.lessons.map((l) => l.id), ch.quiz.id]);
    const n = items.filter((id) => done[id]).length;
    return { ...c, pct: Math.round((n / items.length) * 100), total: items.length, n };
  });
  const flagsTotal = LABS.reduce((s, l) => s + l.flags.length, 0);
  const flagsGot = (profile.labFlags || []).length;
  const nextLesson = allLessons.find((l) => !done[l.id]);

  return (
    <div>
      <div
        className="card"
        style={{
          padding: 22,
          marginBottom: 18,
          position: "relative",
          overflow: "hidden",
        }}
      >
        <div style={{ display: "flex", flexWrap: "wrap", gap: 18, alignItems: "center" }}>
          <div style={{ fontSize: 44 }} aria-hidden>
            👾
          </div>
          <div style={{ flex: 1, minWidth: 220 }}>
            <div className="px" style={{ fontSize: 16, color: T.text }}>
              {profile.name}
            </div>
            <div style={{ marginTop: 8 }}>
              <XPBar xp={profile.xp} />
            </div>
          </div>
          <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
            <div style={{ textAlign: "center", background: T.ink, border: `1px solid ${T.line}`, borderRadius: 14, padding: "10px 16px" }}>
              <div style={{ fontSize: 20 }}>🔥</div>
              <div className="px" style={{ fontSize: 13, color: T.coral }}>
                {profile.streak || 1}
              </div>
              <div style={{ fontSize: 10, color: T.dim }}>day streak</div>
            </div>
            <div style={{ textAlign: "center", background: T.ink, border: `1px solid ${T.line}`, borderRadius: 14, padding: "10px 16px" }}>
              <div style={{ fontSize: 20 }}>🚩</div>
              <div className="px" style={{ fontSize: 13, color: T.cyan }}>
                {flagsGot}/{flagsTotal}
              </div>
              <div style={{ fontSize: 10, color: T.dim }}>lab flags</div>
            </div>
            <div style={{ textAlign: "center", background: T.ink, border: `1px solid ${T.line}`, borderRadius: 14, padding: "10px 16px" }}>
              <div style={{ fontSize: 20 }}>🎖️</div>
              <div className="px" style={{ fontSize: 13, color: T.amber }}>
                {(profile.badges || []).length}
              </div>
              <div style={{ fontSize: 10, color: T.dim }}>badges</div>
            </div>
          </div>
        </div>
        {nextLesson && (
          <button
            className="btn btn-amber"
            style={{ marginTop: 16 }}
            onClick={() => go({ page: "lesson", courseId: nextLesson.courseId, lessonId: nextLesson.id })}
          >
            ▶ Continue quest: {nextLesson.title}
          </button>
        )}
      </div>

      <div className="px" style={{ fontSize: 13, color: T.dim, margin: "4px 0 12px" }}>
        COURSES
      </div>
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(250px, 1fr))", gap: 14 }}>
        {courseStats.map((c) => (
          <button
            key={c.id}
            onClick={() => go({ page: "course", courseId: c.id })}
            className="card rise"
            style={{ padding: 18, textAlign: "left", cursor: "pointer", color: T.text, borderTop: `3px solid ${c.tint}`, font: "inherit" }}
          >
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
              <div style={{ fontSize: 30 }}>{c.icon}</div>
              <Ring pct={c.pct} color={c.tint} />
            </div>
            <div style={{ fontWeight: 700, fontSize: 17, marginTop: 8 }}>
              {c.name} {done["cert:" + c.id] && "🎓"}
            </div>
            <div style={{ fontSize: 13, color: T.dim, marginTop: 4, lineHeight: 1.4 }}>{c.tagline}</div>
            <div className="mono" style={{ fontSize: 11, color: c.tint, marginTop: 10 }}>
              {c.n}/{c.total} quests cleared
            </div>
          </button>
        ))}
        <button
          onClick={() => go({ page: "labs" })}
          className="card rise"
          style={{
            padding: 18,
            textAlign: "left",
            cursor: "pointer",
            color: T.text,
            font: "inherit",
            borderStyle: "dashed",
            borderColor: T.green,
          }}
        >
          <div style={{ fontSize: 30 }}>🧪</div>
          <div style={{ fontWeight: 700, fontSize: 17, marginTop: 8 }}>Cyber Labs</div>
          <div style={{ fontSize: 13, color: T.dim, marginTop: 4, lineHeight: 1.4 }}>
            Hands-on practice: live terminal, crypto CTF, phishing triage.
          </div>
          <div className="mono" style={{ fontSize: 11, color: T.green, marginTop: 10 }}>
            {flagsGot}/{flagsTotal} flags captured
          </div>
        </button>
        <button
          onClick={() => go({ page: "interview" })}
          className="card rise"
          style={{
            padding: 18,
            textAlign: "left",
            cursor: "pointer",
            color: T.text,
            font: "inherit",
            borderStyle: "dashed",
            borderColor: T.coral,
          }}
        >
          <div style={{ fontSize: 30 }}>🎤</div>
          <div style={{ fontWeight: 700, fontSize: 17, marginTop: 8 }}>Interview Gym</div>
          <div style={{ fontSize: 13, color: T.dim, marginTop: 4, lineHeight: 1.4 }}>
            AI mock interviews — behavioral, coding, system design & per-topic — plus flashcards.
          </div>
          <div className="mono" style={{ fontSize: 11, color: T.coral, marginTop: 10 }}>
            {profile.interviewSessions || 0} sessions · get interview-ready
          </div>
        </button>
      </div>
    </div>
  );
}
