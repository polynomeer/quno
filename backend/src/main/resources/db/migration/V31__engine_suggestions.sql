-- Phase 78 (ADR-0081): the engine layer. Engines never edit question_versions; they leave
-- suggestions here, and accepting one becomes an ordinary revision by the question's author.
-- engine/engine_version/input_hash/env_digest/log_ref are the provenance fields of ADR-0068
-- (env_digest and log_ref stay null until an engine executes code, Phase 80).
CREATE TABLE engine_suggestions (
    id                    BIGSERIAL PRIMARY KEY,
    question_id           BIGINT       NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    target_version_number INT          NOT NULL,
    kind                  VARCHAR(40)  NOT NULL,
    status                VARCHAR(20)  NOT NULL,
    engine                VARCHAR(50)  NOT NULL,
    engine_version        INT          NOT NULL,
    input_hash            VARCHAR(64)  NOT NULL,
    summary               VARCHAR(500) NOT NULL,
    evidence              TEXT         NOT NULL,
    proposed_title        VARCHAR(300),
    proposed_body         TEXT,
    proposed_environment  TEXT,
    proposed_logs         TEXT,
    env_digest            VARCHAR(100),
    log_ref               VARCHAR(500),
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    resolved_at           TIMESTAMPTZ,
    resolved_by           BIGINT
);

CREATE INDEX idx_engine_suggestions_question_id ON engine_suggestions(question_id);

-- One row per (question version, engine version): the idempotency key, and how the sweep finds
-- questions an engine hasn't looked at yet.
CREATE TABLE engine_runs (
    id             BIGSERIAL PRIMARY KEY,
    question_id    BIGINT      NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    version_number INT         NOT NULL,
    engine         VARCHAR(50) NOT NULL,
    engine_version INT         NOT NULL,
    input_hash     VARCHAR(64) NOT NULL,
    finding_count  INT         NOT NULL,
    ran_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (question_id, version_number, engine, engine_version)
);

-- Values the author marked "not a secret" by dismissing a redaction suggestion; masking skips them.
CREATE TABLE engine_secret_allowlist (
    id          BIGSERIAL PRIMARY KEY,
    question_id BIGINT      NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    value_hash  VARCHAR(64) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (question_id, value_hash)
);
