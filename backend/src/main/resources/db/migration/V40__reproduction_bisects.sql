-- Phase 83 (ADR-0091): a search for the first version that behaves differently within a range of a
-- capsule's package. Candidates are frozen at the start so a new release can't move the search.
CREATE TABLE reproduction_bisects (
    id           BIGSERIAL PRIMARY KEY,
    capsule_id   BIGINT       NOT NULL REFERENCES reproduction_capsules(id),
    from_version VARCHAR(100) NOT NULL,
    to_version   VARCHAR(100) NOT NULL,
    from_result  VARCHAR(20)  NOT NULL,
    to_result    VARCHAR(20)  NOT NULL,
    candidates   TEXT         NOT NULL,
    status       VARCHAR(20)  NOT NULL CHECK (status IN ('RUNNING', 'FOUND', 'UNRESOLVED', 'BUDGET_REACHED', 'CANCELLED')),
    low_version  VARCHAR(100) NOT NULL,
    high_version VARCHAR(100) NOT NULL,
    confidence   VARCHAR(10)  CHECK (confidence IN ('HIGH', 'MEDIUM', 'LOW')),
    runs_used    INT          NOT NULL DEFAULT 0,
    decision     VARCHAR(10)  NOT NULL DEFAULT 'PENDING' CHECK (decision IN ('PENDING', 'APPROVED', 'REJECTED')),
    created_by   BIGINT       NOT NULL REFERENCES users(id),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    finished_at  TIMESTAMPTZ
);

-- One search at a time per capsule.
CREATE UNIQUE INDEX uq_reproduction_bisects_running ON reproduction_bisects(capsule_id) WHERE status = 'RUNNING';
CREATE INDEX idx_reproduction_bisects_capsule ON reproduction_bisects(capsule_id, id DESC);
