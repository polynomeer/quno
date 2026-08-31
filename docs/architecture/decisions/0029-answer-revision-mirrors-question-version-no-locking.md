# ADR-0029: Answer Revision은 Question/QuestionVersion 분리를 그대로 적용하되, 동시성 잠금은 두지 않는다

- 날짜: 2026-08-31
- 상태: 승인됨

## 배경 (Context)

[design.md #13.1](../../frontend/design.md)은 "답변의 수정 이력은 질문과 동일한 revision UI 패턴을 재사용한다"고 전제하지만, 실제로는 `Answer`에 편집(`edit`) 기능 자체가 없다 — `domain-model.md`의 Aggregate 표가 "Answer | create, edit, softDelete"라고 적어둔 것과 달리, `Answer.kt`에는 `accept()`/`unaccept()`만 있고 본문을 바꾸는 메서드가 아예 없다(문서 착오, 코드가 근거). `Answer.bodyMarkdown`은 생성 시점 값이 영구히 고정된다. Question은 이미 [ADR-0004](0004-question-version-aggregate-boundaries.md)로 "Question(식별자·상태·포인터) / QuestionVersion(실제 콘텐츠, append-only)"를 분리해뒀고, 리비전 동시성은 [ADR-0005](0005-pessimistic-locking-revision-concurrency.md)로 Pessimistic Locking을 채택해뒀다. Answer에도 같은 경험을 주려면 이 두 결정을 그대로 적용할지, 아니면 Answer의 실제 사용 패턴에 맞게 다르게 갈지 정해야 했다.

## 결정 (Decision)

**Aggregate 분리는 Question과 동일하게 적용**하고, **동시성 잠금은 적용하지 않는다.**

- `Answer`는 식별자·작성자·수락 상태·`targetVersionNumber`·**`latestVersionId` 포인터**만 갖고, `bodyMarkdown`은 최신 버전의 캐시 값으로만 남긴다(`questions.title`이 `question_versions`의 최신 제목을 캐시하는 것과 동일한 패턴). 새 `answer_versions` 테이블(`id, answer_id, version_number, body_markdown, created_by, created_at`)이 append-only 이력을 담당한다. 기존 `answers.body_markdown`은 V12 마이그레이션으로 각 답변의 `answer_versions` v1으로 백필한다.
- **Pessimistic Locking은 두지 않는다.** ADR-0005가 이 전략을 택한 이유는 QPR 흐름에서 질문 작성자가 여러 리뷰 요청에 동시에 응답하며 리비전을 만들 수 있는 실질적 동시 편집 압력이 있었기 때문이다. Answer는 **작성자 본인만** 자신의 답변을 수정할 수 있어(다른 참여자가 개입할 경로가 없음) 같은 종류의 동시성 경합이 사실상 없다 — 일반적인 트랜잭션 격리 수준으로 충분하다고 판단해 잠금을 생략한다.
- API는 Question의 버전 API를 그대로 미러링한다: `POST /answers/{id}/versions`(작성자 본인만, 아니면 새 `AnswerAccessDeniedException` 403), `GET /answers/{id}/versions`, `GET /answers/{id}/versions/{version}`, `GET /answers/{id}/versions/{version}/diff?from=`(기존 `TextDiffer.diffLines`를 그대로 재사용 — 마크다운 두 문자열을 비교하는 순수 유틸이라 Question 전용 로직이 아니었음).
- **리비전은 알림을 발생시킨다** — `OutboxEventTypes.ANSWER_REVISION`을 추가해 `NEW_ANSWER`와 동일한 수신자 집합(Ward 구독자 + 질문 작성자)에게 알린다. 답변 본문이 바뀌는 것도 "그 질문을 구독하는 이유가 되는 변화"로 보기 때문이다.
- Answer가 대상 질문 버전을 가리키는 기존 `targetVersionNumber`/`isStale`(Phase 5.1)은 이번 결정과 무관하다 — 그건 "이 답변이 질문의 어떤 버전을 보고 작성됐는가"이고, 이번 건 "이 답변 자체의 버전 이력"이라 서로 다른 축이다.

## 결과 (Consequences)

- 프론트엔드는 Question의 revision history/diff UI 컴포넌트를 Answer에도 거의 그대로 재사용할 수 있다 — API 모양이 의도적으로 동일하기 때문이다.
- 잠금이 없으므로 극히 드문 경우(같은 브라우저 탭 두 개로 동시에 같은 답변을 수정) 나중에 커밋한 리비전이 앞선 리비전을 그냥 덮어쓴다(lost update). 실사용에서 이게 문제로 확인되면 ADR-0005와 같은 방식을 재검토한다.
- `answers.body_markdown`이 이제 파생 캐시가 되므로, 리비전 생성 use case가 `Answer`와 `AnswerVersion`을 같은 트랜잭션에서 함께 갱신해야 한다(Question의 `revise()` 흐름과 동일한 책임).
- 기존 답변 채택/삭제(`accept`/`unaccept`)는 버전과 무관하게 `Answer` 자체의 상태이므로 이번 변경으로 건드리지 않는다.

## 관련 문서

- [ADR-0004](0004-question-version-aggregate-boundaries.md) (그대로 적용하는 Aggregate 분리 패턴)
- [ADR-0005](0005-pessimistic-locking-revision-concurrency.md) (이번엔 채택하지 않기로 한 잠금 전략과 그 이유)
- [PLAN.md](../../../PLAN.md) Phase 17
