-- Repeatable Dev Seed Data
-- Cleans up previous seed rows before re-populating to guarantee idempotency

DELETE FROM claims WHERE note LIKE '[dev-seed]%';
DELETE FROM comments WHERE body LIKE '[dev-seed]%';
DELETE FROM issues WHERE github_url LIKE 'https://github.com/codemaster/%';
DELETE FROM project_tags WHERE tag IN ('spring-boot', 'react', 'python', 'django', 'ai', 'postgres');
DELETE FROM project_countries WHERE country_code IN ('ZA', 'KE', 'NG');
DELETE FROM project_maintainers WHERE role IN ('owner', 'maintainer') AND project_id IN (
    SELECT id FROM projects WHERE github_owner = 'codemaster'
);
DELETE FROM projects WHERE github_owner = 'codemaster';
DELETE FROM users WHERE username IN ('winter_dev', 'montic_codes');

-- 1. Seed 2 Users
INSERT INTO users (id, github_id, username, display_name, email, bio, location, skills, reputation)
VALUES 
    (1001, 881001, 'winter_dev', 'Winter Dev', 'winter@codemaster.za', 'Full-stack builder', 'Johannesburg, ZA', ARRAY['Java', 'Spring Boot', 'PostgreSQL'], 120),
    (1002, 881002, 'montic_codes', 'Montic Codes', 'montic@codemaster.za', 'Python & Data enthusiast', 'Cape Town, ZA', ARRAY['Python', 'FastAPI', 'React'], 85)
ON CONFLICT (id) DO NOTHING;

-- Reset sequence to avoid manual ID collisions
SELECT setval('users_id_seq', (SELECT GREATEST(MAX(id), 1002) FROM users));

-- 2. Seed 4 Projects
-- Spanning 2 connections ('south_african', 'africa_led') and 2 primary languages ('Java', 'Python')
INSERT INTO projects (
    id, github_owner, github_repo, github_url, name, slug, description,
    primary_language, languages, category, connection, license, stars, forks, open_issues, verified
)
VALUES 
    (
        2001, 'codemaster', 'mzansi-open-api', 'https://github.com/codemaster/mzansi-open-api',
        'Mzansi Open API', 'mzansi-open-api', 'A unified open API aggregating South African civic services.',
        'Java', ARRAY['Java', 'SQL'], 'Civic Tech', 'south_african', 'MIT', 35, 8, 3, true
    ),
    (
        2002, 'codemaster', 'afri-lang-nlp', 'https://github.com/codemaster/afri-lang-nlp',
        'Afri-Lang NLP', 'afri-lang-nlp', 'Natural Language Processing models for African indigenous languages.',
        'Python', ARRAY['Python', 'Shell'], 'Machine Learning', 'africa_led', 'Apache-2.0', 142, 29, 4, true
    ),
    (
        2003, 'codemaster', 'sa-tax-calculator', 'https://github.com/codemaster/sa-tax-calculator',
        'SA Tax Calculator', 'sa-tax-calculator', 'Open-source SARS income tax and VAT calculating engine.',
        'Java', ARRAY['Java'], 'FinTech', 'south_african', 'GPL-3.0', 21, 4, 2, false
    ),
    (
        2004, 'codemaster', 'agri-pulse-africa', 'https://github.com/codemaster/agri-pulse-africa',
        'Agri Pulse Africa', 'agri-pulse-africa', 'Crop disease forecasting and soil analytics dashboard.',
        'Python', ARRAY['Python', 'JavaScript'], 'AgriTech', 'africa_led', 'MIT', 67, 12, 2, true
    )
ON CONFLICT (id) DO NOTHING;

SELECT setval('projects_id_seq', (SELECT GREATEST(MAX(id), 2004) FROM projects));

-- Populate project_tags (composite PK: project_id, tag)
INSERT INTO project_tags (project_id, tag) VALUES
    (2001, 'spring-boot'), (2001, 'postgres'),
    (2002, 'python'), (2002, 'ai'),
    (2003, 'spring-boot'),
    (2004, 'django'), (2004, 'python')
ON CONFLICT (project_id, tag) DO NOTHING;

-- Populate project_countries (composite PK: project_id, country_code)
INSERT INTO project_countries (project_id, country_code) VALUES
    (2001, 'ZA'),
    (2002, 'ZA'), (2002, 'KE'), (2002, 'NG'),
    (2003, 'ZA'),
    (2004, 'KE'), (2004, 'ZA')
ON CONFLICT (project_id, country_code) DO NOTHING;

-- Populate maintainers
INSERT INTO project_maintainers (project_id, user_id, role) VALUES
    (2001, 1001, 'owner'),
    (2002, 1002, 'owner'),
    (2002, 1001, 'maintainer'),
    (2004, 1002, 'owner')
ON CONFLICT (project_id, user_id) DO NOTHING;

-- 3. Seed 7 Issues (spread across projects, 4 with difficulty = 'beginner')
INSERT INTO issues (
    id, project_id, github_issue_id, github_issue_number, github_url,
    title, body_excerpt, status, labels, difficulty, is_beginner_friendly
)
VALUES
    (
        3001, 2001, 9101, 1, 'https://github.com/codemaster/mzansi-open-api/issues/1',
        'Add Swagger OpenAPI 3.0 documentation', 'Swagger doc is missing for stage endpoints.',
        'open', ARRAY['documentation', 'good-first-issue'], 'beginner', true
    ),
    (
        3002, 2001, 9102, 2, 'https://github.com/codemaster/mzansi-open-api/issues/2',
        'Handle municipal load-shedding API 503 retries', 'Implement exponential backoff when upstream is down.',
        'open', ARRAY['bug', 'reliability'], 'intermediate', false
    ),
    (
        3003, 2002, 9201, 1, 'https://github.com/codemaster/afri-lang-nlp/issues/1',
        'Fix isiZulu tokenizer punctuation splitting', 'Regex fails to detach exclamation marks in quotes.',
        'open', ARRAY['nlp', 'bug', 'good-first-issue'], 'beginner', true
    ),
    (
        3004, 2002, 9202, 2, 'https://github.com/codemaster/afri-lang-nlp/issues/2',
        'Add Sesotho dictionary lookup test fixtures', 'Provide missing corpus JSON files for test coverage.',
        'claimed', ARRAY['testing', 'good-first-issue'], 'beginner', true
    ),
    (
        3005, 2003, 9301, 1, 'https://github.com/codemaster/sa-tax-calculator/issues/1',
        'Update 2026 tax brackets for medical tax credits', 'Update SARS annual bracket deduction rules.',
        'open', ARRAY['finance', 'compliance'], 'intermediate', false
    ),
    (
        3006, 2003, 9302, 2, 'https://github.com/codemaster/sa-tax-calculator/issues/2',
        'Correct README typo in rebate calculations section', 'Fix markdown links and minor grammar mistakes.',
        'open', ARRAY['documentation', 'good-first-issue'], 'beginner', true
    ),
    (
        3007, 2004, 9401, 1, 'https://github.com/codemaster/agri-pulse-africa/issues/1',
        'Optimize geo-spatial TIFF raster rendering', 'Improve pipeline execution time across large satellite imagery tiles.',
        'open', ARRAY['performance'], 'advanced', false
    )
ON CONFLICT (id) DO NOTHING;

SELECT setval('issues_id_seq', (SELECT GREATEST(MAX(id), 3007) FROM issues));

-- 4. Seed Comments (mix of project-attached and issue-attached)
INSERT INTO comments (user_id, project_id, issue_id, body)
VALUES
    (1001, 2001, NULL, '[dev-seed] We welcome contributions for provincial endpoints!'),
    (1002, NULL, 3001, '[dev-seed] I can take this documentation task this afternoon.'),
    (1001, NULL, 3003, '[dev-seed] Checked the regex pattern, easy fix on line 42.'),
    (1002, 2002, NULL, '[dev-seed] Model weights have been refreshed for v0.4 release.');

-- 5. Seed Claims (1-2 claims)
INSERT INTO claims (issue_id, user_id, status, note)
VALUES
    (3004, 1001, 'active', '[dev-seed] Claiming Sesotho fixtures dataset creation.'),
    (3001, 1002, 'released', '[dev-seed] Claimed earlier but stepped back for other work.')
ON CONFLICT DO NOTHING;