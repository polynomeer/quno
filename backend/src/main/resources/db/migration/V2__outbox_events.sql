-- Transactional Outbox skeleton (see docs/architecture/system-architecture.md
-- "비동기 이벤트 처리 — Transactional Outbox"). Producers (Question/Answer use cases)
-- write here starting in Phase 2.7; a consumer worker is added with Notification (Phase 2.8).

CREATE TABLE outbox_events (
    id             BIGSERIAL PRIMARY KEY,
    event_type     VARCHAR(50) NOT NULL,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id   BIGINT NOT NULL,
    payload        TEXT NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at   TIMESTAMPTZ
);

CREATE INDEX idx_outbox_events_unpublished ON outbox_events(created_at) WHERE published_at IS NULL;
