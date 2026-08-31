# ADR-0023: Vote는 Watch와 같은 독립 side-aggregate로, 이번 범위에서 평판 점수·인기순위에는 반영하지 않는다

- 날짜: 2026-08-30
- 상태: 일부 대체됨(5~7번은 [ADR-0032](0032-vote-score-search-sort-dashboard-reputation.md)로) — 나머지 결정(side-aggregate 구조, 자기 투표 금지, 알림 미발생 등)은 유효

## 배경 (Context)

프론트엔드 [ADR-0020](0020-frontend-scoped-to-backend-support.md)에서 Vote/Comment를 Action Rail·Answer 카드에서 보류한 이유는 백엔드에 투표 기능 자체가 없었기 때문이다. 이제 그 백엔드를 설계한다. Vote는 `Question`/`Answer` 두 Aggregate 모두에 걸리고, 점수는 목록(검색·Dashboard·관련 질문·Cluster 멤버)에 폭넓게 노출돼야 실제로 쓸모가 있다 — 그런데 이 프로젝트는 이미 `UserReputation`(ADR-0018)과 Dashboard의 인기 질문 순위(`watch_count*3 + answer_count*2`, Phase 3.2)라는, Vote 없이 검증된 두 기능을 갖고 있다. Vote를 추가하면서 이 둘의 공식까지 함께 바꾸면 변경 범위가 너무 커지고, 이미 테스트된 로직을 흔들게 된다.

## 결정 (Decision)

1. **Vote는 `Watch`와 동일한 패턴의 독립 side-aggregate**로 만든다 — `domain/vote/Vote`(voterId, targetType: QUESTION|ANSWER, targetId, value: -1|+1)와 `VoteRepository` 포트. `Question`/`Answer` Aggregate는 Vote의 존재를 전혀 모른다(양방향 참조 없음).
2. **점수는 저장하지 않고 항상 집계한다** — `votes` 테이블에 개별 투표만 저장하고, `score`는 `SUM(value)`를 그때그때 계산한다(Reputation/Metrics/Dashboard가 이미 쓰는 방식과 동일). 목록 조회의 N+1을 피하기 위해 `VoteRepository.sumScores(targetType, targetIds)` 배치 메서드를 두고, 이미 여러 기능이 공유하는 `QuestionSummaryHydrator`/`AnswerResultAssembler`에 통합해 `QuestionSearchResultResponse`/`QuestionResponse`/`AnswerResponse`에 `score` 필드로 노출한다 — 이렇게 하면 검색·Dashboard·관련 질문·Cluster 멤버 목록이 한 번의 통합으로 모두 점수를 갖게 된다.
3. **자기 자신의 질문/답변에는 투표할 수 없다**(`SelfVoteException`, 403) — Stack Overflow 등 대부분의 Q&A 서비스와 동일한 관례이며, 없으면 자기 콘텐츠의 점수를 스스로 조작할 수 있다.
4. **투표는 평판 점수(ADR-0018)에 반영하지 않는다** — `UserReputation.score` 공식은 그대로 둔다. 모든 질문/답변에 대한 순 투표 점수를 사용자 단위로 합산하는 것은 비용이 크고(현재 4개 서브쿼리 집계도 이미 무겁다), 어뷰징 방지 없이 그대로 반영하면 점수 조작 유인이 커진다. 필요해지면 별도 ADR로 재설계한다.
5. **Dashboard의 인기 질문 순위 공식도 이번 범위에서 바꾸지 않는다** — `watch_count*3 + answer_count*2`에 투표 점수를 섞는 것은 가중치를 다시 설계해야 하는 별도 결정이라 이번 Phase에 포함하지 않는다.
6. **투표는 알림을 발생시키지 않는다** — 답변 채택·정보 요청과 달리 투표는 개별 행동 하나하나가 알림 가치를 갖지 않고(SO도 투표 알림은 없음), 매 투표마다 Notification을 만들면 알림 목록이 금방 스팸으로 뒤덮인다. `outbox_events`에 새 이벤트 타입을 추가하지 않는다.
7. **`GET /search`의 정렬(Score 옵션)은 이번 범위에 포함하지 않는다** — 프론트엔드 [ADR-0022](0022-search-filters-client-side-tag-and-status-only.md)가 이 데이터 부재를 이유로 Score 정렬을 보류했는데, 이번 Phase로 데이터는 생기지만 정렬 파라미터 자체를 `/search`에 추가하는 것은 전문검색 랭킹과의 상호작용을 새로 설계해야 하는 별도 작업이라 분리한다.

## 결과 (Consequences)

- Vote 구현이 `Question`/`Answer` Aggregate의 기존 불변식을 전혀 건드리지 않아 회귀 위험이 낮다.
- `score`가 여러 응답 DTO에 나타나지만, 그 값을 실제로 정렬·랭킹에 쓰는 것은 아직 아무 데도 없다 — 프론트엔드가 점수를 "표시"만 할 수 있고, "정렬/랭킹 반영"은 후속 Phase(검색 Score 정렬, Dashboard 공식 개정, Reputation 확장)로 남는다. 이 세 가지는 각각 별도 ADR이 필요할 만큼 트레이드오프가 다르다.
- 자기 투표 차단은 UX상 "내 글에 투표 버튼이 그냥 안 보이는" 형태로 프론트엔드가 처리해야 하며(Accept 버튼과 동일한 패턴), 403을 실제로 받는 경로도 함께 대비해야 한다.

## 관련 문서

- [PLAN.md](../../../PLAN.md) Phase 11
- [ADR-0018](0018-simple-reputation-score-only.md)
- [ADR-0020](0020-frontend-scoped-to-backend-support.md)
- [ADR-0022](0022-search-filters-client-side-tag-and-status-only.md)
