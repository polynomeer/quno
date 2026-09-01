# ADR-0040: 태그 상세 정보는 위키 스타일 편집 + 실제 통계 쿼리로 구현하고, 30일 활동 요약은 범위 밖에 둔다

- 날짜: 2026-09-01
- 상태: 승인됨

## 배경 (Context)

[ADR-0021](0021-tag-detail-via-search-approximation.md)이 `Tag` 도메인이 `id`/`name`/`slug`뿐이라는 이유로 범위 밖에 뒀던 태그 설명/공식 문서 링크/상위 기여자/관련 태그/Follow 버튼을 구현한다. 남은 프론트엔드 격차 중 마지막 하나로, [ADR-0038](0038-organization-direct-ask-frontend-no-user-search.md)/[ADR-0039](0039-live-chat-frontend-stompjs-connect-on-demand.md)에 이어 사용자가 이어서 진행을 확인했다.

[design.md 15.2절](../../frontend/design.md#15-태그-경험)이 요구하는 항목은 6가지: 태그 설명/공식 문서 링크/Follow 버튼, Latest/Unanswered/Top 탭, 최근 30일 활동 요약, 태그 상위 기여자, 관련 태그, 태그 작성 가이드.

## 결정 (Decision)

1. **`description`/`docs_url`을 `tags` 테이블에 추가하고, 위키 스타일로 아무 로그인 사용자나 편집할 수 있게 한다.** 태그 자체가 이미 "누구나 질문에 답으로써 만드는" 낮은 신뢰 수준을 갖고 있어(`Tag.slugify`의 find-or-create가 소유자 개념이 없음), 설명 편집에만 별도 권한 모델을 만드는 것은 일관성이 없다고 판단했다. `PUT /tags/{id}`에 소유권 검사가 없다.
2. **ADR-0021의 검색 근사를 실제 `GET /tags/{id}/questions?sort=` 쿼리로 대체한다.** `question_tags`를 직접 조인해 Latest(최신순)/Unanswered(답변 0개)/Top(순 투표점수순) 세 가지 정렬을 native SQL로 구현했다 — ADR-0021이 지적한 "상위 limit개 밖이면 결과 없음으로 보이는" 한계가 해소된다.
3. **태그 상위 기여자는 "이 태그가 달린 질문에 답변을 몇 개 남겼는가"로만 정의한다.** 채택 여부나 받은 투표 점수까지 가중치에 넣는 정교한 공식(레퍼테이션 점수처럼)도 고려했지만, 실사용 데이터가 전혀 없는 상태에서 여러 항목을 조합한 공식은 추측성 판단만 늘릴 뿐이라고 보고 가장 단순한 지표로 시작한다(Phase 20 Vote 반영 때와 같은 태도 — 실사용 후 재조정 가능).
4. **관련 태그는 같은 질문에 함께 달린 횟수로 랭킹한다.** `SearchJpaRepository.findRelatedQuestionIds`가 질문 단위로 하던 것과 동일한 co-occurrence 아이디어를 태그 단위로 옮긴 것뿐이다.
5. **Follow 버튼은 새 백엔드 변경 없이 구현한다.** ADR-0021 당시엔 "내가 팔로우 중인지" 조회 API가 없다고 판단했지만, 그 사이 `GetUserProfileUseCase`가 `followedTags`를 이미 내려주고 있었다(Organization/Direct Ask 프론트엔드 작업에서 확인) — `JoinOrganizationButton`과 같은 패턴(`useUserProfile(내 id).followedTags`로 판단)을 그대로 재사용한다.
6. **최근 30일 활동 요약과 태그 작성 가이드는 이번 범위에서 만들지 않는다.** 활동 요약(질문/답변 증가율 같은 시계열 지표)은 실사용 데이터가 거의 없는 지금 시점에 값이 대부분 0이거나 무의미해 보여줄 가치가 낮고, 필요해지면 Dashboard의 트렌딩 태그 쿼리(`DashboardJpaRepository.findTrendingTags`)를 확장하는 쪽이 새 쿼리를 만드는 것보다 낫다. 태그 작성 가이드는 태그별 데이터가 아니라 고정된 도움말 카피라 이번 기능 범위와 무관하다.

## 결과 (Consequences)

- 태그 설명은 채팅 로그처럼 누구나 고칠 수 있어, 악의적 편집(예: 스팸 링크를 `docsUrl`에 넣기)에 대한 방어가 없다 — 모더레이션이 실제로 필요해지면 Report 대상에 Tag를 추가하는 방향으로 재검토한다(현재 Report는 Question/Answer/Comment만 대상).
- 상위 기여자·관련 태그 쿼리는 모두 이 프로젝트의 다른 집계처럼 검증되지 않은 native SQL이라 curl로 실제 데이터를 넣어 결과를 직접 확인했다(단위 테스트는 유스케이스 오케스트레이션만 커버).
- ADR-0021이 남긴 네 가지 격차(설명/문서 링크, 상위 기여자, 관련 태그, Follow) 중 마지막 하나까지 이번 Phase로 모두 닫혔다 — 프론트엔드 백엔드 격차 표(roadmap.md §7)에 남는 항목이 없어졌다.

## 관련 문서

- [api-design.md](../api-design.md#태그-phase-28)
- [ADR-0021](0021-tag-detail-via-search-approximation.md) (이번 ADR이 닫는 원래 결정)
- [ADR-0038](0038-organization-direct-ask-frontend-no-user-search.md), [ADR-0039](0039-live-chat-frontend-stompjs-connect-on-demand.md) (같은 세션의 앞선 프론트엔드 격차 해소)
- [PLAN.md](../../../PLAN.md) Phase 28
