-- Verified Organization via work/school email domain matching (Phase 23, ADR-0035).
ALTER TABLE organizations ADD COLUMN email_domain VARCHAR(255);

-- Only one Organization per verified domain; NULL (Virtual/Community) is unrestricted.
CREATE UNIQUE INDEX uq_organizations_email_domain ON organizations(email_domain) WHERE email_domain IS NOT NULL;

CREATE TABLE email_domain_verifications (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL REFERENCES users(id),
    email        VARCHAR(255) NOT NULL,
    domain       VARCHAR(255) NOT NULL,
    code         VARCHAR(10) NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at   TIMESTAMPTZ NOT NULL,
    verified_at  TIMESTAMPTZ
);

CREATE INDEX idx_email_domain_verifications_user_id ON email_domain_verifications(user_id);
