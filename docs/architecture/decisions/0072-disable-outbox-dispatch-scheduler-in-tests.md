# ADR-0072: 테스트에서는 Outbox 디스패치 스케줄러를 끈다

- 날짜: 2026-10-22
- 상태: 승인됨

## 배경 (Context)

`QuestionClusterLifecycleE2ETest`가 전체 스위트에서 이따금 `@AfterEach`의 `DELETE FROM users`에서 `notifications_user_id_fkey` 위반으로 실패했다. 2026-09-01에 사용자를 지우기 직전 `user_id` 기준으로 알림을 한 번 더 지우는 정리를 넣었지만([technical-deep-dives.md 1절](../../engineering/technical-deep-dives.md#1-outbox-폴링-스케줄러가-테스트-정리cleanup와-경합한다)) 재발했다.

원인은 정리 SQL의 순서가 아니라 `OutboxDispatchScheduler`(2초 주기 `@Scheduled`)가 테스트와 다른 트랜잭션에서 돈다는 점이다.

- 디스패처 트랜잭션이 정리보다 먼저 outbox 이벤트를 읽고, 알림을 INSERT한 뒤 아직 커밋하지 않은 상태로 있으면 정리의 `DELETE FROM notifications WHERE user_id = ?`에는 보이지 않는다(READ COMMITTED).
- 그 INSERT는 FK 검사로 `users` row에 `FOR KEY SHARE` 잠금을 잡는다. 이어지는 `DELETE FROM users`는 그 잠금에서 기다렸다가, 디스패처가 커밋하는 순간 FK 위반으로 실패한다.
- 스프링 테스트 컨텍스트 캐시 때문에 설정이 다른 컨텍스트마다 스케줄러가 하나씩 살아 있다. 다른 테스트 클래스의 컨텍스트가 이 테스트가 만든 이벤트를 소비할 수도 있다.

정리 쿼리를 아무리 다시 배치해도 "다른 트랜잭션이 커밋 직전에 있다"는 창은 닫히지 않는다.

## 결정 (Decision)

- `OutboxDispatchScheduler`에 `@ConditionalOnProperty(prefix = "quno.outbox.dispatch-scheduler", name = ["enabled"], havingValue = "true", matchIfMissing = true)`를 붙인다. 운영·로컬 실행은 기본값 그대로 켜져 있다.
- Gradle `test` 태스크가 `quno.outbox.dispatch-scheduler.enabled=false`를 시스템 프로퍼티로 넘긴다(이미 `spring.profiles.active=local`을 넘기는 곳). 그러면 모든 `@SpringBootTest` 컨텍스트에서 빈이 사라진다.
- 알림이 필요한 통합 테스트는 지금처럼 `DispatchOutboxEventsUseCase.execute()`를 직접 호출한다. 디스패치는 테스트 스레드 안에서, 정리보다 먼저 끝난다.
- `user_id` 기준 알림 정리는 남겨 두고(사용자에게 직접 가는 알림이 있음), 빠져 있던 `PublicReadAccessE2ETest`에도 추가한다.

### 기각한 대안

- **정리에서 디스패처를 기다리거나 재시도한다**: 진행 중인 디스패처 트랜잭션을 테스트에서 알아낼 방법이 없다. 재시도는 원인을 숨긴다.
- **`DELETE FROM notifications`와 `DELETE FROM users`를 한 트랜잭션으로 묶는다**: 다른 트랜잭션의 미커밋 INSERT는 여전히 보이지 않으므로 창이 그대로다.
- **`src/test/resources/config/application.yml`로 끈다**: IDE 실행에도 적용되는 장점은 있지만 테스트 설정이 Gradle과 리소스 두 곳으로 흩어진다. 같은 성격의 `spring.profiles.active`가 이미 Gradle에 있어 그쪽에 맞췄다.

## 결과 (Consequences)

- 테스트가 만드는 알림은 테스트가 직접 디스패치한 것뿐이다. 명시적 디스패치와 백그라운드 디스패치가 같은 이벤트를 동시에 처리해 알림이 중복되던 경합도 함께 사라진다.
- 스케줄러 배선(2초 주기로 실제로 도는지)은 통합 테스트가 더 이상 간접적으로 검증하지 않는다. 디스패치 로직 자체는 `DispatchOutboxEventsUseCaseTest`와 E2E 테스트의 명시적 호출이 검증한다.
- IDE에서 Gradle 러너가 아닌 JUnit 러너로 실행하면 이 프로퍼티가 넘어가지 않아 예전 경합이 다시 가능하다. `spring.profiles.active=local`도 같은 조건이라 새로 생긴 제약은 아니다.

## 관련 문서

- [technical-deep-dives.md 1절](../../engineering/technical-deep-dives.md#1-outbox-폴링-스케줄러가-테스트-정리cleanup와-경합한다)
- [system-architecture.md — Transactional Outbox](../system-architecture.md#비동기-이벤트-처리--transactional-outbox)
