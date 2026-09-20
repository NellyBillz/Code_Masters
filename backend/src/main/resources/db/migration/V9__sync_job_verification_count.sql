-- Add contributions_verified_count to sync_jobs
ALTER TABLE sync_jobs
    ADD COLUMN contributions_verified_count INTEGER;