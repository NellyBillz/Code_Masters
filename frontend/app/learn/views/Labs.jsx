"use client";

import { useEffect, useRef, useState } from "react";
import { T } from "../theme";
import { LABS, TERMINAL_FS } from "../data";
import { caesarShift } from "../engine";
import { Pill } from "../components/Pieces";

export function LabsHome({ profile, go }) {
  const got = new Set(profile.labFlags || []);
  return (
    <div>
      <h1 className="px" style={{ fontSize: 20, color: T.green, margin: "6px 0" }}>
        🧪 CYBER LABS
      </h1>
      <p style={{ color: T.dim, fontSize: 14, maxWidth: 640 }}>
        Hands-on practice rooms, TryHackMe style. Everything here is a safe sandbox — you&apos;re learning defender instincts by playing attacker in a toy
        world. Capture flags shaped like{" "}
        <span className="mono" style={{ color: T.green }}>
          CodeMasters{"{...}"}
        </span>
        .
      </p>
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(270px, 1fr))", gap: 14, marginTop: 16 }}>
        {LABS.map((lab) => {
          const n = lab.flags.filter((f) => got.has(f.id)).length;
          const doneAll = n === lab.flags.length;
          return (
            <button
              key={lab.id}
              className="card rise"
              onClick={() => go({ page: "lab", labId: lab.id })}
              style={{ padding: 18, textAlign: "left", cursor: "pointer", color: T.text, font: "inherit", borderTop: `3px solid ${lab.tint}` }}
            >
              <div style={{ display: "flex", justifyContent: "space-between" }}>
                <span style={{ fontSize: 30 }}>{lab.icon}</span>
                <Pill color={lab.tint}>{lab.difficulty}</Pill>
              </div>
              <div style={{ fontWeight: 700, fontSize: 17, marginTop: 8 }}>{lab.name}</div>
              <div style={{ fontSize: 13, color: T.dim, marginTop: 4, lineHeight: 1.45 }}>{lab.desc}</div>
              <div className="mono" style={{ fontSize: 11, marginTop: 10, color: doneAll ? T.green : T.dim }}>
                {doneAll ? "✓ ROOM CLEARED" : `🚩 ${n}/${lab.flags.length} flags`}
              </div>
            </button>
          );
        })}
      </div>
    </div>
  );
}

export function FlagSubmit({ lab, profile, award }) {
  const [val, setVal] = useState("");
  const [msg, setMsg] = useState(null);
  const got = new Set(profile.labFlags || []);
  const submit = () => {
    const v = val.trim();
    const match = lab.flags.find((f) => f.value === v);
    if (!match) {
      setMsg({ ok: false, text: "Incorrect flag. Flags look like CodeMasters{...} — exact match required." });
      return;
    }
    if (got.has(match.id)) {
      setMsg({ ok: true, text: "Already captured that one — go find the rest!" });
      return;
    }
    award("flag:" + match.id, match.xp, "Flag captured!", null, match.id, lab);
    setMsg({ ok: true, text: `🚩 ${match.label} captured! +${match.xp} XP` });
    setVal("");
  };
  return (
    <div className="card" style={{ padding: 16, marginTop: 16 }}>
      <div className="px" style={{ fontSize: 12, color: T.green, marginBottom: 8 }}>
        SUBMIT FLAG
      </div>
      <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
        <input
          value={val}
          onChange={(e) => setVal(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && submit()}
          placeholder="CodeMasters{...}"
          aria-label="Flag"
          style={{ flex: 1, minWidth: 220, background: "#070A14", border: `1px solid ${T.line}`, borderRadius: 12, padding: "11px 12px", color: T.green, fontSize: 13 }}
        />
        <button className="btn btn-cyan" onClick={submit}>
          Capture 🚩
        </button>
      </div>
      <div style={{ display: "flex", gap: 8, marginTop: 10, flexWrap: "wrap" }}>
        {lab.flags.map((f) => (
          <Pill key={f.id} color={got.has(f.id) ? T.green : T.dim}>
            {got.has(f.id) ? "✓ " : "🏳 "}
            {f.label}
          </Pill>
        ))}
      </div>
      {msg && <div style={{ marginTop: 10, fontSize: 13, color: msg.ok ? T.green : T.coral }}>{msg.text}</div>}
    </div>
  );
}

function TaskList({ lab }) {
  return (
    <div className="card" style={{ padding: 16, marginBottom: 16 }}>
      <div className="px" style={{ fontSize: 12, color: lab.tint, marginBottom: 8 }}>
        MISSION BRIEFING
      </div>
      {lab.tasks.map((t, i) => (
        <div key={i} style={{ display: "flex", gap: 10, fontSize: 13.5, color: T.text, marginBottom: 6, lineHeight: 1.5 }}>
          <span className="mono" style={{ color: lab.tint }}>
            {String(i + 1).padStart(2, "0")}
          </span>
          {t}
        </div>
      ))}
    </div>
  );
}

function TerminalLab({ lab, profile, award }) {
  const [lines, setLines] = useState([
    { c: T.green, t: "Code Masters Sandbox OS v1.0 — not a real machine, hack away." },
    { c: T.dim, t: "Type 'help' for commands. Your mission briefing is on the left." },
  ]);
  const [cwd, setCwd] = useState(["home", "agent"]);
  const [input, setInput] = useState("");
  const [hist, setHist] = useState([]);
  const [hIdx, setHIdx] = useState(-1);
  const endRef = useRef(null);
  useEffect(() => {
    endRef.current?.scrollIntoView();
  }, [lines]);

  const nodeAt = (path) => path.reduce((n, seg) => (n && typeof n === "object" ? n[seg] : undefined), TERMINAL_FS);

  const exec = (raw) => {
    const out = [{ c: T.text, t: `agent@codemasters:/${cwd.join("/")}$ ${raw}` }];
    const [cmd, ...args] = raw.trim().split(/\s+/);
    const here = nodeAt(cwd);
    const push = (t, c = "#3DE8D6") => out.push({ t, c });
    switch (cmd) {
      case "":
        break;
      case "help":
        push("Commands: ls [-a], cd <dir>, cd .., cat <file>, pwd, whoami, clear, help", T.dim);
        break;
      case "pwd":
        push("/" + cwd.join("/"));
        break;
      case "whoami":
        push("agent");
        break;
      case "clear":
        setLines([]);
        return;
      case "ls": {
        const showHidden = args.includes("-a") || args.includes("-la") || args.includes("-al");
        const names = Object.keys(here).filter((n) => showHidden || !n.startsWith("."));
        if (showHidden) names.unshift(".", "..");
        push(names.map((n) => (typeof here[n] === "object" ? n + "/" : n)).join("  ") || "(empty)");
        break;
      }
      case "cd": {
        const target = args[0];
        if (!target || target === "~") {
          setCwd(["home", "agent"]);
          break;
        }
        if (target === "..") {
          if (cwd.length > 1) setCwd(cwd.slice(0, -1));
          break;
        }
        const clean = target.replace(/\/$/, "");
        if (here[clean] && typeof here[clean] === "object") setCwd([...cwd, clean]);
        else push(`cd: no such directory: ${target}`, T.coral);
        break;
      }
      case "cat": {
        const f = args[0];
        if (!f) {
          push("cat: missing filename", T.coral);
          break;
        }
        if (typeof here[f] === "string") here[f].split("\n").forEach((l) => push(l));
        else if (here[f]) push(`cat: ${f}: is a directory`, T.coral);
        else push(`cat: ${f}: no such file`, T.coral);
        break;
      }
      case "sudo":
        push("agent is not in the sudoers file. This incident will be reported. (Just kidding — but nice try.)", T.amber);
        break;
      case "rm":
        push("rm: permission denied. The sandbox regenerates anyway — chaos is futile.", T.amber);
        break;
      default:
        push(`${cmd}: command not found (try 'help')`, T.coral);
    }
    setLines((L) => [...L, ...out]);
  };

  const onKey = (e) => {
    if (e.key === "Enter") {
      exec(input);
      if (input.trim()) setHist((h) => [input, ...h]);
      setHIdx(-1);
      setInput("");
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      const ni = Math.min(hIdx + 1, hist.length - 1);
      if (hist[ni] !== undefined) {
        setHIdx(ni);
        setInput(hist[ni]);
      }
    } else if (e.key === "ArrowDown") {
      e.preventDefault();
      const ni = hIdx - 1;
      setHIdx(ni);
      setInput(ni >= 0 ? hist[ni] : "");
    }
  };

  return (
    <div>
      <div className="term" style={{ padding: 14, height: 380, overflowY: "auto", fontSize: 13 }} onClick={() => document.getElementById("term-in")?.focus()}>
        {lines.map((l, i) => (
          <div key={i} className="mono" style={{ color: l.c, whiteSpace: "pre-wrap", lineHeight: 1.55 }}>
            {l.t}
          </div>
        ))}
        <div className="mono" style={{ display: "flex", color: T.text }}>
          <span style={{ color: T.green }}>agent@codemasters</span>
          <span style={{ color: T.dim }}>:/{cwd.join("/")}$&nbsp;</span>
          <input
            id="term-in"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={onKey}
            autoFocus
            autoComplete="off"
            aria-label="Terminal input"
            style={{ flex: 1, background: "transparent", border: "none", outline: "none", color: T.text, fontSize: 13, fontFamily: "inherit", padding: 0 }}
          />
        </div>
        <div ref={endRef} />
      </div>
      <FlagSubmit lab={lab} profile={profile} award={award} />
    </div>
  );
}

function CryptoLab({ lab, profile, award }) {
  const [shift, setShift] = useState(1);
  const [b64in, setB64in] = useState("");
  const [b64out, setB64out] = useState("");
  const [caesarIn, setCaesarIn] = useState("");
  const [revIn, setRevIn] = useState("");
  const decodeB64 = () => {
    try {
      setB64out(atob(b64in.trim()));
    } catch {
      setB64out("⚠ Not valid Base64 — check for typos.");
    }
  };
  return (
    <div>
      {lab.intercepts.map((m) => (
        <div key={m.id} className="card" style={{ padding: 14, marginBottom: 10 }}>
          <div className="mono" style={{ fontSize: 11, color: T.dim, marginBottom: 6 }}>
            {m.label}
          </div>
          <div className="mono" style={{ fontSize: 13, color: T.amber, wordBreak: "break-all", background: "#070A14", padding: 10, borderRadius: 8, border: `1px solid ${T.line}` }}>
            {m.data}
          </div>
        </div>
      ))}
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(260px, 1fr))", gap: 12, marginTop: 14 }}>
        <div className="card" style={{ padding: 14 }}>
          <div className="px" style={{ fontSize: 11, color: T.cyan, marginBottom: 8 }}>
            TOOL · BASE64 DECODER
          </div>
          <textarea
            value={b64in}
            onChange={(e) => setB64in(e.target.value)}
            placeholder="Paste Base64 here…"
            aria-label="Base64 input"
            style={{ width: "100%", minHeight: 60, background: "#070A14", border: `1px solid ${T.line}`, borderRadius: 8, padding: 10, color: T.text, fontSize: 12.5, resize: "vertical" }}
          />
          <button className="btn btn-cyan" style={{ marginTop: 8 }} onClick={decodeB64}>
            Decode
          </button>
          {b64out && (
            <div className="mono" style={{ marginTop: 8, fontSize: 12.5, color: T.green, wordBreak: "break-all" }}>
              {b64out}
            </div>
          )}
        </div>
        <div className="card" style={{ padding: 14 }}>
          <div className="px" style={{ fontSize: 11, color: T.cyan, marginBottom: 8 }}>
            TOOL · CAESAR WHEEL
          </div>
          <textarea
            value={caesarIn}
            onChange={(e) => setCaesarIn(e.target.value)}
            placeholder="Paste ciphertext here…"
            aria-label="Caesar input"
            style={{ width: "100%", minHeight: 60, background: "#070A14", border: `1px solid ${T.line}`, borderRadius: 8, padding: 10, color: T.text, fontSize: 12.5, resize: "vertical" }}
          />
          <div style={{ display: "flex", alignItems: "center", gap: 10, marginTop: 8 }}>
            <span className="mono" style={{ fontSize: 12, color: T.dim }}>
              shift
            </span>
            <input type="range" min="1" max="25" value={shift} onChange={(e) => setShift(+e.target.value)} style={{ flex: 1 }} aria-label="Caesar shift" />
            <span className="mono" style={{ fontSize: 13, color: T.amber, width: 22 }}>
              {shift}
            </span>
          </div>
          {caesarIn && (
            <div className="mono" style={{ marginTop: 8, fontSize: 12.5, color: T.green, wordBreak: "break-all" }}>
              {caesarShift(caesarIn, shift)}
            </div>
          )}
        </div>
        <div className="card" style={{ padding: 14 }}>
          <div className="px" style={{ fontSize: 11, color: T.cyan, marginBottom: 8 }}>
            TOOL · MIRROR
          </div>
          <textarea
            value={revIn}
            onChange={(e) => setRevIn(e.target.value)}
            placeholder="Paste text to reverse…"
            aria-label="Reverse input"
            style={{ width: "100%", minHeight: 60, background: "#070A14", border: `1px solid ${T.line}`, borderRadius: 8, padding: 10, color: T.text, fontSize: 12.5, resize: "vertical" }}
          />
          {revIn && (
            <div className="mono" style={{ marginTop: 8, fontSize: 12.5, color: T.green, wordBreak: "break-all" }}>
              {[...revIn].reverse().join("")}
            </div>
          )}
        </div>
      </div>
      <FlagSubmit lab={lab} profile={profile} award={award} />
    </div>
  );
}

function PhishLab({ lab, profile, award }) {
  const [calls, setCalls] = useState({});
  const [revealed, setRevealed] = useState(false);
  const allCalled = lab.emails.every((e) => calls[e.id] !== undefined);
  const allRight = allCalled && lab.emails.every((e) => calls[e.id] === e.phish);
  const check = () => setRevealed(true);
  return (
    <div>
      {lab.emails.map((em) => (
        <div
          key={em.id}
          className="card"
          style={{ padding: 16, marginBottom: 12, borderLeft: revealed ? `3px solid ${calls[em.id] === em.phish ? T.green : T.coral}` : `3px solid ${T.line}` }}
        >
          <div className="mono" style={{ fontSize: 12, color: T.dim }}>
            From: <span style={{ color: T.text }}>{em.from}</span>
          </div>
          <div style={{ fontWeight: 700, fontSize: 15, margin: "6px 0" }}>{em.subject}</div>
          <div style={{ fontSize: 13.5, color: T.text, whiteSpace: "pre-wrap", lineHeight: 1.55, background: "#070A14", padding: 12, borderRadius: 8, border: `1px solid ${T.line}` }}>
            {em.body}
          </div>
          <div style={{ display: "flex", gap: 8, marginTop: 10, alignItems: "center", flexWrap: "wrap" }}>
            <button
              className="btn"
              onClick={() => !revealed && setCalls({ ...calls, [em.id]: false })}
              style={{ borderColor: calls[em.id] === false ? T.green : T.line, background: calls[em.id] === false ? `color-mix(in srgb, ${T.green} 15%, transparent)` : T.panel2 }}
            >
              ✅ Legit
            </button>
            <button
              className="btn"
              onClick={() => !revealed && setCalls({ ...calls, [em.id]: true })}
              style={{ borderColor: calls[em.id] === true ? T.coral : T.line, background: calls[em.id] === true ? `color-mix(in srgb, ${T.coral} 15%, transparent)` : T.panel2 }}
            >
              🎣 Phish
            </button>
            {revealed && (
              <span style={{ fontSize: 13, color: calls[em.id] === em.phish ? T.green : T.coral }}>
                {calls[em.id] === em.phish ? "✓ Correct." : "✗ Wrong call."} {em.why}
              </span>
            )}
          </div>
        </div>
      ))}
      <button className="btn btn-amber" disabled={!allCalled || revealed} onClick={check}>
        Submit verdicts
      </button>
      {revealed && !allRight && (
        <button
          className="btn"
          style={{ marginLeft: 8 }}
          onClick={() => {
            setCalls({});
            setRevealed(false);
          }}
        >
          ⟳ Re-triage inbox
        </button>
      )}
      {revealed && allRight && (
        <div className="pop" style={{ marginTop: 12, padding: 14, borderRadius: 12, border: `1px solid ${T.green}`, background: `color-mix(in srgb, ${T.green} 10%, transparent)` }}>
          <div style={{ fontWeight: 700, color: T.green }}>Perfect triage, analyst. 🕵️</div>
          <div className="mono" style={{ marginTop: 6, fontSize: 13, color: T.green }}>
            Your flag: {lab.flags[0].value}
          </div>
        </div>
      )}
      <FlagSubmit lab={lab} profile={profile} award={award} />
    </div>
  );
}

function QuestionLab({ lab, profile, award }) {
  const [answers, setAnswers] = useState({});
  const [status, setStatus] = useState({});
  const [hints, setHints] = useState({});
  const norm = (s) => (s || "").trim().toLowerCase().replace(/\s+/g, " ");
  const check = (q) => setStatus((st) => ({ ...st, [q.id]: q.answer.some((a) => norm(a) === norm(answers[q.id])) ? "right" : "wrong" }));
  const allRight = lab.questions.every((q) => status[q.id] === "right");
  return (
    <div>
      <div className="term" style={{ padding: 14, marginBottom: 14, overflowX: "auto" }}>
        <div className="mono" style={{ fontSize: 11, color: T.dim, marginBottom: 8 }}>
          /var/log/auth.log — srv01 (excerpt)
        </div>
        <pre className="mono" style={{ margin: 0, fontSize: 12, lineHeight: 1.7, color: "#3DE8D6", whiteSpace: "pre" }}>
          {lab.log}
        </pre>
      </div>
      {lab.questions.map((q, i) => (
        <div key={q.id} className="card" style={{ padding: 14, marginBottom: 10 }}>
          <div style={{ fontSize: 14, fontWeight: 600, marginBottom: 8 }}>
            <span className="mono" style={{ color: lab.tint }}>
              Q{i + 1}
            </span>{" "}
            {q.q}
          </div>
          <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
            <input
              value={answers[q.id] || ""}
              onChange={(e) => setAnswers({ ...answers, [q.id]: e.target.value })}
              onKeyDown={(e) => e.key === "Enter" && check(q)}
              placeholder="Your answer…"
              aria-label={"Answer " + (i + 1)}
              style={{ flex: 1, minWidth: 180, background: "#070A14", border: `1px solid ${T.line}`, borderRadius: 8, padding: "10px 12px", color: T.text, fontSize: 13 }}
            />
            <button className="btn" onClick={() => check(q)}>
              Check
            </button>
            <button className="btn btn-ghost" onClick={() => setHints({ ...hints, [q.id]: true })}>
              💡
            </button>
          </div>
          {hints[q.id] && (
            <div style={{ marginTop: 8, fontSize: 13, color: T.amber }}>
              💡 {q.hint}
            </div>
          )}
          {status[q.id] && (
            <div style={{ marginTop: 8, fontSize: 13, fontWeight: 600, color: status[q.id] === "right" ? T.green : T.coral }}>
              {status[q.id] === "right" ? "✓ Confirmed, analyst." : "✗ Re-read the log — the evidence is there."}
            </div>
          )}
        </div>
      ))}
      {allRight && (
        <div className="pop" style={{ padding: 14, borderRadius: 12, border: `1px solid ${T.green}`, background: `color-mix(in srgb, ${T.green} 10%, transparent)` }}>
          <div style={{ fontWeight: 700, color: T.green }}>Attack traced end to end. 🕵️ Report filed.</div>
          <div className="mono" style={{ marginTop: 6, fontSize: 13, color: T.green }}>
            Your flag: {lab.flags[0].value}
          </div>
        </div>
      )}
      <FlagSubmit lab={lab} profile={profile} award={award} />
    </div>
  );
}

export function LabView({ labId, profile, award, go, setTutorCtx }) {
  const lab = LABS.find((l) => l.id === labId);
  useEffect(() => {
    setTutorCtx(`Cyber lab "${lab.name}": ${lab.desc}. Give hints only, never the flag values.`);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [labId]);
  return (
    <div>
      <button className="btn btn-ghost" onClick={() => go({ page: "labs" })}>
        ← Labs
      </button>
      <div style={{ display: "flex", gap: 12, alignItems: "center", margin: "12px 0 14px" }}>
        <span style={{ fontSize: 34 }}>{lab.icon}</span>
        <div>
          <h1 className="px" style={{ fontSize: 18, margin: 0, color: lab.tint }}>
            {lab.name}
          </h1>
          <Pill color={lab.tint}>{lab.difficulty}</Pill>
        </div>
      </div>
      <div style={{ display: "grid", gridTemplateColumns: "280px minmax(0,1fr)", gap: 16 }} className="lab-grid">
        <style>{`@media (max-width: 900px){ .lab-grid { grid-template-columns: 1fr !important; } }`}</style>
        <TaskList lab={lab} />
        <div>
          {labId === "lab-terminal" && <TerminalLab lab={lab} profile={profile} award={award} />}
          {labId === "lab-crypto" && <CryptoLab lab={lab} profile={profile} award={award} />}
          {labId === "lab-phish" && <PhishLab lab={lab} profile={profile} award={award} />}
          {lab.kind === "questions" && <QuestionLab lab={lab} profile={profile} award={award} />}
        </div>
      </div>
    </div>
  );
}
