"use client";

import { useEffect, useRef, useState } from "react";
import { T } from "../theme";
import { INTERVIEW_TRACKS, FLASHCARDS } from "../data";
import { askTutor } from "../engine";
import { Pill } from "../components/Pieces";

export default function InterviewGym({ profile, award, setTutorCtx }) {
  const [mode, setMode] = useState("home"); // home | mock | cards
  const [track, setTrack] = useState(null);
  const [deck, setDeck] = useState(null);
  useEffect(() => {
    setTutorCtx("The Interview Gym — mock technical & behavioral interviews plus flashcards. Encourage, coach on structure and complexity.");
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (mode === "mock" && track)
    return (
      <MockInterview
        track={track}
        profile={profile}
        award={award}
        onExit={() => {
          setMode("home");
          setTrack(null);
        }}
      />
    );
  if (mode === "cards" && deck)
    return (
      <Flashcards
        deck={deck}
        cards={FLASHCARDS[deck]}
        onExit={() => {
          setMode("home");
          setDeck(null);
        }}
      />
    );

  const sessions = profile.interviewSessions || 0;
  return (
    <div>
      <div style={{ display: "flex", alignItems: "center", gap: 12, marginBottom: 4 }}>
        <span style={{ fontSize: 34 }}>🎤</span>
        <div>
          <h1 className="px" style={{ fontSize: 20, color: T.coral, margin: 0 }}>
            INTERVIEW GYM
          </h1>
          <p style={{ color: T.dim, fontSize: 14, margin: "4px 0 0" }}>
            Train like it&apos;s the real thing. AI plays the interviewer, grades your answers, and shows a model response.
          </p>
        </div>
      </div>
      <div style={{ display: "flex", gap: 8, margin: "10px 0 20px", flexWrap: "wrap" }}>
        <Pill color={T.coral}>🎯 {sessions} mock sessions done</Pill>
        <Pill color={T.amber}>+80 XP per session</Pill>
        {(profile.badges || []).includes("interview-ace") && <Pill color={T.green}>🏆 Interview Ace</Pill>}
      </div>

      <div className="px" style={{ fontSize: 12, color: T.dim, marginBottom: 10 }}>
        🎙️ AI MOCK INTERVIEW — PICK A ROUND
      </div>
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(240px, 1fr))", gap: 12 }}>
        {INTERVIEW_TRACKS.map((t) => (
          <button
            key={t.id}
            className="card rise"
            onClick={() => {
              setTrack(t);
              setMode("mock");
            }}
            style={{ padding: 16, textAlign: "left", cursor: "pointer", color: T.text, font: "inherit", borderLeft: `3px solid ${t.tint}` }}
          >
            <div style={{ fontSize: 24 }}>{t.icon}</div>
            <div style={{ fontWeight: 700, fontSize: 15, marginTop: 6 }}>{t.name}</div>
            <div style={{ fontSize: 12.5, color: T.dim, marginTop: 4, lineHeight: 1.45 }}>{t.blurb}</div>
          </button>
        ))}
      </div>

      <div className="px" style={{ fontSize: 12, color: T.dim, margin: "24px 0 10px" }}>
        ⚡ RAPID-FIRE FLASHCARDS — THE MOST-ASKED QUESTIONS
      </div>
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(220px, 1fr))", gap: 10 }}>
        {Object.keys(FLASHCARDS).map((name) => (
          <button
            key={name}
            className="card"
            onClick={() => {
              setDeck(name);
              setMode("cards");
            }}
            style={{ padding: 14, textAlign: "left", cursor: "pointer", color: T.text, font: "inherit", display: "flex", justifyContent: "space-between", alignItems: "center" }}
          >
            <span style={{ fontWeight: 600, fontSize: 14 }}>{name}</span>
            <Pill color={T.cyan}>{FLASHCARDS[name].length}</Pill>
          </button>
        ))}
      </div>
    </div>
  );
}

function MockInterview({ track, profile, award, onExit }) {
  const TOTAL = 4;
  const [turns, setTurns] = useState([]);
  const [question, setQuestion] = useState(null);
  const [answer, setAnswer] = useState("");
  const [busy, setBusy] = useState(false);
  const [phase, setPhase] = useState("loading"); // loading | answering | grading | done
  const [awarded, setAwarded] = useState(false);
  const [loadError, setLoadError] = useState(null);
  const endRef = useRef(null);

  const getQuestion = async (prev) => {
    setPhase("loading");
    setBusy(true);
    setLoadError(null);
    try {
      const q = await askTutor(
        `You are a friendly but rigorous technical interviewer conducting a mock interview. Ask ONE ${track.sys}

Already asked (do not repeat): ${prev.length ? prev.join(" | ") : "none yet"}.
This is question ${prev.length + 1} of ${TOTAL}. Output ONLY the question itself — no preamble, no numbering, one or two sentences max.`
      );
      setQuestion(q.trim());
      setPhase("answering");
    } catch (e) {
      setLoadError(e?.message || "Couldn't load a question.");
      setQuestion(null);
      setPhase("answering");
    }
    setBusy(false);
  };

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [turns, question, phase]);
  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- fetch-on-mount pattern (load the first interview question); no derived-state alternative for an AI-generated question
    getQuestion([]);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const submit = async () => {
    if (!answer.trim() || busy) return;
    setPhase("grading");
    setBusy(true);
    try {
      const res = await askTutor(
        `You are grading one answer in a mock ${track.name} interview.

QUESTION: ${question}
CANDIDATE ANSWER: ${answer}

Respond in EXACTLY this format, nothing else:
SCORE: <integer 0-10>
FEEDBACK: <2-3 sentences: what was strong, what to improve. Encouraging but honest.>
MODEL: <a crisp 2-4 sentence model answer a strong candidate would give.>`
      );
      const score = parseInt((res.match(/SCORE:\s*(\d+)/i) || [])[1] || "0", 10);
      const feedback = (res.match(/FEEDBACK:\s*([\s\S]*?)(?:MODEL:|$)/i) || [])[1]?.trim() || "";
      const model = (res.match(/MODEL:\s*([\s\S]*)$/i) || [])[1]?.trim() || "";
      const turn = { q: question, a: answer, score, feedback, model };
      const nextTurns = [...turns, turn];
      setTurns(nextTurns);
      setAnswer("");
      setQuestion(null);
      if (nextTurns.length >= TOTAL) finish(nextTurns);
      else getQuestion(nextTurns.map((t) => t.q));
    } catch (e) {
      setLoadError(e?.message || "Couldn't reach the interviewer.");
      setPhase("answering");
    }
    setBusy(false);
  };

  const finish = (all) => {
    setPhase("done");
    if (!awarded) {
      setAwarded(true);
      const avg = all.reduce((s, t) => s + (t.score || 0), 0) / all.length;
      const sessions = (profile.interviewSessions || 0) + 1;
      const highScore = avg >= 8;
      award(
        "interview:" + track.id + ":" + Date.now(),
        80,
        `Mock ${track.name} interview complete`,
        sessions >= 3 || highScore ? "interview-ace" : null,
        null,
        null,
        { interviewSessions: sessions }
      );
    }
  };

  const avg = turns.length ? turns.reduce((s, t) => s + (t.score || 0), 0) / turns.length : 0;

  return (
    <div style={{ maxWidth: 760, margin: "0 auto" }}>
      <button className="btn btn-ghost" onClick={onExit}>
        ← Leave interview
      </button>
      <div style={{ display: "flex", alignItems: "center", gap: 10, margin: "12px 0" }}>
        <span style={{ fontSize: 26 }}>{track.icon}</span>
        <div style={{ flex: 1 }}>
          <div className="px" style={{ fontSize: 13, color: track.tint }}>
            {track.name.toUpperCase()} · MOCK ROUND
          </div>
          <div className="mono" style={{ fontSize: 12, color: T.dim }}>
            Question {Math.min(turns.length + (phase === "done" ? 0 : 1), TOTAL)} / {TOTAL}
          </div>
        </div>
      </div>

      {turns.map((t, i) => (
        <div key={i} className="card" style={{ padding: 14, marginBottom: 10 }}>
          <div style={{ fontSize: 14, fontWeight: 600, color: track.tint }}>
            Q{i + 1}. {t.q}
          </div>
          <div style={{ fontSize: 13, color: T.dim, marginTop: 6, whiteSpace: "pre-wrap" }}>Your answer: {t.a}</div>
          <div style={{ display: "flex", alignItems: "center", gap: 8, marginTop: 8 }}>
            <span className="px" style={{ fontSize: 12, color: t.score >= 7 ? T.green : t.score >= 4 ? T.amber : T.coral }}>
              SCORE {t.score}/10
            </span>
          </div>
          <div style={{ fontSize: 13.5, marginTop: 6, lineHeight: 1.55 }}>💬 {t.feedback}</div>
          <details style={{ marginTop: 6 }}>
            <summary className="mono" style={{ fontSize: 12, color: T.cyan, cursor: "pointer" }}>
              show model answer
            </summary>
            <div style={{ fontSize: 13.5, marginTop: 6, lineHeight: 1.55, color: T.text }}>⭐ {t.model}</div>
          </details>
        </div>
      ))}

      {phase === "loading" && (
        <div className="mono cursor" style={{ color: T.cyan, fontSize: 14 }}>
          interviewer is thinking of a question ▮
        </div>
      )}

      {loadError && !question && phase === "answering" && (
        <div className="card" style={{ padding: 14, borderColor: T.coral, color: T.coral, fontSize: 13.5 }}>
          {loadError}{" "}
          <button className="btn" style={{ marginLeft: 8 }} onClick={() => getQuestion(turns.map((t) => t.q))}>
            Retry
          </button>
        </div>
      )}

      {phase === "answering" && question && (
        <div className="card rise" style={{ padding: 16 }}>
          <div style={{ fontSize: 15, fontWeight: 600, color: track.tint, lineHeight: 1.5 }}>🎙️ {question}</div>
          <textarea
            value={answer}
            onChange={(e) => setAnswer(e.target.value)}
            autoFocus
            spellCheck={false}
            aria-label="Your answer"
            placeholder="Talk it through — approach, reasoning, and (for coding) complexity…"
            style={{ width: "100%", minHeight: 130, marginTop: 12, background: "#070A14", border: `1px solid ${T.line}`, borderRadius: 12, padding: 12, color: T.text, fontSize: 14, lineHeight: 1.6, resize: "vertical", fontFamily: "inherit" }}
          />
          <button className="btn btn-coral" style={{ marginTop: 10 }} onClick={submit} disabled={busy}>
            Submit answer →
          </button>
        </div>
      )}
      {phase === "grading" && (
        <div className="mono cursor" style={{ color: T.amber, fontSize: 14 }}>
          interviewer is evaluating your answer ▮
        </div>
      )}

      {phase === "done" && (
        <div className="card pop" style={{ padding: 24, textAlign: "center", borderWidth: 2, borderStyle: "solid", borderColor: avg >= 8 ? T.green : avg >= 5 ? T.amber : T.coral }}>
          <div style={{ fontSize: 40 }}>{avg >= 8 ? "🌟" : avg >= 5 ? "💪" : "📈"}</div>
          <div className="px" style={{ fontSize: 15, color: avg >= 8 ? T.green : T.amber, marginTop: 6 }}>
            ROUND COMPLETE
          </div>
          <div style={{ fontSize: 15, marginTop: 6 }}>
            Average score: <b>{avg.toFixed(1)}/10</b> · +80 XP
          </div>
          <div style={{ fontSize: 13.5, color: T.dim, marginTop: 8, maxWidth: 420, marginInline: "auto" }}>
            {avg >= 8
              ? "Interview-ready on this track. Review the model answers above to sharpen the last few percent."
              : avg >= 5
              ? "Solid foundation. Reread each model answer, then run the round again — repetition is the whole game."
              : "Every pro started here. Study the model answers, revisit the matching course chapter, and come back for a rematch."}
          </div>
          <div style={{ display: "flex", gap: 8, justifyContent: "center", marginTop: 16, flexWrap: "wrap" }}>
            <button
              className="btn btn-amber"
              onClick={() => {
                setTurns([]);
                setAwarded(false);
                getQuestion([]);
              }}
            >
              ⟳ New round
            </button>
            <button className="btn" onClick={onExit}>
              Back to Gym
            </button>
          </div>
        </div>
      )}
      <div ref={endRef} />
    </div>
  );
}

function Flashcards({ deck, cards, onExit }) {
  const [i, setI] = useState(0);
  const [flipped, setFlipped] = useState(false);
  const [known, setKnown] = useState(0);
  const [done, setDone] = useState(false);
  const card = cards[i];
  const nextCard = (gotIt) => {
    if (gotIt) setKnown((k) => k + 1);
    if (i + 1 < cards.length) {
      setI(i + 1);
      setFlipped(false);
    } else setDone(true);
  };
  if (done)
    return (
      <div style={{ maxWidth: 560, margin: "0 auto" }}>
        <button className="btn btn-ghost" onClick={onExit}>
          ← Gym
        </button>
        <div className="card pop" style={{ padding: 28, textAlign: "center", marginTop: 16 }}>
          <div style={{ fontSize: 40 }}>🧠</div>
          <div className="px" style={{ fontSize: 15, color: T.cyan, marginTop: 6 }}>
            DECK COMPLETE
          </div>
          <div style={{ fontSize: 15, marginTop: 6 }}>
            You felt confident on {known}/{cards.length} of &quot;{deck}&quot;.
          </div>
          <div style={{ display: "flex", gap: 8, justifyContent: "center", marginTop: 16 }}>
            <button
              className="btn btn-cyan"
              onClick={() => {
                setI(0);
                setFlipped(false);
                setKnown(0);
                setDone(false);
              }}
            >
              ⟳ Run again
            </button>
            <button className="btn" onClick={onExit}>
              Back to Gym
            </button>
          </div>
        </div>
      </div>
    );
  return (
    <div style={{ maxWidth: 620, margin: "0 auto" }}>
      <button className="btn btn-ghost" onClick={onExit}>
        ← Gym
      </button>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", margin: "12px 0" }}>
        <div className="px" style={{ fontSize: 13, color: T.cyan }}>
          ⚡ {deck.toUpperCase()}
        </div>
        <div className="mono" style={{ fontSize: 12, color: T.dim }}>
          {i + 1} / {cards.length}
        </div>
      </div>
      <div style={{ height: 5, background: T.panel2, borderRadius: 3, marginBottom: 16 }}>
        <div style={{ width: `${(i / cards.length) * 100}%`, height: "100%", background: T.cyan, borderRadius: 3, transition: "width .3s" }} />
      </div>
      <button
        onClick={() => setFlipped((f) => !f)}
        className="card"
        aria-label="Flip card"
        style={{ width: "100%", minHeight: 220, padding: 26, cursor: "pointer", color: T.text, font: "inherit", textAlign: "left", display: "flex", flexDirection: "column", justifyContent: "center", borderColor: flipped ? T.cyan : T.line }}
      >
        {!flipped ? (
          <>
            <div className="mono" style={{ fontSize: 11, color: T.coral, marginBottom: 10 }}>
              QUESTION · tap to reveal
            </div>
            <div style={{ fontSize: 18, fontWeight: 600, lineHeight: 1.5 }}>{card.q}</div>
          </>
        ) : (
          <>
            <div className="mono" style={{ fontSize: 11, color: T.green, marginBottom: 10 }}>
              MODEL ANSWER
            </div>
            <div style={{ fontSize: 15, lineHeight: 1.65, color: T.text }}>{card.a}</div>
          </>
        )}
      </button>
      {flipped && (
        <div style={{ display: "flex", gap: 8, marginTop: 12, justifyContent: "center" }}>
          <button className="btn" onClick={() => nextCard(false)} style={{ borderColor: T.amber }}>
            ↻ Review again
          </button>
          <button className="btn btn-cyan" onClick={() => nextCard(true)}>
            ✓ Got it
          </button>
        </div>
      )}
    </div>
  );
}
