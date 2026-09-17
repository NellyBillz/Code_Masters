-- Ensure UUID generation extension is available
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Create sync_jobs table
CREATE TABLE sync_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id BIGINT NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('accepted', 'running', 'completed', 'failed')),
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    error_message TEXT,
    retry_after TIMESTAMPTZ,
    issues_created_count INTEGER,
    issues_updated_count INTEGER,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT fk_sync_jobs_projects FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);