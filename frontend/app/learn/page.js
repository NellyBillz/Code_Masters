"use client";

import LearnApp from "./LearnApp";

/**
 * /learn is Code Masters' own gamified learning path: courses, hands-on
 * challenges, CTF-style labs, mock interviews, and an AI tutor ("Byte").
 *
 * This used to be a standalone prototype called "TechAway" that got mounted
 * here as a near-verbatim port. That name has been retired -- everything
 * below now presents itself as Code Masters' Learn section, using this
 * app's own auth (useAuth) and design tokens where the two overlap. See
 * LearnApp.jsx for how it plugs into the shared Sidebar/SiteHeader chrome.
 */
export default function LearnPage() {
  return <LearnApp />;
}
