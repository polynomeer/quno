-- Phase 63.5 (ADR-0078): "내 스택" — the dependencies a user chose to keep from a pasted build
-- file, same shape as question_dependencies. One list per user, private to them. CASCADE because
-- it is the user's own setting, not shared content.
CREATE TABLE user_stack_dependencies (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    ecosystem  VARCHAR(20)  NOT NULL,
    name       VARCHAR(300) NOT NULL,
    version    VARCHAR(100),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_stack_dependencies_user_id ON user_stack_dependencies(user_id);
