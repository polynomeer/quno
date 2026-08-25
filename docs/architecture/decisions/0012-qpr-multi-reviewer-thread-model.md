# ADR-0012: QPR Review의 정보 요청은 다중 리뷰 요청 스레드 모델로 구현

- 날짜: 2026-08-25
- 상태: 승인됨

## 배경 (Context)

Phase 5(협업형 QPR)를 계획하면서, [domain-model.md](../domain-model.md#qpr-이벤트-체인-phase-2)의 이벤트 체인(`RequestMoreInfo → NEEDS_INFO → 리비전 → ReRequestReview`)을 실제 도메인 모델로 옮겨야 했다. 가장 단순한 구현은 `Question`에 "정보 요청 메시지" 필드 하나와 `NEEDS_INFO` 상태만 두는 것이었지만, 이 경우 여러 리뷰어가 동시에 다른 정보를 요청하면 요청을 구분할 수 없고 "재요청" 대상이 항상 마지막 요청자 한 명으로 제한된다는 한계가 있었다.

## 결정 (Decision)

리뷰 요청을 **독립된 Aggregate `ReviewRequest`**(questionId, requestedBy, message, status: OPEN/ADDRESSED, questionVersionNumberAtRequest, createdAt, addressedAt)로 모델링해 여러 리뷰어가 각자 독립적으로 정보를 요청/재요청할 수 있게 한다(GitHub PR review와 동일한 형태). 단일 플래그 방식보다 구현 비용은 크지만, [vision.md](../../product/vision.md#다른-서비스에서-차용하는-개념)가 명시한 QPR(Question Pull Request) 컨셉과 domain-model.md의 이벤트 체인에 더 부합한다고 판단해 이 쪽을 선택했다. (이 결정은 사용자에게 두 옵션을 제시하고 확인받았다.)

## 결과 (Consequences)

- `Question`의 상태(OPEN/NEEDS_INFO/UPDATED/RESOLVED)는 그대로 유지하되, 실제 "누가 무엇을 요청했는지"는 `ReviewRequest` 목록에서 파생된다 — 열려있는 요청이 하나라도 있으면 NEEDS_INFO, 마지막 요청까지 ADDRESSED되면 UPDATED로 복귀하는 규칙을 `Question.requestMoreInfo()`/`reviewAddressed()` 도메인 메서드로 캡슐화한다(PLAN.md 5.2, 5.3).
- 재요청(`re-request`)은 요청 시점(`questionVersionNumberAtRequest`) 이후 실제 리비전이 있어야 허용한다 — 작성자가 리비전 없이 요청을 그냥 닫아버리는 것을 막기 위한 불변식이다.
- Aggregate가 하나 늘고 API가 3~4개 늘어나는 비용을 감수했다 — 이는 "질문 간 연결과 협업 이력을 밀도 있게 남기는 것이 Quno의 moat"라는 vision.md 원칙에 맞춰 의도적으로 받아들인 트레이드오프다.

## 관련 문서

- [vision.md](../../product/vision.md#다른-서비스에서-차용하는-개념), [domain-model.md](../domain-model.md#qpr-이벤트-체인-phase-2)
- [PLAN.md](../../../PLAN.md) Phase 5 (5.1~5.6)
