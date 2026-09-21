"use client";

import { useState } from "react";
import { friendlyErrorMessage } from "../../lib/api";

const STATE_IDLE = "idle";
const STATE_FORM = "form";
const STATE_SUBMITTING = "submitting";
const STATE_REPORTED = "reported";
const STATE_ALREADY_REPORTED = "already_reported";

/**
 * A "Report" action reused on comments and on the project page (FE-03.7).
 * Deliberately doesn't expose anything about the admin queue to the
 * reporter — just a simple, reassuring confirmation, or a clear
 * "already reported" state on a repeat attempt (caught from the backend's
 * 409 REPORT_ALREADY_EXISTS, not tracked client-side).
 */
export default function ReportButton({ onSubmit, label = "Report" }) {
    const [state, setState] = useState(STATE_IDLE);
    const [reason, setReason] = useState("");
    const [error, setError] = useState("");

    async function handleSubmit(event) {
        event.preventDefault();

        const trimmed = reason.trim();
        if (trimmed.length < 3) {
            setError("Tell us a bit more (at least 3 characters).");
            return;
        }

        setState(STATE_SUBMITTING);
        setError("");

        try {
            await onSubmit(trimmed);
            setState(STATE_REPORTED);
        } catch (err) {
            if (err?.code === "REPORT_ALREADY_EXISTS") {
                setState(STATE_ALREADY_REPORTED);
            } else {
                setError(friendlyErrorMessage(err, "Failed to send report. Please try again."));
                setState(STATE_FORM);
            }
        }
    }

    if (state === STATE_REPORTED) {
        return (
            <p role="status" style={{ fontSize: "0.85rem", color: "#166534" }}>
                Thanks — this has been sent to our moderation team.
            </p>
        );
    }

    if (state === STATE_ALREADY_REPORTED) {
        return (
            <p style={{ fontSize: "0.85rem", color: "#555" }}>
                You&apos;ve already reported this.
            </p>
        );
    }

    if (state === STATE_IDLE) {
        return (
            <button
                type="button"
                onClick={() => setState(STATE_FORM)}
                style={{
                    fontSize: "0.85rem",
                    background: "none",
                    border: "none",
                    color: "#b00020",
                    cursor: "pointer",
                    padding: 0,
                    textDecoration: "underline",
                }}
            >
                {label}
            </button>
        );
    }

    const submitting = state === STATE_SUBMITTING;

    return (
        <form onSubmit={handleSubmit} style={{ marginTop: "0.5rem" }}>
            <textarea
                value={reason}
                onChange={(event) => setReason(event.target.value)}
                placeholder="Why are you reporting this?"
                rows={2}
                disabled={submitting}
                style={{ width: "100%", padding: "0.4rem", fontSize: "0.85rem" }}
            />

            <div style={{ display: "flex", gap: "0.5rem", marginTop: "0.35rem" }}>
                <button type="submit" disabled={submitting}>
                    {submitting ? "Sending..." : "Submit report"}
                </button>

                <button
                    type="button"
                    onClick={() => {
                        setState(STATE_IDLE);
                        setReason("");
                        setError("");
                    }}
                    disabled={submitting}
                >
                    Cancel
                </button>
            </div>

            {error && (
                <p
                    role="alert"
                    style={{ color: "#b00020", fontSize: "0.8rem", marginTop: "0.35rem" }}
                >
                    {error}
                </p>
            )}
        </form>
    );
}
