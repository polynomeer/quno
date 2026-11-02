-- Phase 93.3 (ADR-0084): the failure symptom the collector named (OUT_OF_MEMORY, DB_TIMEOUT, ...),
-- kept as a column so snapshots can be grouped by symptom within an environment fingerprint —
-- the first indexed core field of the document. Null for Tier 0-only snapshots.
ALTER TABLE question_environment_snapshots ADD COLUMN symptom VARCHAR(40);

CREATE INDEX idx_question_environment_snapshots_symptom ON question_environment_snapshots(symptom, fingerprint) WHERE symptom IS NOT NULL;
