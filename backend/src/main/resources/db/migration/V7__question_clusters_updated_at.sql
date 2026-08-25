-- Needed to detect "recently designated Super Answer" for Quno Flow / advanced Dashboard
-- (PLAN.md 10.1) without a separate event log for this aggregate.
ALTER TABLE question_clusters ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
