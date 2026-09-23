# 장애 시나리오 테스트 — 기획

[운영 런북](runbook.md)의 "컴포넌트별 흔한 원인" 표(2.2절)는 "Mongo가 죽으면 Live Chat만 영향받고 다른 기능은 정상이다" 같은 **주장**을 담고 있다. 이 문서는 그 주장들을 실제로 장애를 주입해 검증하고, 검증 과정에서 드러나는 실제 버그(런북의 주장과 실제 동작이 다른 지점)를 찾아 고치기 위한 계획이다.

## 목적과 원칙

1. **런북의 가정을 검증한다.** "이러면 이렇게 될 것이다"라고 문서에 적힌 것을 실제로 그렇게 만들어보고 확인한다 — 확인되지 않은 가정은 장애 상황에서 오히려 대응을 늦춘다.
2. **버그를 찾으면 그 자리에서 고친다.** 이 프로젝트 전체의 관행(검증하다가 발견한 버그는 별도 Phase로 미루지 않고 바로 고친다)을 그대로 따른다. 고친 것은 [technical-deep-dives.md](../engineering/technical-deep-dives.md)에 새 항목으로, 정책으로 남을 결정이면 ADR로 남긴다.
3. **로컬 Docker Compose 환경의 한계를 인정한다.** 이 프로젝트는 아직 실제 배포 대상이 없다([production-readiness.md](../product/production-readiness.md) A 항목). 그래서 다중 인스턴스 페일오버, 로드밸런서 헬스체크 제외, 실제 네트워크 파티션 같은 시나리오는 **범위 밖**이다 — 단일 인스턴스가 의존 컴포넌트 하나를 잃었을 때 스스로 어떻게 행동하는지까지만 다룬다.
4. **측정 가능한 것만 판정한다.** "아마 이럴 것이다"가 아니라 실제 HTTP 응답 코드, 로그, 화면 상태로 PASS/FAIL을 가른다.

## 실행 방법론

| 목적 | 도구 | 비고 |
|---|---|---|
| 컴포넌트 완전 정지 | `docker compose stop <service>` / `start` | Postgres/Mongo/Redis/Mailpit 각각 개별 정지 가능 |
| 컴포넌트 "먹통"(응답 지연/무응답이지만 연결은 유지) | `docker pause <container>` / `unpause` | 완전 정지와 다른 실패 모드 — TCP 연결은 유지된 채 아무 응답도 안 옴(타임아웃 로직이 실제로 있는지 가려낼 수 있음) |
| DB 커넥션 풀 점유/지연 | `pg_sleep(n)`을 거는 별도 세션을 `psql`로 다수 열어 HikariCP 풀 소진 재현 | 별도 도구 설치 없이 재현 가능 |
| 외부 API(Toss 결제 확인) 실패/지연 | 로컬 Python HTTP 목 서버(`quno.toss.api-base-url` 임시 오버라이드) | [ADR-0037](../architecture/decisions/0037-paid-direct-ask-toss-payments-test-mode.md)에서 이미 쓴 방식 재사용 — 진짜 Toss 서버에는 절대 요청하지 않음 |
| 부하/동시 요청 | k6(`scripts/load-test.js` 확장) 또는 셸 `curl` 병렬 실행 | k6는 이미 로컬에 설치돼 있음 |
| 프로세스 이상 종료 | `kill -TERM` / `kill -9` (PID) | graceful shutdown vs 강제 종료 구분 |
| 토큰 조작 | 만료/변조된 JWT를 curl 헤더로 직접 전송 | 서명 검증 로직 경로를 강제로 태움 |

**의도적으로 안 쓰는 것**: `pumba`/`toxiproxy`/`tc` 같은 네트워크 계층 카오스 도구는 로컬에 설치돼 있지 않고, macOS에서 패킷 수준 지연을 걸려면 `pfctl` 같은 시스템 방화벽을 건드려야 해서 이번 범위에서는 위험 대비 이득이 낮다고 판단해 제외한다. `docker pause`(응답 없음)와 `pg_sleep`(느린 쿼리)만으로 "완전 다운"과 "그냥 느림"이라는 두 가지 실패 모드를 충분히 재현할 수 있다.

## 시나리오 카테고리

### A. 인프라 의존성 완전 장애

런북 2.2절이 "이것만 영향받고 나머지는 정상"이라고 주장하는 부분을 정면으로 검증한다.

- [x] A1. PostgreSQL 정지 상태에서 백엔드를 새로 기동 — 기동 자체가 실패하는지, 실패 메시지가 원인을 알 수 있게 나오는지
- [x] A2. 정상 기동 후 PostgreSQL을 정지 — 이미 처리 중이던 요청과 새 요청 모두 어떤 응답(500? 503? 타임아웃?)을 받는지, `/actuator/health`가 즉시 `DOWN`으로 바뀌는지
- [x] A3. MongoDB 정지 — 런북 주장("Live Chat 메시지 저장만 영향")대로 질문 작성/답변/검색/투표 등 Mongo를 안 쓰는 기능이 실제로 전부 정상인지, Live Chat 메시지 전송 시도는 어떤 에러가 나는지
- [x] A4. Redis 정지 — 런북 주장("Live Chat 접속자 표시·인증/일반 API는 영향 없음")을 검증. 추가로 Rate Limiting(Bucket4j, 인메모리)과 조직 검색 캐시(Redis cache-aside)가 실제로 Redis 장애와 무관하게 동작하는지 — 특히 캐시 read가 Redis 장애 시 예외를 던지는지 DB로 우회하는지 확인(코드 리뷰로는 아직 확인 안 됨)
- [x] A5. Mailpit(SMTP) 정지 — Verified Organization 이메일 인증 요청 시 재시도 로직(ADR-0046)이 실제로 3회 재시도하는지, 최종 실패 시 사용자에게 어떤 에러 메시지가 가는지

### B. 지연과 타임아웃

완전히 죽는 것보다 "느려지는 것"이 실제 운영에서는 더 흔하고 더 위험하다 — 타임아웃이 없으면 스레드 풀이 서서히 고갈된다.

- [x] B1. `psql`로 다수의 `pg_sleep(60)` 세션을 열어 HikariCP 커넥션 풀(기본 10개, 명시적 설정 없음을 확인함)을 고갈시켰을 때, 풀을 못 받은 요청이 무한 대기하는지 타임아웃 후 에러를 반환하는지
- [x] B2. `docker pause postgres`(정지가 아니라 응답 없음)로 "연결은 됐는데 응답이 없는" 상태를 만들어 A2와 다른 실패 모드로 재현
- [x] B3. 로컬 Toss 목 서버가 확인 요청에 응답하지 않도록(sleep) 만들어 `TossPaymentGateway`의 타임아웃 설정이 실제로 있는지, 없다면 어떻게 되는지
- [x] B4. Mailpit을 `docker pause`해 `application-local.yml`의 `connectiontimeout`/`timeout`/`writetimeout`(5000ms)이 실제로 5초 뒤에 발동하는지 시간을 재서 확인

### C. 부하와 레이트 리미팅

- [x] C1. 로그인 엔드포인트에 짧은 시간 안에 반복 요청을 보내 Rate Limiting(prod 기준 분당 10회)이 로컬 프로필에서는 사실상 무제한임을 확인 → prod 프로필로 임시 기동해 실제로 429가 11번째부터 나오는지, 1분 경과 후 다시 허용되는지
- [x] C2. k6로 순간적으로 동시 요청 스파이크(예: VUs 50, 5초)를 짧게 걸어 응답 지연·에러율이 [production-readiness.md](../product/production-readiness.md) B-5가 검증한 정상 부하(VUs=3)와 어떻게 달라지는지
- [x] C3. 같은 질문에 대한 동시 리비전 요청을 실제 HTTP 레벨(curl 병렬 실행 또는 k6)로 폭주시켜, 단위 테스트(`ReviseQuestionConcurrencyIntegrationTest`)가 이미 검증한 락 방어가 실제 서버 스레드 풀 위에서도 동일하게 버전 번호 중복 없이 동작하는지

### D. 외부 의존성(결제) 실패

- [ ] D1. Toss 결제 확인 요청에 목 서버가 500을 반환하도록 만들어, `ConfirmDirectAskPaymentUseCase`가 사용자에게 어떤 응답을 주는지, `DirectAskPayment` 상태가 어중간하게 남는지
- [ ] D2. 결제 확인 도중(요청은 갔지만 응답 전) 백엔드를 강제 종료 → 재기동 후 그 결제/요청의 상태가 일관적인지(멱등하게 재시도 가능한지, 아니면 영구히 `AWAITING_PAYMENT`로 남는지 — 이미 ADR-0037이 "만료 처리 없음"을 알려진 단순화로 남겨뒀는데, 그 상태에서 실제로 뭐가 보이는지 확인)

### E. 프로세스 생명주기

- [x] E1. 진행 중인 요청(예: 느린 대시보드 집계 쿼리)이 있는 상태에서 `kill -TERM`(graceful shutdown 유도) — 그 요청이 정상 완료되는지 즉시 끊기는지
- [x] E2. `kill -9`(강제 종료) 직후 재기동 — Flyway가 다시 정상적으로 "마이그레이션 없음"을 확인하는지, 커밋 안 된 트랜잭션의 흔적(예: 잠긴 row)이 남아있는지
- [ ] E3. Outbox 이벤트가 쌓인 상태(리비전 여러 개를 빠르게 만들어 outbox_events에 미처리 row를 남김)에서 백엔드를 죽였다가 재기동 — 재기동 후 스케줄러가 밀린 이벤트를 마저 처리해 알림이 (지연됐을 뿐) 결국 전부 가는지, 유실되는 게 있는지

### F. 프론트엔드 회복력

- [x] F1. 백엔드를 완전히 내린 상태로 프론트엔드에 접속 — 홈/질문 상세/로그인 등 주요 페이지가 흰 화면이나 처리되지 않은 에러 대신 `error.tsx`/적절한 에러 상태를 보여주는지
- [x] F2. 백엔드는 떠 있지만 응답이 느린 상태(`docker pause postgres`로 DB 의존 엔드포인트만 느리게)에서, 로딩 스켈레톤이 계속 보이는지 아니면 일정 시간 후 타임아웃 에러로 전환되는지(프론트 http-client에 타임아웃이 있는지 코드로 확인 안 됐음 — 있는지부터 확인)
- [x] F3. 만료되거나 서명이 틀린 JWT를 `localStorage`에 수동으로 심어두고 페이지를 새로고침 — `useSession`이 401을 어떻게 처리하는지(자동 로그아웃? 무한 재시도? 조용히 실패?), `http-client.test.ts`가 이미 다루는 "401 재발급/재시도" 로직이 실제 브라우저에서도 그대로 동작하는지

### G. 데이터 복구

- [ ] G1. `scripts/db-backup.sh`로 백업 → 임의로 데이터 일부 삭제(테스트 데이터에 한정) → `scripts/db-restore.sh`로 복구 → 삭제 이전 상태로 정확히 돌아오는지(런북에 "마지막 리허설 2026-09-10"이라고 적혀있는 것을 재검증)

## 우선순위 제안

전부 한 번에 하지 않는다. 진단 가치가 크고 로컬에서 안전하게 반복 가능한 순서로 제안한다.

1. **A(인프라 완전 장애)** — 가장 기본적이고, 런북의 핵심 주장을 직접 검증한다.
2. **F(프론트엔드 회복력)** — A에서 백엔드를 내린 김에 자연스럽게 이어서 확인할 수 있다.
3. **B(지연/타임아웃)** — 완전 장애보다 진단이 까다롭고, A/F에서 쓴 환경을 재사용할 수 있다.
4. **C(부하/레이트리밋)** — 이미 있는 스크립트를 확장하는 수준이라 상대적으로 가볍다.
5. **E(프로세스 생명주기)** — 재현에 시간이 걸리지만(이벤트를 쌓아야 함) 확인 가치가 크다.
6. **D(결제 실패)** / **G(데이터 복구)** — 각각 결제·백업 스크립트라는 별도 준비가 필요해 마지막.

## 리포트 형식

각 시나리오는 실행 후 다음 형식으로 이 문서 하단(또는 별도 `failure-scenario-report.md`)에 기록한다.

```markdown
### A1. PostgreSQL 정지 상태에서 백엔드 기동

- **가설**: (런북/설계 문서가 주장하는 기대 동작)
- **주입 방법**: (정확한 명령어)
- **관찰**: (실제 응답/로그/화면, 스크린샷이나 로그 발췌 포함)
- **판정**: PASS / FAIL / PARTIAL / N/A(로컬 환경 제약)
- **발견 및 조치**: (버그를 찾았다면 무엇을 어떻게 고쳤는지, 안 고쳤다면 왜 — 후속 이슈로 남기는 경우 포함)
```

전체 시나리오를 마치면 문서 최상단에 요약표(카테고리별 PASS/FAIL 개수, 발견한 버그 목록과 수정 여부)를 추가해 실행 결과 전체를 한눈에 보게 한다 — 이것이 최종 "리포트"가 된다.

## 실행 결과

### A1. PostgreSQL 정지 상태에서 백엔드 기동

- **가설**: DB에 붙지 못하면 애플리케이션 기동이 빠르게 실패하고, 원인이 로그에서 바로 보여야 한다.
- **주입 방법**: `docker compose stop postgres`로 정지시킨 채 `./gradlew --no-daemon bootRun` 실행.
- **관찰**: 약 10초 만에 `BUILD FAILED`. 예외 체인이 `BeanCreationException(flywayInitializer)` → `FlywaySqlUnableToConnectToDbException` → `PSQLException: Connection to localhost:5442 refused` → `ConnectException`으로 정확히 이어져, 로그만 보고도 "Postgres에 못 붙어서 Flyway가 실패했다"를 바로 알 수 있었다.
- **판정**: PASS
- **발견 및 조치**: 버그 없음. Spring Boot의 기본 fail-fast 동작(Flyway → HikariCP 초기 연결 실패 시 컨텍스트 기동 중단)이 그대로 잘 작동한다.

### A2. 정상 기동 후 PostgreSQL 정지

- **가설**: 이미 떠 있는 상태에서 DB가 죽으면 `/actuator/health`가 곧바로 `db: DOWN`으로 바뀌고, DB 의존 API는 타임아웃 없이 명확한 에러(500/503)를 즉시 반환해야 한다.
- **주입 방법**: Postgres/백엔드를 정상 기동한 뒤 `docker compose stop postgres`, 이어서 `GET /actuator/health`와 `GET /api/v1/tags` 호출.
- **관찰**: **수정 전**에는 둘 다 정확히 30초 뒤에야 응답이 왔다 — `/actuator/health`가 `HTTP:503 TIME:30.035s`(`db.status: DOWN`, `CannotGetJdbcConnectionException`), `/api/v1/tags`가 `HTTP:500 TIME:30.035s`(`{"code":"INTERNAL_ERROR","message":"예기치 못한 오류가 발생했습니다."}`). 두 응답이 같은 30.035초라는 점에서 원인이 동일함(HikariCP `connectionTimeout` 기본값 30초)을 특정했다. `/actuator/health` 자체가 30초씩 걸리는 것은 오케스트레이터 헬스체크 probe의 일반적인 타임아웃(5~10초)보다 길어, probe가 먼저 타임아웃돼 "정상 프로세스인데 응답 없음"으로 오판될 위험이 있었다.
- **판정**: FAIL → 조치 후 PASS
- **발견 및 조치**: `spring.datasource.hikari.connection-timeout`이 어느 프로필에도 설정돼 있지 않아 HikariCP 기본값(30초)이 그대로 쓰이고 있었다. `backend/src/main/resources/application.yml`에 `connection-timeout: 3000`(3초)을 추가해 모든 프로필에 적용했다. **수정 후 동일한 방법으로 재현**한 결과 `/actuator/health`가 `HTTP:503 TIME:3.038s`, `/api/v1/tags`가 `HTTP:500 TIME:3.113s`로 단축됨을 확인했다. Postgres를 다시 살리자 별도 재시작 없이 `db.status`가 자동으로 `UP`으로 복귀함도 확인(HikariCP가 자체적으로 재연결). 정책 결정이라 [ADR-0054](../architecture/decisions/0054-hikari-connection-timeout-fast-fail.md)로 남겼다.

### A3. MongoDB 정지

- **가설**: 런북 2.2절 주장대로 Mongo 장애는 Live Chat 메시지 관련 기능에만 영향을 주고, 질문/답변/검색/투표 등 나머지 기능은 정상이어야 한다. 영향받는 범위 안에서는 명확한 에러가 빠르게 와야 한다.
- **주입 방법**: 로컬 27017 포트가 이 머신에서 상시 다른 프로젝트 컨테이너에 점유돼 있어(`monticker-mongodb`), Quno 자체 Mongo로 "정상 → 장애"를 온전히 재현하기 위해 임시로 `docker run -d --name quno-mongo-a3-test -p 27018:27017 mongo:7`을 띄우고 `SPRING_MONGODB_PORT=27018`로 백엔드를 재기동해 정상 동작을 먼저 확인한 뒤, `docker stop quno-mongo-a3-test`로 정지시켰다. 질문 887에 라이브챗 룸(`POST /api/v1/questions/887/live-chat`, roomId=2)을 만들고 `GET /api/v1/live-chat/2/messages`(Mongo 조회)와 `GET /api/v1/questions/887`·`GET /api/v1/tags`·`GET /api/v1/search?q=kotlin`(Mongo 미사용)을 비교했다.
- **관찰**: Mongo 정지 후 `/api/v1/questions/887`(200, 46ms), `/api/v1/tags`(200, 11ms), `/api/v1/search?q=kotlin`(200, 59ms) 모두 즉시 정상 응답 — 런북의 "Mongo 미사용 기능은 무관" 주장은 확인됨(PASS). 하지만 `GET /api/v1/live-chat/2/messages`는 **수정 전** 정확히 30초 뒤에야 `HTTP:500`(`INTERNAL_ERROR`)을 반환했다 — A2에서 본 것과 같은 패턴(드라이버 기본 타임아웃 30초, 이번엔 MongoDB 드라이버의 `serverSelectionTimeoutMS`)이었다. `/actuator/health`의 `mongo` 컴포넌트도 30초 뒤에야 `DOWN`으로 바뀌었다.
- **판정**: PARTIAL(비-Mongo 기능 격리는 PASS, 장애 감지 속도는 FAIL) → 조치 후 PASS
- **발견 및 조치**: `infrastructure/config/MongoConfig.kt`에 `MongoClientSettingsBuilderCustomizer` 빈을 추가해 `serverSelectionTimeout`을 3초로 낮췄다. **수정 후 동일하게 재현**한 결과 `/api/v1/live-chat/2/messages`가 `HTTP:500 TIME:3.24s`, `/actuator/health`가 `HTTP:503 TIME:3.36s`로 단축됨을 확인했다. [ADR-0055](../architecture/decisions/0055-mongo-server-selection-timeout-fast-fail.md)로 남겼다. 테스트에 쓴 임시 Mongo 컨테이너(`quno-mongo-a3-test`)는 검증 후 삭제했다.

### A4. Redis 정지

- **가설**: 런북 주장대로 Rate Limiting(인메모리)과 인증/일반 API는 Redis 장애와 무관해야 하고, Live Chat 접속자 표시만 영향받아야 한다. 코드 리뷰로 미확인이었던 지점 — 조직 검색 등 Redis cache-aside 캐시가 Redis 장애 시 예외를 던지는지 DB로 우회하는지 — 을 실제로 확인한다.
- **주입 방법**: `docker compose stop redis`. `POST /api/v1/auth/login`(레이트리밋 필터 경유), `GET /api/v1/tags`(Mongo/Redis 미사용), `GET /api/v1/organizations?q=quno`(조직 검색 캐시), `GET /api/v1/dashboard`(인기 질문/트렌딩 태그 + qunobot 스파이크 감지 캐시)를 순서대로 호출.
- **관찰**: 로그인(200, 160ms)과 태그 조회(200, 13ms)는 예상대로 Redis와 무관하게 즉시 정상 — Explore 서브에이전트로 `RateLimitFilter`(`ConcurrentHashMap` 기반 순수 인메모리, `production-readiness.md` B-2가 이미 알려진 단순화로 남겨둠)를 먼저 확인해 근거를 세웠다. 반면 `GET /api/v1/organizations?q=quno`는 즉시(4.6ms) `HTTP:500`(`INTERNAL_ERROR`) — `redisTemplate.opsForValue().get()`이 `RedisSystemException`을 그대로 던졌다. 같은 패턴을 쓰는 `DashboardRepositoryAdapter`와, 로그 스택트레이스로 추가 발견한 `SpikeDetectionRepositoryAdapter`(호출 경로: `GetDashboardUseCase` → `GetActivityFeedUseCase` → `SpikeDetectionRepositoryAdapter.findSpikingTags`)도 `GET /api/v1/dashboard`에서 동일하게 `RedisConnectionFailureException`으로 500이 났다. 세 곳 모두 DB에 완전한 정답이 있는 cache-aside 구조였는데도 캐시 실패가 기능 전체를 죽였다.
- **판정**: PARTIAL(Rate Limiting/일반 API 격리는 PASS, 캐시-aside 3곳은 FAIL) → 조치 후 PASS
- **발견 및 조치**: `infrastructure/persistence/redis/RedisCacheSupport.kt`에 `safeCacheGet`/`safeCacheSet` 확장 함수(`DataAccessException`을 잡아 캐시 미스로 취급)를 추가하고, `OrganizationRepositoryAdapter`/`DashboardRepositoryAdapter`/`SpikeDetectionRepositoryAdapter` 세 곳의 직접 호출을 전부 교체했다. **수정 후 동일하게 재현**한 결과 `GET /api/v1/organizations?q=quno`(200, 45ms), `GET /api/v1/dashboard`(200, 377ms) 모두 DB 폴백으로 정상 응답함을 확인했다. Redis가 유일한 원본인 `RedisLiveChatPresenceTracker`(Live Chat 접속자 표시)는 의도적으로 제외했다 — 이건 런북이 이미 예상한 "영향받는 게 정상"인 부분이다. [ADR-0056](../architecture/decisions/0056-redis-cache-aside-graceful-degradation.md)으로 남겼다.

### A5. Mailpit(SMTP) 정지

- **가설**: [ADR-0046](../architecture/decisions/0046-reliability-account-withdrawal-data-export-n-plus-1.md)이 도입한 이메일 발송 재시도 로직이 실제로 3회 시도하고, 최종 실패 시 사용자에게 어떤 형태로든 응답이 가야 한다(무한 대기가 아니어야 한다).
- **주입 방법**: Mailpit이 정상일 때 `POST /api/v1/organizations/verify-email`(`{"email":"demo@quno.dev"}`)로 기준 응답(200)을 확인한 뒤 `docker compose stop mailpit`으로 정지시키고 동일 요청을 재호출.
- **관찰**: Mailpit 정지 후 `HTTP:500`(`INTERNAL_ERROR`)이 **1.57초** 만에 왔다 — `SmtpVerificationEmailSender.sendWithRetry`(`backend/src/main/kotlin/.../infrastructure/external/SmtpVerificationEmailSender.kt:32`)가 `MAX_ATTEMPTS=3`, 지수 아닌 선형 백오프(`RETRY_DELAY_MS(500) * attempt`, 즉 500ms→1000ms)로 정확히 3회 시도한 뒤 마지막 예외를 그대로 던졌다(로그에서 `Connection refused` 5회 확인 — 3회는 재시도 자체, 나머지는 Mailpit actuator 헬스 폴링). 최종 에러는 이메일 관련임을 알 수 없는 범용 `INTERNAL_ERROR` 메시지였지만, 코드 주석("인증 메일 발송은 재시도해도 안전하다")대로 인증 요청 자체(코드 생성·DB 저장)는 이미 커밋된 상태라 사용자가 같은 이메일로 재요청하면 새 코드가 이전 코드를 대체한다 — 무한 대기나 좀비 상태 없이 안전하게 재시도 가능한 실패 모드였다.
- **판정**: PASS
- **발견 및 조치**: 버그 없음. 재시도 3회, 최종 실패까지 총 1.57초(선형 백오프 1.5초 + 즉시 실패하는 연결거부 3회)로 사용자를 오래 붙잡지 않았고, 실패해도 재요청으로 복구 가능한 안전한 상태였다. 에러 메시지가 범용적인 점은 다른 모든 `INTERNAL_ERROR` 응답과 동일한 기존 관례([GlobalExceptionHandler](../../backend/src/main/kotlin/com/quno/qunobackend/interfaces/api/common/GlobalExceptionHandler.kt))라 이번 범위에서 별도로 고치지 않았다. Mailpit은 검증 후 재기동해 원상 복구했다.

### F1. 백엔드 완전 다운 상태에서 프론트엔드 접속

- **가설**: 홈/질문 상세/로그인 등 주요 페이지가 흰 화면이나 처리되지 않은 예외 대신 적절한 에러 상태를 보여줘야 한다.
- **주입 방법**: 백엔드 프로세스를 완전히 종료한 채 프론트엔드 dev 서버(`localhost:3000`)에서 홈(`/`, 비로그인), 질문 상세(`/questions/887`), 로그인(`/login`, 실제 로그인 시도)을 브라우저로 직접 확인.
- **관찰**: 홈(비로그인 게스트 뷰)은 백엔드 데이터 없이도 정적 콘텐츠라 정상 렌더링됐다. 로그인 페이지는 폼 제출 시 `FormError` 컴포넌트를 통해 "로그인에 실패했습니다."를 보여주고 크래시나 흰 화면 없이 정상 처리됐다. 하지만 질문 상세 페이지(`QuestionDetailContent.tsx:52`)는 `useQuestion`의 `isError || !question` 조건만으로 "질문을 찾을 수 없습니다."를 띄우고 있었다 — 백엔드가 완전히 죽어 `ERR_CONNECTION_REFUSED`가 나는 상황과, 질문이 실제로 삭제/존재하지 않는 404 상황을 구분하지 않아, 일시적 장애를 "이 질문은 삭제됐다"처럼 잘못 전달하고 있었다.
- **판정**: PARTIAL(홈/로그인은 PASS, 질문 상세는 FAIL) → 조치 후 PASS
- **발견 및 조치**: `error instanceof ApiError && error.status === 404`일 때만 "질문을 찾을 수 없습니다."를 보여주고, 그 외 에러(네트워크 실패, 5xx 등)는 "질문을 불러오지 못했습니다. 잠시 후 다시 시도해주세요."로 구분했다 — `moderation/page.tsx`가 이미 쓰던 것과 같은 패턴(`error instanceof ApiError && error.status === ...`)이다. 기존 테스트(`QuestionDetailContent.test.tsx`)가 "에러면 무조건 찾을 수 없음"을 전제로 하고 있어 404 케이스와 비404 케이스로 나눠 갱신했다(15개 테스트 통과, `tsc`/`eslint` 클린). 참고: 이 저장소의 브라우저 자동화 도구(Claude Browser pane)에서는 TanStack Query의 `networkMode`가 실제 사용자 브라우저와 다르게 동작해(재시도 후 `fetchStatus: "paused"`로 멈춤, `navigator.onLine`은 `true`인데도) 라이브 브라우저로 404 케이스를 직접 재현하지 못했다 — 이 로직은 목(mock) 기반 단위 테스트로 검증했다.

### F2. 백엔드는 살아있지만 DB 응답이 없는 상태(docker pause)

- **가설**: `shared/api/http-client.ts`에 타임아웃 로직이 있는지부터 코드로 확인한 뒤(사전 확인 결과, `fetch()` 호출에 `AbortController`/`signal`이 전혀 없음을 이미 발견함), DB가 응답 없이 멈췄을 때 로딩 스켈레톤이 무한정 보이는지, 아니면 일정 시간 후 에러로 전환되는지 실제로 확인한다.
- **주입 방법**: 질문 상세 페이지(`/questions/887`)를 정상 로드해 데이터가 보이는 것을 먼저 확인한 뒤 `docker compose pause postgres`(정지가 아니라 프로세스를 멈춰 TCP 연결은 유지한 채 응답만 없는 상태)로 만들고 같은 페이지를 새로고침, 동시에 백엔드에 직접 `curl`로 응답 시간을 측정.
- **관찰**: 백엔드 직접 호출은 **5.06초** 후 `HTTP:500`으로 응답했다 — HikariCP `connection-timeout`(ADR-0054, 3초)이 아니라 기본값 그대로인 `validationTimeout`(5000ms)에 걸린 것으로 보인다(이미 풀에 있던 연결을 꺼내 유효성 검사하다가 응답이 없는 DB에 막힘). 프론트엔드는 로딩 스켈레톤에 무한정 멈추지 않고 5초 남짓 뒤 F1에서 고친 "질문을 불러오지 못했습니다. 잠시 후 다시 시도해주세요."로 정상 전환됐다 — `http-client.ts`에 클라이언트 자체 타임아웃이 없는데도, 백엔드가 (A2/ADR-0054 덕에) 유한한 시간 안에 반드시 응답을 주기 때문에 사실상 문제가 되지 않았다.
- **판정**: PASS(현재 상태 기준) — 단, 잔여 위험 있음
- **발견 및 조치**: 코드 수정 없음. `http-client.ts`의 `fetch()` 호출에는 `AbortController`/`signal` 기반의 클라이언트 자체 타임아웃이 없다는 것을 코드로 확인했다 — 지금은 백엔드 쪽 타임아웃(HikariCP connection-timeout 3초, validationTimeout 5초 등)이 사실상의 상한선 역할을 해줘서 실사용에 문제가 없지만, 프론트엔드가 백엔드의 내부 설정에 암묵적으로 의존하는 구조다. 백엔드에 타임아웃 없는 새 API 경로가 추가되면 프론트는 그 즉시 무한 로딩에 노출된다. 지금 당장 망가진 것을 고치는 원칙(이 문서 목적 2)에는 해당하지 않아 이번엔 손대지 않았지만, 후속 과제로 남긴다(별도 세션으로 분리함). Postgres는 검증 후 `docker compose unpause`로 정상화했다.

### F3. localStorage에 변조/만료된 JWT를 심은 뒤 새로고침

- **가설**: `http-client.ts`의 401 재발급/재시도 로직(`http-client.test.ts`가 이미 목 기반으로 검증)이 실제 브라우저에서도 그대로 동작해야 한다 — access token만 무효하면 조용히 갱신 후 성공하고, refresh token까지 무효하면 무한 재시도나 크래시 없이 깔끔하게 로그아웃 상태로 떨어져야 한다.
- **주입 방법**: 데모 계정(`demo@quno.dev`)으로 실제 로그인해 정상 세션을 만든 뒤, ① `localStorage`의 `quno.accessToken`만 서명이 깨진 문자열로 덮어쓰고 새로고침, ② 이어서 `quno.accessToken`과 `quno.refreshToken`을 둘 다 변조하고 다시 새로고침.
- **관찰**: ① access token만 깨졌을 때는 홈 대시보드가 로그인 상태 그대로(인기 질문·Ward 업데이트·관심 태그 피드) 정상 렌더링됐고, `localStorage`를 재확인하니 access token 값이 내가 심은 변조 문자열과 달라져 있었다 — 401 → `refreshAccessToken()` → 재시도가 사용자가 눈치채지 못하게 조용히 성공한 것. ② 두 토큰을 모두 깨자 홈이 게스트 뷰("로그인하면 인기 질문과 Ward 업데이트를 볼 수 있습니다")로 즉시 전환됐고, `localStorage`의 두 키가 모두 `null`로 지워져 있었다 — `refreshAccessToken()`이 갱신 요청 자체의 401을 받아 `tokenStorage.clear()`를 호출한 경로(`http-client.ts:34`)가 그대로 동작했다. 콘솔에는 예상된 401 두 건(원 요청 1회 + 리프레시 시도 1회)만 있었고, 무한 재시도나 처리되지 않은 예외는 없었다.
- **판정**: PASS
- **발견 및 조치**: 버그 없음. 기존 유닛 테스트(`http-client.test.ts`)가 검증한 대로 실제 브라우저에서도 동일하게 동작함을 확인했다.

### B1. HikariCP 커넥션 풀 고갈

- **가설**: 명시적 풀 크기 설정이 없어 기본값(10개)인 HikariCP 풀이 전부 소진되면, 풀을 못 받은 초과 요청은 무한 대기가 아니라 [ADR-0054](../architecture/decisions/0054-hikari-connection-timeout-fast-fail.md)의 `connection-timeout`(3초) 안에 명확한 에러로 실패해야 한다.
- **주입 방법**: `psql`로 `BEGIN; LOCK TABLE questions IN ACCESS EXCLUSIVE MODE; SELECT pg_sleep(25); COMMIT;`를 실행해 `questions` 테이블에 배타 락을 25초간 걸어둔 뒤, `GET /api/v1/questions/887`(이 테이블을 읽는 엔드포인트)로 **12개 동시 요청**을 발사해 풀(10개)보다 많은 커넥션을 요구하게 만들었다.
- **관찰**: 10개 요청은 풀에서 커넥션을 받아 락이 풀릴 때까지 블로킹된 채 대기하다 락 해제 시점(약 17.5초 후)에 전부 `HTTP:200`으로 정상 완료됐다. 풀 용량을 초과한 나머지 2개 요청은 **정확히 3.0초**(`3.026794s`, `3.026898s`) 만에 `HTTP:500`으로 실패했다 — 로그에서 `java.sql.SQLTransientConnectionException: HikariPool-1 - Connection is not available, request timed out after 3011ms (total=10, active=10, idle=0, waiting=0)`을 확인해 풀이 정확히 10/10 소진된 상태에서 `connection-timeout`이 발동했음을 검증했다. 무한 대기나 스레드 고갈로 이어지는 연쇄 장애 없이, 초과분만 깔끔하게 빠른 실패로 처리됐다.
- **판정**: PASS
- **발견 및 조치**: 버그 없음. A2에서 고친 `connection-timeout`(ADR-0054)이 "DB 자체가 죽었을 때"뿐 아니라 "커넥션 풀이 정상 DB에 대해 고갈됐을 때"도 동일하게 3초 안에 빠른 실패를 보장한다는 것을 확인했다 — ADR-0054의 결과 섹션이 예고했던 재검증이다. 테스트 후 `pg_terminate_backend`로 락 세션을 정리해 원상 복구했다.

### B2. `docker pause postgres`(응답 없음 상태)

- **가설**: `docker compose stop`(연결 자체 거부)과 다르게, `docker pause`는 TCP 연결은 유지된 채 응답만 없는 상태를 만든다 — 이 경우도 무한 대기 없이 유한한 시간 안에 실패해야 한다.
- **주입 방법 및 관찰**: 이 시나리오는 F2([위 F2 항목](#f2-백엔드는-살아있지만-db-응답이-없는-상태docker-pause) 참고) 실행 중 이미 동일한 방법(`docker compose pause postgres`)으로 재현하고 관찰했다 — 프론트엔드 회복력 검증과 백엔드의 실제 실패 모드 검증이 같은 주입으로 동시에 답이 나오는 경우라 별도로 반복하지 않았다. 백엔드 직접 호출은 5.06초 후 `HTTP:500`으로 응답했다(`connection-timeout` 3초가 아니라 커넥션 유효성 검사 단계의 `validationTimeout` 기본값 5초에 걸린 것으로 보임 — 이미 풀에 있던 연결을 꺼내 쓰려다 응답 없는 DB에 막힌 경우와 새 연결을 맺으려는 경우가 서로 다른 타임아웃 설정의 적용을 받는다).
- **판정**: PASS
- **발견 및 조치**: 버그 없음. `docker pause`도 무한 대기로 이어지지 않고 5초 안에 실패한다. 다만 이 5초는 `connection-timeout`(3초, ADR-0054)이 아니라 HikariCP `validationTimeout` 기본값이 우연히 맞아떨어진 것이라, 정확히 어떤 코드 경로가 어떤 타임아웃에 걸리는지는 이번 범위에서 더 파고들지 않았다 — 필요해지면 `validationTimeout`도 명시적으로 낮추는 것을 후속 과제로 남긴다.

### B4. Mailpit을 `docker pause`(응답 없음 상태)로 만들어 SMTP 타임아웃 검증

- **가설**: `application-local.yml`에 명시된 `connectiontimeout`/`timeout`/`writetimeout`(5000ms)이 실제로 각 시도마다 5초 뒤 발동해야 하고, A5(정지=연결거부)와 다른 실패 모드(응답없음)에서도 재시도 로직이 무한 대기 없이 끝나야 한다.
- **주입 방법**: `docker compose pause mailpit`으로 SMTP 포트는 열려 있지만 응답이 없는 상태를 만든 뒤 `POST /api/v1/organizations/verify-email`을 호출.
- **관찰**: `HTTP:500`이 **16.64초** 만에 왔다 — A5(연결거부, 1.57초)보다 훨씬 길다. `connectiontimeout=5000ms`짜리 시도 3번(3×5s=15s) + 재시도 사이 선형 백오프(500ms+1000ms=1.5s) ≈ 16.5초로 계산이 거의 정확히 들어맞아, 설정된 타임아웃 값이 실제로 그대로 적용되고 있음을 확인했다. 무한 대기는 아니었지만, "응답 없음"이 "연결 거부"보다 실패를 확정 짓는 데 10배 이상 오래 걸린다는 것이 이번에 드러났다.
- **판정**: PASS
- **발견 및 조치**: 버그 없음(설계대로 동작). 16.6초는 사용자 입장에서 체감상 길지만, 이 값(5초×3회)은 A2/A3의 30초 사례처럼 "아무도 설정한 적 없는 기본값"이 아니라 이미 누군가 명시적으로 정한 값이고, 로컬 Mailpit이 아닌 실제 외부 SMTP 릴레이(운영 환경)를 향한 왕복이라면 5초가 딱히 과한 값도 아니다 — A2/A3와 달리 "고쳐야 할 방치된 기본값"이 아니라 "이미 내려진 트레이드오프"라 판단해 이번 범위에서 값을 바꾸지 않았다. Mailpit은 검증 후 `docker compose unpause`로 정상화했다.

### B3. Toss 결제 확인 지연(로컬 목 서버)

- **가설**: `TossPaymentGateway`에 타임아웃 설정이 있는지 코드로 먼저 확인한 뒤(사전 확인 결과 `SimpleClientHttpRequestFactory`에 `connectTimeout=5000ms`, `readTimeout=10000ms`가 이미 명시돼 있음을 발견), 실제로 응답 없는 Toss 서버에 대해 그 타임아웃이 발동하는지, 발동 시 결제 상태가 어중간하게 남지 않는지 확인한다.
- **주입 방법**: 15초간 응답을 지연시키는 로컬 Python HTTP 목 서버(`127.0.0.1:9999`)를 띄우고 `quno.toss.api-base-url=http://localhost:9999`로 백엔드를 재기동([ADR-0037](../architecture/decisions/0037-paid-direct-ask-toss-payments-test-mode.md)이 쓴 것과 같은 방식, 실제 Toss 서버에는 요청하지 않음). 신규 테스트 계정(`b3-target@quno.dev`, `acceptsDirectAsk=true`)을 만들어 질문 887에 실제로 Direct Ask 요청(`POST /questions/887/direct-asks`)을 생성해 진짜 `orderId`를 확보한 뒤 `POST /direct-asks/payments/confirm`을 호출.
- **관찰**: 정확히 **10.30초** 후 `HTTP:500`(`INTERNAL_ERROR`)이 왔다 — 코드에 명시된 `readTimeout=10000ms`가 그대로 발동한 것이다. `TossPaymentGateway.confirm()`은 `RestClientResponseException`(4xx/5xx 응답)만 잡아 `PaymentConfirmationFailedException`으로 바꾸는데, 타임아웃은 `ResourceAccessException`이라 이 catch에 안 걸리고 그대로 위로 전파돼 `GlobalExceptionHandler`의 범용 500이 됐다 — 하지만 `ConfirmDirectAskPaymentUseCase.execute()`가 `@Transactional`이고 `directAskPaymentRepository.save(payment.confirm(...))`가 Toss 호출 *이후*에 실행되므로, 어떤 예외 타입이든 트랜잭션이 통째로 롤백돼 `direct_ask_payments.status`가 계속 `PENDING`으로 남아 있음을 DB에서 직접 확인했다(재시도 시 `PaymentAlreadyProcessedException` 가드에도 안 걸림 — 안전하게 재시도 가능).
- **판정**: PASS
- **발견 및 조치**: 버그 없음. 타임아웃이 설정대로 작동하고, 실패해도 결제 상태가 어중간하게 남지 않아 D1/D2가 우려하는 "좀비 상태"가 되지 않음을 확인했다. `ResourceAccessException`을 `RestClientResponseException`과 함께 잡아 `PaymentConfirmationFailedException`으로 통일하면 에러 메시지 일관성은 조금 나아지겠지만, 트랜잭션 안전성에는 차이가 없어(Spring 기본 롤백 규칙이 unchecked exception 전체에 적용됨) 이번 범위에서는 고치지 않았다. 목 서버와 재정의한 `api-base-url`은 검증 후 원상 복구했다.

### C1. 로그인 엔드포인트 Rate Limiting (prod 기준 값)

- **가설**: 로컬 프로필 기본값(`capacity: 1000`)은 사실상 무제한이라 개발 중 E2E 테스트에 방해가 안 되지만, prod 값(`capacity: 10`, `refill-period-seconds: 60`)을 적용하면 11번째 요청부터 `429`가 나야 하고, 일정 시간 뒤에는 다시 허용돼야 한다.
- **주입 방법**: 전체 `prod` 프로필로 전환하지 않고(필수 환경변수 다수 때문에 로컬에서 기동 불가) `quno.rate-limit.capacity`/`refill-period-seconds`만 prod 값(10, 60)으로 오버라이드해 `local` 프로필로 재기동. `POST /api/v1/auth/login`(틀린 비밀번호)을 12회 연속 호출.
- **관찰**: 1~10번째 요청은 전부 `HTTP:401`(비밀번호가 틀렸을 뿐 레이트리밋에는 안 걸림)이었고, **11번째부터 정확히** `HTTP:429`(`TOO_MANY_REQUESTS`)로 바뀌었다 — `capacity=10`이 정확히 적용됨을 확인했다. 이어서 **7초**만 기다린 뒤 재시도하니 2건이 다시 `401`(통과)로, 3번째가 다시 `429`였다 — Bucket4j의 `refillGreedy`가 "60초마다 10개를 한 번에 채우는" 방식이 아니라 시간에 비례해 점진적으로 토큰을 채우는 방식이라, 원래 계획 문서가 표현한 "1분 경과 후 다시 허용"이라는 문구보다 훨씬 빨리(이론상 토큰 1개당 약 6초) 부분적으로 회복된다는 것이 실측으로 드러났다.
- **판정**: PASS
- **발견 및 조치**: 버그 없음, 오히려 계획 문서의 가설(1분 뒤 회복)보다 실제 동작(수 초 단위로 점진적 회복)이 사용자에게 더 유리한 방향이었다. 코드 수정은 필요 없어 관찰 내용만 기록해둔다. 검증 후 rate-limit 오버라이드 없이 재기동해 원상 복구했다.

### C2. k6 스파이크 부하(VUs 50)

- **가설**: [production-readiness.md](../product/production-readiness.md) B-5가 검증한 정상 부하(VUs=3)보다 훨씬 큰 순간 스파이크(VUs 50)에서 응답 지연이 급격히 늘거나 에러율이 뜨는지 확인한다.
- **주입 방법**: `scripts/load-test.js`와 같은 비로그인 공개 읽기 경로(검색/태그/질문 상세)를 대상으로, 램프업 2초 → VUs 50 유지 5초 → 램프다운 2초의 짧은 스파이크 시나리오(`ramping-vus`)를 임시 스크립트로 작성해 k6로 실행했다(기존 `load-test.js`는 지속 부하용이라 그대로 재사용하지 않고 범위 밖의 별도 스크립트로 실행, 저장소에는 반영하지 않음).
- **관찰**: 9초 동안 **총 22,500건 체크, 실패 0건(100% 성공)**, `http_req_duration` p95 **42.54ms**(최대 208.79ms)로, 기존 기준값(B-5의 `p(95)<500ms` 임계치)에 크게 못 미쳤다. 초당 약 2,499 요청까지 몰렸는데도 HikariCP 풀 고갈이나 에러 없이 소화했다 — B1에서 본 것과 달리 이 경로들은 전부 인덱스가 걸린 단순 조회라 커넥션을 오래 붙잡지 않기 때문으로 보인다. 스파이크 종료 후에도 백엔드는 정상 응답(`db: UP`)했다.
- **판정**: PASS
- **발견 및 조치**: 버그 없음. 로컬 단일 인스턴스 기준으로도 순간 스파이크에 여유가 있음을 확인했다 — 다만 이 결과는 로컬 개발 머신 스펙과 데모 데이터 규모에서 나온 것이라, `load-test.js` 주석이 이미 밝힌 대로 실제 용량 산정에는 쓸 수 없고 스테이징 인프라에서 재검증이 필요하다(범위 밖, production-readiness.md A).

### C3. 같은 질문에 대한 동시 리비전 요청 폭주(실제 HTTP 레벨)

- **가설**: `ReviseQuestionConcurrencyIntegrationTest`가 이미 검증한 비관적 락 방어(ADR-0005)가 단위 테스트의 시뮬레이션이 아니라 실제 Tomcat 스레드 풀 위에서 동시에 들어오는 진짜 HTTP 요청에 대해서도 동일하게 버전 번호 중복/유실 없이 동작해야 한다.
- **주입 방법**: 데모 계정으로 새 질문(id=910, 초기 versionNumber=1)을 만든 뒤, `curl`을 백그라운드(`&`)로 **15개 동시 실행**해 같은 질문에 `POST /api/v1/questions/910/versions`를 동시에 요청.
- **관찰**: 15개 요청이 전부 성공(`status: UPDATED`)했고, 응답으로 돌아온 `versionNumber`가 2~16까지 정확히 15개의 서로 다른 값으로 나뉘어졌다. 이후 `GET /api/v1/questions/910/versions`로 전체 버전 목록을 조회해 1~16번이 중복이나 누락 없이 정확히 존재함을 확인했다.
- **판정**: PASS
- **발견 및 조치**: 버그 없음. 단위 테스트가 검증한 동시성 방어가 실제 서버 스레드 풀 환경에서도 동일하게 성립함을 확인했다.

### E1. 요청 처리 중 `kill -TERM`(graceful shutdown)

- **가설**: 처리 중인 요청이 있을 때 `SIGTERM`을 받으면, 그 요청은 강제로 끊기지 않고 끝까지 완료된 뒤에 프로세스가 종료돼야 한다.
- **주입 방법**: `questions` 테이블에 `LOCK TABLE ... IN ACCESS EXCLUSIVE MODE; SELECT pg_sleep(8)`로 8초짜리 지연을 만들고, 그 락에 걸려 블로킹될 `GET /api/v1/questions/887` 요청을 백그라운드로 먼저 쏜 뒤 1초 뒤 백엔드 프로세스에 `kill -TERM`을 보냈다.
- **관찰**: `kill -TERM` 직후 로그에 `Commencing graceful shutdown. Waiting for active requests to complete`가 찍혔고, curl 요청은 끊기지 않고 **7.03초** 뒤(락이 풀리는 시점과 일치) `HTTP:200`으로 정상 완료됐다. 그 직후 `Graceful shutdown complete`가 찍히며 프로세스가 종료됐다. 이 저장소의 `application.yml`/`application-{local,prod}.yml` 어디에도 `server.shutdown: graceful`을 명시한 곳이 없는데도 이렇게 동작했다 — Spring Boot 4의 기본값이거나 다른 자동 설정이 관여하는 것으로 보이나, 정확한 원인까지는 이번 범위에서 추적하지 않았다.
- **판정**: PASS
- **발견 및 조치**: 버그 없음. 명시적으로 설정한 적이 없는데도 graceful shutdown이 실제로 동작하는 것은 좋은 결과이지만, "왜 되는지 아무도 모른다"는 상태 자체가 위험 요소다 — 향후 Spring Boot를 업그레이드하다 이 기본값이 바뀌면 아무도 모르게 조용히 회귀할 수 있다. 그래서 `server.shutdown: graceful`을 `application.yml`에 명시적으로 추가해 동작을 코드로 고정했다(공통 설정이라 모든 프로필에 적용).

### E2. `kill -9`(강제 종료) 직후 재기동

- **가설**: 트랜잭션이 진행 중일 때 프로세스를 강제 종료해도 PostgreSQL이 클라이언트 연결 끊김을 감지해 트랜잭션을 스스로 롤백해야 하고(좀비 락 없음), 재기동 시 Flyway가 정상적으로 "마이그레이션 없음"을 확인해야 한다.
- **주입 방법**: `questions` 테이블에 6초 락을 건 상태에서 `POST /api/v1/questions/910/versions`(리비전 생성, 행 락을 필요로 함)를 호출해 그 요청이 락을 기다리며 블로킹된 동안(1.5초 후) 백엔드 프로세스에 `kill -9`를 보냈다.
- **관찰**: curl은 `HTTP:000`(응답 없이 연결 끊김)으로 끝나 정확히 트랜잭션 도중에 죽었음을 확인했다. 락이 풀리는 시점(주입 후 6초) 이후 `pg_locks`/`pg_stat_activity`를 조회하니 `questions` 테이블에 남은 락이 0건, 앱이 열어뒀던 세션도 전부 사라져 있었다 — PostgreSQL이 TCP 연결 끊김을 감지해 스스로 트랜잭션을 정리한 것. 재기동 로그에는 `Successfully validated 23 migrations`, `Schema "public" is up to date. No migration necessary.`가 정상적으로 찍혔다. 재기동 후 질문 910을 다시 조회하니 `versionNumber`가 여전히 16(C3에서 만든 마지막 버전)으로, kill -9 도중 시도했던 리비전(제목 "E2 테스트")은 전혀 반영되지 않았다 — 부분 커밋 없이 깨끗하게 전부 롤백됐다.
- **판정**: PASS
- **발견 및 조치**: 버그 없음. PostgreSQL의 연결 끊김 감지와 Flyway의 체크섬 기반 검증이 강제 종료 뒤에도 데이터 일관성과 재기동 안전성을 그대로 보장했다.

## 관련 문서

- [runbook.md](runbook.md) — 이 테스트가 검증하는 주장들의 원본
- [production-readiness.md](../product/production-readiness.md) B-4(신뢰성) — 이미 다룬 재시도/백업 정책
- [technical-deep-dives.md](../engineering/technical-deep-dives.md) — 이 테스트로 새로 발견하는 버그의 기록 위치
