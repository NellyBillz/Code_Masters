"use client";

import { useState } from "react";
import { T } from "../theme";
import { COURSES } from "../data";

export default function QuizView({ courseId, chapterId, profile, award, go }) {
  const c = COURSES.find((x) => x.id === courseId);
  const ch = c.chapters.find((x) => x.id === chapterId);
  const quiz = ch.quiz;
  const [i, setI] = useState(0);
  const [picked, setPicked] = useState(null);
  const [score, setScore] = useState(0);
  const [finished, setFinished] = useState(false);
  const q = quiz.questions[i];
  const alreadyDone = !!(profile.completed || {})[quiz.id];

  const pick = (idx) => {
    if (picked === null) {
      setPicked(idx);
      if (idx === q.answer) setScore((s) => s + 1);
    }
  };
  const next = () => {
    if (i + 1 < quiz.questions.length) {
      setI(i + 1);
      setPicked(null);
    } else {
      setFinished(true);
      const finalScore = score;
      if (!alreadyDone && finalScore >= Math.ceil(quiz.questions.length * 0.75)) {
        award(quiz.id, quiz.questions.length * 10, "Boss defeated: chapter quiz", finalScore === quiz.questions.length ? "quiz-whiz" : null);
      }
    }
  };

  if (finished) {
    const pass = score >= Math.ceil(quiz.questions.length * 0.75);
    return (
      <div className="card pop" style={{ maxWidth: 520, margin: "40px auto", padding: 32, textAlign: "center" }}>
        <div style={{ fontSize: 52 }}>{pass ? (score === quiz.questions.length ? "🏆" : "🏅") : "💀"}</div>
        <h2 className="px" style={{ fontSize: 18, color: pass ? T.amber : T.coral }}>
          {pass ? "BOSS DEFEATED" : "BOSS WINS THIS ROUND"}
        </h2>
        <p style={{ color: T.dim }}>
          You scored {score}/{quiz.questions.length}.{" "}
          {pass ? (alreadyDone ? "Already banked this XP — nice revision." : `+${quiz.questions.length * 10} XP banked.`) : "Score 75%+ to clear it. Review the lessons and rematch!"}
        </p>
        <div style={{ display: "flex", gap: 8, justifyContent: "center" }}>
          {!pass && (
            <button
              className="btn btn-amber"
              onClick={() => {
                setI(0);
                setPicked(null);
                setScore(0);
                setFinished(false);
              }}
            >
              ⟳ Rematch
            </button>
          )}
          <button className="btn" onClick={() => go({ page: "course", courseId })}>
            Back to {c.name}
          </button>
        </div>
      </div>
    );
  }

  return (
    <div style={{ maxWidth: 620, margin: "0 auto" }}>
      <button className="btn btn-ghost" onClick={() => go({ page: "course", courseId })}>
        ← Retreat
      </button>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", margin: "14px 0" }}>
        <div className="px" style={{ fontSize: 13, color: T.violet }}>
          🧠 BOSS FIGHT
        </div>
        <div className="mono" style={{ fontSize: 12, color: T.dim }}>
          Q{i + 1}/{quiz.questions.length} · score {score}
        </div>
      </div>
      <div style={{ height: 6, background: T.panel2, borderRadius: 3, marginBottom: 18 }}>
        <div style={{ width: `${(i / quiz.questions.length) * 100}%`, height: "100%", background: T.violet, borderRadius: 3, transition: "width .3s" }} />
      </div>
      <div className="card rise" style={{ padding: 22 }} key={i}>
        <h2 style={{ fontSize: 18, marginTop: 0 }}>{q.q}</h2>
        {q.options.map((opt, oi) => {
          const state = picked === null ? "idle" : oi === q.answer ? "right" : oi === picked ? "wrong" : "dim";
          return (
            <button
              key={oi}
              className="opt"
              onClick={() => pick(oi)}
              disabled={picked !== null}
              style={{
                borderColor: state === "right" ? T.green : state === "wrong" ? T.coral : T.line,
                background:
                  state === "right"
                    ? `color-mix(in srgb, ${T.green} 12%, transparent)`
                    : state === "wrong"
                    ? `color-mix(in srgb, ${T.coral} 12%, transparent)`
                    : T.panel2,
                opacity: state === "dim" ? 0.5 : 1,
              }}
            >
              <span className="mono" style={{ color: T.dim, marginRight: 10 }}>
                {String.fromCharCode(65 + oi)}
              </span>
              {opt}
              {state === "right" && " ✓"}
              {state === "wrong" && " ✗"}
            </button>
          );
        })}
        {picked !== null && (
          <div style={{ marginTop: 8, fontSize: 13.5, color: T.dim, lineHeight: 1.5 }}>
            {picked === q.answer ? "✓ Correct. " : "✗ Not quite. "}
            {q.why}
            <div style={{ marginTop: 12 }}>
              <button className="btn btn-amber" onClick={next}>
                {i + 1 < quiz.questions.length ? "Next →" : "Finish"}
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
