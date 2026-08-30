-- Comment — flat clarification comments on Question/Answer, distinct from QPR review_requests
-- (see docs/architecture/decisions/0024-comment-flat-no-edit-tombstone-delete.md).
-- No threading, no edit; soft-delete via deleted_at tombstones the row (body kept, nulled out
-- by the application layer in responses) rather than removing it.
CREATE TABLE comments (
    id          BIGSERIAL PRIMARY KEY,
    target_type VARCHAR(20) NOT NULL,
    target_id   BIGINT NOT NULL,
    author_id   BIGINT NOT NULL REFERENCES users(id),
    body        VARCHAR(600) NOT NULL,
    deleted_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_comments_target ON comments(target_type, target_id);
