# ADR-0007: 태그 중복 판정을 slug 기준으로 통일하고 `/error`를 보안 필터에서 permitAll 처리

- 날짜: 2026-08-24
- 상태: 승인됨

## 배경 (Context)

Phase 2.6에서 대소문자만 다른 태그명("Kotlin"과 "kotlin")을 각각 생성하면 `name`은 다르지만 `slug`는 동일해져 `uq_tags_slug_active` unique 제약을 위반하는 버그를 발견했다. 더 심각한 것은 이 `DataIntegrityViolationException`이 Spring Boot의 내부 `/error` forward를 타면서, `SecurityConfig`의 `anyRequest().authenticated()` 규칙에 다시 걸려 클라이언트에는 실제 원인(500)이 아니라 **의미 없는 401**로 보였다는 점이다. curl로 재현하며 처음엔 "가끔 실패하는 버그"로 오인했다가, 서버 로그의 `SQLState: 23505` / `uq_tags_slug_active`를 확인하고서야 결정적 재현 조건을 찾았다.

## 결정 (Decision)

두 가지를 함께 고친다.

1. **태그 find-or-create 조회 기준을 `name`이 아니라 `slug`로 바꾼다.** `CreateQuestionUseCase`가 `Tag.slugify(name)`으로 후보 slug를 먼저 계산하고 `TagRepository.findBySlug`로 조회한다 — 대소문자가 달라도 같은 slug면 기존 태그를 재사용한다.
2. **`SecurityConfig`에 `/error`를 permitAll로 추가한다.** 이는 이 버그 하나만을 위한 땜질이 아니라, "컨트롤러에서 어떤 uncaught exception이 나든 `/error`가 막혀 있으면 원인이 401로 위장된다"는 일반적인 문제에 대한 근본 수정이다.

## 결과 (Consequences)

- 태그 이름의 대소문자 변형이 더 이상 DB 제약 위반을 일으키지 않는다.
- `/error`가 열려 있으므로, 앞으로 새 예외 타입을 추가할 때 `GlobalExceptionHandler`에 매핑을 빠뜨려도 최소한 500이 500으로 보인다(401로 위장되지 않는다) — 그래도 매핑 자체는 빠뜨리지 않도록 주의해야 한다는 점을 `api-design.md`에 남겨뒀다.
- 이 사례는 "curl로 재현 가능한 버그는 추측하지 말고 서버 로그의 SQLState까지 확인한다"는 디버깅 관행을 이 세션에 정착시켰다.

## 관련 문서

- [api-design.md](../api-design.md#인증-확정--2026-08-24) `/error`를 막아두면 생기는 문제 설명
- [PLAN.md](../../../PLAN.md) Phase 2.6
- 커밋 `80eb63e` (Tag CRUD/팔로우 구현, 버그 및 수정 포함)
