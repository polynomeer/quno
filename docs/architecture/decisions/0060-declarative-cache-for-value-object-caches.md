# ADR-0060: 값 객체를 캐싱하는 곳은 `@Cacheable`로, 도메인 애그리거트를 캐싱하는 곳은 수동 cache-aside로 남긴다

- 날짜: 2026-09-24
- 상태: 승인됨

## 배경 (Context)

[ADR-0059](0059-archunit-layering-guardrail.md)에 이어 백엔드 아키텍처를 계속 개선하면서, [ADR-0056](0056-redis-cache-aside-graceful-degradation.md)이 도입한 `safeCacheGet`/`safeCacheSet` 패턴이 `OrganizationRepositoryAdapter`/`DashboardRepositoryAdapter`/`SpikeDetectionRepositoryAdapter` 세 곳에 거의 동일한 형태로 반복되고 있는 것을 다음 개선 후보로 다뤘다(자세한 조사 과정은 [backend-architecture-improvements.md](../../engineering/backend-architecture-improvements.md) 2절). Spring의 `@Cacheable`/`@CacheEvict` 선언적 캐싱으로 옮기면 이 반복(키 조합, `objectMapper.writeValueAsString`/`readValue`, TTL 상수)을 어노테이션 한 줄로 줄일 수 있다는 게 출발점이었다.

바로 전체를 옮기기 전에 실제로 안전한지부터 조사했다. 핵심 발견 두 가지:

1. **캐싱 자동설정이 이 프로젝트에 전혀 없었다.** `@EnableCaching`도, `CacheErrorHandler` 빈도, `spring-boot-starter-cache` 의존성도 없었다. 게다가 이 프로젝트는 Jackson 3(`tools.jackson.*`)를 쓰는데, `spring-data-redis`가 전이 의존성으로 Jackson 2(`com.fasterxml.jackson.*`)도 classpath에 끌어와 있어, Spring Boot의 Redis 캐시 자동설정에 그대로 맡기면 어느 `ObjectMapper`로 직렬화기를 만드는지 확신할 수 없는 상태였다.
2. **`OrganizationRepositoryAdapter`만 캐싱 대상이 도메인 애그리거트였다.** `DashboardRepositoryAdapter`/`SpikeDetectionRepositoryAdapter`가 캐싱하는 `TagTrend`/`TagSpike`/`List<Long>`은 전부 public 생성자를 가진 순수 데이터 클래스(값 객체)라 Jackson이 있는 그대로 직렬화/역직렬화할 수 있다. 반면 `Organization`은 `private constructor` + `reconstitute()` 팩토리로 캡슐화된 애그리거트라, `OrganizationRepositoryAdapter`는 이미 `CachedOrganization`이라는 별도 평범한 데이터 클래스로 우회해서 캐싱하고 있었다 — `@Cacheable`로 옮겨도 이 우회 자체는 그대로 필요해, 선언적 캐싱의 이점(어노테이션 한 줄로 끝)이 크게 줄어든다.

## 결정 (Decision)

**전부 옮기지 않고 절반만 옮긴다.**

1. `infrastructure/config/CacheConfig.kt`를 새로 만들어 `@EnableCaching` + 이 앱이 이미 쓰는 Jackson 3 `ObjectMapper` 빈을 `GenericJacksonJsonRedisSerializer`에 직접 넘기는 `RedisCacheManager`를 수동으로 구성한다 — Spring Boot의 Redis 캐시 자동설정(`spring-boot-starter-cache`)에 의존하지 않아, 어느 `ObjectMapper`가 선택될지 모호했던 문제를 원천적으로 없앤다. `CachingConfigurer.errorHandler()`를 오버라이드해 Redis 장애 시 캐시 get/put을 조용히 무시하는 `LoggingCacheErrorHandler`를 등록한다 — ADR-0056의 정책(캐시 실패는 곧 캐시 미스, 요청을 죽이지 않음)을 `safeCacheGet`/`safeCacheSet`을 어댑터마다 호출하는 대신 프레임워크 레벨에서 한 번에 적용한다.
2. `DashboardRepositoryAdapter.findPopularQuestionIds`/`findTrendingTags`, `SpikeDetectionRepositoryAdapter.findSpikingTags`를 `@Cacheable(cacheNames = [...], key = "#limit")`로 옮기고, `StringRedisTemplate`/`ObjectMapper` 의존성과 수동 캐시 코드를 전부 제거한다.
3. `OrganizationRepositoryAdapter`는 손대지 않는다 — `safeCacheGet`/`safeCacheSet` 기반 수동 cache-aside를 그대로 유지한다. 도메인 애그리거트를 캐싱해야 하는 한 어차피 DTO 변환 코드가 필요하므로, 지금 굳이 옮길 이유가 없다.

캐시 이름은 기존 Redis 키(`dashboard:popular-questions:5` 같은 콜론 구분 문자열)에서 Spring Cache의 기본 형식(`dashboard-popular-questions::5`)으로 바뀐다 — 이 캐시들은 TTL 60초짜리 순수 파생 데이터라 다른 소비자가 키 형식에 의존하지 않고, 기존 키는 그냥 자연 만료되므로 마이그레이션이 필요 없다.

## 검증

로컬에서 실제로 확인했다: 첫 호출 후 `redis-cli KEYS`로 `dashboard-popular-questions::5` 등 3개 캐시 이름의 키가 생성됨을 확인, `TTL`이 60초에서 시작해 감소하는 것과 두 번째 호출이 그 TTL을 리셋하지 않는 것(진짜 캐시 히트)을 확인, `GET`으로 저장된 JSON이 타입 메타데이터 없이 깨끗한 형태임을 확인. `docker compose stop redis`로 장애를 재현하자 `LoggingCacheErrorHandler`가 캐시별로 경고 로그를 남기고 대시보드/스파이크 감지 API가 여전히 `HTTP:200`으로 DB 직접 조회 결과를 반환함을 확인해, ADR-0056이 보장하던 동작이 그대로 유지됨을 검증했다. `./gradlew build`(테스트 351개 + ktlint) 전부 통과.

## 결과 (Consequences)

- `DashboardRepositoryAdapter`/`SpikeDetectionRepositoryAdapter`에서 캐시 키 조합·직렬화·TTL 상수·Redis 장애 처리 코드가 전부 사라지고 어노테이션 한 줄만 남았다.
- `OrganizationRepositoryAdapter`는 여전히 ADR-0056 방식이라, 이 저장소에 캐싱 패턴이 두 가지(선언적/수동) 공존한다 — 새로 캐시를 추가하는 사람은 "캐싱 대상이 순수 값 객체인가, 아니면 private 생성자를 가진 도메인 애그리거트인가"로 어느 쪽을 쓸지 판단해야 한다. 이 판단 기준 자체가 이 ADR의 핵심 산출물이다.
- `spring-boot-starter-cache`를 추가하지 않았다 — `@Cacheable`/`@EnableCaching`/`CachingConfigurer`는 `spring-context`에, `RedisCacheManager`는 `spring-data-redis`에 이미 포함돼 있어 별도 의존성 없이 구현했다. 나중에 Boot의 Redis 캐시 자동설정을 쓰고 싶어지면, 그때 Jackson 2/3 직렬화기 선택 문제를 다시 검증해야 한다.

## 관련 문서

- [ADR-0056](0056-redis-cache-aside-graceful-degradation.md) — 이 결정이 부분적으로 대체하는 원래 결정
- [ADR-0059](0059-archunit-layering-guardrail.md) — 같은 "백엔드 아키텍처 개선" 흐름의 앞선 결정
- [docs/engineering/backend-architecture-improvements.md](../../engineering/backend-architecture-improvements.md) 2절 — 조사부터 검증까지의 상세 과정
