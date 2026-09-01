-- Virtual/Community Organization (Phase 22, ADR-0034) — user-created groups, no external
-- identity verification. Verified Organization (email-domain matching to a real company/school)
-- is deliberately out of scope for this Phase.
CREATE TABLE organizations (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(100) NOT NULL,
    description TEXT,
    created_by  BIGINT NOT NULL REFERENCES users(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_organizations_slug ON organizations(slug);

CREATE TABLE organization_memberships (
    organization_id BIGINT NOT NULL REFERENCES organizations(id),
    user_id         BIGINT NOT NULL REFERENCES users(id),
    joined_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (organization_id, user_id)
);

CREATE INDEX idx_organization_memberships_user_id ON organization_memberships(user_id);
