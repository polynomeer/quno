# ADR-0024: Comment는 스레드 없는 평면 목록, 수정 불가, soft-delete로 tombstone 처리

- 날짜: 2026-08-30
- 상태: 승인됨

## 배경 (Context)

[design.md 14장](../../frontend/design.md#14-댓글토론-ux)은 댓글을 "질문/답변의 clarification을 위한 보조 채널"로 정의하지만, 이 백엔드에는 댓글 자체가 없다. 지금까지 "추가 정보가 필요하다"는 요구는 QPR `ReviewRequest`([ADR-0012](0012-qpr-multi-reviewer-thread-model.md))로 모델링해왔는데, 이는 "요청→리비전→재요청"이라는 워크플로 전용이라 design.md가 말하는 범용 clarification 댓글과 성격이 다르다 — 프론트엔드 [ADR-0020](0020-frontend-scoped-to-backend-support.md)에서도 "QPR로 대체하지 않는다"고 이미 선을 그었다. 이번에 Comment를 실제로 새로 설계한다.

## 결정 (Decision)

1. **대상은 Question과 Answer 둘 다** — `domain/comment/Comment`(targetType: QUESTION|ANSWER, targetId, authorId, body, isDeleted). `Question`/`Answer` Aggregate와 마찬가지로 독립 side-aggregate([ADR-0023](0023-vote-as-side-aggregate-no-reputation-impact.md)의 Vote와 같은 패턴)로 둔다.
2. **스레드(대댓글) 없이 평면 목록**이다 — Stack Overflow의 댓글처럼 대상 하나에 시간순으로 나열되는 단일 목록. design.md도 "일부만 노출 후 더 보기"를 언급할 뿐 중첩 답글을 요구하지 않는다. 대댓글은 자기 참조 관계와 depth 관리가 필요해 지금 범위에 비해 과하다.
3. **수정(edit)은 지원하지 않는다** — 생성과 삭제만 지원한다. 댓글은 "짧은 보조 발언"이라는 성격상 오탈자 수정보다 삭제 후 재작성이 자연스럽고, 수정 이력을 추적할 가치도 낮다(Answer의 리비전 없음과 같은 판단). 나중에 수요가 확인되면 별도로 추가한다.
4. **삭제는 soft-delete + tombstone**이다 — `isDeleted` 플래그만 세우고 행은 남긴다(design.md의 "스레드 맥락을 깨지 않는 tombstone" 요구). 응답 DTO는 삭제된 댓글의 `body`를 `null`로 반환한다(작성자만 알 수 있는 게 아니라 완전히 비공개) — isDeleted만 내려주고 프론트가 원문을 감추는 방식은 네트워크 응답에 삭제된 내용이 그대로 남는 문제가 있어 채택하지 않는다. 삭제는 작성자 본인만 가능하다(`CommentAccessDeniedException`, 403) — 모더레이터 개념이 아직 없다([ADR-0020](0020-frontend-scoped-to-backend-support.md)).
5. **`@mention` 파싱은 이번 범위에 넣지 않는다** — design.md는 자동완성과 멘션 표시를 언급하지만, 이건 사용자명 룩업과 새 알림 조건이 필요한 별도 기능이다. `body`는 그냥 원문 텍스트로 저장하고 `@닉네임` 문자열은 특별 취급하지 않는다. 프론트엔드 자동완성은 백엔드 변경 없이 기존 사용자 검색 API가 생기면 그때 붙일 수 있다.
6. **새 댓글은 알림을 발생시킨다** — `OutboxEventTypes.NEW_COMMENT`를 추가해 `NEW_ANSWER`와 동일한 fan-out 파이프라인(`DispatchOutboxEventsUseCase`)을 태운다. 질문에 댓글이면 Ward 구독자 + 질문 작성자, 답변에 댓글이면 Ward 구독자 + 질문 작성자 + 그 답변의 작성자(둘 다 필요하면 payload에 `questionAuthorId`/`answerAuthorId`를 함께 담아 `when` 분기에서 둘 다 추출 — 기존 코드 구조를 그대로 재사용). 댓글 작성자 본인은 제외된다.
7. **질문 상태(Question.status)를 건드리지 않는다** — QPR과 달리 댓글은 "질문을 다시 봐야 한다"는 워크플로 신호가 아니라 가벼운 보조 발언이므로, 어떤 상태에서도(RESOLVED 포함) 자유롭게 달 수 있다.
8. **본문 길이는 600자로 제한한다** — Stack Overflow의 댓글 글자 수 제한과 동일한 값을 그대로 채택해 "짧은 보조 발언"이라는 성격을 강제한다. 별도 마크다운 렌더링 플래그는 두지 않고 평문으로 저장한다(질문/답변 본문과 시각적으로도 구분하기 위해 프론트엔드가 마크다운 렌더러 없이 그대로 표시하는 편을 권장).
9. **평판 점수에 반영하지 않는다** — Vote와 동일한 이유([ADR-0023](0023-vote-as-side-aggregate-no-reputation-impact.md) 4번)로 댓글 작성 자체는 `UserReputation` 공식에 포함하지 않는다.

## 결과 (Consequences)

- Comment 구현이 `Question`/`Answer` 불변식을 건드리지 않아 회귀 위험이 낮다.
- 삭제된 댓글은 "누가 몇 시에 무언가를 남겼다가 지웠다"는 사실만 남고 원문은 서버 응답에서도 사라진다 — 신고/감사 목적의 원문 보존이 필요해지면 별도 저장소(예: 감사 로그)를 검토해야 한다.
- 대댓글·멘션·수정 이력이 실제로 필요해지면 각각 새 ADR로 재설계한다 — 이 결정은 "안 만든다"가 아니라 "지금 범위에서는 뺀다"는 뜻이다.

## 관련 문서

- [docs/frontend/design.md 14장](../../frontend/design.md#14-댓글토론-ux)
- [PLAN.md](../../../PLAN.md) Phase 12
- [ADR-0012](0012-qpr-multi-reviewer-thread-model.md)
- [ADR-0020](0020-frontend-scoped-to-backend-support.md)
- [ADR-0023](0023-vote-as-side-aggregate-no-reputation-impact.md)
