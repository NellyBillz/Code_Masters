-- Narrow the platform from pan-African to South-Africa-only scope.
--
-- 'africa_focused'/'africa_led' described a project's relationship to Africa
-- broadly (an African-focused or African-led project, not necessarily South
-- African) — that breadth is exactly what's being cut. 'community_verified'
-- is redefined at the product level to mean "verified as South African",
-- not dropped, since it isn't a geography claim on its own.
--
-- Backfill first (defensive — protects any row already using a value that's
-- about to become invalid) before tightening the constraint.
UPDATE projects
SET connection = 'south_african'
WHERE connection IN ('africa_focused', 'africa_led');

ALTER TABLE projects
    DROP CONSTRAINT IF EXISTS projects_connection_check;

ALTER TABLE projects
    ADD CONSTRAINT chk_projects_connection CHECK (
        connection IN ('south_african', 'community_verified')
    );
