-- Live Chat room metadata (Phase 24, ADR-0036) — at most one room per question. Messages
-- themselves live in MongoDB (high-volume append-only, no fixed schema pressure); this table
-- is just the fixed, low-volume "does a room exist for this question" record.
CREATE TABLE live_chat_rooms (
    id          BIGSERIAL PRIMARY KEY,
    question_id BIGINT NOT NULL REFERENCES questions(id),
    created_by  BIGINT NOT NULL REFERENCES users(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_live_chat_rooms_question_id ON live_chat_rooms(question_id);
