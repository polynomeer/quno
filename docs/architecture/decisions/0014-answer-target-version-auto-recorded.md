# ADR-0014: 답변의 대상 버전은 작성 시점 최신 버전으로 자동 기록하고, 명시적 버전 선택 UI는 만들지 않음

- 날짜: 2026-08-25
- 상태: 승인됨

## 배경 (Context)

[vision.md](../../product/vision.md)는 "답변이 어느 시점의 질문을 대상으로 했는지 불명확"을 기존 Q&A의 문제로, "답변이 특정 QuestionVersion을 target으로 명시"를 Quno의 차별점으로 든다. PLAN.md 5.1을 구현하며 이를 어떻게 채울지 두 가지 방식이 있었다: (1) 답변 작성 폼에 버전 선택 드롭다운을 두고 사용자가 명시적으로 대상 버전을 고르게 하는 방식, (2) 답변 작성 시점의 질문 최신 버전을 서버가 자동으로 기록하는 방식.

## 결정 (Decision)

**자동 기록** 방식을 택한다. `WriteAnswerUseCase`가 답변 생성 시 `Question.latestVersionId`가 가리키는 `QuestionVersion.versionNumber`를 조회해 `Answer.targetVersionNumber`에 그대로 기록한다. 버전 선택 UI/파라미터는 만들지 않는다 — API도 `WriteAnswerCommand`에 버전 지정 필드를 두지 않았다.

명시적 선택 UI를 기각한 이유: 실제로 답변자가 "일부러 과거 버전을 대상으로 답한다"는 시나리오는 드물고(대개는 지금 보이는 최신 질문에 답한다), 이 드문 경우를 위해 프론트엔드에 버전 피커를 만드는 비용이 지금 단계에서는 정당화되지 않는다고 판단했다.

## 결과 (Consequences)

- `AnswerResult`/`AnswerResponse`에 `targetVersionNumber`와, 그 이후 질문이 리비전됐는지 나타내는 `isStale`(조회 시점 계산, 저장하지 않음)이 노출된다. `application/common/AnswerResultAssembler`가 이 계산을 전담하며 `WriteAnswerUseCase`/`ListAnswersUseCase`/`GetUserProfileUseCase`가 공유한다.
- 답변자가 실제로 과거 버전을 보고 답했지만 그 사이 질문이 이미 리비전된 경우(레이스 컨디션)에도 "작성 시점의 최신 버전"이 그대로 기록된다 — 답변자가 화면에 렌더링된 버전과 서버가 기록하는 버전이 미세하게 다를 수 있다는 뜻이지만, 그 차이가 실질적으로 문제된 사례는 아직 없다.
- 나중에 "이 답변은 사실 Qv1을 보고 쓴 것이다"처럼 사용자가 명시적으로 과거 버전을 지정해야 할 요구가 실제로 생기면(예: QPR Review 스레드에서 특정 리비전을 콕 집어 답해야 하는 경우), 그때 버전 선택 UI/파라미터를 추가하는 것으로 재검토한다.

## 관련 문서

- [api-design.md](../api-design.md#답변–질문버전-연결-phase-51)
- [PLAN.md](../../../PLAN.md) Phase 5.1
