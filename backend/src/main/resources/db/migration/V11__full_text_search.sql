-- 1. Enable pg_trgm extension for trigram similarity and typo-tolerant matching
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 2. Projects: generated tsvector column weighted name ('A'), description ('B'), github_owner ('C')
ALTER TABLE projects
    ADD COLUMN tsv tsvector GENERATED ALWAYS AS (
        setweight(to_tsvector('english', coalesce(name, '')), 'A') ||
        setweight(to_tsvector('english', coalesce(description, '')), 'B') ||
        setweight(to_tsvector('english', coalesce(github_owner, '')), 'C')
    ) STORED;

-- GIN index for full-text search
CREATE INDEX idx_projects_tsv ON projects USING gin(tsv);

-- Trigram GIN index for typo-tolerant fallback matching on name
CREATE INDEX idx_projects_name_trgm ON projects USING gin(name gin_trgm_ops);

-- 3. Issues: generated tsvector column weighted title ('A'), body_excerpt ('B')
ALTER TABLE issues
    ADD COLUMN tsv tsvector GENERATED ALWAYS AS (
        setweight(to_tsvector('english', coalesce(title, '')), 'A') ||
        setweight(to_tsvector('english', coalesce(body_excerpt, '')), 'B')
    ) STORED;

-- GIN index for full-text search
CREATE INDEX idx_issues_tsv ON issues USING gin(tsv);

-- Trigram GIN index for typo-tolerant fallback matching on title
CREATE INDEX idx_issues_title_trgm ON issues USING gin(title gin_trgm_ops);