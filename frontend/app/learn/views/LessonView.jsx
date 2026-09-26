"use client";

import { useEffect, useState } from "react";
import { T } from "../theme";
import { COURSES } from "../data";
import { checkChallenge, askTutor } from "../engine";
import { Pill, CodeBlock } from "../components/Pieces";

export default function LessonView({ courseId, lessonId, profile, award, go, setTutorCtx }) {
  const c = COURSES.find((x) => x.id === courseId);
  const ch = c.chapters.find((x) => x.lessons.some((l) => l.id === lessonId));
  const lesson = ch.lessons.find((l) => l.id === lessonId);
  const idx = ch.lessons.indexOf(lesson);
  const chal = lesson.challenge;
  const [code, setCode] = useState(chal.starter || "");
  const [result, setResult] = useState(null);
  const [showHint, setShowHint] = useState(false);
  const [aiReview, setAiReview] = useState(null);
  const [reviewBusy, setReviewBusy] = useState(false);
  const isDone = !!(profile.completed || {})[lesson.id];

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- resetting the challenge editor/state when the route param (lessonId) changes; no derived-state alternative for editor-local state
    setCode(chal.starter || "");
    setResult(null);
    setShowHint(false);
    setAiReview(null);
    setTutorCtx(`Lesson "${lesson.title}" in the ${c.name} course. Challenge: ${chal.prompt}`);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [lessonId]);

  const run = () => {
    const r = checkChallenge(chal, code);
    setResult(r);
    if (r.pass && !isDone) award(lesson.id, 40, `Quest cleared: ${lesson.title}`);
  };

  const review = async () => {
    if (reviewBusy) return;
    setReviewBusy(true);
    setAiReview(null);
    try {
      const txt = await askTutor(
        `You are Byte, a concise coding tutor. A student is solving this ${c.name} exercise: "${chal.prompt}". Their code/answer:\n\n${code}\n\nIn under 120 words: say what's right, point out any issue, and give ONE nudge toward the fix without writing the full solution. Plain text.`
      );
      setAiReview(txt);
    } catch (e) {
      setAiReview(e?.message || "Couldn't reach the AI reviewer — check your connection and try again.");
    }
    setReviewBusy(false);
  };

  const isText = chal.type === "text";
  return (
    <div style={{ display: "grid", gridTemplateColumns: "minmax(0,1fr) minmax(0,1fr)", gap: 18, alignItems: "start" }} className="lesson-grid">
      <style>{`@media (max-width: 900px){ .lesson-grid { grid-template-columns: 1fr !important; } }`}</style>
      <div>
        <button className="btn btn-ghost" onClick={() => go({ page: "course", courseId })}>
          ← {c.name}
        </button>
        <h1 style={{ fontSize: 24, margin: "12px 0 2px" }}>{lesson.title}</h1>
        <div style={{ marginBottom: 14 }}>
          <Pill color={c.tint}>{c.name}</Pill> <Pill color={T.dim}>{lesson.minutes} min</Pill> {isDone && <Pill color={T.green}>✓ cleared</Pill>}
        </div>
        {lesson.content.map((b, i) =>
          b.t === "p" ? (
            <p key={i} style={{ lineHeight: 1.7, fontSize: 15, color: T.text }}>
              {b.v}
            </p>
          ) : b.t === "code" ? (
            <CodeBlock key={i} v={b.v} />
          ) : (
            <div
              key={i}
              style={{
                borderLeft: `3px solid ${T.amber}`,
                background: `color-mix(in srgb, ${T.amber} 10%, transparent)`,
                padding: "10px 14px",
                borderRadius: "0 12px 12px 0",
                fontSize: 14,
                color: T.text,
              }}
            >
              💡 {b.v}
            </div>
          )
        )}
      </div>

      <div className="card" style={{ padding: 18, position: "sticky", top: 16 }}>
        <div className="px" style={{ fontSize: 12, color: c.tint, marginBottom: 8 }}>
          ⚔ CHALLENGE · +40 XP
        </div>
        <p style={{ fontSize: 14, lineHeight: 1.5, marginTop: 0 }}>{chal.prompt}</p>
        {isText ? (
          <input
            value={code}
            onChange={(e) => setCode(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && run()}
            placeholder="Type your answer…"
            aria-label="Answer"
            style={{ width: "100%", background: "#070A14", border: `1px solid ${T.line}`, borderRadius: 12, padding: "12px", color: T.text, fontSize: 14 }}
          />
        ) : (
          <textarea
            value={code}
            onChange={(e) => setCode(e.target.value)}
            spellCheck={false}
            aria-label="Code editor"
            style={{
              width: "100%",
              minHeight: 170,
              background: "#070A14",
              border: `1px solid ${T.line}`,
              borderRadius: 12,
              padding: 12,
              color: T.text,
              fontSize: 13,
              lineHeight: 1.6,
              resize: "vertical",
            }}
          />
        )}
        {chal.type === "html" && (
          <div style={{ marginTop: 10 }}>
            <div className="mono" style={{ fontSize: 11, color: T.dim, marginBottom: 4 }}>
              LIVE PREVIEW
            </div>
            <iframe
              title="preview"
              sandbox=""
              srcDoc={`<style>body{font-family:sans-serif;background:#fff;color:#111;padding:10px}</style>` + code}
              style={{ width: "100%", height: 150, border: `1px solid ${T.line}`, borderRadius: 12, background: "#fff" }}
            />
          </div>
        )}
        <div style={{ display: "flex", gap: 8, marginTop: 12, flexWrap: "wrap" }}>
          <button className="btn btn-amber" onClick={run}>
            {chal.type === "js" ? "▶ Run & Check" : "✓ Check"}
          </button>
          <button className="btn" onClick={() => setShowHint(true)}>
            💡 Hint
          </button>
          {!isText && (
            <button className="btn" onClick={review} disabled={reviewBusy}>
              {reviewBusy ? "Reviewing…" : "🤖 AI review"}
            </button>
          )}
        </div>
        {showHint && (
          <div style={{ marginTop: 10, fontSize: 13, color: T.amber }}>
            💡 {chal.hint}
          </div>
        )}
        {result && chal.type === "js" && (
          <div className="mono" style={{ marginTop: 12, background: "#070A14", border: `1px solid ${T.line}`, borderRadius: 12, padding: 12, fontSize: 12.5 }}>
            <div style={{ color: T.dim, marginBottom: 4 }}>CONSOLE OUTPUT</div>
            {result.error ? (
              <div style={{ color: T.coral }}>{result.error}</div>
            ) : result.logs.length ? (
              result.logs.map((l, i) => (
                <div key={i} style={{ color: "#3DE8D6" }}>
                  {l}
                </div>
              ))
            ) : (
              <div style={{ color: T.dim }}>(no output — did you console.log?)</div>
            )}
          </div>
        )}
        {result && (
          <div
            className="pop"
            style={{
              marginTop: 12,
              padding: "10px 14px",
              borderRadius: 12,
              fontSize: 14,
              fontWeight: 600,
              background: `color-mix(in srgb, ${result.pass ? T.green : T.coral} 12%, transparent)`,
              border: `1px solid ${result.pass ? T.green : T.coral}`,
              color: result.pass ? T.green : T.coral,
            }}
          >
            {result.pass ? "✓ Cleared! +40 XP" : "✗ Not yet — tweak it and try again."}
          </div>
        )}
        {aiReview && (
          <div style={{ marginTop: 12, fontSize: 13.5, lineHeight: 1.55, background: T.panel2, border: `1px solid ${T.line}`, borderRadius: 12, padding: 12, whiteSpace: "pre-wrap" }}>
            🤖 {aiReview}
          </div>
        )}
        {result?.pass && (
          <button
            className="btn btn-cyan"
            style={{ marginTop: 12, width: "100%" }}
            onClick={() =>
              idx + 1 < ch.lessons.length
                ? go({ page: "lesson", courseId, lessonId: ch.lessons[idx + 1].id })
                : go({ page: "quiz", courseId, chapterId: ch.id })
            }
          >
            {idx + 1 < ch.lessons.length ? "Next quest →" : "Boss fight: chapter quiz →"}
          </button>
        )}
      </div>
    </div>
  );
}
