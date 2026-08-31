-- Moderation MVP (see docs/architecture/decisions/0028-moderation-mvp-report-dismiss-hide-only.md):
-- role is DB-only for now, no self-service promotion API.
ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';

CREATE TABLE reports (
    id          BIGSERIAL PRIMARY KEY,
    reporter_id BIGINT NOT NULL REFERENCES users(id),
    target_type VARCHAR(20) NOT NULL,
    target_id   BIGINT NOT NULL,
    reason      VARCHAR(20) NOT NULL,
    message     TEXT,
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    resolved_by BIGINT REFERENCES users(id),
    resolved_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_reports_status ON reports(status);
CREATE INDEX idx_reports_target ON reports(target_type, target_id);
