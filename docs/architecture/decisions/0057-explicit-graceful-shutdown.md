# ADR-0057: `server.shutdown: graceful`을 명시적으로 고정한다

- 날짜: 2026-09-23
- 상태: 승인됨

## 배경 (Context)

[failure-scenario-testing.md](../../operations/failure-scenario-testing.md) E1(요청 처리 중 `SIGTERM`) 시나리오를 실행하며 확인했다. `questions` 테이블에 8초짜리 락을 걸어 응답을 지연시킨 뒤, 그 요청이 진행 중인 상태에서 백엔드 프로세스에 `kill -TERM`을 보냈다.

기대와 달리(이 저장소의 `application.yml`/`application-{local,prod}.yml` 어디에도 `server.shutdown` 설정이 없었다), 결과는 이미 올바르게 동작하고 있었다: 로그에 `Commencing graceful shutdown. Waiting for active requests to complete`가 찍혔고, 진행 중이던 요청은 끊기지 않고 락이 풀리는 시점(7.03초 후)에 정상적으로 `HTTP:200`을 반환했다. 그 뒤에야 `Graceful shutdown complete`로 프로세스가 종료됐다.

즉 이 프로젝트는 설정한 적 없는 값에 의존해 우연히 안전하게 동작하고 있었다 — 어떤 자동 설정이나 기본값이 이걸 제공하는지 명확히 추적하지 않았고, 향후 Spring Boot 버전을 올리다 이 기본값이 바뀌면(예: 다시 immediate로) 아무도 눈치채지 못한 채 배포 시마다 처리 중이던 요청이 강제로 끊기는 회귀가 생길 수 있다. 이는 ADR-0036(MongoDB prefix 변경)·ADR-0045(Sentry Spring 통합 비호환)와 같은 종류의 "프레임워크 버전에 암묵적으로 의존하던 동작"이다.

## 결정 (Decision)

`application.yml`(모든 프로필 공통)에 `server.shutdown: graceful`과 `spring.lifecycle.timeout-per-shutdown-phase: 30s`를 명시적으로 추가한다. 동작 자체는 이미 검증된 대로 바뀌지 않지만, 이제부터는 "왜 되는지 아무도 모르는 우연"이 아니라 "명시적으로 선택하고 코드에 고정한 동작"이 된다. 30초는 Spring Boot의 `timeout-per-shutdown-phase` 기본값과 같은 값을 그대로 명시한 것으로, 이번에 별도로 조정하지는 않았다 — 다만 앞으로 프레임워크 기본값이 바뀌어도 이 값만은 고정된다.

## 결과 (Consequences)

- 배포/재기동 시 처리 중인 요청이 안전하게 끝까지 완료된 뒤 프로세스가 종료된다는 보장이 설정 파일에 명시적으로 남는다.
- 처리 시간이 `timeout-per-shutdown-phase`(30초)를 넘는 요청이 있다면, graceful 종료 유예 시간이 끝난 뒤 강제로 종료될 수 있다 — 이번 테스트 범위에서는 30초를 넘는 요청을 만들어 검증하지 않았다.
- 앞으로 Spring Boot를 업그레이드할 때 이 값들을 지우거나 바꾸지 않는 한, 이 동작은 프레임워크 기본값 변경과 무관하게 유지된다.

## 관련 문서

- [failure-scenario-testing.md](../../operations/failure-scenario-testing.md) E1
- [0036-live-chat-websocket-mongodb-redis-presence.md](0036-live-chat-websocket-mongodb-redis-presence.md), [0045-observability-logging-metrics-error-tracking.md](0045-observability-logging-metrics-error-tracking.md) — 같은 종류의 프레임워크 버전 암묵 의존성을 먼저 발견한 사례들
