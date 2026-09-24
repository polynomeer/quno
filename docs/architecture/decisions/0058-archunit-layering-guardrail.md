# ADR-0058: ArchUnit으로 DDD 계층 규칙을 테스트로 강제한다

- 날짜: 2026-09-24
- 상태: 승인됨

## 배경 (Context)

[system-architecture.md](../system-architecture.md)의 "계층 규칙" 표는 `domain`(엔티티·정책, Spring/JPA 의존 없이 순수 Kotlin, Repository는 포트만 정의) → `application`(유스케이스, 트랜잭션 경계) → `infrastructure`(JPA/Mongo/Redis 어댑터) / `interfaces`(컨트롤러)의 의존 방향을 문서로 명시하고 있다. 하지만 이걸 지키도록 강제하는 장치는 전혀 없었다 — 코드 리뷰(사람의 주의력)에만 의존하는 규칙이었다.

"백엔드 코드 아키텍처를 개선해달라"는 요청을 받고 실제로 레이어링이 지켜지고 있는지부터 확인했다: `domain`이 `infrastructure`/`interfaces`를 import하는 사례, `domain`이 `org.springframework`/`jakarta.persistence`를 import하는 사례, `application`이 `interfaces`나 `infrastructure`를 import하는 사례를 grep으로 전수 조사한 결과 **위반은 0건**이었다. 즉 지금 시점의 코드는 문서의 규칙을 정확히 지키고 있다 — 하지만 강제 장치가 없으니, 다음 기여자(또는 미래의 나 자신)가 실수로 어기더라도 코드 리뷰에서 놓치면 그대로 머지될 수 있는 상태였다.

한 가지 예외를 발견했다: `infrastructure/messaging/OutboxDispatchScheduler`, `infrastructure/websocket/*`, `infrastructure/security/Jwt*` 등 9개 파일이 `application` 패키지(유스케이스)를 직접 import하고 있었다. 이건 위반이 아니라 의도된 구조다 — HTTP가 아닌 진입점(스케줄러, WebSocket, 시큐리티 필터)이 `interfaces/api`의 컨트롤러와 같은 역할("driving adapter")을 infrastructure 안에서 수행하며 유스케이스를 직접 호출하는 패턴이다. 이 예외를 규칙에 반영하지 않으면 정상적인 코드가 위반으로 잡힌다.

## 결정 (Decision)

`com.tngtech.archunit:archunit-junit5:1.5.0`을 테스트 의존성으로 추가하고, `src/test/kotlin/com/quno/qunobackend/architecture/LayeringArchitectureTest.kt`에 세 가지 규칙을 코드로 고정한다:

1. **계층 의존 방향**: `Domain`은 어떤 레이어도 참조할 수 없고, `Application`은 `Domain`만, `Interfaces`는 `Application`/`Domain`만 참조할 수 있다. `Infrastructure`는 `Domain`(포트 구현)과 `Application`(driving adapter의 유스케이스 호출) 둘 다 참조할 수 있게 허용한다 — 위에서 확인한 의도된 예외를 규칙 자체에 반영했다.
2. **도메인 순수성**: `domain` 패키지의 클래스는 `org.springframework.*`/`jakarta.persistence.*`에 의존할 수 없다.
3. **Repository는 포트만**: `domain` 패키지에서 이름이 `Repository`로 끝나는 타입은 반드시 인터페이스여야 한다(구현체가 아니라 포트).

테스트 클래스는 `ClassFileImporter().withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)`로 프로덕션 클래스만 스캔한다 — 테스트 소스셋의 mock/fake 구현체(예: `application.organization.usecase` 아래의 `InMemoryOrganizationMembershipRepository`)까지 규칙 대상에 넣으면 테스트 전용 코드의 자유도가 부당하게 줄어든다.

도입 검증 방법으로, 실제로 위반을 만들어봤다 — `domain.common.OutboxEventTypes.kt`에 `infrastructure.messaging.OutboxDispatchScheduler` 타입의 필드를 임시로 추가하고 테스트를 돌리자 정확히 그 파일·그 필드를 지목하는 `AssertionError`로 실패했고(다른 두 규칙은 영향받지 않고 그대로 통과), 되돌리자 다시 3개 전부 통과했다. 즉 이 가드레일이 실제로 작동함을 확인한 뒤에 도입했다.

## 결과 (Consequences)

- 앞으로 레이어링을 어기는 커밋은 `./gradlew test`(또는 `./gradlew build`) 단계에서 바로 실패한다 — 코드 리뷰에서 놓쳐도 빌드가 잡아준다.
- `domain` 패키지에 실수로 Spring 애노테이션이나 JPA 의존성을 추가하면(예: 도메인 객체에 `@Entity`를 직접 붙이는 것) 이 테스트가 즉시 알려준다.
- 규칙 자체를 바꿔야 할 때(예: 새로운 종류의 driving adapter가 다른 레이어를 참조해야 하는 경우)는 `LayeringArchitectureTest.kt`의 `whereLayer(...)` 절을 수정해야 한다 — 이 결정을 내릴 때는 왜 예외가 필요한지 이 ADR을 갱신하거나 새 ADR로 남긴다.
- 테스트 스위트가 3개 늘어(348→351) 빌드 시간이 약간 증가하지만(전체 스위트 기준 체감 오차 범위 내), 아키텍처 회귀를 잡는 이득이 훨씬 크다고 판단했다.

## 관련 문서

- [system-architecture.md](../system-architecture.md) "계층 규칙" 표 — 이 테스트가 코드로 강제하는 원본 규칙
- [backend/src/test/kotlin/com/quno/qunobackend/architecture/LayeringArchitectureTest.kt](../../../backend/src/test/kotlin/com/quno/qunobackend/architecture/LayeringArchitectureTest.kt)
- [docs/engineering/backend-architecture-improvements.md](../../engineering/backend-architecture-improvements.md) — 이 개선을 어떻게 진행했는지의 상세 과정 기록
