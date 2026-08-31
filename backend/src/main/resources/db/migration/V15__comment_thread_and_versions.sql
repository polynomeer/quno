-- Comment extensions (Phase 19, ADR-0031): one-level reply nesting + append-only edit history.
-- No diff table/columns — comments are short enough that a plain before/after body list suffices.
ALTER TABLE comments ADD COLUMN parent_comment_id BIGINT REFERENCES comments(id);
ALTER TABLE comments ADD COLUMN version_number INT NOT NULL DEFAULT 1;

CREATE INDEX idx_comments_parent_comment_id ON comments(parent_comment_id) WHERE parent_comment_id IS NOT NULL;

CREATE TABLE comment_versions (
    id             BIGSERIAL PRIMARY KEY,
    comment_id     BIGINT NOT NULL REFERENCES comments(id),
    version_number INT NOT NULL,
    body           VARCHAR(600) NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (comment_id, version_number)
);

CREATE INDEX idx_comment_versions_comment_id ON comment_versions(comment_id);

-- No backfill: unlike answer_versions, a comment_versions row only exists once a comment has been
-- edited at least once (the current body/version_number on `comments` already IS v1 until then) —
-- see EditCommentUseCase. Every pre-existing comment simply starts at version 1 with no history,
-- same as a freshly created one.
