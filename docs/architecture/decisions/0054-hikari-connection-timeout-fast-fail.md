# ADR-0054: HikariCP connection-timeout을 3초로 단축해 DB 장애 시 빠르게 실패하도록 한다

- 날짜: 2026-09-23
- 상태: 승인됨

## 배경 (Context)

[failure-scenario-testing.md](../../operations/failure-scenario-testing.md) A2(정상 기동 중 PostgreSQL이 갑자기 정지됐을 때의 동작) 시나리오를 실행하다가 발견했다. `backend/src/main/resources/application.yml`과 `application-local.yml`, `application-prod.yml` 어디에도 `spring.datasource.hikari.*` 설정이 없어 HikariCP 기본값(`connectionTimeout: 30000ms`)이 모든 프로필에 그대로 적용되고 있었다.

PostgreSQL을 `docker compose stop postgres`로 정지시킨 채로:

- `/actuator/health`를 호출하면 **30초**가 지나서야 `db: DOWN`(`CannotGetJdbcConnectionException`) 응답이 왔다(`HTTP:503`, `TIME:30.035s`).
- DB에 의존하는 일반 API(`GET /api/v1/tags`)를 호출하면 마찬가지로 **30초**가 지나서야 `HTTP:500`(`{"code":"INTERNAL_ERROR","message":"예기치 못한 오류가 발생했습니다."}`)이 왔다.

두 응답 시간이 30.035s로 사실상 동일했는데, 이는 우연이 아니라 HikariCP가 커넥션 풀에서 연결을 얻으려고 기본 타임아웃(30초)만큼 대기하다가 포기하는 동일한 원인 때문임을 뜻한다. 이 상태로 실서비스에 배포하면:

- 오케스트레이터(K8s 등)의 liveness/readiness probe는 보통 타임아웃이 수 초 단위라, probe 자체가 30초짜리 헬스체크 응답을 기다리지 못하고 먼저 타임아웃돼 "응답 없음"으로 처리된다 — 프로세스가 실제로는 살아있는데도 강제 재시작될 위험이 있고, DB가 DOWN이라는 정확한 원인이 오케스트레이터에 전달되지 않는다.
- 사용자 요청은 DB 장애 시마다 30초씩 붙잡혀 있다가 실패한다 — Tomcat 스레드가 오래 점유되어 정상 요청까지 처리 지연/거부로 번질 위험이 커진다(스레드 고갈).
- 에러 메시지(`예기치 못한 오류가 발생했습니다`)가 30초 뒤에야 오므로, 클라이언트 입장에서 "느린 것"과 "장애난 것"을 구분하기 더 어렵다.

## 결정 (Decision)

`backend/src/main/resources/application.yml`(모든 프로필 공통)에 `spring.datasource.hikari.connection-timeout: 3000`(3초)을 추가한다. 로컬/운영을 분리하지 않고 공통 설정으로 둔 이유는, 이 값이 "커넥션 풀에서 연결을 못 얻을 때 얼마나 기다릴지"를 결정하는 값이라 환경별로 달라질 이유가 없고(운영 DB가 로컬보다 응답이 특별히 느릴 이유가 없음), 오히려 실서비스에서 빠른 실패가 더 중요하기 때문이다.

3초를 고른 근거: 정상적인 커넥션 획득은 보통 수 ms~수십 ms 안에 끝나므로, 3초는 "정상 상황에서는 절대 걸리지 않지만 장애 상황에서는 충분히 빨리 포기하는" 값이다. 헬스체크 probe의 일반적인 타임아웃(5~10초)보다도 짧게 잡아, probe가 먼저 타임아웃되기 전에 애플리케이션이 스스로 `DOWN`을 보고할 수 있게 했다.

풀 크기(`maximum-pool-size` 등 나머지 HikariCP 옵션)는 이번 결정 범위 밖이다 — 기본값(10)을 유지하며, 이는 B1(풀 고갈) 시나리오에서 별도로 다룬다.

## 결과 (Consequences)

- DB 장애 시 헬스체크와 API 응답이 30초에서 3초로 단축됐다(검증: 동일한 `docker compose stop postgres` 재현으로 `HTTP:503 TIME:3.038s`, `HTTP:500 TIME:3.113s` 확인).
- DB가 정말 일시적으로만 느려서 3초 넘게 걸리는 정상 커넥션 획득이 있다면(예: DB 서버가 순간적으로 과부하) 이 요청도 실패로 처리된다 — 장애와 "일시적으로 느림"을 3초 기준으로 나누는 트레이드오프이며, 이 경계값이 실무에서 너무 짧다고 판단되면(예: 오탐 재시도 급증) 재검토한다.
- 풀 고갈(B1) 시나리오는 이 타임아웃 변경 이후에 재검증이 필요하다 — 대기 중인 요청이 실패로 전환되는 속도 자체가 빨라졌으므로 B1 결과 해석 시 이 점을 반영한다.

## 관련 문서

- [failure-scenario-testing.md](../../operations/failure-scenario-testing.md) A2, B1
- [runbook.md](../../operations/runbook.md) 컴포넌트별 흔한 원인
- [system-architecture.md](../system-architecture.md)
