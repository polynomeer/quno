-- Answer Revision (Phase 17, ADR-0029): mirrors the Question/QuestionVersion split —
-- answers.body_markdown becomes a cache of the latest AnswerVersion, the same pattern
-- questions.title already uses to cache the latest QuestionVersion's title.
CREATE TABLE answer_versions (
    id             BIGSERIAL PRIMARY KEY,
    answer_id      BIGINT NOT NULL REFERENCES answers(id),
    version_number INT NOT NULL,
    body_markdown  TEXT NOT NULL,
    created_by     BIGINT NOT NULL REFERENCES users(id),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (answer_id, version_number)
);

CREATE INDEX idx_answer_versions_answer_id ON answer_versions(answer_id);

ALTER TABLE answers ADD COLUMN latest_version_id BIGINT REFERENCES answer_versions(id);

-- Backfill: every existing answer's current body becomes its v1.
INSERT INTO answer_versions (answer_id, version_number, body_markdown, created_by, created_at)
SELECT id, 1, body_markdown, author_id, created_at FROM answers;

UPDATE answers a
SET latest_version_id = av.id
FROM answer_versions av
WHERE av.answer_id = a.id AND av.version_number = 1;
