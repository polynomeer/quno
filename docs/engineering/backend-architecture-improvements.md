# 백엔드 아키텍처 개선 기록

[technical-deep-dives.md](technical-deep-dives.md)가 "이미 벌어진 버그를 어떻게 진단했는가"를 기록한다면, 이 문서는 "버그가 아직 없는 상태에서 아키텍처를 어떻게 더 단단하게 만들었는가"를 기록한다. 형식은 개선마다 배경 → 조사 → 설계 → 구현 → 검증 → 결과 순서를 따른다. ADR이 "무엇을 결정했는가"를 남긴다면, 이 문서는 "그 결정에 이르기까지 실제로 어떤 조사를 했고 어떤 대안을 왜 버렸는가"라는 과정 자체를 남긴다.

## 목차

- [1. DDD 계층 규칙을 ArchUnit으로 강제 (2026-09-24)](#1-ddd-계층-규칙을-archunit으로-강제-2026-09-24)

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

- **Redis cache-aside 코드 중복** — `OrganizationRepositoryAdapter`/`DashboardRepositoryAdapter`/`SpikeDetectionRepositoryAdapter` 세 곳이 거의 동일한 캐시 read/write 코드를 반복한다(이미 `safeCacheGet`/`safeCacheSet` 확장 함수로 한 번 정리됨, [ADR-0056](../architecture/decisions/0056-redis-cache-aside-graceful-degradation.md)). Spring `@Cacheable`/`@CacheEvict` + 커스텀 `CacheErrorHandler`로 더 줄일 수 있지만, 캐시 키/TTL을 어노테이션으로 옮기면서 제어권이 줄어드는 트레이드오프가 있어 이번 범위에서는 보류했다.
- **모듈 분리** — 지금은 단일 Gradle 모듈인데, ArchUnit으로 레이어 규칙을 강제하기 시작했으니 향후 실제로 규모가 커지면 `domain`/`application`을 별도 Gradle 모듈로 물리적으로 분리하는 것도 검토할 수 있다. 지금 시점에는 코드량 대비 과한 조치로 판단해 시도하지 않았다.

## 관련 문서

- [system-architecture.md](../architecture/system-architecture.md) — 이 문서가 강제하는 계층 규칙의 원본
- [ADR 목록](../architecture/decisions/README.md)
- [technical-deep-dives.md](technical-deep-dives.md) — 같은 형식(배경→조사→해결→검증)을 버그 진단에 적용한 자매 문서
