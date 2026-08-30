# ADR-0025: Save(북마크)는 Watch와 구조는 같지만 별도 테이블·독립 side-aggregate로 분리한다

- 날짜: 2026-08-30
- 상태: 승인됨

## 배경 (Context)

[design.md #18](../../frontend/design.md)은 Watch(질문 변화 구독)와 Save(나중에 다시 읽기 위한 개인 보관)를 의도적으로 분리된 개념으로 설명한다 — "하나의 북마크 기능으로 합치면 알림 기대가 모호해진다"는 이유다. 백엔드에는 지금 Watch만 있다([ADR-0020](0020-frontend-scoped-to-backend-support.md)에서 확인한 격차 중 하나). 데이터 모양만 보면 Save는 `watches` 테이블과 완전히 동일하다 — `(user_id, question_id)` 복합키, hard delete, idempotent toggle. 이 유사성 때문에 "Watch 테이블에 `type` 판별 컬럼을 추가해 재사용할지, 아니면 완전히 별도로 만들지"를 정해야 했다.

## 결정 (Decision)

**별도로 만든다.** 새 `domain/save` 패키지에 `SaveRepository` 포트(`save`/`unsave`/`isSaved`/`findSavedQuestionIds` — Watch의 메서드 시그니처를 그대로 미러링)와 별도 `saves` 테이블을 둔다. `Watch`처럼 상태를 가진 별도 클래스가 필요 없는 순수 관계 데이터라 Aggregate 클래스 없이 리포지토리 포트만으로 표현한다(Watch와 동일).

Watch 테이블에 discriminator 컬럼(`kind: WATCH | SAVE`)을 얹어 하나로 합치는 대안을 검토했지만 기각했다:

- Watch는 `findWatcherIds(questionId)`(알림 fan-out 대상 조회, [domain-model.md](../domain-model.md) Watch 사용자 알림 fan-out 참고)가 필요하지만 Save는 "누가 이 질문을 저장했는지" 조회할 이유가 전혀 없다 — 두 개념의 쿼리 패턴이 이미 다르다.
- 지금은 5~6개 메서드가 거의 동일해 보여도, Watch는 향후 알림 정책이 늘어날 가능성이 있고(예: Ward 알림 세분화) Save는 순수 개인 보관이라 앞으로도 변하지 않을 가능성이 높다 — 방향이 다른 두 개념을 하나의 테이블에 우겨넣으면 나중에 풀어내는 비용이 지금 아끼는 코드량보다 크다.

자기 자신의 질문을 저장하는 것은 막지 않는다(Vote와 다르게 `SelfSaveException` 같은 건 없음) — 본인 질문을 나중에 다시 찾기 위해 저장하는 것은 자연스러운 행동이다.

## 결과 (Consequences)

- `WatchRepositoryAdapter`/`WatchJpaEntity`/`WatchJpaRepository`를 파일 단위로 거의 그대로 복사해 `Save`용으로 만든다 — 중복 코드가 존재하지만, 두 개념이 독립적으로 진화할 수 있다는 이득이 이 정도의 중복 비용보다 크다고 판단했다.
- Bounded Context 배치(Engagement에 편입할지, 별도로 둘지)는 착수 시점에 확정한다 — Watch/Notification과 성격이 가장 가깝다는 점에서 Engagement 편입이 유력하지만, 이 ADR은 그 배치까지 확정하지 않는다.
- Save에는 알림이 없다 — "저장한 질문이 바뀌었다"는 알림을 원한다면 그건 Watch의 역할이지 Save의 역할이 아니다(design.md의 원래 구분을 그대로 지킴).

## 관련 문서

- [design.md #18 Watch·북마크·팔로우](../../frontend/design.md)
- [ADR-0020](0020-frontend-scoped-to-backend-support.md) (Save가 없다고 확인한 원 갭 분석)
- [PLAN.md](../../../PLAN.md) Phase 13
