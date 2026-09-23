"use client";

import { useEffect, useState } from "react";

export default function LiveClock() {
  // Must start as null on both server and client's first render, the
  // server has no Date to show, and the client's first render has to
  // match that or React logs a hydration mismatch. The real value is set
  // in the effect below, which only ever runs post-mount, client-side.
  const [now, setNow] = useState(null);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- intentional: this IS the client-only-value pattern the rule is guarding against misuse of; there's no derived-state alternative for "the current time".
    setNow(new Date());
    const id = setInterval(() => setNow(new Date()), 30000);
    return () => clearInterval(id);
  }, []);

  if (!now) return null;

  return (
    <span style={{ fontSize: "12px", color: "var(--cm-text-secondary)", whiteSpace: "nowrap" }}>
      {now.toLocaleTimeString(undefined, { hour: "2-digit", minute: "2-digit" })}
    </span>
  );
}
