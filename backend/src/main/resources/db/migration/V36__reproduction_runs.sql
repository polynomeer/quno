-- Phase 80 (ADR-0087): a question's reproduction capsule (a public GitHub repository, a ref and
-- what to run) and the runs its owner's GitHub Actions reported, each proven by an OIDC token.
CREATE TABLE reproduction_capsules (
    id                  BIGSERIAL PRIMARY KEY,
    question_id         BIGINT       NOT NULL REFERENCES questions(id),
    repository          VARCHAR(140) NOT NULL,
    ref                 VARCHAR(255) NOT NULL,
    kind                VARCHAR(20)  NOT NULL CHECK (kind IN ('SCRIPT', 'GRADLE', 'DOCKERFILE')),
    command             VARCHAR(1000) NOT NULL,
    created_by          BIGINT       NOT NULL REFERENCES users(id),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    superseded_at       TIMESTAMPTZ,
    rerun_requested_at  TIMESTAMPTZ,
    rerun_requested_by  BIGINT REFERENCES users(id),
    rerun_reason        VARCHAR(500)
);

-- One active capsule per question; replacing one supersedes it.
CREATE UNIQUE INDEX uq_reproduction_capsules_active ON reproduction_capsules(question_id) WHERE superseded_at IS NULL;

CREATE TABLE reproduction_runs (
    id                  BIGSERIAL PRIMARY KEY,
    capsule_id          BIGINT       NOT NULL REFERENCES reproduction_capsules(id),
    result              VARCHAR(20)  NOT NULL CHECK (result IN ('REPRODUCED', 'NOT_REPRODUCED', 'BUILD_FAILED', 'INCONCLUSIVE')),
    exit_code           INT,
    commit_sha          VARCHAR(40)  NOT NULL,
    environment         VARCHAR(200) NOT NULL,
    runner_environment  VARCHAR(20)  NOT NULL,
    trusted_workflow    BOOLEAN      NOT NULL,
    workflow_ref        VARCHAR(500) NOT NULL,
    run_id              BIGINT       NOT NULL,
    run_attempt         INT          NOT NULL,
    run_url             VARCHAR(500) NOT NULL,
    duration_ms         BIGINT,
    log_tail            TEXT,
    masked_secret_count INT          NOT NULL DEFAULT 0,
    finished_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    -- A token is replayable until it expires; the same run attempt is recorded once.
    CONSTRAINT uq_reproduction_runs_attempt UNIQUE (capsule_id, run_id, run_attempt)
);

CREATE INDEX idx_reproduction_runs_capsule_finished ON reproduction_runs(capsule_id, finished_at DESC);
