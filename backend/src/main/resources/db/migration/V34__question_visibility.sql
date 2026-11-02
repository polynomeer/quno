-- Phase 79 (ADR-0085): a question is PUBLIC or belongs to one verified organization (ORG). Fixed
-- at creation. Every existing question stays PUBLIC. organization_id is set exactly when ORG.
ALTER TABLE questions ADD COLUMN visibility VARCHAR(10) NOT NULL DEFAULT 'PUBLIC';
ALTER TABLE questions ADD COLUMN organization_id BIGINT REFERENCES organizations(id);
ALTER TABLE questions ADD CONSTRAINT chk_questions_visibility
    CHECK ((visibility = 'ORG') = (organization_id IS NOT NULL) AND visibility IN ('PUBLIC', 'ORG'));

CREATE INDEX idx_questions_organization_id ON questions(organization_id) WHERE organization_id IS NOT NULL;
