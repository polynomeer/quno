-- Quno MVP P0 core schema.
-- See docs/architecture/domain-model.md for the ERD and delete/FK policy this follows.

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    nickname      VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- latest_version_id / accepted_answer_id reference tables created below;
-- their FK constraints are added after those tables exist to break the cycle.
CREATE TABLE questions (
    id                 BIGSERIAL PRIMARY KEY,
    author_id          BIGINT NOT NULL REFERENCES users(id),
    title              VARCHAR(300) NOT NULL,
    status             VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    latest_version_id  BIGINT,
    accepted_answer_id BIGINT,
    deleted_at         TIMESTAMPTZ,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_questions_author_id ON questions(author_id);
CREATE INDEX idx_questions_status ON questions(status) WHERE deleted_at IS NULL;

CREATE TABLE question_versions (
    id             BIGSERIAL PRIMARY KEY,
    question_id    BIGINT NOT NULL REFERENCES questions(id),
    version_number INT NOT NULL,
    title          VARCHAR(300) NOT NULL,
    body_markdown  TEXT NOT NULL,
    environment    TEXT,
    logs           TEXT,
    created_by     BIGINT NOT NULL REFERENCES users(id),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (question_id, version_number)
);

CREATE INDEX idx_question_versions_question_id ON question_versions(question_id);

ALTER TABLE questions
    ADD CONSTRAINT fk_questions_latest_version
    FOREIGN KEY (latest_version_id) REFERENCES question_versions(id);

CREATE TABLE answers (
    id            BIGSERIAL PRIMARY KEY,
    question_id   BIGINT NOT NULL REFERENCES questions(id),
    author_id     BIGINT NOT NULL REFERENCES users(id),
    body_markdown TEXT NOT NULL,
    is_accepted   BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at    TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_answers_question_id ON answers(question_id) WHERE deleted_at IS NULL;

ALTER TABLE questions
    ADD CONSTRAINT fk_questions_accepted_answer
    FOREIGN KEY (accepted_answer_id) REFERENCES answers(id);

CREATE TABLE tags (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    slug       VARCHAR(100) NOT NULL,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_tags_name_active ON tags(name) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_tags_slug_active ON tags(slug) WHERE deleted_at IS NULL;

CREATE TABLE question_tags (
    question_id BIGINT NOT NULL REFERENCES questions(id),
    tag_id      BIGINT NOT NULL REFERENCES tags(id),
    PRIMARY KEY (question_id, tag_id)
);

CREATE INDEX idx_question_tags_tag_id ON question_tags(tag_id);

CREATE TABLE user_tag_follows (
    user_id    BIGINT NOT NULL REFERENCES users(id),
    tag_id     BIGINT NOT NULL REFERENCES tags(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, tag_id)
);

CREATE INDEX idx_user_tag_follows_tag_id ON user_tag_follows(tag_id);

CREATE TABLE watches (
    user_id     BIGINT NOT NULL REFERENCES users(id),
    question_id BIGINT NOT NULL REFERENCES questions(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, question_id)
);

CREATE INDEX idx_watches_question_id ON watches(question_id);

CREATE TABLE notifications (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id),
    type        VARCHAR(50) NOT NULL,
    question_id BIGINT,
    answer_id   BIGINT,
    payload_json JSONB NOT NULL DEFAULT '{}'::JSONB,
    is_read     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user_id_unread ON notifications(user_id) WHERE is_read = FALSE;
