-- Tracks the last-known latest release per tracked technology tag (Phase 21, ADR-0033).
-- One row per tag_slug (curated set — domain/qunobot/TrackedTechnologies.MAPPING). Not a history
-- log: each scan overwrites the row so the app can detect "did the latest version change since
-- last check", which is what drives whether to fan out a TECH_VERSION_IMPACT_DETECTED event.
CREATE TABLE technology_releases (
    id                    BIGSERIAL PRIMARY KEY,
    tag_slug              VARCHAR(100) NOT NULL,
    product_slug          VARCHAR(100) NOT NULL,
    latest_version        VARCHAR(50) NOT NULL,
    latest_release_date   DATE NOT NULL,
    checked_at            TIMESTAMPTZ NOT NULL,
    updated_at            TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX uq_technology_releases_tag_slug ON technology_releases(tag_slug);
