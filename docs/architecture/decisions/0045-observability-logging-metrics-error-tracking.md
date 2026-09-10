# ADR-0045: 관측 가능성 — 구조화 로깅, 요청 추적 ID, Prometheus 메트릭, 에러 트래킹

- 날짜: 2026-09-10
- 상태: 승인됨

## 배경 (Context)

상용 전환 체크리스트([production-readiness.md](../../product/production-readiness.md) B-3)에 따라 배포(Phase 32)·보안 강화(Phase 33)에 이어 관측 가능성을 진행한다. 지금까지는 로그가 사람이 읽기 좋은 콘솔 포맷뿐이고, 요청을 관통 추적할 방법이 없고, 메트릭이 노출되지 않고, 에러 트래킹이 전혀 없었다 — 문제가 생겨도 실서비스에서 원인을 찾을 방법이 마땅치 않은 상태였다.

## 결정 (Decision)

1. **구조화 로깅은 Spring Boot 4의 내장 기능만 쓴다.** 별도 의존성(logstash-logback-encoder 등) 없이 `logging.structured.format.console: ecs`(Elastic Common Schema)를 `application-prod.yml`에만 추가한다. 로컬/테스트는 기존 사람이 읽기 좋은 콘솔 포맷을 그대로 유지한다.
2. **요청 추적 ID는 자체 구현한다.** `RequestIdFilter`(`infrastructure/logging`)가 `X-Request-Id` 헤더를 읽거나 없으면 발급해 MDC에 넣고 응답 헤더로 돌려준다. Spring Security 체인 밖(actuator 등)의 요청도 놓치지 않도록 `@Component` + `@Order(HIGHEST_PRECEDENCE)`로 Boot 전역 필터로 등록한다 — `JwtAuthenticationFilter`/`RateLimitFilter`가 `SecurityConfig`에서 수동으로 체인에 끼워 넣는 것과는 다른 패턴인데, 이 필터는 Security 전용이 아니라 모든 요청에 적용돼야 하기 때문이다. MDC에 넣은 값은 ECS 포맷이 모든 로그 라인에 자동으로 실어준다.
3. **Prometheus 메트릭은 앱 차원 인증 없이 노출한다.** `/actuator/prometheus`를 `health`/`info`와 같이 `permitAll`로 열었다 — Prometheus 서버는 우리 JWT를 발급받을 수 없어 인증을 걸면 스크레이핑 자체가 불가능하다. 실제 접근 통제는 애플리케이션이 아니라 인프라 레벨(내부망 전용 배치, 리버스 프록시 IP 제한)에서 해야 하며, 이는 인프라를 실제로 구성하는 시점(사람이 할 일)의 책임으로 남긴다.
4. **백엔드 에러 트래킹은 Sentry 코어 SDK만 쓰고 Spring 통합은 쓰지 않는다.** `io.sentry:sentry-spring-boot-starter-jakarta`를 처음 붙였을 때 `ApplicationContext` 로드 자체가 `NoClassDefFoundError: org/springframework/boot/web/client/RestClientCustomizer`로 실패했다 — 이 스타터의 자동 설정이 아직 Spring Boot 4의 재구성된 패키지 구조를 지원하지 않는다(테스트로 확인, MongoDB prefix 변경을 발견했던 ADR-0036과 같은 종류의 버전 호환성 함정). 대신 `io.sentry:sentry`(Spring 비의존 코어)만 추가하고 `SentryConfig`가 `@PostConstruct`에서 직접 `Sentry.init()`을 호출한다. `GlobalExceptionHandler`에 `Exception::class`를 잡는 catch-all 핸들러를 추가해 예상된 도메인 예외(중복/404/권한 없음 등, 이미 각각 처리됨)가 아닌 진짜 예기치 못한 예외만 `Sentry.captureException`으로 보낸다. `sentry.dsn`이 비어 있으면(기본값) SDK가 스스로 비활성화되므로 계정 개설 전에도 정상 기동한다.
5. **프론트엔드도 같은 원칙으로 에러 리포팅을 붙인다.** 전용 Next.js 통합(`@sentry/nextjs`, 소스맵 업로드용 빌드 플러그인 포함)은 이 Next.js 버전(AGENTS.md가 경고하는 대로 학습 데이터와 다른 최신 버전)에서의 빌드 통합 위험이 있어 채택하지 않고, 프레임워크 비의존 `@sentry/browser`만 쓴다. `shared/lib/errorReporting.ts`의 `reportError()`가 `NEXT_PUBLIC_SENTRY_DSN`이 있을 때만 지연 초기화 후 캡처하고, 없으면 `console.error`만 한다. 이 함수를 신규 `app/error.tsx`(라우트 세그먼트 에러 경계)와 `app/global-error.tsx`(루트 레이아웃 에러, 자체 html/body 필요)에서 호출한다. `app/not-found.tsx`도 이번에 함께 추가했다 — 지금까지 커스텀 404가 전혀 없었다.
6. **소스맵 업로드·서버 사이드 스택 트레이스 심볼화는 범위 밖이다.** 코어 SDK만 쓰는 대가로, 프로덕션 빌드에서 난독화된(minified) 스택 트레이스를 그대로 받는다 — 실제로 에러 추적이 부담될 만큼 잦아지면 그때 `@sentry/nextjs`/Spring 통합으로 넘어가는 것을 재검토한다.

## 결과 (Consequences)

- `/actuator/prometheus`가 인증 없이 열려 있으므로, 실 배포 시 반드시 인프라 레벨에서 외부 접근을 막아야 한다 — 이 문서만으로는 강제되지 않는 사람의 책임이다.
- Sentry 백엔드 통합이 표준 Spring Boot Starter 방식이 아니라 수동 초기화 방식이라, 향후 Sentry의 다른 Spring 자동 설정 기능(예: 성능 트레이싱, 요청 브레드크럼 자동 수집)은 기본으로 딸려오지 않는다 — 필요해지면 그때 개별적으로 추가한다.
- 프론트엔드 에러가 난독화된 스택으로 리포팅되므로, 실제 운영에서 원인 파악이 어려우면 `@sentry/nextjs` 전환이 다음 후속 과제가 된다.

## 관련 문서

- [production-readiness.md](../../product/production-readiness.md) B-3
- [0036-live-chat-websocket-mongodb-redis-presence.md](0036-live-chat-websocket-mongodb-redis-presence.md) (같은 종류의 Spring Boot 4 버전 호환성 함정을 먼저 발견한 사례)
- [PLAN.md](../../../PLAN.md) Phase 34
