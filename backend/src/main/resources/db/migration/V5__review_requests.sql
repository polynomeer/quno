-- QPR Review — multi-reviewer thread model (see docs/architecture/decisions/0012,
-- PLAN.md Phase 5.2). Each row is one reviewer's independent request for more info;
-- a question can have several open at once.
CREATE TABLE review_requests (
    id                                  BIGSERIAL PRIMARY KEY,
    question_id                         BIGINT NOT NULL REFERENCES questions(id),
    requested_by                        BIGINT NOT NULL REFERENCES users(id),
    message                             TEXT NOT NULL,
    status                              VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    question_version_number_at_request  INT NOT NULL,
    created_at                          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    addressed_at                        TIMESTAMPTZ
);

CREATE INDEX idx_review_requests_question_id ON review_requests(question_id);
