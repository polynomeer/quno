# ADR-0022: 고급 검색 필터는 클라이언트 사이드 Tags/Status만, Score/Date/Sort는 보류

- 날짜: 2026-08-27
- 상태: 승인됨

## 배경 (Context)

[design.md 10장](../../frontend/design.md#10-질문-목록검색)은 검색 결과 화면에 `[Tags] [Answered] [Date] [Score]` 필터와 `Sort: Relevance` 정렬을 요구한다. 그러나 `GET /search?q=&limit=`는 텍스트 검색만 지원하고 필터/정렬 파라미터를 받지 않으며, 응답 DTO(`QuestionSearchResultResponse` — id/title/status/tags)에는 `createdAt`, `answerCount`가 없다. 즉:

- **Score 필터/정렬**: 투표 기능 자체가 없어([ADR-0020](0020-frontend-scoped-to-backend-support.md)) 애초에 존재하지 않는 값이다.
- **Date 필터**: 응답에 `createdAt`이 없어 클라이언트에서도 근사할 방법이 없다.
- **Answered/Unanswered 필터**: `status`만으로는 판단할 수 없다 — `OPEN`이어도 답변이 달려 있을 수 있고(단지 채택되지 않았을 뿐), 답변 수 자체가 응답에 없다.
- **Sort: Relevance 외 옵션**(Newest 등): 정렬 기준이 될 필드(`createdAt`)가 없어 backend의 기본 순서를 그대로 쓰는 것 외에 선택지가 없다.

반면 `tags: string[]`와 `status`는 이미 응답에 있어 정확하게 필터링할 수 있다.

## 결정 (Decision)

`/questions` 검색 결과 화면에 **Tags**와 **Status** 필터만 클라이언트 사이드로 구현한다 — `GET /search`가 이미 반환한 결과 집합 안에서 `Array.filter`로 좁히는 방식이며, 서버에 별도 요청을 보내지 않는다. 필터 상태는 design.md의 요구대로 URL query string(`?q=&tags=&status=`)에 반영해 공유·뒤로가기를 보장한다. Answered/Date/Score 필터와 Score/Newest 정렬 옵션은 이번 범위에서 만들지 않는다 — 자리표시(placeholder)도 두지 않는다(그 자체로 오해를 유발하는 UI보다 아예 없는 편이 낫다고 판단).

## 결과 (Consequences)

- 검색 결과 화면이 design.md 목업보다 단순하다 — Tags/Status 두 종류의 필터 칩만 존재한다.
- 필터는 `GET /search`가 이미 가져온 `limit`개 결과 안에서만 좁혀진다 — 서버 쪽에 더 정확히 맞는 결과가 있어도 초기 응답에 없으면 보이지 않는다. 실사용에서 문제가 되면 `limit`을 높이거나 서버 필터 파라미터 추가를 재검토한다.
- Answered/Date/Score 필터가 실제로 필요해지면 백엔드에 `answerCount`/`createdAt`을 응답에 추가하거나(또는 투표 기능 자체를 설계하거나) 하는 선행 작업이 필요하다 — 이 결정은 "안 만든다"가 아니라 "필요한 데이터가 생기기 전까지 미룬다"는 뜻이다.

## 관련 문서

- [docs/frontend/design.md 10장](../../frontend/design.md#10-질문-목록검색)
- [PLAN.md](../../../PLAN.md) Frontend Phase 4 (F4.1)
- [ADR-0020](0020-frontend-scoped-to-backend-support.md)
- [ADR-0021](0021-tag-detail-via-search-approximation.md)
