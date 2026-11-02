-- Phase 81 (ADR-0088): a capsule can walk several versions of one package; each run records its
-- matrix cell and what started it. rerun_request_count keeps 이의 제기 countable after they close.
ALTER TABLE reproduction_capsules ADD COLUMN matrix_package VARCHAR(200);
ALTER TABLE reproduction_capsules ADD COLUMN matrix_versions VARCHAR(600);
ALTER TABLE reproduction_capsules ADD COLUMN rerun_request_count INT NOT NULL DEFAULT 0;

ALTER TABLE reproduction_runs ADD COLUMN dependency_version VARCHAR(100);
ALTER TABLE reproduction_runs ADD COLUMN event_name VARCHAR(40);

CREATE INDEX idx_reproduction_runs_finished ON reproduction_runs(finished_at);
