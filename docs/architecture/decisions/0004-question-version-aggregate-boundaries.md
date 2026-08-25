# ADR-0004: Question/QuestionVersion 분리, Answer는 독립 Aggregate, 리비전은 append-only

- 날짜: 2026-08-24
- 상태: 승인됨

## 배경 (Context)

Quno의 핵심 철학은 질문을 "죽은 게시물"이 아니라 리비전 가능한 Living Question Card로 다루는 것이다([vision.md](../../product/vision.md)). 이를 데이터 모델로 옮기려면: 질문 콘텐츠를 수정할 때 기존 데이터를 덮어쓸지 이력을 남길지, 답변을 Question의 하위 컬렉션으로 둘지 별도 Aggregate로 둘지를 정해야 했다.

## 결정 (Decision)

- **Question과 QuestionVersion을 분리**한다. `Question`은 식별자·작성자·상태·`latest_version_id`·`accepted_answer_id`만 갖는 얇은 Aggregate Root이고, 실제 제목/본문/환경/로그는 `QuestionVersion`에 저장한다.
- 리비전은 **append-only**다 — 기존 `QuestionVersion` row를 UPDATE하지 않고 `version_number`를 증가시켜 새 row를 추가하고 `Question.latest_version_id`만 갱신한다. `(question_id, version_number)`에 unique 제약을 건다.
- `QuestionVersion` 전체를 Question Aggregate 안에 JPA 컬렉션으로 항상 로딩하지 않는다 — 리비전이 쌓여도 Question 조회 비용이 커지지 않게 하기 위함.
- **Answer는 Question과 별도의 Aggregate**로 둔다. 질문 하나에 답변이 아무리 많아져도 Question Aggregate가 비대해지지 않는다.

## 결과 (Consequences)

- 리비전 이력이 항상 보존되므로 diff(`TextDiffer`, LCS 기반)와 "답변이 어느 버전을 대상으로 했는지" 같은 후속 기능(ADR-0012 관련, PLAN.md 5.1)이 자연스럽게 가능하다.
- Question과 QuestionVersion을 같은 트랜잭션에서 만들어야 하므로, 리비전 생성 경로에 동시성 방어가 필요해졌다 — 이는 ADR-0005에서 별도로 다룬다.
- Answer가 별도 Aggregate이기 때문에 "채택 답변은 반드시 해당 질문에 속한 활성 답변이어야 한다"는 불변식은 DB 제약이 아니라 애플리케이션 계층(`AcceptAnswerUseCase`)에서 명시적으로 검증해야 한다.
- 리비전을 UPDATE가 아닌 append로 처리하기 때문에 저장 공간은 계속 늘어난다. MVP 단계에서는 문제 삼지 않지만, 리비전이 극단적으로 많은 질문이 실제로 나타나면 재검토한다.

## 관련 문서

- [system-architecture.md](../system-architecture.md#question-aggregate-설계-원칙)
- [domain-model.md](../domain-model.md#aggregate)
- [PLAN.md](../../../PLAN.md) Phase 2.2, 2.3
- 커밋 `0b35e77` (Question 생성/조회), `04306a1` (Question Revision/Diff)
