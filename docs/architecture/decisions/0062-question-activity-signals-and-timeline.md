# ADR-0062: 질문 요약의 활동 신호와 "질문의 생애" 타임라인 API

- 날짜: 2026-10-09
- 상태: 승인됨

## 배경 (Context)

[ADR-0061](0061-frontend-visual-refresh-from-design-canvas.md)로 디자인 시안을 프론트엔드에 적용할 때, 시안의 핵심인 Living Question Card 신호가 API에 없어 넣지 못했다. 빠진 신호는 다음과 같다.

- 피드 카드: 답변 수, 수락 여부, `rev N`, "최근 활동"
- 질문 상세: "질문의 생애" 타임라인

사용자가 다음 단계로 이 누락 요소의 구현을 선택했다(`AskUserQuestion`).

## 결정 (Decision)

- **질문 요약에 필드를 더한다**: `QuestionSearchResult`/`QuestionSearchResultResponse`에 `answerCount`, `hasAcceptedAnswer`, `versionNumber`, `createdAt`, `updatedAt`을 추가한다.
  - 계산은 모든 목록이 공유하는 `QuestionSummaryHydrator` 한 곳에서만 한다. 검색·대시보드·관련·클러스터·태그·프로필이 함께 받는다.
  - 답변 수와 최신 버전 번호는 질문 id 목록 단위의 `GROUP BY` 네이티브 쿼리 1회씩으로 가져온다. 포트는 `AnswerRepository.countByQuestionIds`, `QuestionVersionRepository.findLatestVersionNumbersByQuestionIds`다.
  - 반정규화 컬럼(`questions.answer_count` 등)은 기각했다. 답변 작성·삭제(모더레이션 Hide)·리비전마다 동기화 지점이 늘어나는 데 비해, 목록 크기(최대 수십 개)에서 집계 쿼리 비용은 무시할 만하다.
- **`updatedAt`을 "최근 활동"으로 쓴다**: Question의 `updatedAt`은 리비전뿐 아니라 수락·클러스터 합류·Outdated 표시에서도 바뀐다. 그래서 프론트는 "N일 전 수정"이 아니라 "N일 전 활동"으로 표기한다.
  - 답변 작성 같은 하위 엔티티 변화까지 반영하는 진짜 `lastActivityAt`은 만들지 않았다. 필요해지면 재검토한다.
- **타임라인은 기존 애그리거트를 합성한다**: `GET /questions/{id}/timeline`은 새 이벤트 저장소 없이 다음 데이터를 최신순으로 합친다.
  - QuestionVersion: 생성 / 리비전
  - Answer: 작성 + 수락 플래그
  - ReviewRequest: 요청 / 반영
  - 댓글은 design.md 3.3절대로 제외한다.
- **수락 시각은 지어내지 않는다**: 수락은 ANSWER_POSTED 이벤트의 `accepted` 플래그로만 표시한다. Answer·Question 어느 쪽도 수락 시각을 따로 저장하지 않고, 두 `updatedAt`은 이후 수정에서 다시 바뀌기 때문이다. 시안의 "답변 수락 · 20시간 전" 항목은 "답변 · 수락됨"(답변 작성 시각)으로 바뀐다.
- **공개 범위**: 타임라인은 질문 상세와 같은 이유로 비로그인 공개다(ADR-0041의 메서드 단위 `permitAll`).

## 결과 (Consequences)

- 피드 카드가 상세를 열지 않고도 "답이 달렸는지 / 수락됐는지 / 몇 번 고쳐졌는지 / 최근에 움직였는지"를 보여준다.
- 질문 상세 우측에 타임라인 패널이 생겼다. 답변 이벤트는 해당 답변 카드 앵커로 연결된다.
- 좁은 사이드 패널(관련 질문)에서는 숫자 열 대신 한 줄 메타로 접는 `compact` 카드를 쓴다.
- 수락 시각을 보여줘야 하면 Answer에 `acceptedAt` 컬럼(마이그레이션)을 추가하고 이 ADR을 대체한다.
- `GET /dashboard`는 `@Cacheable`(60초 TTL, ADR-0060)이다. 배포 직후 최대 60초 동안 새 필드가 없는 캐시 항목이 남을 수 있고, 역직렬화에 실패하면 `LoggingCacheErrorHandler`가 DB 조회로 폴백한다.

## 관련 문서

- [docs/architecture/api-design.md — Living Question Card 신호와 질문 타임라인](../api-design.md#living-question-card-신호와-질문-타임라인-adr-0062)
- [docs/frontend/design.md 3.2절/3.3절](../../frontend/design.md#3-핵심-ui-개념--살아있는-질문)
- [ADR-0061](0061-frontend-visual-refresh-from-design-canvas.md)
