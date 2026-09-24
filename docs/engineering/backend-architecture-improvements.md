# 백엔드 아키텍처 개선 기록

[technical-deep-dives.md](technical-deep-dives.md)가 "이미 벌어진 버그를 어떻게 진단했는가"를 기록한다면, 이 문서는 "버그가 아직 없는 상태에서 아키텍처를 어떻게 더 단단하게 만들었는가"를 기록한다. 형식은 개선마다 배경 → 조사 → 설계 → 구현 → 검증 → 결과 순서를 따른다. ADR이 "무엇을 결정했는가"를 남긴다면, 이 문서는 "그 결정에 이르기까지 실제로 어떤 조사를 했고 어떤 대안을 왜 버렸는가"라는 과정 자체를 남긴다.

## 목차

- [1. DDD 계층 규칙을 ArchUnit으로 강제 (2026-09-24)](#1-ddd-계층-규칙을-archunit으로-강제-2026-09-24)
- [2. Redis 캐시-aside를 선언적 캐싱으로 부분 전환 (2026-09-24)](#2-redis-캐시-aside를-선언적-캐싱으로-부분-전환-2026-09-24)

## 1. DDD 계층 규칙을 ArchUnit으로 강제 (2026-09-24)

### 배경

"백엔드 애플리케이션 코드 아키텍처를 개선해달라"는 요청을 받았다. 이 저장소는 단일 Gradle 모듈 안에서 `domain`/`application`/`infrastructure`/`interfaces` 네 개 패키지로 DDD 레이어링을 하고 있고, [system-architecture.md](../architecture/system-architecture.md)에 그 계층별 책임과 의존 방향이 표로 명시돼 있다. 문제는 "명시돼 있다"는 것과 "실제로 지켜지고 있다"는 것, 그리고 "지켜지지 않게 됐을 때 알아챌 수 있다"는 것이 전부 다른 문제라는 점이었다 — 이 세 가지 중 세 번째가 완전히 비어 있었다.

### 조사

바로 코드를 바꾸는 대신, 먼저 "지금 실제로 지켜지고 있는가"를 확인했다. Explore 서브에이전트로 패키지 구조 전수 조사를 시키고, 이어서 직접 `grep`으로 네 가지 의존 방향을 확인했다:

| 검사 | 명령 | 결과 |
|---|---|---|
| `domain` → `infrastructure` | `grep -rn "^import com.quno.qunobackend.infrastructure" .../domain/` | 0건 |
| `domain` → Spring/JPA | `grep -rln "^import org.springframework\|^import jakarta.persistence" .../domain/` | 0건 |
| `application` → `interfaces` | `grep -rn "^import com.quno.qunobackend.interfaces" .../application/` | 0건 |
| `application` → `infrastructure` | `grep -rn "^import com.quno.qunobackend.infrastructure" .../application/` | 0건 |
| `interfaces` → `infrastructure` | `grep -rn "^import com.quno.qunobackend.infrastructure" .../interfaces/` | 0건 |
| `domain`의 `Repository` 타입이 전부 `interface`인지 | `grep -rn "class.*Repository\b" .../domain/ \| grep -v interface` | 0건(37개 전부 interface) |

전부 위반 없음 — 규칙은 실제로 지켜지고 있었다. 다만 한 가지 방향은 확인해보니 "위반처럼 보이지만 의도된 것"이었다: `infrastructure` → `application` 방향으로 9개 파일이 걸렸다.

```
infrastructure/websocket/PresenceEventListener.kt
infrastructure/websocket/StompAuthChannelInterceptor.kt
infrastructure/websocket/LiveChatWebSocketController.kt
infrastructure/config/SecurityConfig.kt
infrastructure/security/JwtTokenProvider.kt
infrastructure/security/JwtAuthenticationFilter.kt
infrastructure/observability/MetricsLoggingScheduler.kt
infrastructure/qunobot/TechnologyVersionScanScheduler.kt
infrastructure/messaging/OutboxDispatchScheduler.kt
```

전부 HTTP 컨트롤러가 아닌 진입점(스케줄러, WebSocket 핸들러, 시큐리티 필터)이었다. 이들은 `interfaces/api`의 컨트롤러와 똑같은 역할 — 외부 이벤트를 받아 유스케이스를 호출하는 "driving adapter" — 을 수행하는데, 그 위치가 HTTP 요청이 아니라 스케줄/메시지/WebSocket 프레임이라 `infrastructure` 안에 있을 뿐이었다(예: [OutboxDispatchScheduler](../../backend/src/main/kotlin/com/quno/qunobackend/infrastructure/messaging/OutboxDispatchScheduler.kt)의 `@Scheduled fun dispatch() { dispatchOutboxEventsUseCase.execute() }`). 이건 헥사고날 아키텍처에서 흔한 패턴이고, 이 프로젝트에서 이미 여러 곳에 일관되게 적용돼 있었다 — "위반"이 아니라 "규칙에 아직 반영 안 된 예외"였다.

이 구분이 중요했던 이유: 만약 이걸 무시하고 "infrastructure는 domain만 참조 가능"으로 규칙을 짰다면, 정상적으로 동작하는 기존 코드 9곳이 전부 새 테스트에서 실패했을 것이다. 실제 아키텍처를 먼저 읽지 않고 문서의 이상형만 보고 규칙을 짰다면 저지를 뻔한 실수였다.

### 설계

강제 수단으로 ArchUnit(`com.tngtech.archunit:archunit-junit5`)을 선택했다. 대안으로 Kotlin 컴파일러 플러그인이나 커스텀 Gradle 태스크도 있었지만, ArchUnit은 "패키지 의존 방향을 선언적으로 표현하고 실패 시 정확히 어느 클래스의 어느 필드가 어긴 건지 알려주는" JUnit 5 통합 테스트로 붙일 수 있어 이 저장소의 기존 테스트 파이프라인(`./gradlew test`)에 자연스럽게 들어간다. 버전은 로컬 Gradle 캐시에 이미 받아져 있던 `1.5.0`(다른 프로젝트에서 내려받은 것)을 그대로 썼다 — 별도 버전 조사 없이 즉시 검증 가능했다.

규칙 세 가지로 정리했다:

1. **계층 의존 방향** — `layeredArchitecture()` DSL로: `Domain`은 아무 레이어도 참조 불가, `Application`은 `Domain`만, `Interfaces`는 `Application`+`Domain`만, `Infrastructure`는 `Domain`+`Application`(위에서 확인한 driving adapter 예외 반영) 참조 가능.
2. **도메인 순수성** — `noClasses().that().resideInAPackage("..domain..").should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta.persistence..")`.
3. **Repository는 포트만** — `classes().that().resideInAPackage("..domain..").and().haveSimpleNameEndingWith("Repository").should().beInterfaces()`.

테스트 클래스가 스캔할 클래스 범위도 고민이 필요했다: `ClassFileImporter().importPackages("com.quno.qunobackend")`을 기본값 그대로 쓰면 테스트 소스셋(`src/test/kotlin`)의 mock/fake 구현체도 같이 스캔 대상에 들어간다 — 예를 들어 `application.organization.usecase` 테스트 패키지 아래에 있는 `InMemoryOrganizationMembershipRepository` 같은 테스트 전용 클래스까지 "Repository는 인터페이스여야 한다" 규칙에 걸릴 이유가 없다. `ImportOption.Predefined.DO_NOT_INCLUDE_TESTS`로 프로덕션 클래스만 스캔하도록 명시했다.

### 구현

- `backend/build.gradle.kts`에 `testImplementation("com.tngtech.archunit:archunit-junit5:1.5.0")` 한 줄 추가.
- `backend/src/test/kotlin/com/quno/qunobackend/architecture/LayeringArchitectureTest.kt` 신규 작성(테스트 3개).

### 검증

**1) 정상 상태에서 통과 확인.** `./gradlew test --tests "...LayeringArchitectureTest"` → `BUILD SUCCESSFUL`, `tests="3" failures="0"`.

**2) 이 가드레일이 실제로 위반을 잡아내는지 스스로 증명.** 검증 없이 "통과했다"만으로는 이 테스트가 정말 뭔가를 검사하고 있는지, 아니면 조건을 잘못 짜서 항상 통과하는 죽은 테스트인지 구분할 수 없다. 그래서 의도적으로 위반을 하나 심었다 — `domain/common/OutboxEventTypes.kt`에 `infrastructure.messaging.OutboxDispatchScheduler` 타입을 참조하는 필드를 임시로 추가:

```kotlin
import com.quno.qunobackend.infrastructure.messaging.OutboxDispatchScheduler
private val deliberateViolationForArchUnitVerification: OutboxDispatchScheduler? = null
```

다시 테스트를 돌리자 정확히 이 위반만 잡혔다:

```
LayeringArchitectureTest > layers only depend in the documented direction() FAILED
    java.lang.AssertionError: ... Field <com.quno.qunobackend.domain.common.OutboxEventTypesKt.
    deliberateViolationForArchUnitVerification> has type
    <com.quno.qunobackend.infrastructure.messaging.OutboxDispatchScheduler> in (OutboxEventTypes.kt:0)
3 tests completed, 1 failed
```

나머지 두 규칙(도메인 순수성, Repository 인터페이스)은 이 위반과 무관해 그대로 통과했다 — 세 규칙이 서로 독립적으로 정확한 대상만 검사한다는 것도 같이 확인됐다. `git checkout`으로 되돌린 뒤 재실행해 다시 3개 전부 통과함을 확인했다.

**3) 전체 빌드 회귀 확인.** `./gradlew build`(테스트 351개 + ktlint + jacoco 전부 포함) → `BUILD SUCCESSFUL`. 이 과정에서 `./gradlew ktlintCheck`가 이번 변경과 무관한 기존 파일([InMemoryOrganizationMembershipRepository.kt](../../backend/src/test/kotlin/com/quno/qunobackend/application/organization/usecase/InMemoryOrganizationMembershipRepository.kt))에서 스타일 위반 1건을 잡아냈다 — `git log`로 확인해보니 이번 세션과 무관한 예전 커밋(`24763b5`)부터 있던 것이었다. 별도 이슈로 미루지 않고 `./gradlew ktlintFormat`으로 그 자리에서 같이 고쳤다(공교롭게도 어제 세션에서 새로 만든 `MongoConfig.kt`도 같은 스타일 규칙에 걸려 있어서 같이 정리됐다).

### 결과

- 테스트 3개 신규 추가(348→351), 전부 통과.
- 앞으로 레이어링을 어기는 코드는 `./gradlew test` 단계에서 특정 클래스·필드까지 정확히 짚어주는 에러로 즉시 드러난다.
- 결정 자체는 [ADR-0058](../architecture/decisions/0058-archunit-layering-guardrail.md)에 남겼다.

### 다루지 않은 것 / 다음 후보

같은 세션에서 함께 검토했지만 이번엔 건드리지 않은 것들:

- **Redis cache-aside 코드 중복** — `OrganizationRepositoryAdapter`/`DashboardRepositoryAdapter`/`SpikeDetectionRepositoryAdapter` 세 곳이 거의 동일한 캐시 read/write 코드를 반복한다(이미 `safeCacheGet`/`safeCacheSet` 확장 함수로 한 번 정리됨, [ADR-0056](../architecture/decisions/0056-redis-cache-aside-graceful-degradation.md)). **후속: [2절](#2-redis-캐시-aside를-선언적-캐싱으로-부분-전환-2026-09-24)에서 실제로 조사하고 절반만 옮겼다.**
- **모듈 분리** — 지금은 단일 Gradle 모듈인데, ArchUnit으로 레이어 규칙을 강제하기 시작했으니 향후 실제로 규모가 커지면 `domain`/`application`을 별도 Gradle 모듈로 물리적으로 분리하는 것도 검토할 수 있다. 지금 시점에는 코드량 대비 과한 조치로 판단해 시도하지 않았다.

## 2. Redis 캐시-aside를 선언적 캐싱으로 부분 전환 (2026-09-24)

### 배경

1절에서 "다음 후보"로만 남겨뒀던 Redis cache-aside 코드 중복을 이어서 다뤘다. `OrganizationRepositoryAdapter`/`DashboardRepositoryAdapter`/`SpikeDetectionRepositoryAdapter` 세 곳이 `StringRedisTemplate` + 수동 JSON 직렬화 + `safeCacheGet`/`safeCacheSet`([ADR-0056](../architecture/decisions/0056-redis-cache-aside-graceful-degradation.md))으로 거의 동일한 캐시 코드를 반복하고 있었다. Spring의 `@Cacheable`/`@CacheEvict` 선언적 캐싱으로 옮기면 이 반복을 어노테이션 한 줄로 줄일 수 있어 보였다.

### 조사

바로 세 곳을 다 옮기지 않고, Explore 서브에이전트로 타당성부터 조사했다. 조사에서 두 가지 위험 요소가 나왔다:

1. **캐싱 자동설정 자체가 없었다.** `@EnableCaching`도, `CacheErrorHandler` 빈도, `spring-boot-starter-cache` 의존성도 이 프로젝트에 없었다. 게다가 앱 코드는 Jackson 3(`tools.jackson.*`)만 쓰는데, `./gradlew dependencies`로 확인한 실제 classpath에는 `spring-data-redis`가 전이 의존성으로 끌어온 Jackson 2(`com.fasterxml.jackson.core:jackson-databind:2.21.5`)도 함께 있었다. Spring Boot의 Redis 캐시 자동설정에 그대로 맡기면 어느 `ObjectMapper`로 직렬화기를 만드는지 확신할 수 없는 상태였다 — `TossPaymentGateway.kt`의 기존 주석에도 이 팀이 예전에 Jackson 2/3 혼재로 한 번 걸려 넘어진(`RestClient.builder()`가 Jackson 3 컨버터를 못 찾아 요청 바디가 `{}`로 직렬화된) 전례가 적혀 있어, 가볍게 넘길 문제가 아니었다.
2. **`Organization`만 도메인 애그리거트였다.** `TagTrend`/`TagSpike`는 전부 public 생성자를 가진 순수 `data class`라 Jackson이 그대로 직렬화/역직렬화할 수 있다. `Organization`은 `private constructor` + `reconstitute()`라 이미 `CachedOrganization`이라는 우회 DTO를 쓰고 있었다 — `@Cacheable`로 옮겨도 이 우회는 그대로 필요해 선언적 캐싱의 이점이 줄어든다.

이 두 발견을 근거로 "지금 전부 옮기는 건 권장하지 않는다"는 게 조사의 1차 결론이었다. 하지만 (2)를 뜯어보니 위험은 `Organization` 한 곳에만 있고, `Dashboard`/`SpikeDetection`은 애초에 도메인 애그리거트를 캐싱하는 게 아니라서 그 위험이 적용되지 않았다. 그래서 "전부 하거나 전부 안 하거나"가 아니라 "위험이 없는 절반만 먼저 한다"로 범위를 좁혔다.

(1)의 Jackson 버전 불확실성도 직접 해소했다: `spring-data-redis-4.0.7.jar`를 `javap`으로 까 보니 `GenericJacksonJsonRedisSerializer`라는 새 클래스가 이미 있고, 그 생성자가 정확히 `tools.jackson.databind.ObjectMapper`를 받는다는 것을 바이트코드로 직접 확인했다 — 자동설정에 의존해 어느 `ObjectMapper`가 선택될지 추측하는 대신, 이 클래스에 앱이 이미 쓰는 `ObjectMapper` 빈을 내가 직접 넘기면 모호함 자체가 사라진다는 걸 알았다.

### 설계

1. `infrastructure/config/CacheConfig.kt`: `@EnableCaching` + `CachingConfigurer`를 구현하는 설정 클래스. `RedisCacheManager`는 Boot 자동설정을 쓰지 않고 직접 빌드한다 — `RedisCacheConfiguration.defaultCacheConfig().entryTtl(60s).serializeValuesWith(GenericJacksonJsonRedisSerializer(앱의 ObjectMapper 빈))`. `errorHandler()`는 `LoggingCacheErrorHandler`를 반환해 Redis 장애 시 캐시 get/put 예외를 로그만 남기고 삼킨다 — ADR-0056의 정책을 프레임워크 레벨 하나로 통합.
2. `spring-boot-starter-cache`는 추가하지 않는다 — `@Cacheable`/`@EnableCaching`/`CachingConfigurer`는 `spring-context`에, `RedisCacheManager`는 `spring-data-redis`에 이미 있어서 내가 `CacheManager`를 직접 빈으로 등록하면 Boot의 캐시 자동설정 경로 자체를 타지 않는다. 즉 Jackson 버전 문제를 "해결"한 게 아니라 "그 경로를 안 쓰기로" 우회했다.
3. `Dashboard`/`SpikeDetection`의 세 메서드에 `@Cacheable(cacheNames = [...], key = "#limit")`을 붙이고 수동 캐시 코드·`StringRedisTemplate`/`ObjectMapper` 의존성을 제거한다.
4. `Organization`은 손대지 않는다.

### 구현

- `CacheConfig.kt` 신규 작성.
- `DashboardRepositoryAdapter.kt`: 생성자에서 `redisTemplate`/`objectMapper` 제거, 두 메서드에 `@Cacheable` 부착, 캐시 키 상수·`safeCacheGet`/`safeCacheSet` 호출 제거.
- `SpikeDetectionRepositoryAdapter.kt`: 동일하게 정리.
- `OrganizationRepositoryAdapter.kt`: 변경 없음.

### 검증

로컬 Redis/Postgres를 실제로 띄운 채 라이브로 확인했다(단위 테스트로는 "캐시가 실제로 붙었는지"를 확인하기 어려워 의도적으로 실제 서버+`redis-cli`로 검증):

1. **캐시 생성 확인.** `GET /api/v1/dashboard` 첫 호출 후 `redis-cli KEYS "dashboard*"` → `dashboard-popular-questions::5`, `dashboard-trending-tags::10` 등 생성 확인. `TTL` 조회 결과 45(60에서 시작해 감소 중).
2. **캐시 히트 확인.** 두 번째 호출 후 `TTL`이 35로 계속 줄어들고 있어(리셋되지 않음) 두 번째 호출이 재계산·재저장 없이 캐시를 그대로 읽었음을 확인. 응답 시간도 0.557s → 0.131s로 감소.
3. **직렬화 형태 확인.** `redis-cli GET "dashboard-trending-tags::10"`으로 저장된 값을 직접 확인 — 타입 메타데이터(`@class`) 없이 깨끗한 JSON 배열이었다.
4. **Redis 장애 시 폴백 확인(ADR-0056이 보장하던 것).** `docker compose stop redis` 후 같은 엔드포인트를 다시 호출 — `HTTP:200`, 62ms, 데이터 정상(인기 질문 5건·트렌딩 태그 7건 그대로). 백엔드 로그에 `LoggingCacheErrorHandler`가 캐시별로 `Redis unavailable, skipping cache read for cache=... key=...` 경고를 남긴 것도 확인해, 실제로 이 에러 핸들러 경로를 탔음을(우연히 다른 이유로 통과한 게 아님을) 검증했다.
5. **회귀 없음 확인.** `docker compose start redis`로 복구한 뒤 대시보드·조직 검색(수정 안 한 경로) 둘 다 정상. `./gradlew build`(테스트 351개 + ktlint) `BUILD SUCCESSFUL`. 이 과정에서 새로 쓴 `DashboardRepositoryAdapter.kt`가 ktlint의 `function-signature` 규칙에 걸려(1절 검증 때 고쳤던 것과 같은 종류) `ktlintFormat`으로 그 자리에서 정리했다.

### 결과

- `Dashboard`/`SpikeDetection` 세 캐시 메서드에서 캐시 키 조합·직렬화·TTL 상수·Redis 장애 처리 코드가 전부 사라지고 어노테이션 한 줄만 남았다.
- `Organization`은 여전히 수동 방식이라 두 가지 캐싱 패턴이 공존한다 — "캐싱 대상이 순수 값 객체인가 도메인 애그리거트인가"가 어느 쪽을 쓸지 가르는 기준이다.
- 결정 자체는 [ADR-0059](../architecture/decisions/0059-declarative-cache-for-value-object-caches.md)에 남겼고, [ADR-0056](../architecture/decisions/0056-redis-cache-aside-graceful-degradation.md)은 "부분 대체됨"으로 갱신했다.

### 다루지 않은 것 / 다음 후보

- **`Organization`도 언젠가 옮길 수 있는가** — `Organization.reconstitute()`가 `internal`이나 `companion object` 팩토리가 아니라 진짜 `private constructor`라, `CachedOrganization` 우회 자체를 없애려면 도메인 모델 쪽 캡슐화 정책을 다시 논의해야 한다. 이번 범위 밖.
- **`spring-boot-starter-cache` 자동설정 재검토** — 지금은 수동으로 `CacheManager`를 구성해 우회했지만, 캐시가 더 늘어나 설정이 복잡해지면 Boot 자동설정 + 명시적 `ObjectMapper` 커스터마이저 조합으로 다시 정리하는 것도 고려할 수 있다.

## 관련 문서

- [system-architecture.md](../architecture/system-architecture.md) — 이 문서가 강제하는 계층 규칙의 원본
- [ADR 목록](../architecture/decisions/README.md)
- [technical-deep-dives.md](technical-deep-dives.md) — 같은 형식(배경→조사→해결→검증)을 버그 진단에 적용한 자매 문서
