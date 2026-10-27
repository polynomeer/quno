-- Phase 63 (ADR-0076): the stack fingerprint a question's author chose to attach — parsed from a
-- dependency file they pasted, which itself is never stored. Latest version only; version_number
-- records which version it came with. CASCADE for the same reason as question_error_signatures.
CREATE TABLE question_dependencies (
    id             BIGSERIAL PRIMARY KEY,
    question_id    BIGINT       NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    version_number INT          NOT NULL,
    ecosystem      VARCHAR(20)  NOT NULL,
    name           VARCHAR(300) NOT NULL,
    version        VARCHAR(100),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_question_dependencies_question_id ON question_dependencies(question_id);
CREATE INDEX idx_question_dependencies_name ON question_dependencies(ecosystem, name);
