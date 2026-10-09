-- Question view counter (ADR-0063) — a side table rather than a questions column, so saving the
-- Question aggregate can never overwrite a concurrently incremented count, and a view never moves
-- questions.updated_at (which the feed reads as "last activity").
CREATE TABLE question_view_counts (
    question_id BIGINT PRIMARY KEY REFERENCES questions(id),
    view_count  BIGINT NOT NULL DEFAULT 0
);
