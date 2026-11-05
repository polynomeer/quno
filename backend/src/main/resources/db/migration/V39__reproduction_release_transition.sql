-- Phase 82 (ADR-0090): a run of a newly released version that behaves differently from the version
-- before it, in the same environment. Keeps which run it was compared with, so the two logs can be
-- read side by side; those logs are exempt from pruning.
ALTER TABLE reproduction_runs ADD COLUMN transition VARCHAR(10) CHECK (transition IN ('BROKE', 'FIXED'));
ALTER TABLE reproduction_runs ADD COLUMN compared_run_id BIGINT REFERENCES reproduction_runs(id);
ALTER TABLE reproduction_runs ADD COLUMN compared_version VARCHAR(100);

CREATE INDEX idx_reproduction_runs_transition ON reproduction_runs(capsule_id, finished_at DESC) WHERE transition IS NOT NULL;
