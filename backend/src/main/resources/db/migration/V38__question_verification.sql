-- Phase 94 (ADR-0070, ADR-0089): the level a question was last evaluated at, for ranking, and the
-- author's classification. The level shown is always re-evaluated on read; no row means nothing has
-- happened that ranking cares about. Runs keep the keys of the errors in their log so "the same
-- failure?" can be asked again when the question changes.
CREATE TABLE question_verifications (
    question_id        BIGINT PRIMARY KEY REFERENCES questions(id) ON DELETE CASCADE,
    level              VARCHAR(2)  NOT NULL CHECK (level IN ('L0', 'L1', 'L2', 'L3', 'L4')),
    last_reproduced_at TIMESTAMPTZ,
    classification     VARCHAR(20) CHECK (classification IN ('MISUSE', 'NOT_A_BUG')),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_question_verifications_verified ON question_verifications(question_id) WHERE level IN ('L2', 'L3', 'L4');

ALTER TABLE reproduction_runs ADD COLUMN signature_hashes VARCHAR(200);
ALTER TABLE reproduction_runs ADD COLUMN message_keys VARCHAR(200);
ALTER TABLE reproduction_runs ADD COLUMN exception_class VARCHAR(255);
