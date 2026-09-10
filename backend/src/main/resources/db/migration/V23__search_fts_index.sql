-- 검색 성능 점검(quality-improvement-plan.md Q-2). SearchJpaRepository의 두 쿼리 모두
-- to_tsvector('simple', title || ' ' || body_markdown || ' ' || coalesce(logs, ''))를
-- 매 검색마다 모든 행에 대해 즉석에서 계산하고 있었다 — 인덱스가 없으면 seq scan을 피할 수 없다.
-- 쿼리의 표현식과 완전히 동일한 함수형 GIN 인덱스를 만들면, 별도 컬럼 없이도 플래너가 이 인덱스를
-- 골라 쓴다(표현식이 정확히 일치해야 함 — 쿼리 쪽 표현식을 바꾸면 이 인덱스도 함께 바꿔야 한다).
CREATE INDEX idx_question_versions_fts ON question_versions
    USING GIN (to_tsvector('simple', title || ' ' || body_markdown || ' ' || coalesce(logs, '')));

-- 검색 쿼리의 태그명 조건(t.name ILIKE '%...%')도 접두어가 아닌 부분 문자열 매치라 btree
-- 인덱스를 못 쓴다 — pg_trgm의 GIN 인덱스로 ILIKE '%...%'까지 가속한다.
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX idx_tags_name_trgm ON tags USING GIN (name gin_trgm_ops);
