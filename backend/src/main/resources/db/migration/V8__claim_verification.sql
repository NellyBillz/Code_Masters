-- 1. Add verification and pull request columns to claims
ALTER TABLE claims
    ADD COLUMN pull_request_url TEXT,
    ADD COLUMN pull_request_state TEXT NOT NULL DEFAULT 'none'
        CONSTRAINT chk_claims_pull_request_state CHECK (
            pull_request_state IN ('none', 'open', 'merged', 'closed_unmerged')
        ),
    ADD COLUMN maintainer_feedback TEXT,
    ADD COLUMN completion_source TEXT
        CONSTRAINT chk_claims_completion_source CHECK (
            completion_source IS NULL OR completion_source IN ('github_verified', 'maintainer_confirmed')
        ),
    ADD COLUMN completed_at TIMESTAMPTZ;

-- 2. Drop existing status check constraint and re-add with 'changes_requested'
ALTER TABLE claims 
    DROP CONSTRAINT IF EXISTS claims_status_check;

ALTER TABLE claims
    ADD CONSTRAINT chk_claims_status CHECK (
        status IN ('active', 'released', 'completed', 'changes_requested')
    );