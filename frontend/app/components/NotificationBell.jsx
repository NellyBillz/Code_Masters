"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { Bell, CheckCheck } from "lucide-react";
import {
  getNotifications,
  getUnreadNotificationCount,
  markNotificationRead,
  markAllNotificationsRead,
} from "../../lib/api";
import { useAuth } from "../context/AuthContext";

const POLL_INTERVAL_MS = 45000;

function formatRelative(isoString) {
  if (!isoString) return "";
  const date = new Date(isoString);
  if (Number.isNaN(date.getTime())) return "";
  const seconds = Math.max(0, Math.floor((Date.now() - date.getTime()) / 1000));
  if (seconds < 60) return "just now";
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  return `${days}d ago`;
}

/**
 * The bell icon in the header: a poll-based unread count and a dropdown
 * list of the caller's own notifications. Deliberately not real-time
 * (no websocket) — a periodic poll is simple, safe, and good enough for a
 * small, fixed set of state-change events. Renders nothing for a
 * logged-out visitor.
 */
export default function NotificationBell() {
  const { user } = useAuth();
  const [unreadCount, setUnreadCount] = useState(0);
  const [open, setOpen] = useState(false);
  const [notifications, setNotifications] = useState(null);
  const [error, setError] = useState("");
  const containerRef = useRef(null);

  useEffect(() => {
    if (!user) return undefined;

    let cancelled = false;

    async function pollCount() {
      try {
        const result = await getUnreadNotificationCount();
        if (!cancelled) setUnreadCount(result.count || 0);
      } catch {
        // Silent — the bell just keeps showing its last known count.
      }
    }

    pollCount();
    const interval = setInterval(pollCount, POLL_INTERVAL_MS);
    return () => {
      cancelled = true;
      clearInterval(interval);
    };
  }, [user]);

  useEffect(() => {
    function handleClickOutside(event) {
      if (containerRef.current && !containerRef.current.contains(event.target)) {
        setOpen(false);
      }
    }
    if (open) {
      document.addEventListener("mousedown", handleClickOutside);
      return () => document.removeEventListener("mousedown", handleClickOutside);
    }
    return undefined;
  }, [open]);

  async function loadList() {
    setError("");
    try {
      const result = await getNotifications({ size: 10 });
      setNotifications(result.items || []);
    } catch (err) {
      setError(err.message || "Failed to load notifications.");
    }
  }

  function handleToggle() {
    const next = !open;
    setOpen(next);
    if (next) loadList().catch(() => {});
  }

  async function handleItemClick(notification) {
    if (!notification.read) {
      try {
        await markNotificationRead(notification.id);
        setUnreadCount((count) => Math.max(0, count - 1));
        setNotifications((prev) =>
          (prev || []).map((n) => (n.id === notification.id ? { ...n, read: true } : n))
        );
      } catch {
        // Non-fatal — the click still navigates via the link below regardless.
      }
    }
    setOpen(false);
  }

  async function handleMarkAllRead() {
    try {
      await markAllNotificationsRead();
      setUnreadCount(0);
      setNotifications((prev) => (prev || []).map((n) => ({ ...n, read: true })));
    } catch (err) {
      setError(err.message || "Failed to mark all as read.");
    }
  }

  if (!user) return null;

  return (
    <div ref={containerRef} style={{ position: "relative" }}>
      <button
        type="button"
        onClick={handleToggle}
        aria-label={unreadCount > 0 ? `${unreadCount} unread notifications` : "Notifications"}
        style={{
          position: "relative",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          width: "32px",
          height: "32px",
          borderRadius: "999px",
          background: "none",
          border: "none",
          color: "var(--cm-text-secondary)",
          cursor: "pointer",
        }}
      >
        <Bell size={17} strokeWidth={1.8} aria-hidden="true" />
        {unreadCount > 0 && (
          <span
            aria-hidden="true"
            style={{
              position: "absolute",
              top: "2px",
              right: "2px",
              minWidth: "15px",
              height: "15px",
              padding: "0 3px",
              borderRadius: "999px",
              background: "var(--cm-orange)",
              color: "#2B1108",
              fontSize: "9.5px",
              fontWeight: 700,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
            }}
          >
            {unreadCount > 9 ? "9+" : unreadCount}
          </span>
        )}
      </button>

      {open && (
        <div
          className="cm-glass"
          style={{
            position: "absolute",
            top: "calc(100% + 8px)",
            right: 0,
            width: "320px",
            maxHeight: "420px",
            overflowY: "auto",
            borderRadius: "16px",
            padding: "8px",
            zIndex: 50,
          }}
        >
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: "6px 8px 10px" }}>
            <span style={{ fontSize: "12.5px", fontWeight: 700, color: "var(--cm-text-primary)" }}>Notifications</span>
            {unreadCount > 0 && (
              <button
                type="button"
                onClick={handleMarkAllRead}
                style={{ display: "inline-flex", alignItems: "center", gap: "4px", background: "none", border: "none", padding: 0, fontSize: "11px", fontWeight: 600, color: "var(--cm-lime-text)", cursor: "pointer" }}
              >
                <CheckCheck size={11} strokeWidth={2} aria-hidden="true" />
                Mark all read
              </button>
            )}
          </div>

          {error && (
            <p role="alert" style={{ fontSize: "11.5px", color: "var(--cm-orange-text)", padding: "0 8px 8px" }}>
              {error}
            </p>
          )}

          {notifications === null ? (
            <p style={{ fontSize: "12px", color: "var(--cm-text-muted)", padding: "8px" }}>Loading…</p>
          ) : notifications.length === 0 ? (
            <p style={{ fontSize: "12px", color: "var(--cm-text-muted)", padding: "8px" }}>No notifications yet.</p>
          ) : (
            <div style={{ display: "flex", flexDirection: "column", gap: "4px" }}>
              {notifications.map((notification) => {
                const content = (
                  <div
                    style={{
                      display: "flex",
                      alignItems: "flex-start",
                      gap: "8px",
                      borderRadius: "10px",
                      padding: "8px",
                      background: notification.read ? "transparent" : "var(--cm-surface-alt)",
                    }}
                  >
                    <span
                      aria-hidden="true"
                      style={{
                        marginTop: "5px",
                        width: "6px",
                        height: "6px",
                        borderRadius: "999px",
                        background: notification.read ? "transparent" : "var(--cm-orange)",
                        flexShrink: 0,
                      }}
                    />
                    <div style={{ minWidth: 0 }}>
                      <p style={{ fontSize: "12.5px", color: "var(--cm-text-primary)", margin: 0, lineHeight: 1.4 }}>
                        {notification.message}
                      </p>
                      <p style={{ fontSize: "10.5px", color: "var(--cm-text-muted)", margin: "3px 0 0" }}>
                        {formatRelative(notification.createdAt)}
                      </p>
                    </div>
                  </div>
                );

                return notification.link ? (
                  <Link
                    key={notification.id}
                    href={notification.link}
                    onClick={() => handleItemClick(notification)}
                  >
                    {content}
                  </Link>
                ) : (
                  <div key={notification.id} onClick={() => handleItemClick(notification)} style={{ cursor: "pointer" }}>
                    {content}
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
