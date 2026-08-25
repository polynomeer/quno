-- Answer-to-QuestionVersion linkage (see docs/product/vision.md "답변이 어느 시점의
-- 질문을 대상으로 했는지 불명확" and docs/architecture/decisions/0012 관련 PLAN.md 5.1).
-- DEFAULT 1 exists only to backfill pre-existing rows created before this column existed;
-- the application always supplies an explicit value on insert.
ALTER TABLE answers ADD COLUMN target_version_number INT NOT NULL DEFAULT 1;
