# ADR-0050: 성능 — Live Chat 코드 스플리팅, 추가 N+1 제거, 조직 검색 캐싱, 검색 인덱스

- 날짜: 2026-09-10
- 상태: 승인됨

## 배경 (Context)

품질 개선 [quality-improvement-plan.md](../../product/quality-improvement-plan.md) Q-2(성능)를 진행한다: 프론트엔드 번들, 이미지, 백엔드 N+1(production-readiness.md B-4/Phase 35와는 "장애 예방"이 아니라 "체감 응답속도" 관점으로 다시 봄), 캐싱, 검색 성능.

## 결정 (Decision)

1. **번들 분석 도구는 Turbopack 네이티브 `next experimental-analyze`를 쓴다.** `@next/bundle-analyzer`(webpack 전용)를 먼저 설치했다가 이 프로젝트의 `next build` 기본 실행기인 Turbopack과 호환되지 않는 것을 발견했다(AGENTS.md가 경고하는 API 드리프트의 또 다른 사례) — 별도 패키지 설치 없이 이미 내장된 `next experimental-analyze`로 교체했다.
2. **Live Chat 패널을 진짜로 지연 로드한다.** `LiveChatPanel`이 항상 `useLiveChatSocket`(`@stomp/stompjs` 의존)을 호출하고 있어서, ADR-0039가 "연결은 참여 후에만"이라고 정했음에도 실제로는 로그인한 방문자 전원의 질문 상세 페이지 초기 번들에 stompjs가 얹혀 있었다. 소켓을 실제로 쓰는 코드를 `LiveChatSession`으로 분리하고 `next/dynamic(..., { ssr: false })`으로만 불러오게 바꿔, "채팅 참여하기"를 누르기 전까지는 그 청크 자체가 다운로드되지 않는다 — 프로덕션 빌드로 실제 네트워크 요청을 확인해 검증했다(초기 로드에는 없다가 클릭 시점에 별도 청크가 로드되고, 메시지 송수신까지 정상 동작).
3. **이미지 최적화는 대상이 없다.** 코드 전체를 확인한 결과 `<img>`/`next/image`/이미지 파일 참조가 전혀 없다 — Quno는 텍스트/Markdown 기반 Q&A라 아바타나 첨부 이미지 기능이 없다. `create-next-app`이 남긴 미사용 SVG 5개(file.svg 등)만 정리했다. 아바타/첨부 이미지 기능이 생기면 그때 `next/image`를 도입한다.
4. **N+1을 4곳 더 배치 쿼리로 바꿨다** — Phase 35에서 `QuestionSummaryHydrator`만 고쳤던 것과 같은 패턴을 재사용/확장:
   - `AnswerResultAssembler`: 답변마다 투표 점수를 따로 조회 → `VoteRepository.sumScoresByTargets`(이미 있음) 재사용.
   - `GetActivityFeedUseCase`(`/api/v1/flow`): 인기/재활성화 질문 섹션이 각각 항목마다 `findById` → `QuestionRepository.findAllByIds`(이미 있음) 재사용. 클러스터/Super Answer 섹션(3단 조회)은 발생 빈도가 낮아 그대로 둠.
   - `SearchOrganizationsUseCase`(`GET /organizations`, 공개): 조직마다 멤버 수 조회 → `OrganizationMembershipRepository`에 `countMembersByOrganizations` 배치 메서드 신설.
   - `GetUserProfileUseCase`(`GET /users/{id}/profile`, 공개): 팔로우 태그·소속 조직 둘 다 항목마다 `findById`(+조직은 멤버 수까지) → `TagRepository`/`OrganizationRepository`에 `findAllByIds` 배치 메서드 신설.
5. **조직 검색만 캐싱하고, 태그 검색은 캐싱하지 않는다.** `DashboardRepositoryAdapter`가 이미 쓰던 Redis cache-aside 패턴(`StringRedisTemplate` + Jackson, TTL 60초)을 `OrganizationRepositoryAdapter.search()`에 그대로 적용했다 — 조직은 생성이 드물고 위키 편집 대상도 아니다. 태그는 반대로 설명/문서 링크가 위키 스타일로 자주 편집되므로(Phase 28, ADR-0040), 캐싱하면 "방금 수정한 설명이 최대 60초간 안 보이는" 회귀처럼 느껴질 위험이 이득보다 크다고 판단해 캐싱 대상에서 뺐다. `Organization`/`Tag` 도메인 객체는 private 생성자라 Jackson이 직접 역직렬화할 수 없어, `TagTrend`와 같은 방식으로 평범한 캐시 전용 데이터 클래스(`CachedOrganization`)를 두고 캐시 안팎에서 변환한다.
6. **검색 성능 인덱스를 추가했지만, 현재 쿼리 구조로는 아직 안 쓰인다는 것까지 확인했다.** `SearchJpaRepository`의 두 쿼리가 `to_tsvector(...)`를 매 검색마다 모든 행에 대해 즉석 계산하고 있어, 쿼리 표현식과 정확히 일치하는 함수형 GIN 인덱스(`idx_question_versions_fts`)와 태그명 부분 문자열 매치용 트라이그램 인덱스(`idx_tags_name_trgm`, `pg_trgm` 확장)를 추가하는 마이그레이션(V23)을 만들었다. `EXPLAIN`으로 확인한 결과, 현재 쿼리가 두 테이블에 걸친 `OR` 조건(`tsvector 매치 OR 태그명 매치`)을 JOIN 이후의 필터로 평가하고 있어 `enable_seqscan = off`로 강제해도 새 인덱스를 타지 않는 것을 발견했다 — 인덱스를 실제로 활용하려면 두 조건을 각각 인덱스를 탈 수 있는 서브쿼리로 분리해 `UNION`하는 재작성이 필요한데, 이 프로젝트에 `SearchJpaRepository`의 네이티브 쿼리 자체를 검증하는 자동화 테스트가 없어(`QuestionSearchUseCaseTest`는 인메모리 페이크만 검증) 지금 재작성하는 것은 회귀 안전망 없이 SQL을 바꾸는 셈이라 보류했다. 인덱스 자체는 무해하고 향후 재작성 시 바로 쓸 수 있어 남겨둔다.

## 결과 (Consequences)

- 검색 성능 개선은 이번 인덱스만으로는 실현되지 않는다 — 쿼리 재작성이 남은 작업이며, 착수 전에 `SearchJpaRepository`를 직접 검증하는 통합 테스트부터 추가해야 한다.
- 조직 검색이 최대 60초까지 지연 반영될 수 있다 — 사용자가 조직을 만들자마자 검색에 안 보이면 "방금 만들었는데 왜 안 보이지" 문의가 들어올 수 있다(Dashboard의 popular questions/trending tags와 같은 수준의 트레이드오프로 수용).
- Live Chat 코드 스플리팅으로 채팅을 실제로 여는 순간 약간의 로딩 지연(청크 다운로드)이 생긴다 — `loading: () => <p>불러오는 중...</p>`로 완화했다.

## 관련 문서

- [quality-improvement-plan.md](../../product/quality-improvement-plan.md) Q-2
- [0035-verified-organization-email-domain-mailpit.md](0035-verified-organization-email-domain-mailpit.md), [0040-tag-detail-wiki-editable-and-real-stats.md](0040-tag-detail-wiki-editable-and-real-stats.md) (태그 캐싱을 뺀 이유의 배경)
- [PLAN.md](../../../PLAN.md) Phase 39
