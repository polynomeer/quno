# ADR-0065: `@Cacheable` 캐시마다 값 타입을 지정한 직렬화기를 쓴다

- 날짜: 2026-10-13
- 상태: 승인됨 (ADR-0060 보완)

## 배경 (Context)

ADR-0060에서 대시보드 인기 질문·트렌드 태그와 스파이크 감지를 `@Cacheable`(Redis, TTL 60초)로 옮겼다. 직렬화기는 앱의 `ObjectMapper`를 넘긴 `GenericJacksonJsonRedisSerializer`였다. 그런데 이 `ObjectMapper`에는 default typing이 없어서, 캐시에 저장된 JSON에 클래스 정보가 남지 않는다.

그 결과 캐시 미스(첫 요청)는 정상이지만, TTL 안의 캐시 히트에서는 값이 다른 타입으로 복원됐다.
- `List<TagTrend>`, `List<TagSpike>` → `List<LinkedHashMap>`
- `List<Long>`의 작은 값 → `Integer`

이 값이 사용되는 지점에서 `ClassCastException`이 나 `GET /dashboard`와 `GET /flow`가 500을 냈다. 로그인 상태 화면용 axe e2e를 추가하다가, 홈이 스켈레톤에서 넘어가지 않는 것을 추적해 발견했다. 진단 과정은 [technical-deep-dives.md 14절](../../engineering/technical-deep-dives.md#14-캐시-히트에서만-깨지는-대시보드--타입-정보-없는-redis-직렬화)에 있다.

## 결정 (Decision)

- 캐시마다 반환 타입을 명시한 `JacksonJsonRedisSerializer(objectMapper, JavaType)`를 `RedisCacheManager.withInitialCacheConfigurations`로 등록한다(`CacheConfig.typedCaches()`).
  - `dashboard-popular-questions` → `List<java.lang.Long>`
  - `dashboard-trending-tags` → `List<TagTrend>`
  - `qunobot-tag-spikes` → `List<TagSpike>`
- 기본(`cacheDefaults`) 직렬화기는 그대로 둔다. 등록되지 않은 새 캐시가 생겨도 기존 동작에서 더 나빠지지 않게 하기 위해서다. 새 `@Cacheable`을 추가할 때는 `typedCaches()`에 값 타입을 등록하는 것을 규칙으로 한다.
- 기각한 대안: `ObjectMapper`에 default typing(`@class` 저장)을 켜는 방식.
  - 다형 역직렬화는 신뢰하지 않는 데이터가 들어올 때 공격면이 된다.
  - Kotlin data class는 final이라 `NON_FINAL` 설정으로는 적용되지 않는다. `EVERYTHING`이 필요한데, 위험이 더 크다.
  - 앱 전역 `ObjectMapper`를 바꾸면 HTTP 응답 직렬화에도 영향이 간다.

## 결과 (Consequences)

- 캐시 히트도 캐시 미스와 같은 타입을 돌려준다. 저장 JSON에 클래스 정보가 없어 Redis에서 사람이 읽기도 쉽다.
- `CacheSerializationRoundTripIntegrationTest`가 실제 Redis로 세 캐시의 넣기/꺼내기 왕복을 검증한다. 수정 전 코드에서는 3개 모두 실패하는 것을 확인했다.
- 캐시 쓰기가 바로 직후의 읽기보다 늦게 Redis에 도착할 수 있다(MONITOR로 관찰). 그래서 테스트는 값이 나타날 때까지 잠깐 기다린다. `@Cacheable` 운영 동작에는 영향이 없다.
- 배포 직후 TTL(60초) 동안 남아 있는 옛 형식 항목도 같은 JSON 구조라 그대로 읽힌다(클래스 정보가 원래 없었으므로).

## 관련 문서

- [ADR-0060](0060-declarative-cache-for-value-object-caches.md), [ADR-0056](0056-redis-cache-aside-graceful-degradation.md)
- [technical-deep-dives.md 14절](../../engineering/technical-deep-dives.md)
