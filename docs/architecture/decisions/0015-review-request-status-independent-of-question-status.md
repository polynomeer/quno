# ADR-0015: ReviewRequest.status는 Question.status를 다시 게이팅하지 않는다

- 날짜: 2026-08-25
- 상태: 승인됨

## 배경 (Context)

PLAN.md 5.3을 원래 "재요청한 ReviewRequest를 ADDRESSED로 바꾸고, 그 질문에 열려있는 요청이 더 없으면 `Question.reviewAddressed()`로 NEEDS_INFO→UPDATED 복귀"로 계획했다. 구현 중 작성한 단위 테스트(`두 번째 리뷰 요청이 열려있는 동안 질문은 NEEDS_INFO를 유지해야 한다`)가 실패하면서, 이 계획이 이미 Phase 2.3에서 확정된 `Question.revise()`의 동작과 모순된다는 것을 발견했다: `revise()`는 열려있는 ReviewRequest 개수와 무관하게 **어떤 리비전이든 즉시 NEEDS_INFO를 벗어나 UPDATED로 전이**시킨다(RESOLVED만 예외). 게다가 재요청 자체가 "요청 시점 이후 실제 리비전이 있어야" 허용되므로(PLAN.md 5.3의 또 다른 규칙), 재요청이 호출되는 시점에는 이미 그 리비전 때문에 질문이 UPDATED로 넘어가 있다 — 즉 `reviewAddressed()`가 실행될 때 질문은 이미 NEEDS_INFO가 아니어서, 이 메서드는 **항상 도달 불가능한 코드**였다.

## 결정 (Decision)

`Question.reviewAddressed()`를 제거하고, `ReRequestReviewUseCase`에서 "열려있는 요청이 더 없으면 Question을 갱신"하는 로직을 삭제한다. `ReRequestReviewUseCase`는 이제 해당 `ReviewRequest`를 ADDRESSED로 전환하고 `REVIEW_RE_REQUESTED` 알림을 보내는 것만 한다 — Question.status는 건드리지 않는다.

대신 상태 모델을 다음과 같이 명확히 정리한다: **Question.status는 revise()/resolve()/requestMoreInfo()만으로 결정되고, ReviewRequest.status(OPEN/ADDRESSED)는 리뷰어별로 독립된 부기 정보일 뿐 Question.status를 다시 게이팅하지 않는다.** NEEDS_INFO는 "가장 최근 정보 요청 이후 아직 리비전이 없다"는 뜻이고, UPDATED는 "그 이후 리비전이 있었다"는 뜻이다 — 특정 리뷰어가 그 리비전에 만족했는지는 각 `ReviewRequest.status`가 별도로 기록한다.

## 결과 (Consequences)

- 여러 리뷰어가 동시에 요청을 걸어도 작성자가 한 번만 리비전하면 Question은 즉시 UPDATED로 넘어간다. 아직 자신의 요청이 ADDRESSED로 표시되지 않은 리뷰어도 "질문 자체는 이미 갱신됐다"는 걸 알 수 있고, 재요청 알림을 통해 "당신이 요청한 내용을 반영했다"는 개별 신호를 받는다 — 두 정보가 분리되어 오히려 더 명확하다.
- 이 설계는 유닛 테스트가 실제 모순을 잡아낸 사례다: 계획 문서(PLAN.md)에 적어둔 규칙이라도 이미 존재하는 도메인 규칙과 충돌하면 구현 전에 재검증해야 한다는 것을 보여준다.
- 코드가 더 단순해졌다 — `ReRequestReviewUseCase`는 Question 애그리게잇을 조회만 하고 저장하지 않는다(`questionVersionRepository`로 최신 버전 번호만 확인).

## 관련 문서

- [domain-model.md](../domain-model.md#qpr-이벤트-체인-phase-2)
- [ADR-0012](0012-qpr-multi-reviewer-thread-model.md)
- [PLAN.md](../../../PLAN.md) Phase 5.3
