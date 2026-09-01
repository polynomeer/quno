-- Paid Direct Ask via Toss Payments test mode (Phase 25, ADR-0037) — supersedes Phase 22's free
-- flow. Every request now goes through AWAITING_PAYMENT before it becomes PENDING (visible to
-- the target), so the duplicate-request guard must cover both states.
DROP INDEX uq_direct_ask_requests_pending;
CREATE UNIQUE INDEX uq_direct_ask_requests_open ON direct_ask_requests(question_id, target_user_id)
    WHERE status IN ('AWAITING_PAYMENT', 'PENDING');

CREATE TABLE direct_ask_payments (
    id                     BIGSERIAL PRIMARY KEY,
    direct_ask_request_id  BIGINT NOT NULL REFERENCES direct_ask_requests(id),
    order_id               VARCHAR(64) NOT NULL,
    amount                 BIGINT NOT NULL,
    status                 VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    toss_payment_key       VARCHAR(200),
    created_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    confirmed_at           TIMESTAMPTZ,
    cancelled_at           TIMESTAMPTZ
);

CREATE UNIQUE INDEX uq_direct_ask_payments_order_id ON direct_ask_payments(order_id);
CREATE INDEX idx_direct_ask_payments_direct_ask_request_id ON direct_ask_payments(direct_ask_request_id);
