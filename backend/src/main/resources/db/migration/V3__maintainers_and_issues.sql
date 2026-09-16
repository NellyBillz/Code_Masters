-- 1. Create project_maintainers table
CREATE TABLE project_maintainers (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role TEXT NOT NULL CHECK (role IN ('owner', 'maintainer')),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT fk_project_maintainers_projects FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_project_maintainers_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_project_maintainers_project_user UNIQUE (project_id, user_id)
);

-- 2. Create issues table
CREATE TABLE issues (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    github_issue_id BIGINT,
    github_issue_number INTEGER NOT NULL,
    github_url TEXT NOT NULL,
    title TEXT NOT NULL,
    body_excerpt TEXT,
    status TEXT NOT NULL CHECK (status IN ('open', 'closed', 'claimed')),
    labels TEXT[],
    difficulty TEXT CHECK (difficulty IN ('beginner', 'intermediate', 'advanced', 'unknown')),
    is_beginner_friendly BOOLEAN DEFAULT FALSE,
    difficulty_overridden_by_user_id BIGINT,
    maintainer_response_score NUMERIC,
    project_health_score NUMERIC,
    freshness_score NUMERIC,
    contribution_score NUMERIC,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT fk_issues_projects FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_issues_override_users FOREIGN KEY (difficulty_overridden_by_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uq_issues_project_issue_number UNIQUE (project_id, github_issue_number)
);