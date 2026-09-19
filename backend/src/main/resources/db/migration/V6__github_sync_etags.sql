-- Conditional GitHub request validators used by the project sync endpoint.
ALTER TABLE projects ADD COLUMN github_metadata_etag TEXT;
ALTER TABLE projects ADD COLUMN github_issues_etag TEXT;
