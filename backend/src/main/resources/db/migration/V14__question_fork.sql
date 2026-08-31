-- Question Fork (Phase 18, ADR-0030): pure lineage pointer, set once at creation. A forked
-- question is otherwise fully independent — no cascading relationship to its origin.
ALTER TABLE questions ADD COLUMN origin_question_id BIGINT REFERENCES questions(id);
CREATE INDEX idx_questions_origin_question_id ON questions(origin_question_id) WHERE origin_question_id IS NOT NULL;
