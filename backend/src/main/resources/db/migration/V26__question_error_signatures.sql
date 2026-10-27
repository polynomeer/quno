-- Phase 64 (ADR-0075): error signatures of each question's latest version. Rewritten whole on
-- create/revise/fork; version_number only records which version they came from. CASCADE so
-- deleting a question (tests, account removal) never trips over this index table.
CREATE TABLE question_error_signatures (
    id              BIGSERIAL PRIMARY KEY,
    question_id     BIGINT       NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    version_number  INT          NOT NULL,
    exception_class VARCHAR(300) NOT NULL,
    signature_hash  VARCHAR(16),
    message_key     VARCHAR(16)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_question_error_signatures_question_id ON question_error_signatures(question_id);
CREATE INDEX idx_question_error_signatures_signature_hash ON question_error_signatures(signature_hash);
CREATE INDEX idx_question_error_signatures_message_key ON question_error_signatures(message_key);
