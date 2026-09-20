-- 1. Add is_site_admin to users
ALTER TABLE users 
    ADD COLUMN is_site_admin BOOLEAN NOT NULL DEFAULT FALSE;

-- 2. Add lifecycle, documentation, and contribution flags to projects
ALTER TABLE projects
    ADD COLUMN has_contributing_guide BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN has_code_of_conduct BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN listing_status TEXT NOT NULL DEFAULT 'pending' 
        CONSTRAINT chk_projects_listing_status CHECK (listing_status IN ('pending', 'published', 'rejected')),
    ADD COLUMN accepting_contributions BOOLEAN NOT NULL DEFAULT TRUE;

-- 3. Backfill pre-existing projects so legacy/seeded rows remain publicly discoverable
UPDATE projects 
SET listing_status = 'published';