-- A contributor can flag one comment on an issue as a blocking question —
-- "I need this answered before I can start" — distinct from general
-- discussion. Surfaces in the maintainer activity rollup as its own
-- triaged queue (unansweredQuestions), the same "Needs your review"
-- pattern already used for claims with a pull request attached.
ALTER TABLE comments
    ADD COLUMN is_question BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN resolved_at TIMESTAMPTZ;
