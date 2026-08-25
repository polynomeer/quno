# ADR-0006: 비동기 이벤트 처리는 Transactional Outbox + in-process 스케줄러로, 별도 메시징 인프라는 아직 도입하지 않음

- 날짜: 2026-08-24
- 상태: 승인됨

## 배경 (Context)

리비전/답변/채택이 일어나면 Ward 알림 fan-out과(향후) 검색 재색인 같은 부수효과가 필요하다. DB 트랜잭션 커밋과 메시지 발행을 단순히 연속 호출하면 "커밋은 성공했는데 발행은 실패"하는 dual-write 문제가 생긴다. Kafka/RabbitMQ 같은 내구성 있는 메시징을 바로 도입할지, 더 가벼운 방식으로 시작할지 결정해야 했다.

## 결정 (Decision)

Transactional Outbox 패턴을 쓰되, MVP 단계에서는 **Kafka/Redis pub-sub 없이 in-process 스케줄러**로 구현한다.

- `outbox_events` 테이블(V2 마이그레이션)에 `event_type`/`aggregate_type`/`aggregate_id`/`payload`/`published_at`을 기록한다.
- `ReviseQuestionUseCase`/`WriteAnswerUseCase`/`AcceptAnswerUseCase`가 도메인 변경과 **같은 트랜잭션**에서 이벤트 row를 기록한다(`domain/common/OutboxEvent`).
- `OutboxDispatchScheduler`가 `@Scheduled(fixedDelay = 2000)`로 2초마다 미발행 이벤트를 폴링해 `DispatchOutboxEventsUseCase`(Watch 기반 fan-out)를 실행하고 `published_at`을 채운다.

## 결과 (Consequences)

- dual-write 문제는 해결된다 — 이벤트 기록이 원본 트랜잭션에 포함되므로 "도메인 변경은 커밋됐는데 이벤트는 유실"되는 경우가 없다.
- 별도 메시지 브로커 없이 단일 애플리케이션 인스턴스로 동작하므로 배포/운영이 단순하다. 대신 알림은 최대 2초 지연되는 결과적 일관성(eventual consistency)을 갖는다 — 이 지연은 E2E 테스트(ADR-0011)와 데모 스크립트에서 실제로 관찰되어, 알림 확인 전에 짧은 대기 또는 `DispatchOutboxEventsUseCase` 직접 호출이 필요했다.
- 애플리케이션 인스턴스가 여러 대로 늘어나면 스케줄러가 같은 이벤트를 중복 처리할 수 있다 — 지금은 인스턴스가 하나뿐이라 문제가 없지만, 다중 인스턴스 배포 시점에 분산 락 또는 컨슈머 그룹(Kafka) 도입을 재검토해야 한다.
- `system-architecture.md`의 확장 시점 기준대로, "이벤트 유실 방지가 실제로 중요해지는 시점"에 Kafka/RabbitMQ/SQS로 교체하는 것을 이미 예정된 경로로 남겨둔다 — 이 ADR은 그 전환을 막는 것이 아니라 "아직은 필요하지 않다"는 판단만 기록한다.

## 관련 문서

- [system-architecture.md](../system-architecture.md#비동기-이벤트-처리--transactional-outbox)
- [domain-model.md](../domain-model.md#domain-events)
- [PLAN.md](../../../PLAN.md) Phase 2.7, 2.8
- 커밋 `cf7994b` (Outbox 이벤트 발행 골격), `27b45a3` (Notification fan-out과 소비 워커)
