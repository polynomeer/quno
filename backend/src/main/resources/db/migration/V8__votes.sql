-- Vote — independent side-aggregate like watches, not part of Question/Answer
-- (see docs/architecture/decisions/0023-vote-as-side-aggregate-no-reputation-impact.md).
-- Score is never stored; always computed as SUM(value) over this table.
CREATE TABLE votes (
    voter_id    BIGINT NOT NULL REFERENCES users(id),
    target_type VARCHAR(20) NOT NULL,
    target_id   BIGINT NOT NULL,
    value       INTEGER NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (voter_id, target_type, target_id),
    CONSTRAINT chk_votes_value CHECK (value IN (-1, 1))
);

CREATE INDEX idx_votes_target ON votes(target_type, target_id);
