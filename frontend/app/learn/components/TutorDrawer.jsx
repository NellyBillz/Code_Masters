"use client";

import { useEffect, useRef, useState } from "react";
import { T } from "../theme";
import { askTutor } from "../engine";

/**
 * "Byte", the AI tutor drawer. Logic ported from the original TutorDrawer;
 * the only behavioral change is that askClaude() (a direct, keyless browser
 * call to api.anthropic.com that only worked in the Artifacts sandbox) is
 * now askTutor(), which calls our own /api/learn/ai proxy and can
 * genuinely fail with "not configured" in a real deployment — handled
 * below with an honest inline notice instead of pretending Byte is just
 * being slow.
 */
export default function TutorDrawer({ open, onClose, context }) {
  const [msgs, setMsgs] = useState([
    { role: "assistant", text: "Hey! I'm Byte, your Code Masters tutor. Stuck on a lesson, a challenge, or a lab? Ask me anything — I'll nudge, not spoil." },
  ]);
  const [input, setInput] = useState("");
  const [busy, setBusy] = useState(false);
  const endRef = useRef(null);
  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [msgs, open]);

  const send = async () => {
    const q = input.trim();
    if (!q || busy) return;
    const next = [...msgs, { role: "user", text: q }];
    setMsgs(next);
    setInput("");
    setBusy(true);
    try {
      const history = next
        .slice(-8)
        .map((m) => `${m.role === "user" ? "Student" : "Byte"}: ${m.text}`)
        .join("\n");
      const prompt = `You are Byte, the friendly in-app tutor for Code Masters, a gamified platform teaching programming (Python, Java, JavaScript, HTML/CSS, SQL, cloud basics) and beginner cybersecurity fundamentals (defensive concepts, CTF-style puzzle labs). Personality: encouraging, playful, concise (under 150 words). Teach with hints and questions before full answers. Never reveal lab flag values outright. Refuse anything about real-world attacking, malware, or targeting real systems — redirect to defensive concepts.

Current student context: ${context || "browsing the dashboard"}.

Conversation so far:
${history}

Reply as Byte (plain text, no markdown headers):`;
      const reply = await askTutor(prompt);
      setMsgs((m) => [...m, { role: "assistant", text: reply }]);
    } catch (e) {
      setMsgs((m) => [...m, { role: "assistant", text: e?.message || "Hmm, I couldn't reach my brain just now. Try again in a moment!" }]);
    }
    setBusy(false);
  };

  if (!open) return null;
  return (
    <div
      style={{
        position: "fixed",
        top: 0,
        right: 0,
        bottom: 0,
        width: "min(400px, 100vw)",
        zIndex: 50,
        background: T.panel,
        borderLeft: `1px solid ${T.line}`,
        display: "flex",
        flexDirection: "column",
        backdropFilter: "var(--cm-card-blur)",
      }}
      className="rise"
    >
      <div style={{ padding: "14px 16px", borderBottom: `1px solid ${T.line}`, display: "flex", alignItems: "center", gap: 10 }}>
        <span style={{ fontSize: 22 }}>🤖</span>
        <div style={{ flex: 1 }}>
          <div className="px" style={{ fontSize: 12, color: T.cyan }}>
            BYTE · AI TUTOR
          </div>
          <div style={{ fontSize: 12, color: T.dim }}>Hints over spoilers</div>
        </div>
        <button className="btn btn-ghost" onClick={onClose} aria-label="Close tutor">
          ✕
        </button>
      </div>
      <div style={{ flex: 1, overflowY: "auto", padding: 16 }}>
        {msgs.map((m, i) => (
          <div key={i} style={{ marginBottom: 12, display: "flex", justifyContent: m.role === "user" ? "flex-end" : "flex-start" }}>
            <div
              style={{
                maxWidth: "85%",
                padding: "10px 14px",
                fontSize: 14,
                lineHeight: 1.5,
                whiteSpace: "pre-wrap",
                background: m.role === "user" ? `color-mix(in srgb, ${T.violet} 20%, transparent)` : T.panel2,
                border: `1px solid ${m.role === "user" ? `color-mix(in srgb, ${T.violet} 40%, transparent)` : T.line}`,
                borderRadius: m.role === "user" ? "14px 14px 4px 14px" : "14px 14px 14px 4px",
              }}
            >
              {m.text}
            </div>
          </div>
        ))}
        {busy && (
          <div className="mono cursor" style={{ color: T.cyan, fontSize: 13 }}>
            Byte is thinking ▮
          </div>
        )}
        <div ref={endRef} />
      </div>
      <div style={{ padding: 12, borderTop: `1px solid ${T.line}`, display: "flex", gap: 8 }}>
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && send()}
          placeholder="Ask Byte anything…"
          style={{ flex: 1, background: T.ink, border: `1px solid ${T.line}`, borderRadius: 999, padding: "10px 14px", color: T.text, fontSize: 13 }}
        />
        <button className="btn btn-cyan" onClick={send} disabled={busy}>
          Send
        </button>
      </div>
    </div>
  );
}
