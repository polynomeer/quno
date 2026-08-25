# ADR-0003: Stateless JWT(Access/Refresh 분리) 인증 채택

- 날짜: 2026-08-24
- 상태: 승인됨

## 배경 (Context)

[system-architecture.md](../system-architecture.md)의 전체 구조상 React Web Client는 백엔드와 별도로 배포된다. 서버 세션 기반 인증(쿠키 + 서버 측 세션 저장소)과 stateless 토큰 인증(JWT) 중 하나를 골라야 인증 필터 체인과 API 설계(`api-design.md`)를 구체화할 수 있었다.

## 결정 (Decision)

Spring Security + JWT로 Access/Refresh Token을 분리한 stateless 인증을 사용한다.

- Access Token은 짧은 만료(15~30분), Refresh Token은 별도 만료 정책.
- 비밀번호는 BCrypt로 단방향 해시.
- 클라이언트가 `authorId`/`userId`를 직접 지정하지 않고, `JwtAuthenticationFilter`가 검증한 SecurityContext에서 얻는다.
- Refresh Token은 서버 측 저장/revocation 목록 없이 서명·만료만 검증하는 순수 stateless 방식으로 시작한다. 탈취 대응이 필요해지면 Redis 기반 revocation을 후속으로 추가한다(아직 미착수).
- 기본 필터 체인은 `/error`, `/actuator/health`, `/actuator/info`, `/api/v1/auth/**`만 공개하고 나머지는 인증을 요구한다.

## 결과 (Consequences)

- 서버가 세션 상태를 갖지 않으므로 수평 확장이 단순해진다. 대신 Refresh Token 탈취 시 즉시 무효화할 수단이 없다는 트레이드오프를 의도적으로 받아들였다 — MVP 단계에서는 우선순위가 낮다고 판단.
- `/error`를 permitAll에 넣지 않으면 컨트롤러의 uncaught exception이 Boot 내부 forward를 통해 이 필터 체인에서 다시 미인증 처리되어 실제 원인(500)이 401로 위장된다는 부작용을 Phase 2.6에서 실제로 겪었다 — 이 규칙은 ADR-0007에서 별도로 다룬다.
- 관리자/모더레이터 역할이 추가되면 Role과 세부 권한을 분리해야 하는데, 현재는 인증 여부만 검사하고 역할 기반 인가는 없다 — Organization/모더레이션 기능(로드맵 Phase 8) 착수 시 재검토 대상.

## 관련 문서

- [api-design.md](../api-design.md#인증-확정--2026-08-24)
- [PLAN.md](../../../PLAN.md) Phase 1.6, 2.1
- 커밋 `fb3513f` (인증 방식을 JWT로 확정), `31d91cc` (User 도메인/JWT 인증 구현)
