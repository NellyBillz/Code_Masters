-- In-app notifications: a small, fixed set of state-change events a user
-- can't otherwise learn about without manually revisiting a page (a claim
-- reviewed, a collaboration request received/responded to, a project
-- submission moderated). Deliberately not every event — a new comment, for
-- instance, is a conversational reply, not a state change, and notifying on
-- every one would turn this into noise rather than signal.
--
-- The message is pre-rendered server-side (not a structured payload the
-- frontend has to know how to interpret per type) — simplest thing that
-- works, and every event type has a fixed, simple shape anyway.
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type TEXT NOT NULL
        CONSTRAINT chk_notifications_type CHECK (
            type IN ('claim_reviewed', 'collaboration_requested', 'collaboration_responded', 'project_moderated')
        ),
    message TEXT NOT NULL,
    link TEXT,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Backs both the paginated list (newest first) and the unread-count query.
CREATE INDEX idx_notifications_user_created ON notifications (user_id, created_at DESC);
CREATE INDEX idx_notifications_user_unread ON notifications (user_id) WHERE read_at IS NULL;
