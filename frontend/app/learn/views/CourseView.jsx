"use client";

import { T } from "../theme";
import { COURSES, PROJECTS } from "../data";
import { Pill } from "../components/Pieces";

export default function CourseView({ courseId, profile, go }) {
  const c = COURSES.find((x) => x.id === courseId);
  const done = profile.completed || {};
  const tierColor = { Beginner: T.cyan, Intermediate: T.amber, Pro: T.coral };
  const allChaptersDone = c.chapters.every((ch) => ch.lessons.every((l) => done[l.id]) && done[ch.quiz.id]);
  const certified = !!done["cert:" + c.id];
  const proj = PROJECTS[c.id];
  return (
    <div>
      <button className="btn btn-ghost" onClick={() => go({ page: "dash" })}>
        ← Back
      </button>
      <div style={{ display: "flex", gap: 14, alignItems: "center", margin: "14px 0 6px" }}>
        <span style={{ fontSize: 40 }}>{c.icon}</span>
        <div>
          <h1 className="px" style={{ fontSize: 20, margin: 0, color: c.tint }}>
            {c.name} {certified && "🎓"}
          </h1>
          <p style={{ margin: "4px 0 0", color: T.dim, fontSize: 14 }}>{c.desc}</p>
        </div>
      </div>
      <div style={{ display: "flex", gap: 6, margin: "10px 0 4px", flexWrap: "wrap" }}>
        {c.chapters.map((ch) => (
          <Pill key={ch.id} color={tierColor[ch.tier] || T.dim}>
            {ch.tier}
          </Pill>
        ))}
        <Pill color={certified ? T.green : T.violet}>{certified ? "✓ Certified" : "🎓 Final project at the end"}</Pill>
      </div>
      {c.chapters.map((ch, ci) => {
        const chLocked = ci > 0 && !done[c.chapters[ci - 1].quiz.id];
        return (
          <div key={ch.id} style={{ marginTop: 22, opacity: chLocked ? 0.45 : 1 }}>
            <div style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 10 }}>
              <span className="px" style={{ fontSize: 12, color: T.dim }}>
                {ch.name.toUpperCase()}
              </span>
              <Pill color={tierColor[ch.tier] || T.dim}>{ch.tier}</Pill>
              {chLocked && <Pill color={T.dim}>🔒 beat the previous boss to unlock</Pill>}
            </div>
            <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
              {ch.lessons.map((l, i) => {
                const isDone = !!done[l.id];
                const locked = chLocked || (i > 0 && !done[ch.lessons[i - 1].id]);
                return (
                  <button
                    key={l.id}
                    disabled={locked}
                    onClick={() => go({ page: "lesson", courseId, lessonId: l.id })}
                    className="card"
                    style={{
                      padding: "14px 16px",
                      display: "flex",
                      alignItems: "center",
                      gap: 14,
                      textAlign: "left",
                      cursor: locked ? "not-allowed" : "pointer",
                      opacity: locked ? 0.45 : 1,
                      color: T.text,
                      font: "inherit",
                    }}
                  >
                    <span className="px" style={{ fontSize: 14, color: isDone ? T.green : locked ? T.dim : c.tint, width: 28 }}>
                      {isDone ? "✓" : locked ? "🔒" : String(i + 1).padStart(2, "0")}
                    </span>
                    <span style={{ flex: 1, fontWeight: 600, fontSize: 15 }}>{l.title}</span>
                    <Pill color={T.dim}>{l.minutes} min</Pill>
                    <Pill color={T.amber}>+40 XP</Pill>
                  </button>
                );
              })}
              {(() => {
                const quizLocked = chLocked || !ch.lessons.every((l) => done[l.id]);
                const quizDone = !!done[ch.quiz.id];
                return (
                  <button
                    disabled={quizLocked}
                    onClick={() => go({ page: "quiz", courseId, chapterId: ch.id })}
                    className="card"
                    style={{
                      padding: "14px 16px",
                      display: "flex",
                      alignItems: "center",
                      gap: 14,
                      textAlign: "left",
                      cursor: quizLocked ? "not-allowed" : "pointer",
                      opacity: quizLocked ? 0.45 : 1,
                      color: T.text,
                      font: "inherit",
                      borderColor: quizDone ? T.green : T.violet,
                    }}
                  >
                    <span style={{ fontSize: 18 }}>{quizDone ? "🏅" : "🧠"}</span>
                    <span style={{ flex: 1, fontWeight: 600, fontSize: 15 }}>Chapter Quiz — boss fight</span>
                    <Pill color={T.violet}>+{ch.quiz.questions.length * 10} XP</Pill>
                  </button>
                );
              })()}
            </div>
          </div>
        );
      })}
      {proj && (
        <div style={{ marginTop: 22 }}>
          <div className="px" style={{ fontSize: 12, color: T.dim, marginBottom: 10 }}>
            FINAL PROJECT · PRO CERTIFICATION
          </div>
          <button
            disabled={!allChaptersDone}
            onClick={() => go({ page: "project", courseId })}
            className="card"
            style={{
              padding: "16px 18px",
              display: "flex",
              alignItems: "center",
              gap: 14,
              textAlign: "left",
              width: "100%",
              cursor: allChaptersDone ? "pointer" : "not-allowed",
              opacity: allChaptersDone ? 1 : 0.45,
              color: T.text,
              font: "inherit",
              borderStyle: "dashed",
              borderColor: certified ? T.green : T.amber,
            }}
          >
            <span style={{ fontSize: 24 }}>{certified ? "🎓" : allChaptersDone ? "🏗️" : "🔒"}</span>
            <span style={{ flex: 1 }}>
              <span style={{ fontWeight: 700, fontSize: 15, display: "block" }}>
                {certified ? "Certified — view your certificate" : "Capstone: build something real"}
              </span>
              <span style={{ fontSize: 13, color: T.dim }}>
                {allChaptersDone ? "Graded by AI against pro criteria." : "Clear every chapter and boss fight to unlock."}
              </span>
            </span>
            <Pill color={T.amber}>+{proj.xp} XP</Pill>
          </button>
        </div>
      )}
    </div>
  );
}
