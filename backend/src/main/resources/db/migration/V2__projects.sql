-- 1. Create projects table
CREATE TABLE projects (
    id BIGSERIAL PRIMARY KEY,
    github_owner TEXT NOT NULL,
    github_repo TEXT NOT NULL,
    github_url TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    slug TEXT NOT NULL UNIQUE,
    description TEXT,
    primary_language TEXT,
    languages TEXT[],
    category TEXT,
    connection TEXT NOT NULL CHECK (
        connection IN ('south_african', 'africa_focused', 'africa_led', 'community_verified')
    ),
    license TEXT,
    stars INTEGER DEFAULT 0,
    forks INTEGER DEFAULT 0,
    open_issues INTEGER DEFAULT 0,
    contributors INTEGER DEFAULT 0,
    has_beginner_friendly_issues BOOLEAN DEFAULT FALSE,
    last_activity_at TIMESTAMPTZ,
    verified BOOLEAN DEFAULT FALSE,
    verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. Create project_tags table
CREATE TABLE project_tags (
    project_id BIGINT NOT NULL,
    tag TEXT NOT NULL,
    PRIMARY KEY (project_id, tag),
    CONSTRAINT fk_project_tags_projects FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

-- 3. Create project_countries table
CREATE TABLE project_countries (
    project_id BIGINT NOT NULL,
    country_code TEXT NOT NULL,
    PRIMARY KEY (project_id, country_code),
    CONSTRAINT fk_project_countries_projects FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

-- Index for ?country=ZA filtering
CREATE INDEX idx_project_countries_country_code ON project_countries(country_code);