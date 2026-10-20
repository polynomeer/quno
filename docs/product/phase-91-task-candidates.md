# Phase 91 — 과제 후보 목록

[phase-91-validation-experiment.md](phase-91-validation-experiment.md) 2.2절의 과제 30개(A 최신 10, B 롱테일 10, C 통제군 10)를 고르기 위한 **후보 목록**이다. 조사일은 2026-10-19이고, 이슈 정보는 GitHub API로 직접 조회했다. 후보는 **확정이 아니다.** 확정 시 목록을 별도 커밋으로 고정하고(사전 등록), 그 뒤에 시드를 만든다.

## 1. 읽는 법

- **확인 수준**: `본문` = 이슈 본문을 읽고 에러·원인을 확인함 / `제목` = 제목과 메타데이터만 확인함(확정 전 본문 확인 필요). 해결 방법은 이슈에 적힌 것만 옮겼고, 적혀 있지 않으면 "확인 필요"로 표시했다.
- **형식**: `R` = 저장소와 명령만으로 재현(성공 판정 명령 있음) / `D` = Docker(Testcontainers·compose)가 필요한 R / `L` = 특정 호스트 환경에 의존해 저장소로는 재현할 수 없어, 캡처한 로그·환경 설명을 주고 진단을 요구하는 **로그 재생형**.
- **†** 표시는 이슈가 아니라 **수정 PR** 번호다. Spring Boot는 백포트 이슈가 수정 PR을 가리키는 경우가 있어, 이 항목들은 PR 본문(증상·원인·재현·검증이 적혀 있음)을 읽고 확인했다. 별도의 버그 보고 이슈가 있는지는 확정 전에 확인한다.
- **보고일**은 백포트·포워드 포팅이 아닌 가장 앞선 항목(이슈 또는 †수정 PR)의 생성일이다. 실험 대상 에이전트의 학습 시점 이후 지식인지 판단하는 기준이다.
- 이슈 번호는 `저장소#번호`. 링크는 `https://github.com/<저장소>/issues/<번호>`.
- 모든 과제는 Kotlin + Gradle + Spring 프로젝트로 다시 작성해 재현한다(원 이슈가 Java 코드여도 Kotlin으로 옮긴다).

## 2. 과제 요건과 선정 기준

1. 근본 원인이 하나이고, 보이는 증상(예외·잘못된 결과)이 있다.
2. **수정 전 버전을 고정한** 최소 프로젝트로 재현된다. 성공 판정은 명령 하나(테스트 통과, 예외 소멸, 출력 일치)다.
3. 알려진 해결(업그레이드 버전, 의존성 추가, 설정 변경)이 있다. 없으면 정답을 정할 수 없으므로 뺀다.
4. A층은 **보고일이 2026-07-19 이후**(최근 90일)다. 에이전트가 학습 시점 이후의 지식이 필요하도록 하기 위해서다.
5. 이 저장소가 쓰는 스택(Spring Boot 4.0.x, Kotlin)에 가까운 것을 우선한다.
6. 과제로 쓴 이슈의 해결 내용은 **시드 코퍼스에 넣지 않는다.** 시드는 과제 목록 확정 뒤에 별도 출처로 만든다.

## 3. A층 — 최신 (보고일 ≥ 2026-07-19), 10개 + 예비

Spring Boot 4.0.8(이 저장소의 버전) 주변에서 최근 3개월에 보고·수정된 실제 이슈다. 수정 버전을 알아야 풀리는 문제가 많아서, "업그레이드하면 해결"이 정답인 과제와 "설정·의존성으로 우회"가 정답인 과제를 섞는다.

| ID | 이슈 | 보고일 | 증상 | 영향·수정 | 형식 | 성공 판정 | 확인 |
|---|---|---|---|---|---|---|---|
| A01 | spring-boot#51872† | 09-28 | DevTools 사용 중 `DataSource`를 직접 구성하면 기동 실패. `NoClassDefFoundError: org/springframework/boot/jdbc/autoconfigure/DataSourceProperties` | 4.0부터 해당 클래스가 `spring-boot-jdbc`(devtools의 optional 의존성)로 이동. 수정 4.0.9·4.1.2. 우회: 해당 모듈 추가(이슈 본문 근거) | R | 앱 컨텍스트 기동 성공 | 본문 |
| A02 | spring-boot#52249† | 10-15 | `jwk-set-uri`만 쓰려고 `issuer-uri=${ISSUER_URI:}`로 두면 모든 JWT가 `The iss claim is not valid`로 거부 | 빈 문자열 `issuer-uri`에도 `JwtIssuerValidator`가 붙음. 수정 4.0.9 | R | 유효한 JWT로 보호된 엔드포인트가 200 | 본문 |
| A03 | spring-framework#37309 | 09-20 | `@CacheEvict`가 붙은 `Flux` 반환 메서드가 첫 요소만 방출 | Framework 7.0.9·Boot 4.0.8에서 확인. 수정 7.0.10 | R | `StepVerifier`로 요소 2개 확인 | 본문 |
| A04 | spring-framework#37264 | 09-10 | **Kotlin** WebFlux에서 멀티파트의 빈 파일이 마지막 파트가 아니면 `null`로 수신 | Boot 4.1.1·Framework 7.0.9·Kotlin 2.3.10. 수정 7.0.10 | R | 멀티파트 요청 후 빈 파일 파트 수신 | 본문 |
| A05 | spring-data-jpa#4359 | 09-19 | `@Query("... c.duration % 2 = 0")`가 `BadJpqlGrammarException: token recognition error at: '%'` | `Hql.g4`에 `%` 없음. 수정 4.1.2(2026.0.2). **Boot 4.0.8(Spring Data JPA 4.0.7)에서 재현 확인됨** — 메시지는 `mismatched input '%'`로 이슈 본문과 다르다 | R | 쿼리 메서드 호출 성공 | 본문 |
| A06 | spring-data-jpa#4341 | 09-03 | `SELECT DISTINCT` 파생 count 쿼리가 `coalesce(u.name, u.lastname)`를 `coalescename, lastname`처럼 잘못 생성 | 수정 Spring Data JPA 4.0.8(2025.1.8). **Boot 4.0.8 BOM(Data 2025.1.7)에는 미포함.** Hibernate(HQL)에서는 별칭 없는 생성자 표현식 쿼리에서만 재현되고, 이슈 본문의 `case when`·`cast` 사례는 JPQL/EQL 경로에서만 깨진다 | R | 페이지 조회(count 포함) 성공 | 본문 |
| A07 | spring-boot#52121† | 10-09 | `spring.mongodb.password`에 공백이 있으면 `+`로 인코딩되어 인증 실패 | 수정 4.0.9. 우회·재현 방식은 확정 전 확인 | D | 공백 포함 비밀번호로 연결 성공 | 본문 |
| A08 | spring-boot#52207† | 10-14 | `.properties`의 `\#hash=...`처럼 이스케이프된 `#`·`!`로 시작하는 키가 조용히 사라짐 | `java.util.Properties`와 결과가 다름. 수정 4.0.9 | R | 로드한 맵에 키 존재 | 본문 |
| A09 | spring-framework#37427 | 10-08 | `Duration`이 약 24.8일을 넘으면 `(int) toMillis()`가 오버플로해 타임아웃이 무시되거나 즉시 만료 | `SimpleClientHttpRequestFactory`·`ReactorClientHttpRequestFactory`. 수정 마일스톤 7.1.0-RC1(7.0.x 백포트 여부는 확인 필요) | R | 기대한 타임아웃 값 반영 | 본문(일부) |
| A10 | spring-boot#51447 | 08-25 | 사용자 정의 `ClientRegistrationRepository` 빈이 있으면 `OAuth2ClientProperties`를 쓸 수 없음 | 수정 4.0.9. 정확한 실패 형태는 확정 전 확인(본문이 변경 요청 형태) | R | 프로퍼티 빈 주입 성공 | 본문(일부) |

### A층 예비 (순위순)

| ID | 이슈 | 보고일 | 증상 | 예비로 둔 이유 |
|---|---|---|---|---|
| A-s1 | spring-boot#51892† | 09-28 | `Period.ZERO`를 `PeriodStyle.SIMPLE` + `WEEKS`로 출력하면 `IllegalStateException: intValue cannot be extracted from WEEKS` | 재현이 단위 수준이라 현실감이 낮다 |
| A-s2 | spring-security#19317 | **06-10** | `spring-security-crypto` 7.1.0 단독 사용 시 `BCryptPasswordEncoder.matches`가 `NoClassDefFoundError: org/springframework/util/StringUtils` | 보고일이 90일 기준 미달. 해결 방식 미확인 |
| A-s3 | spring-boot#50489† | **05-23** | Gradle 증분 빌드에서 설정 프로퍼티 설명이 `spring-configuration-metadata.json`에서 사라짐 | 보고일 미달, 검증이 파일 내용 비교라 약하다 |
| A-s4 | spring-boot#51492 | 08-28 | `developmentOnly`/`testAndDevelopmentOnly` 구성에 속성이 없어 Gradle이 잘못된 변형을 고르거나 모호성 오류 | 재현 조건이 불명확 |
| A-s5 | spring-boot#51840 | 09-19 | Kotlinx Serialization JSON 코덱 커스터마이징이 적용되지 않음(Kotlin) | 본문이 템플릿 위주라 증상 확인 필요 |
| A-s6 | spring-data-commons#3533 | 08-24 | camelCase와 점 경로가 섞인 프로퍼티 경로 해석 오류 | 본문이 비어 있음 |
| A-s7 | spring-security#19474 | 07-28 | AWS에서만 간헐적 `NimbusJwtDecoder` JWKS `Read timed out` | 환경 의존·간헐적이라 `L` 형식만 가능 |

## 4. B층 — 롱테일·환경 의존, 10개 + 예비

환경·버전 조합이 맞아야 풀리고 답이 흩어져 있는 문제다. **정직한 한계**: 진짜 환경 의존 문제(Docker 엔진 버전, Colima, Podman, CI 러너)는 저장소만으로 재현할 수 없다. 그래서 B층은 재현 가능한 `R/D` 6개와 로그 재생형 `L` 4개로 나눈다. `L`은 성공 판정이 "정답 원인·해결과 일치하는가"라 `R`보다 엄밀성이 낮고, 이 점을 결과 보고에 명시해야 한다.

| ID | 이슈 | 보고일 | 증상 | 환경·조건 | 형식 | 확인 |
|---|---|---|---|---|---|---|
| B01 | spring-boot#49012 | 2026-01-28 | Flyway 사용 앱이 `Unsupported Database: PostgreSQL 18.1`로 기동 실패(`BeanCreationException`) | Boot 4.0.2 + PostgreSQL 18.1. 상태 `invalid`로 닫힘 — 정답은 이슈 댓글 확인 필요(`flyway-database-postgresql` 모듈 추정, 미확인) | D | 본문 |
| B02 | spring-boot#49311 | 2026-02-24 | 3.5 → 4.0 업그레이드 후 다중 파트 업로드가 `413 Payload Too Large` | Tomcat 기본 `max-part-count`가 낮아짐. `server.tomcat.max-part-count`로 우회. 수정 4.0.4 | R | 본문 |
| B03 | spring-boot#48880 | 2026-01-19 | `spring-boot-starter-kafka`만으로는 `TransactionAutoConfiguration`이 없음(4.0 모듈화) | 수정 4.0.2 | R | 본문 |
| B04 | spring-boot#48653 | 2025-12-31 | `@SpringBootTest(RANDOM_PORT)`가 관리 서버에는 적용되지 않아 `PortInUseException: Port 8081 is already in use` | 로컬에서 앱이 떠 있는 상태에서 테스트. 수정 4.0.2 | R | 본문 |
| B05 | spring-boot#48240 | 2025-11-22 | Spring Data JDBC + AOT 사용 시 `JdbcDialect` 빈이 필요하다는 안내가 없음 | 문서화 이슈. 실제 오류 메시지는 확정 전 확인 | R | 제목 |
| B06 | spring-boot#48254 | 2025-11-23 | WAR 배포(외부 Tomcat)에서 `ApplicationServletEnvironment`가 구성되지 않음 | 외부 서블릿 컨테이너 필요. 수정 마일스톤 확인 필요 | D | 제목 |
| B07 | testcontainers-java#11212 | 2025-11-11 | Docker 29.0.0에서 `Could not find a valid Docker environment` | Docker Engine 29.0.0. 댓글 40개 — 가장 많이 논의됨 | L | 제목 |
| B08 | testcontainers-java#12104 | 2026-10-05 | Colima에서 Ryuk 컨테이너가 시작하지 않음 | macOS + Colima | L | 제목 |
| B09 | spring-boot#48050 | 2025-11-11 | `bootBuildImage`가 최신 Docker에서 HTTP 400 Bad Request로 실패 | Docker API 버전이 코드에 하드코딩. 수정 3.4.12 이상 | L | 본문 |
| B10 | testcontainers-java#11254 | 2025-12-01 | Testcontainers 2.0.2에서 Docker 소켓 경로 덮어쓰기가 안 되어 Podman이 동작하지 않음 | Podman + 2.0.2 | L | 제목 |

### B층 예비

| ID | 이슈 | 증상 | 예비로 둔 이유 |
|---|---|---|---|
| B-s1 | testcontainers-java#11491 | Ubuntu GitHub 러너 + Docker 29.1에서 비호환 | `L`, B07과 중복 성격 |
| B-s2 | spring-boot#49724 | 3.5.12에서 외부 `application.properties` 오버라이드가 무시됨 | Boot 3.5 계열이라 스택이 다르다 |
| B-s3 | spring-boot#48587 | `@SpringBootTest(useMainMethod=ALWAYS)`가 상위 클래스의 static `main`을 못 찾음 | `declined`로 닫힘(정답이 "고치지 않음") |
| B-s4 | spring-boot#50795 | 설정 프로퍼티 검증 중 `IllegalStateException` | 본문이 템플릿 위주, 증상 확인 필요 |
| B-s5 | gradle#35782 | Gradle 9가 Java 8·11 툴체인을 요구하는 패키징 문제 | `not-fixed`, 특수한 빌드 환경 |

## 5. C층 — 통제군 (흔한 에러), 10개

에이전트가 혼자 풀 것으로 **예상되는** 문제다. 여기서 Quno가 이득을 못 내야 "수요는 최신·롱테일에 남는다"는 주장이 선다. 모두 이 스택의 대표적이고 문서화된 에러이며, 최근 이슈를 쓰지 않고 합성 재현 프로젝트로 만든다. 링크는 모두 200 응답을 확인했다(C08 제외). 재현 프로젝트는 [experiments/phase-91-tasks](../../experiments/phase-91-tasks/README.md)에 있고 C01~C10 모두 재현·정답 검증을 통과했다.

| ID | 에러 | 원인·해결 | 근거 문서 |
|---|---|---|---|
| C01 | `BeanCurrentlyInCreationException` (순환 참조) | Boot 2.6부터 순환 참조 금지. 설계 변경 또는 `@Lazy` | [Boot 2.6 릴리스 노트](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-2.6-Release-Notes) |
| C02 | `@Configuration` 클래스가 final이라 프록시 불가 | Kotlin 클래스는 기본 final. `kotlin-spring`(all-open) 플러그인 | [Kotlin all-open](https://kotlinlang.org/docs/all-open-plugin.html) |
| C03 | JPA 엔티티 "No default constructor" | Kotlin 데이터 클래스. `kotlin-jpa`(no-arg) 플러그인 | [Kotlin no-arg](https://kotlinlang.org/docs/no-arg-plugin.html) |
| C04 | Gradle `Unsupported class file major version` | JDK와 도구 버전 불일치. 구현은 JDK 21 + 구버전 JaCoCo(0.8.7)로 `jacocoTestReport`가 실패하는 형태(Gradle 래퍼를 과제별로 낮출 수 없어 변형) | [Gradle 호환성 표](https://docs.gradle.org/current/userguide/compatibility.html) |
| C05 | `PortInUseException`: 포트 8080 사용 중 | 포트 충돌. 포트 변경 또는 프로세스 종료 | [Boot how-to: 웹 서버](https://docs.spring.io/spring-boot/how-to/webserver.html) |
| C06 | `Failed to configure a DataSource: 'url' attribute is not specified` | 데이터소스 설정 누락 | [Boot how-to: 데이터 접근](https://docs.spring.io/spring-boot/how-to/data-access.html) |
| C07 | `NoUniqueBeanDefinitionException` | 같은 타입 빈 둘. `@Primary`/`@Qualifier` | [Spring: @Primary](https://docs.spring.io/spring-framework/reference/core/beans/annotation-config/autowired-primary.html) |
| C08 | `LazyInitializationException` (Hibernate 7 문구: `Cannot lazily initialize collection of role ...`) | 트랜잭션 밖 지연 로딩 | 링크 미확인 |
| C09 | `@Transactional`이 적용되지 않음(자기 호출) | 프록시 기반 AOP의 한계 | [Spring: 프록시 메커니즘](https://docs.spring.io/spring-framework/reference/core/aop/proxying.html) |
| C10 | `UninitializedPropertyAccessException: lateinit property ... has not been initialized` | Kotlin `lateinit` 초기화 전 접근 | [Kotlin: 프로퍼티](https://kotlinlang.org/docs/properties.html) |

C층 예비: Mockito `UnnecessaryStubbingException`, `HttpMessageNotReadableException`(JSON 필드 불일치).

## 6. 편향·오염 점검

- **A층과 에이전트 학습 시점**: A층 10개는 모두 보고일이 2026-07-19 이후다. 실험할 에이전트의 학습 시점이 이보다 늦다면 "최신성" 효과가 줄어든다. 시작 시점에 에이전트의 학습 시점을 기록하고, 그 이전에 보고된 이슈는 B층으로 옮긴다.
- **수정 버전이 곧 정답**인 A층 과제가 많다(예: 4.0.9로 올리면 해결). 에이전트가 "최신 버전으로 올려 보라"고만 해도 풀릴 수 있다. 이를 막기 위해 **버전 고정**을 과제 조건으로 명시하거나, 업그레이드 불가 조건(다른 의존성과의 호환 때문에)을 과제 설명에 둔다. 이 조건을 어떻게 둘지는 확정할 때 정한다.
- **이 저장소 스택과의 가까움**: A01~A03, A07은 Boot 4.0.8 위에서 이 저장소 코드로도 겪을 수 있는 문제다. 과제를 만들 때 이 저장소 코드를 직접 쓰지 않고 별도 최소 프로젝트로 만든다(시드 유출 방지).
- **GitHub 이슈 편향**: 후보를 GitHub 이슈에서만 모았으므로 "이슈로 올라온, 메인테이너가 인정한" 문제에 치우친다. 실제 개발자가 겪는 문제 중 이슈로 가지 않고 사라지는 것(설정 실수, 환경 문제)은 B층 `L`과 C층에서만 다룬다.
- **중도 폐기 규칙**: 확정 단계에서 재현에 실패하는 과제(수정 전 버전에서 증상이 안 나타남)는 예비로 교체하고, 교체 사유를 이 문서의 변경 이력에 남긴다.

## 7. 확정까지 남은 일

1. **[사람]** 후보를 훑어 제외·교체할 것을 정한다. 특히 B층 `L` 4개를 유지할지(엄밀성 vs 환경 현실감).
2. 과제마다 최소 재현 프로젝트(Kotlin + Gradle, 버전 고정)와 성공 판정 명령을 만든다 — **이 단계에서 수정 전 버전에서 실제로 증상이 나는지 확인**하고, 안 나면 교체한다.
3. `본문`이 "일부"이거나 `제목`인 후보의 본문·댓글을 읽어 정답(해결)을 확정한다.
4. 확정한 30개 목록과 정답 기록을 별도 커밋으로 고정한다. 이 커밋 해시를 사전 등록으로 삼는다.
5. 그 뒤에 시드 큐레이션을 시작한다(과제 정답을 보고 시드를 만들지 않는다).

## 변경 이력

| 날짜 | 변경 | 이유 |
|---|---|---|
| 2026-10-19 | 최초 작성 (A 10 + 예비 7, B 10 + 예비 5, C 10) | PLAN.md 91.2 |
