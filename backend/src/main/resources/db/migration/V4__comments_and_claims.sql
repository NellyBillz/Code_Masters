-- 1. Create comments table
CREATE TABLE comments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    project_id BIGINT,
    issue_id BIGINT,
    body TEXT NOT NULL,
    edited BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT fk_comments_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_projects FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_issues FOREIGN KEY (issue_id) REFERENCES issues(id) ON DELETE CASCADE,
    -- Exactly one of project_id or issue_id must be non-null
    CONSTRAINT chk_comments_target_exclusive CHECK (
        (project_id IS NOT NULL AND issue_id IS NULL) OR
        (project_id IS NULL AND issue_id IS NOT NULL)
    )
);

-- 2. Create claims table
CREATE TABLE claims (
    id BIGSERIAL PRIMARY KEY,
    issue_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('active', 'released', 'completed')),
    note TEXT, -- max 280 chars enforced in application layer
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT fk_claims_issues FOREIGN KEY (issue_id) REFERENCES issues(id) ON DELETE CASCADE,
    CONSTRAINT fk_claims_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Partial unique index: Exactly one 'active' claim per (issue_id, user_id)
CREATE UNIQUE INDEX uq_claims_active_per_user_issue 
ON claims (issue_id, user_id) 
WHERE status = 'active';