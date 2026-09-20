-- Create reports table backing abuse reporting
CREATE TABLE reports (
    id BIGSERIAL PRIMARY KEY,
    reporter_user_id BIGINT NOT NULL,
    target_type TEXT NOT NULL CONSTRAINT chk_reports_target_type CHECK (target_type IN ('comment', 'project')),
    target_id BIGINT NOT NULL,
    reason TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'open' CONSTRAINT chk_reports_status CHECK (status IN ('open', 'resolved', 'dismissed')),
    resolution TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    resolved_at TIMESTAMPTZ,
    CONSTRAINT fk_reports_reporter_user FOREIGN KEY (reporter_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_reports_reporter_target UNIQUE (reporter_user_id, target_type, target_id)
);