"use client";

import { useEffect, useState } from "react";
import { RefreshCw, CheckCircle2, AlertTriangle } from "lucide-react";
import { syncProject, getSyncJob } from "../../lib/api";

const POLL_INTERVAL_MS = 1500;
const MAX_POLL_ATTEMPTS = 15;

// Maps 1:1 to the `status` values SyncJob actually returns (accepted,
// running, completed, failed) — "accepted" is shown to the user as
// "Pending" since a sync job hasn't started running yet at that point.
// Deliberately no progress percentage anywhere here: the API doesn't
// return one, so nothing here should invent one.
const STATUS_LABELS = {
  accepted: "Pending",
  running: "Running",
  completed: "Completed",
  failed: "Failed",
};

/**
 * Surfaces GitHub sync status on a project page, previously a sync could
 * fail (or still be running) with zero visible feedback, which just looked
 * like the page was stuck. Tracks one job at a time: either the job just
 * created by /projects/new's auto-sync-on-submit (passed in via
 * `initialJobId`, e.g. from a `?syncJobId=` URL param), or one a maintainer
 * starts manually from the button this component renders.
 */
export default function SyncStatusBanner({ projectId, initialJobId, isMaintainer, onSynced }) {
  const [jobId, setJobId] = useState(initialJobId || null);
  const [job, setJob] = useState(null);
  const [triggering, setTriggering] = useState(false);
  const [triggerError, setTriggerError] = useState("");
  const [timedOut, setTimedOut] = useState(false);

  const [pollGeneration, setPollGeneration] = useState(0);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- resetting timeout state for the newly-tracked job/recheck, not a fetch
    setTimedOut(false);

    if (!jobId) {
      // eslint-disable-next-line react-hooks/set-state-in-effect -- clearing stale job state when the tracked jobId is cleared, not a fetch
      setJob(null);
      return;
    }

    let cancelled = false;

    async function poll() {
      for (let attempt = 0; attempt < MAX_POLL_ATTEMPTS && !cancelled; attempt += 1) {
        let current;
        try {
          current = await getSyncJob(jobId);
        } catch {
          return;
        }
        if (cancelled) return;
        setJob(current);

        if (current.status === "completed" || current.status === "failed") {
          if (current.status === "completed") onSynced?.();
          return;
        }
        await new Promise((resolve) => setTimeout(resolve, POLL_INTERVAL_MS));
      }
      // Exhausted every attempt and the job never resolved — say so, rather
      // than leaving "Syncing…" up forever with no indication we gave up.
      if (!cancelled) setTimedOut(true);
    }

    poll();
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps -- onSynced is a fresh function each render; re-polling on that change would restart an in-flight poll for no reason. jobId/pollGeneration are the only things that should start a new poll.
  }, [jobId, pollGeneration]);

  async function handleTrigger() {
    setTriggering(true);
    setTriggerError("");
    setTimedOut(false);
    try {
      const newJob = await syncProject(projectId);
      setJob(newJob);
      setJobId(newJob.id);
    } catch (err) {
      setTriggerError(err.message || "Failed to start sync.");
    } finally {
      setTriggering(false);
    }
  }

  function handleRecheck() {
    setTriggerError("");
    setPollGeneration((g) => g + 1);
  }

  const pending = job && !timedOut && job.status === "accepted";
  const running = job && !timedOut && job.status === "running";
  const inProgress = pending || running;
  const failed = job && job.status === "failed";
  const completed = job && job.status === "completed";

  const triggerLabel = triggering
    ? "Starting…"
    : failed
      ? "Retry sync"
      : timedOut
        ? "Check again"
        : completed
          ? "Sync again"
          : "Sync GitHub data";

  // Nothing tracked yet and nothing this viewer can do about it.
  if (!jobId && !job && !isMaintainer) return null;

  // A maintainer with no active/past job to show yet: a plain small trigger,
  // not a full banner, there's nothing to report until they ask for it.
  if (!jobId && !job) {
    return (
      <button
        type="button"
        onClick={handleTrigger}
        disabled={triggering}
        style={{
          display: "inline-flex",
          alignItems: "center",
          gap: "6px",
          fontSize: "12.5px",
          fontWeight: 600,
          color: "var(--cm-text-secondary)",
          background: "none",
          border: "none",
          cursor: triggering ? "not-allowed" : "pointer",
          marginBottom: "16px",
        }}
      >
        <RefreshCw size={13} strokeWidth={2} aria-hidden="true" />
        {triggerLabel}
      </button>
    );
  }

  const style = failed
    ? { bg: "var(--cm-orange-soft)", text: "var(--cm-orange-text)", Icon: AlertTriangle }
    : completed
      ? { bg: "var(--cm-lime-soft)", text: "var(--cm-lime-text)", Icon: CheckCircle2 }
      : { bg: "var(--cm-surface-alt)", text: "var(--cm-text-secondary)", Icon: RefreshCw };

  return (
    <div
      className="cm-glass"
      style={{
        display: "flex",
        alignItems: "center",
        justifyContent: "space-between",
        gap: "12px",
        borderRadius: "16px",
        padding: "12px 18px",
        marginBottom: "20px",
        background: style.bg,
        flexWrap: "wrap",
      }}
    >
      <div style={{ display: "flex", alignItems: "center", gap: "8px", color: style.text, fontSize: "13px", fontWeight: 600, minWidth: 0 }}>
        <style.Icon size={15} strokeWidth={2} aria-hidden="true" />
        {job && !timedOut && (
          <span
            style={{
              fontSize: "10.5px",
              fontWeight: 700,
              letterSpacing: "0.04em",
              textTransform: "uppercase",
              padding: "2px 8px",
              borderRadius: "999px",
              background: "rgba(0,0,0,0.16)",
              flexShrink: 0,
            }}
          >
            {STATUS_LABELS[job.status] || job.status}
          </span>
        )}
        <span>
          {!job && "Checking sync status…"}
          {pending && "Waiting to start…"}
          {running && "Syncing project data from GitHub…"}
          {timedOut && "Still running in the background — taking longer than expected. Check back shortly, or try again."}
          {completed &&
            `Synced, ${job.issuesCreatedCount ?? 0} issue${job.issuesCreatedCount === 1 ? "" : "s"} created, ${job.issuesUpdatedCount ?? 0} updated.`}
          {failed && (job.errorMessage || "GitHub sync failed.")}
        </span>
      </div>

      {isMaintainer && !inProgress && (
        <button
          type="button"
          onClick={timedOut ? handleRecheck : handleTrigger}
          disabled={triggering}
          style={{
            fontSize: "12px",
            fontWeight: 700,
            padding: "6px 14px",
            borderRadius: "999px",
            border: "none",
            cursor: triggering ? "not-allowed" : "pointer",
            opacity: triggering ? 0.6 : 1,
            background: "var(--cm-sidebar)",
            color: "#FFFFFF",
            flexShrink: 0,
          }}
        >
          {triggerLabel}
        </button>
      )}

      {triggerError && (
        <p role="alert" style={{ width: "100%", fontSize: "11.5px", color: "var(--cm-orange-text)", margin: 0 }}>
          {triggerError}
        </p>
      )}
    </div>
  );
}