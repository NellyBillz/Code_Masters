-- Repeatable Dev Seed Data
-- Cleans up previous seed rows before re-populating to guarantee idempotency
--
-- Scope note: this only ever touches the synthetic 'codemaster'-owned projects
-- (2001-2004) and their own users/issues/comments/claims. Real projects
-- submitted through the running app (e.g. via real GitHub OAuth logins) live
-- under their own real github_owner and are never touched by this script.

DELETE FROM claims WHERE note LIKE '[dev-seed]%';
DELETE FROM comments WHERE body LIKE '[dev-seed]%';
DELETE FROM issues WHERE github_url LIKE 'https://github.com/codemaster/%';
DELETE FROM project_tags WHERE tag IN ('spring-boot', 'react', 'python', 'django', 'ai', 'postgres');
DELETE FROM project_countries WHERE country_code = 'ZA';
DELETE FROM project_maintainers WHERE role IN ('owner', 'maintainer') AND project_id IN (
    SELECT id FROM projects WHERE github_owner = 'codemaster'
);
DELETE FROM projects WHERE github_owner = 'codemaster';
DELETE FROM users WHERE username IN (
    'winter_stone', 'montic_codes', 'thabo_dev', 'lerato_codes', 'sipho_builds',
    'aisha_k', 'naledi_writes', 'jaco_vd', 'zanele_m'
);

-- 1. Seed 9 Users (2 maintainers of the seed projects, 7 contributors with
-- varied skills/locations so per-user stats, the leaderboard, and the
-- contributor passport all have real texture instead of one data point)
INSERT INTO users (id, github_id, username, display_name, email, bio, location, skills, reputation)
VALUES
    (1001, 881001, 'winter_stone', 'Winter Stone', 'winter@codemaster.za', 'Full-stack builder', 'Johannesburg, ZA', ARRAY['Java', 'Spring Boot', 'PostgreSQL'], 120),
    (1002, 881002, 'montic_codes', 'Montic Codes', 'montic@codemaster.za', 'Python & Data enthusiast', 'Cape Town, ZA', ARRAY['Python', 'FastAPI', 'React'], 85),
    (1004, 881004, 'thabo_dev', 'Thabo Nkosi', 'thabo@codemaster.za', 'Backend engineer, loves clean APIs', 'Pretoria, ZA', ARRAY['Java', 'Spring Boot', 'AWS'], 64),
    (1005, 881005, 'lerato_codes', 'Lerato Mokoena', 'lerato@codemaster.za', 'Data scientist, open-source tax nerd', 'Bloemfontein, ZA', ARRAY['Python', 'Django', 'Machine Learning'], 58),
    (1006, 881006, 'sipho_builds', 'Sipho Dlamini', 'sipho@codemaster.za', 'Frontend and DX tooling', 'Durban, ZA', ARRAY['JavaScript', 'React', 'Node.js'], 71),
    (1007, 881007, 'aisha_k', 'Aisha Khan', 'aisha@codemaster.za', 'Infra and performance', 'Cape Town, ZA', ARRAY['Go', 'Docker', 'Kubernetes'], 49),
    (1008, 881008, 'naledi_writes', 'Naledi Sithole', 'naledi@codemaster.za', 'NLP researcher, isiZulu speaker', 'Johannesburg, ZA', ARRAY['Python', 'NLP', 'FastAPI'], 53),
    (1009, 881009, 'jaco_vd', 'Jaco van der Merwe', 'jaco@codemaster.za', 'AgriTech and geospatial systems', 'Stellenbosch, ZA', ARRAY['Python', 'PostgreSQL', 'GIS'], 45),
    (1010, 881010, 'zanele_m', 'Zanele Mahlangu', 'zanele@codemaster.za', 'QA and testing advocate', 'Gqeberha, ZA', ARRAY['TypeScript', 'React', 'Testing'], 38)
ON CONFLICT (id) DO NOTHING;

-- Reset sequence to avoid manual ID collisions
SELECT setval('users_id_seq', (SELECT GREATEST(MAX(id), 1010) FROM users));

-- 2. Seed 4 Projects
-- Spanning 2 connections ('south_african', 'community_verified') and 2 primary languages ('Java', 'Python')
INSERT INTO projects (
    id, github_owner, github_repo, github_url, name, slug, description,
    primary_language, languages, category, connection, license, stars, forks, open_issues, verified,
    listing_status
)
VALUES
    (
        2001, 'codemaster', 'mzansi-open-api', 'https://github.com/codemaster/mzansi-open-api',
        'Mzansi Open API', 'mzansi-open-api', 'A unified open API aggregating South African civic services.',
        'Java', ARRAY['Java', 'SQL'], 'Civic Tech', 'south_african', 'MIT', 35, 8, 3, true, 'published'
    ),
    (
        2002, 'codemaster', 'afri-lang-nlp', 'https://github.com/codemaster/afri-lang-nlp',
        'Afri-Lang NLP', 'afri-lang-nlp', 'Natural Language Processing models for South African indigenous languages.',
        'Python', ARRAY['Python', 'Shell'], 'Machine Learning', 'community_verified', 'Apache-2.0', 142, 29, 4, true, 'published'
    ),
    (
        2003, 'codemaster', 'sa-tax-calculator', 'https://github.com/codemaster/sa-tax-calculator',
        'SA Tax Calculator', 'sa-tax-calculator', 'Open-source SARS income tax and VAT calculating engine.',
        'Java', ARRAY['Java'], 'FinTech', 'south_african', 'GPL-3.0', 21, 4, 2, false, 'published'
    ),
    (
        2004, 'codemaster', 'agri-pulse-africa', 'https://github.com/codemaster/agri-pulse-africa',
        'Agri Pulse Africa', 'agri-pulse-africa', 'Crop disease forecasting and soil analytics dashboard.',
        'Python', ARRAY['Python', 'JavaScript'], 'AgriTech', 'south_african', 'MIT', 67, 12, 2, true, 'published'
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
    (2002, 'ZA'),
    (2003, 'ZA'),
    (2004, 'ZA')
ON CONFLICT (project_id, country_code) DO NOTHING;

-- Populate maintainers
INSERT INTO project_maintainers (project_id, user_id, role) VALUES
    (2001, 1001, 'owner'),
    (2002, 1002, 'owner'),
    (2002, 1001, 'maintainer'),
    (2004, 1002, 'owner')
ON CONFLICT (project_id, user_id) DO NOTHING;

-- 3. Seed 19 Issues (7 original + 12 new, spread across projects, mixing
-- open/closed status and every difficulty so filters/dashboards have real
-- variety to show instead of one thin slice)
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
    ),
    (
        3008, 2001, 9103, 3, 'https://github.com/codemaster/mzansi-open-api/issues/3',
        'Add rate limiting per API consumer key', 'Prevent a single client from exhausting upstream municipal API quotas.',
        'open', ARRAY['bug', 'api'], 'intermediate', false
    ),
    (
        3009, 2001, 9104, 4, 'https://github.com/codemaster/mzansi-open-api/issues/4',
        'Fix timezone handling in load-shedding schedule endpoint', 'Schedule times are returned in UTC instead of SAST.',
        'closed', ARRAY['bug', 'good-first-issue'], 'beginner', true
    ),
    (
        3010, 2001, 9105, 5, 'https://github.com/codemaster/mzansi-open-api/issues/5',
        'Add health check endpoint for k8s liveness probes', 'Need a lightweight /healthz endpoint for orchestration.',
        'open', ARRAY['good-first-issue', 'infra'], 'beginner', true
    ),
    (
        3011, 2002, 9203, 3, 'https://github.com/codemaster/afri-lang-nlp/issues/3',
        'Add isiXhosa click-consonant normalization', 'Tokenizer currently drops click consonants during normalization.',
        'open', ARRAY['nlp', 'bug'], 'intermediate', false
    ),
    (
        3012, 2002, 9204, 4, 'https://github.com/codemaster/afri-lang-nlp/issues/4',
        'Benchmark tokenizer speed against spaCy baseline', 'Need a reproducible benchmark script and results table.',
        'closed', ARRAY['performance'], 'advanced', false
    ),
    (
        3013, 2002, 9205, 5, 'https://github.com/codemaster/afri-lang-nlp/issues/5',
        'Write contributing guide for adding a new language model', 'No documented process for onboarding a new indigenous language.',
        'open', ARRAY['documentation', 'good-first-issue'], 'beginner', true
    ),
    (
        3014, 2003, 9303, 3, 'https://github.com/codemaster/sa-tax-calculator/issues/3',
        'Add unit tests for provisional tax calculation', 'Provisional tax path currently has zero test coverage.',
        'open', ARRAY['testing'], 'intermediate', false
    ),
    (
        3015, 2003, 9304, 4, 'https://github.com/codemaster/sa-tax-calculator/issues/4',
        'Support 2025/2026 UIF contribution ceiling update', 'UIF ceiling constant is hardcoded to the previous tax year.',
        'closed', ARRAY['compliance', 'good-first-issue'], 'beginner', true
    ),
    (
        3016, 2003, 9305, 5, 'https://github.com/codemaster/sa-tax-calculator/issues/5',
        'Refactor bracket lookup to use binary search', 'Linear scan over tax brackets is unnecessary at this scale.',
        'open', ARRAY['performance'], 'advanced', false
    ),
    (
        3017, 2004, 9402, 2, 'https://github.com/codemaster/agri-pulse-africa/issues/2',
        'Add drought risk index for Free State region', 'Extend regional risk scoring beyond the current Western Cape pilot.',
        'open', ARRAY['feature'], 'intermediate', false
    ),
    (
        3018, 2004, 9403, 3, 'https://github.com/codemaster/agri-pulse-africa/issues/3',
        'Fix soil moisture sensor data ingestion timeout', 'Ingestion job times out when a sensor batch exceeds 10k readings.',
        'closed', ARRAY['bug', 'good-first-issue'], 'beginner', true
    ),
    (
        3019, 2004, 9404, 4, 'https://github.com/codemaster/agri-pulse-africa/issues/4',
        'Write onboarding doc for new contributors', 'No CONTRIBUTING.md walkthrough for setting up the geospatial pipeline locally.',
        'open', ARRAY['documentation', 'good-first-issue'], 'beginner', true
    )
ON CONFLICT (id) DO NOTHING;

SELECT setval('issues_id_seq', (SELECT GREATEST(MAX(id), 3019) FROM issues));

-- 4. Seed Comments (mix of project-attached and issue-attached, including
-- one flagged/resolved question and one flagged/still-open question so the
-- maintainer Q&A queue has real seed data too)
INSERT INTO comments (user_id, project_id, issue_id, body, is_question, resolved_at)
VALUES
    (1001, 2001, NULL, '[dev-seed] We welcome contributions for provincial endpoints!', false, NULL),
    (1002, NULL, 3001, '[dev-seed] I can take this documentation task this afternoon.', false, NULL),
    (1001, NULL, 3003, '[dev-seed] Checked the regex pattern, easy fix on line 42.', false, NULL),
    (1002, 2002, NULL, '[dev-seed] Model weights have been refreshed for v0.4 release.', false, NULL),
    (1005, NULL, 3008, '[dev-seed] Should the rate limit be per API key or per IP as a fallback for anonymous callers?', true, NULL),
    (1006, NULL, 3011, '[dev-seed] Is there an existing corpus with click-consonant examples I should validate against?', true, now() - interval '4 days')
ON CONFLICT DO NOTHING;

-- 5. Seed Claims: a realistic spread of completed (GitHub-verified and
-- maintainer-confirmed), active, released, and changes-requested claims
-- across every new contributor and every new issue, with completed_at
-- spread over the last ~2 months so activity-over-time charts have real
-- shape instead of a single data point.
INSERT INTO claims (
    issue_id, user_id, status, note, pull_request_url, pull_request_state,
    completion_source, completed_at, maintainer_feedback
)
VALUES
    (3004, 1001, 'active', '[dev-seed] Claiming Sesotho fixtures dataset creation.', NULL, 'none', NULL, NULL, NULL),
    (3001, 1002, 'released', '[dev-seed] Claimed earlier but stepped back for other work.', NULL, 'none', NULL, NULL, NULL),
    (3008, 1005, 'active', '[dev-seed] Working on per-key rate limiting with a Redis token bucket.', 'https://github.com/codemaster/mzansi-open-api/pull/47', 'open', NULL, NULL, NULL),
    (3016, 1006, 'changes_requested', '[dev-seed] Swapped bracket lookup to binary search.', 'https://github.com/codemaster/sa-tax-calculator/pull/22', 'open', NULL, NULL, '[dev-seed] Please add a benchmark comparing old vs new lookup before merging.'),
    (3002, 1004, 'completed', '[dev-seed] Added exponential backoff for upstream 503s.', 'https://github.com/codemaster/mzansi-open-api/pull/41', 'merged', 'github_verified', now() - interval '52 days', NULL),
    (3006, 1005, 'completed', '[dev-seed] Fixed README typos and broken links.', 'https://github.com/codemaster/sa-tax-calculator/pull/12', 'merged', 'maintainer_confirmed', now() - interval '46 days', NULL),
    (3009, 1006, 'completed', '[dev-seed] Converted schedule times to SAST.', 'https://github.com/codemaster/mzansi-open-api/pull/45', 'merged', 'github_verified', now() - interval '40 days', NULL),
    (3012, 1007, 'completed', '[dev-seed] Added benchmark script and results table.', 'https://github.com/codemaster/afri-lang-nlp/pull/33', 'merged', 'github_verified', now() - interval '35 days', NULL),
    (3015, 1008, 'completed', '[dev-seed] Updated UIF ceiling constant for the new tax year.', 'https://github.com/codemaster/sa-tax-calculator/pull/19', 'merged', 'maintainer_confirmed', now() - interval '30 days', NULL),
    (3018, 1009, 'completed', '[dev-seed] Batched sensor ingestion to avoid the timeout.', 'https://github.com/codemaster/agri-pulse-africa/pull/8', 'merged', 'github_verified', now() - interval '25 days', NULL),
    (3003, 1010, 'completed', '[dev-seed] Fixed punctuation-splitting regex for isiZulu.', 'https://github.com/codemaster/afri-lang-nlp/pull/34', 'merged', 'github_verified', now() - interval '20 days', NULL),
    (3005, 1004, 'completed', '[dev-seed] Updated medical tax credit brackets for 2026.', 'https://github.com/codemaster/sa-tax-calculator/pull/20', 'merged', 'maintainer_confirmed', now() - interval '18 days', NULL),
    (3007, 1005, 'completed', '[dev-seed] Optimized raster tiling for large imagery.', 'https://github.com/codemaster/agri-pulse-africa/pull/9', 'merged', 'github_verified', now() - interval '15 days', NULL),
    (3011, 1006, 'completed', '[dev-seed] Preserved click consonants through normalization.', 'https://github.com/codemaster/afri-lang-nlp/pull/35', 'merged', 'github_verified', now() - interval '12 days', NULL),
    (3013, 1007, 'completed', '[dev-seed] Added a new-language-model contributing guide.', 'https://github.com/codemaster/afri-lang-nlp/pull/36', 'merged', 'maintainer_confirmed', now() - interval '10 days', NULL),
    (3014, 1008, 'completed', '[dev-seed] Added provisional tax unit test coverage.', 'https://github.com/codemaster/sa-tax-calculator/pull/21', 'merged', 'github_verified', now() - interval '8 days', NULL),
    (3017, 1009, 'completed', '[dev-seed] Added Free State drought risk scoring.', 'https://github.com/codemaster/agri-pulse-africa/pull/10', 'merged', 'github_verified', now() - interval '5 days', NULL),
    (3010, 1010, 'completed', '[dev-seed] Added /healthz liveness endpoint.', 'https://github.com/codemaster/mzansi-open-api/pull/46', 'merged', 'maintainer_confirmed', now() - interval '3 days', NULL),
    (3019, 1004, 'completed', '[dev-seed] Wrote local geospatial pipeline setup guide.', 'https://github.com/codemaster/agri-pulse-africa/pull/11', 'merged', 'github_verified', now() - interval '2 days', NULL)
ON CONFLICT DO NOTHING;
