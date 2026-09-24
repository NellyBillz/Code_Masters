-- Request to join an existing claim as a collaborator. A contributor may
-- request to collaborate on any other user's active/changes_requested claim
-- (not their own); the claim's owner accepts or declines. On acceptance,
-- the application layer releases the requester's own separate active claim
-- on the same issue, if they held one — a person works on an issue as
-- either the sole claimant or a collaborator on someone else's claim, not
-- both at once.
CREATE TABLE claim_collaboration_requests (
    id BIGSERIAL PRIMARY KEY,
    claim_id BIGINT NOT NULL,
    requester_user_id BIGINT NOT NULL,
    status TEXT NOT NULL DEFAULT 'pending'
        CONSTRAINT chk_claim_collaboration_requests_status CHECK (
            status IN ('pending', 'accepted', 'declined', 'cancelled')
        ),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    responded_at TIMESTAMPTZ,
    CONSTRAINT fk_claim_collaboration_requests_claim
        FOREIGN KEY (claim_id) REFERENCES claims(id) ON DELETE CASCADE,
    CONSTRAINT fk_claim_collaboration_requests_requester
        FOREIGN KEY (requester_user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Partial unique index, same idiom as uq_claims_active_per_user_issue
-- (V4__comments_and_claims.sql): only one live (pending or accepted)
-- request per (claim, requester) at a time, but a declined/cancelled
-- request doesn't block a later re-request — full history is preserved.
CREATE UNIQUE INDEX uq_claim_collaboration_requests_live_per_user
ON claim_collaboration_requests (claim_id, requester_user_id)
WHERE status IN ('pending', 'accepted');
