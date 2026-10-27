-- Phase 63.3 (ADR-0077): where an answer holds, per package — from_version inclusive,
-- until_version exclusive. Latest answer version only; version_number records which one.
-- question_id is copied from the answer (which never moves) so impact detection can read every
-- range of a question without joining answers. CASCADE like the other derived tables (ADR-0076).
CREATE TABLE answer_applicability_ranges (
    id             BIGSERIAL PRIMARY KEY,
    answer_id      BIGINT       NOT NULL REFERENCES answers(id) ON DELETE CASCADE,
    question_id    BIGINT       NOT NULL,
    version_number INT          NOT NULL,
    ecosystem      VARCHAR(20)  NOT NULL,
    name           VARCHAR(300) NOT NULL,
    from_version   VARCHAR(100),
    until_version  VARCHAR(100),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_answer_applicability_ranges_answer_id ON answer_applicability_ranges(answer_id);
CREATE INDEX idx_answer_applicability_ranges_question_id ON answer_applicability_ranges(question_id);
