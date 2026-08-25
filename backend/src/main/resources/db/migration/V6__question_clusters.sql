-- Question Cluster — user-marked "same problem" grouping, not automatic similarity
-- clustering (see docs/architecture/decisions/0016-manual-duplicate-marking-cluster.md).
-- A question belongs to at most one cluster; merging two established clusters is not
-- supported yet (deferred to a future Merge feature).
CREATE TABLE question_clusters (
    id                        BIGSERIAL PRIMARY KEY,
    representative_answer_id BIGINT REFERENCES answers(id),
    created_at                TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE questions ADD COLUMN cluster_id BIGINT REFERENCES question_clusters(id);
CREATE INDEX idx_questions_cluster_id ON questions(cluster_id) WHERE cluster_id IS NOT NULL;
