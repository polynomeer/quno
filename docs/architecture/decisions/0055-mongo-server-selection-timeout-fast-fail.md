# ADR-0055: MongoDB 드라이버 serverSelectionTimeout을 3초로 단축해 빠르게 실패하도록 한다

- 날짜: 2026-09-23
- 상태: 승인됨

## 배경 (Context)

[failure-scenario-testing.md](../../operations/failure-scenario-testing.md) A3(MongoDB 장애) 시나리오를 실행하다가 [ADR-0054](0054-hikari-connection-timeout-fast-fail.md)(HikariCP)와 같은 종류의 문제를 MongoDB 드라이버에서도 발견했다.

로컬 27017 포트가 다른 프로젝트 컨테이너와 상시 충돌해 Quno 자체 Mongo를 항상 검증하기 어려운 환경이라, 이번 테스트에서는 임시로 Mongo를 27018 포트에 별도로 띄워 "정상 동작 확인 → 정지 → 재검증"의 순서를 온전히 재현했다.

Mongo가 정상 기동된 상태에서 Live Chat 메시지 조회(`GET /api/v1/live-chat/{roomId}/messages`)는 즉시(수십 ms) 응답했다. 이 상태에서 Mongo를 정지시키자:

- `/actuator/health`가 **30초**가 지나서야 `mongo: DOWN`을 보고했다.
- `GET /api/v1/live-chat/{roomId}/messages`도 **30초**가 지나서야 `HTTP:500`(`INTERNAL_ERROR`)을 반환했다.

두 지연 시간이 거의 동일했고, 원인은 MongoDB Java 드라이버의 `serverSelectionTimeoutMS` 기본값(30000ms)이었다. `application.yml`/`application-local.yml`/`application-prod.yml` 어디에도 이를 낮추는 설정이 없었고, MongoDB는 Spring Boot의 `spring.mongodb.*` 프로퍼티(호스트/포트/DB명)만으로 연결 설정을 하고 있어 커스텀 `MongoClientSettingsBuilderCustomizer` 빈이 없으면 드라이버 기본값이 그대로 쓰인다.

다른 기능(질문 상세, 태그 목록, 검색)은 Mongo가 죽은 동안에도 전부 정상(200, 수십 ms 이내)이었다 — 런북 2.2절의 "Mongo 장애는 Live Chat에만 영향"이라는 주장 자체는 맞았지만, "그 영향받는 범위 안에서 얼마나 빨리 실패하는가"는 검증되지 않은 채로 30초 지연이라는 별도의 문제가 숨어 있었다.

## 결정 (Decision)

`infrastructure/config/MongoConfig.kt`에 `MongoClientSettingsBuilderCustomizer` 빈을 새로 추가해 `ClusterSettings.serverSelectionTimeout`을 3초로 낮춘다. Spring Boot 4의 `spring.mongodb.*` 프로퍼티에는 이 값을 직접 노출하는 항목이 없어(ADR-0036에서 확인한 prefix 변경 이후에도 타임아웃류는 여전히 프로퍼티로 없음), 프로퍼티가 아니라 코드 레벨 커스터마이저로 처리했다 — Spring Boot가 공식적으로 열어둔 확장 지점(`MongoClientSettingsBuilderCustomizer`)을 그대로 쓰므로 자동 설정을 깨지 않는다.

3초라는 값은 ADR-0054와 동일한 근거(정상 상황에서는 절대 걸리지 않지만, 장애 상황에서는 오케스트레이터 헬스체크 probe보다 먼저 실패를 보고할 수 있는 값)로 통일했다 — 두 인프라 의존성(PostgreSQL/MongoDB)의 장애 감지 속도를 일관되게 맞추는 것이 유지보수 관점에서 더 낫다고 판단했다.

## 결과 (Consequences)

- Mongo 장애 시 헬스체크와 Live Chat 메시지 조회가 30초에서 3초로 단축됐다(검증: 동일한 방법 재현으로 `/actuator/health HTTP:503 TIME:3.36s`, `GET .../messages HTTP:500 TIME:3.24s` 확인).
- 이 타임아웃은 클러스터 전체에 적용되므로, Live Chat이 쓰는 다른 Mongo 오퍼레이션(메시지 저장 등)에도 동일하게 3초 안에 실패한다 — 별도로 각 오퍼레이션을 검증하지는 않았으나 같은 `MongoClient`/`ClusterSettings`를 공유하므로 동일하게 적용된다.
- Mongo가 일시적으로만 느려서 3초 넘게 서버 선택이 걸리는 경우도 실패로 처리된다 — ADR-0054와 같은 트레이드오프.

## 관련 문서

- [failure-scenario-testing.md](../../operations/failure-scenario-testing.md) A3
- [0054-hikari-connection-timeout-fast-fail.md](0054-hikari-connection-timeout-fast-fail.md) — 같은 문제의 PostgreSQL/HikariCP 버전
- [0036-live-chat-websocket-mongodb-redis-presence.md](0036-live-chat-websocket-mongodb-redis-presence.md) — `spring.mongodb.*` prefix 변경을 먼저 발견한 ADR
