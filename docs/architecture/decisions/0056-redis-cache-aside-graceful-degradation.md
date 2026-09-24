# ADR-0056: Redis cache-aside 읽기/쓰기는 Redis 장애 시 DB로 조용히 대체한다

- 날짜: 2026-09-23
- 상태: 부분 대체됨(ADR-0059로) — `DashboardRepositoryAdapter`/`SpikeDetectionRepositoryAdapter`는 `@Cacheable` 기반으로 옮겨졌다. `OrganizationRepositoryAdapter`는 여기 남긴 `safeCacheGet`/`safeCacheSet` 방식 그대로다(사유는 ADR-0059 참고).

## 배경 (Context)

[failure-scenario-testing.md](../../operations/failure-scenario-testing.md) A4(Redis 장애) 시나리오에서, 런북 2.2절의 "Rate Limiting(인메모리)과 일반 API는 Redis 장애와 무관하다"는 주장을 검증했다.

- **Rate Limiting**은 실제로 `RateLimitFilter`가 `ConcurrentHashMap`으로 버킷을 관리하는 순수 인메모리 구현이라(분산 버킷 아님, 단일 인스턴스 전제 — [production-readiness.md](../../product/production-readiness.md) B-2가 이미 알려진 단순화로 남겨둠), Redis 정지와 완전히 무관하게 정상 동작했다(로그인 API 정상 200 확인).
- 하지만 캐시가 붙어있는 세 곳 — `OrganizationRepositoryAdapter.search()`(ADR-0050이 언급하는 조직 검색 캐시), `DashboardRepositoryAdapter`(인기 질문/트렌딩 태그), `SpikeDetectionRepositoryAdapter`(qunobot 태그 스파이크 감지, ADR-0009) — 는 전부 `redisTemplate.opsForValue().get()`/`.set()`을 try-catch 없이 그대로 호출하고 있었다. `docker compose stop redis`로 Redis를 정지시키자 셋 다 `RedisConnectionFailureException`/`RedisSystemException`이 컨트롤러까지 그대로 전파돼 `HTTP:500`(`{"code":"INTERNAL_ERROR", ...}`)이 됐다 — 캐시는 원래 DB 위에 얹은 최적화일 뿐인데, 캐시가 죽으면 기능 자체가 죽는 셈이었다. 세 곳 모두 DB가 캐시와 별개로 완전한 정답을 갖고 있는(cache-aside) 구조라 이 실패는 불필요했다.
- Live Chat 접속자 표시(`RedisLiveChatPresenceTracker`, `opsForSet`)는 다른 종류다 — DB에 별도 원본이 없고 Redis Set 자체가 접속자 목록의 유일한 저장소(ephemeral, 재시작 시 리셋되는 것이 설계 의도, ADR-0036)라서 여기서는 "Redis 장애 시 기능이 영향받는 것"이 런북이 이미 예상한 정상 동작이다 — 이번 결정 대상에서 제외한다.

## 결정 (Decision)

`infrastructure/persistence/redis/RedisCacheSupport.kt`에 `StringRedisTemplate.safeCacheGet(key)`/`safeCacheSet(key, value, ttl)` 확장 함수를 추가한다. 둘 다 `org.springframework.dao.DataAccessException`(Redis 관련 예외가 실제로 던지는 구체적인 서브타입 — `RedisConnectionFailureException`, `RedisSystemException`, `QueryTimeoutException` 등 — 을 모두 포괄하는 Spring Data 공통 상위 타입)을 잡아 로그만 남기고 `get`은 `null`(캐시 미스 취급), `set`은 아무 것도 하지 않는다(호출자는 이미 계산된 결과를 그대로 반환하므로 캐시 쓰기 실패가 응답에 영향을 주지 않는다).

캐시-DB 이중 소스를 가진 세 어댑터(`OrganizationRepositoryAdapter`, `DashboardRepositoryAdapter`, `SpikeDetectionRepositoryAdapter`)의 `redisTemplate.opsForValue()` 직접 호출을 전부 이 확장 함수로 교체했다. `RedisLiveChatPresenceTracker`는 대상에서 제외했다 — Redis가 유일한 원본이라 "안전하게 실패"할 대체 경로 자체가 없다.

구체적인 예외 타입(`RedisConnectionFailureException` 등)이 아니라 `DataAccessException`으로 넓게 잡은 이유: Lettuce/Spring Data Redis가 근본 원인(연결 거부, 타임아웃, 프로토콜 에러)에 따라 어떤 서브타입으로 감쌀지가 버전/상황마다 달라질 수 있는데, 이 함수의 목적은 "Redis가 어떤 이유로든 응답하지 못하면 캐시를 우회한다"이지 특정 실패 모드만 처리하는 게 아니기 때문이다.

## 결과 (Consequences)

- Redis 장애 시 조직 검색/대시보드/스파이크 감지가 캐시 없이 DB 직접 조회로 계속 동작한다(검증: `docker compose stop redis` 재현으로 세 엔드포인트 모두 200 확인, 이전에는 전부 500).
- Redis가 죽어있는 동안은 모든 요청이 캐시를 건너뛰고 매번 DB를 때리므로, Redis 장애가 길어지면 DB 부하가 캐시 적중률만큼 증가한다 — 캐시가 원래 있던 이유(반복 계산 비용 절감)가 사라지는 대가이며, 의도된 트레이드오프다.
- 새로운 Redis 캐시-aside 코드를 추가할 때는 이 두 확장 함수를 기본으로 쓰는 것을 관례로 삼는다(Redis가 유일한 원본인 경우는 예외).

## 관련 문서

- [failure-scenario-testing.md](../../operations/failure-scenario-testing.md) A4
- [0054-hikari-connection-timeout-fast-fail.md](0054-hikari-connection-timeout-fast-fail.md), [0055-mongo-server-selection-timeout-fast-fail.md](0055-mongo-server-selection-timeout-fast-fail.md) — 같은 장애 시나리오 테스트에서 발견한, 인프라 장애 대응에 관한 앞선 결정들
- [production-readiness.md](../../product/production-readiness.md) B-2(Rate Limiting 인메모리 한계, 이미 알려진 단순화)
