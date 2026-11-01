-- Phase 65.1 (ADR-0079): tokens for the CLI and MCP server. Only the SHA-256 of the secret is
-- stored; display_prefix is the first characters of the secret so people can tell tokens apart.
-- scopes is a comma-separated subset of READ,ASK. Rows stay after revocation for the token list.
CREATE TABLE personal_access_tokens (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name           VARCHAR(100) NOT NULL,
    token_hash     VARCHAR(64)  NOT NULL UNIQUE,
    display_prefix VARCHAR(20)  NOT NULL,
    scopes         VARCHAR(100) NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expires_at     TIMESTAMPTZ  NOT NULL,
    last_used_at   TIMESTAMPTZ,
    revoked_at     TIMESTAMPTZ
);

CREATE INDEX idx_personal_access_tokens_user_id ON personal_access_tokens(user_id);
