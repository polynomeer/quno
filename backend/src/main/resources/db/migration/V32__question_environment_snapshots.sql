-- Phase 93 (ADR-0070, ADR-0083): the environment snapshot attached to a question — the scrubbed
-- `quno.snapshot` document plus what the server derived: the fingerprint it computed itself and
-- how many values it still had to redact. Latest only, like question_dependencies; forks don't copy
-- it (the forker's machine is not the asker's). CASCADE like the other per-question derived tables.
CREATE TABLE question_environment_snapshots (
    id                BIGSERIAL PRIMARY KEY,
    question_id       BIGINT      NOT NULL UNIQUE REFERENCES questions(id) ON DELETE CASCADE,
    version_number    INT         NOT NULL,
    schema_version    INT         NOT NULL,
    source            VARCHAR(20) NOT NULL,
    collector         VARCHAR(100),
    fingerprint       VARCHAR(16) NOT NULL,
    server_redactions INT         NOT NULL,
    document          JSONB       NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_question_environment_snapshots_fingerprint ON question_environment_snapshots(fingerprint);
