-- Direct Ask, no payment (Phase 22, ADR-0034) — asking a specific user for an answer.
ALTER TABLE users ADD COLUMN accepts_direct_ask BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE direct_ask_requests (
    id             BIGSERIAL PRIMARY KEY,
    question_id    BIGINT NOT NULL REFERENCES questions(id),
    requester_id   BIGINT NOT NULL REFERENCES users(id),
    target_user_id BIGINT NOT NULL REFERENCES users(id),
    message        TEXT,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    responded_at   TIMESTAMPTZ
);

CREATE INDEX idx_direct_ask_requests_requester_id ON direct_ask_requests(requester_id);
CREATE INDEX idx_direct_ask_requests_target_user_id ON direct_ask_requests(target_user_id);

-- At most one open (PENDING) request per (question, target) — spam guard, not a hard business
-- rule: once it's ACCEPTED/DECLINED, the same pair can ask again.
CREATE UNIQUE INDEX uq_direct_ask_requests_pending ON direct_ask_requests(question_id, target_user_id) WHERE status = 'PENDING';
