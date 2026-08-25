# ADR-0008: 전용 검색엔진 도입 전, PostgreSQL 네이티브 전문검색으로 시작

- 날짜: 2026-08-24
- 상태: 승인됨

## 배경 (Context)

MVP 성공 지표 중 하나가 검색/관련 질문 경험이지만([mvp-scope.md](../../product/mvp-scope.md)), `system-architecture.md`는 애초에 OpenSearch/Elasticsearch 계열을 "이후 검토" 대상으로만 남겨뒀다. 검색 인덱스를 별도로 운영하려면 인덱싱 파이프라인(Outbox → Worker → 색인)까지 갖춰야 하는데, MVP 단계에서 그 투자가 정당화되는지 판단해야 했다.

## 결정 (Decision)

전용 검색엔진 없이 PostgreSQL 기능만으로 검색/관련 질문을 구현한다.

- `GET /search`: `to_tsvector('simple', title || body_markdown || logs) @@ plainto_tsquery('simple', q)`로 최신 버전의 제목/본문/에러로그를 전문검색하고, `tags.name ILIKE '%q%'`를 OR로 결합한다. 형태소 분석기는 `simple`(토큰화만, 어간 추출 없음)만 쓴다.
- `GET /questions/{id}/related`: `question_tags` 자기 조인으로 공유 태그 수를 계산해 내림차순 정렬한다(mvp-scope.md "태그 매칭 우선").
- 두 기능 모두 soft-delete된 질문/태그는 제외한다.

## 결과 (Consequences)

- 별도 인프라(검색엔진, 인덱싱 워커) 없이 Phase 2.9를 하루 만에 구현할 수 있었다 — MVP 검증 속도를 우선한 선택이 실제로 맞아떨어졌다.
- `simple` 설정은 어간 추출이 없어 한국어 등 비영어 검색 품질이 낮다. 실제 사용자가 늘어 이 품질이 문제가 되면 `pg_bigm` 확장이나 외부 검색엔진으로 교체하는 것이 다음 단계다.
- 관련 질문 추천은 태그가 전혀 없는 질문에는 결과가 비어 있을 수 있다 — 본문 유사도 기반 추천(임베딩 등)은 의도적으로 MVP 이후로 미뤘다.
- 이 결정은 "검색 트래픽이나 품질 요구가 실제로 임계점을 넘을 때" 재검토 대상이며, 그 전까지는 `to_tsvector` 기반 구현을 유지한다.

## 관련 문서

- [api-design.md](../api-design.md#검색·관련-질문-구현-phase-29)
- [system-architecture.md](../system-architecture.md#확정-기술-스택) Search 행
- [PLAN.md](../../../PLAN.md) Phase 2.9
- 커밋 `8e0f399` (PostgreSQL 전문검색과 관련 질문 추천 구현)
