"use client";

import { useEffect, useState } from "react";
import { T } from "../theme";
import { COURSES, PROJECTS, levelFromXp, titleForLevel } from "../data";
import { askTutor } from "../engine";
import { Pill } from "../components/Pieces";

export default function ProjectView({ courseId, profile, award, go, setTutorCtx }) {
  const c = COURSES.find((x) => x.id === courseId);
  const proj = PROJECTS[courseId];
  const [code, setCode] = useState(proj.placeholder);
  const [busy, setBusy] = useState(false);
  const [verdict, setVerdict] = useState(null);
  const certified = !!(profile.completed || {})["cert:" + courseId];
  useEffect(() => {
    setTutorCtx(`Final project for the ${c.name} course: ${proj.brief}. Coach with hints, never write the full solution.`);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [courseId]);

  const submit = async () => {
    if (busy || !code.trim()) return;
    setBusy(true);
    setVerdict(null);
    try {
      const txt = await askTutor(
        `You are the final-project examiner for Code Masters' ${c.name} course (a gamified learning platform). Project requirements: ${proj.brief}

Student submission:
---
${code}
---
Grade it for a motivated beginner completing a pro-track capstone: every listed requirement must be genuinely attempted and the work must be plausible and coherent (it will not be executed, so judge structure and correctness by reading). Be encouraging but honest. Reply in plain text, starting with EXACTLY the word PASS: or RETRY: on the first line, followed by feedback under 120 words — what's strong, and if RETRY, the specific gaps to fix.`
      );
      const pass = /^\s*PASS\b/i.test(txt);
      setVerdict({ pass, text: txt.replace(/^\s*(PASS|RETRY)\s*:?\s*/i, "") });
      if (pass && !certified) award("cert:" + courseId, proj.xp, `${c.name} — certified!`, "graduate");
    } catch (e) {
      setVerdict({ pass: false, text: e?.message || "Couldn't reach the examiner. Your work is safe in the editor — resubmit in a moment." });
    }
    setBusy(false);
  };

  return (
    <div style={{ maxWidth: 760, margin: "0 auto" }}>
      <button className="btn btn-ghost" onClick={() => go({ page: "course", courseId })}>
        ← {c.name}
      </button>
      {certified && (
        <div className="card pop" style={{ margin: "16px 0", padding: 28, textAlign: "center", borderWidth: 2, borderColor: T.amber, borderStyle: "solid" }}>
          <div style={{ fontSize: 44 }}>🎓</div>
          <div className="px" style={{ fontSize: 16, color: T.amber, margin: "8px 0 2px" }}>
            CERTIFICATE OF MASTERY
          </div>
          <div style={{ fontSize: 14, color: T.dim }}>This certifies that</div>
          <div className="px" style={{ fontSize: 20, margin: "6px 0" }}>
            {profile.name}
          </div>
          <div style={{ fontSize: 14, color: T.dim }}>completed the full {c.name} path — beginner to pro — on Code Masters</div>
          <div className="mono" style={{ fontSize: 11, color: T.dim, marginTop: 10 }}>
            {c.icon} · issued {new Date().toLocaleDateString()} · LV {levelFromXp(profile.xp)} {titleForLevel(levelFromXp(profile.xp))}
          </div>
        </div>
      )}
      <div style={{ display: "flex", gap: 12, alignItems: "center", margin: "14px 0 8px" }}>
        <span style={{ fontSize: 32 }}>🏗️</span>
        <div>
          <h1 className="px" style={{ fontSize: 17, margin: 0, color: T.amber }}>
            FINAL PROJECT · {c.name.toUpperCase()}
          </h1>
          <Pill color={T.amber}>+{proj.xp} XP</Pill> <Pill color={T.violet}>AI-graded</Pill>
        </div>
      </div>
      <div className="card" style={{ padding: 16, fontSize: 14.5, lineHeight: 1.6, color: T.text }}>
        {proj.brief}
      </div>
      <textarea
        value={code}
        onChange={(e) => setCode(e.target.value)}
        spellCheck={false}
        aria-label="Project submission"
        style={{ width: "100%", minHeight: 260, marginTop: 12, background: "#070A14", border: `1px solid ${T.line}`, borderRadius: 14, padding: 14, color: T.text, fontSize: 13, lineHeight: 1.6, resize: "vertical" }}
      />
      <div style={{ display: "flex", gap: 8, marginTop: 12 }}>
        <button className="btn btn-amber" onClick={submit} disabled={busy}>
          {busy ? "Examiner reviewing…" : certified ? "Resubmit for feedback" : "Submit for grading 🎓"}
        </button>
      </div>
      {busy && (
        <div className="mono cursor" style={{ color: T.cyan, fontSize: 13, marginTop: 10 }}>
          grading against pro criteria ▮
        </div>
      )}
      {verdict && (
        <div
          className="pop"
          style={{ marginTop: 14, padding: 16, borderRadius: 14, border: `1px solid ${verdict.pass ? T.green : T.coral}`, background: `color-mix(in srgb, ${verdict.pass ? T.green : T.coral} 10%, transparent)` }}
        >
          <div className="px" style={{ fontSize: 13, color: verdict.pass ? T.green : T.coral, marginBottom: 6 }}>
            {verdict.pass ? "✓ PASS — CERTIFIED" : "↻ RETRY — SO CLOSE"}
          </div>
          <div style={{ fontSize: 14, lineHeight: 1.6, whiteSpace: "pre-wrap" }}>{verdict.text}</div>
        </div>
      )}
    </div>
  );
}
