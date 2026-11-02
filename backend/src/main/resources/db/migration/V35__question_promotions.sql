-- Phase 79.3 (ADR-0086): a resolved private question can be promoted once to a new, sanitized
-- public card. The link lives only on the private side: the public card never points back, so it
-- reveals nothing about the private question it came from.
CREATE TABLE question_promotions (
    private_question_id BIGINT PRIMARY KEY REFERENCES questions(id),
    public_question_id  BIGINT NOT NULL UNIQUE REFERENCES questions(id),
    promoted_by         BIGINT NOT NULL REFERENCES users(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
