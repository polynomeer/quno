# ADR-0049: 코드 품질 게이트 — ktlint 전체 재포맷, detekt 보류, 커버리지 가시화

- 날짜: 2026-09-10
- 상태: 승인됨

## 배경 (Context)

상용 전환(Phase 32~37)이 끝나고 품질 개선([quality-improvement-plan.md](../../product/quality-improvement-plan.md) Q-1)으로 넘어왔다. Q-1은 "이후 모든 개선의 기반"으로 명시돼 있어 가장 먼저 진행한다: 백엔드 정적 분석(ktlint+detekt), 프론트엔드 ESLint 강화, 테스트 커버리지 측정 도구.

## 결정 (Decision)

1. **ktlint를 도입하고 기존 코드 전체를 한 번에 재포맷한다.** 처음 `ktlintFormat`을 돌렸을 때 408개 파일·9천 줄 넘게 바뀌는 것을 보고 원인을 조사했다 — `.editorconfig`가 저장소에 하나도 없으면 ktlint가 관대한 `intellij_idea` 스타일을 기본으로 쓰지만, `.editorconfig` 파일이 하나라도 생기는 순간 `ktlint_code_style`을 명시하지 않는 한 더 엄격한 `ktlint_official`로 자동 전환된다는 것을 발견했다. `backend/.editorconfig`에 `ktlint_code_style = intellij_idea`를 명시해 기존 스타일을 유지하도록 고치니 실제 변경 규모가 174개 파일·약 1,300줄로 줄었다(순수 포맷팅, 동작 변경 없음 — 재포맷 후 전체 테스트 스위트 348개 통과로 확인). 이 정도 규모의 일회성 재포맷을 그대로 적용할지, 도구만 준비해두고 강제하지 않을지 사용자에게 확인했고, "전체 재포맷 후 CI 게이트로 적용"을 선택받아 그대로 진행하고 `ktlintCheck`를 CI에 연결했다.
2. **파일당 단일 클래스를 강제하는 `filename` 규칙은 끈다.** 이 프로젝트는 관련 DTO를 종류별로 묶어 `Commands.kt`/`Results.kt` 하나에 모아두는 컨벤션을 처음부터 써왔다 — 지금 그 안에 클래스가 하나뿐인 파일도 나중에 늘어날 걸 전제로 이 이름을 쓰므로, ktlint 기본 규칙과 충돌해 `ktlint_standard_filename = disabled`로 명시적으로 껐다.
3. **detekt는 이번엔 보류한다.** `io.gitlab.arturbosch.detekt` 1.23.8(현재 최신, Maven Central 확인)은 자기 자신을 컴파일한 Kotlin 2.0.21로만 실행되도록 하드 `check()`를 갖고 있고 우회 옵션이 전혀 없다(detekt 공식 문서·소스 확인). 이 프로젝트의 `kotlin("jvm") 2.2.21` 플러그인이 detekt 자체 classpath의 `kotlin-compiler-embeddable`까지 2.2.21로 강제 정렬시켜 그 체크에 걸린다 — detekt 쪽에서 `{strictly 2.0.21}`을 선언했는데도, 그리고 이 프로젝트가 `configurations.matching { it.name == "detekt" }`에 직접 버전을 `force`해도 여전히 2.2.21로 정렬되는 것을 실측으로 확인했다(Kotlin Gradle Plugin의 정렬 메커니즘이 더 높은 우선순위를 가짐). Sentry(ADR-0045)나 MongoDB prefix(ADR-0036) 때와 같은 종류의 "이 프로젝트가 최신 버전을 너무 빨리 쓴다" 문제이지만, 이번엔 우회 경로 자체가 없어 detekt 새 릴리스가 Kotlin 2.2.x를 지원할 때까지 보류한다.
4. **커버리지는 도구만 연결하고 임계값은 두지 않는다.** 백엔드는 Gradle `jacoco` 플러그인(버전 불필요, Gradle 내장) — `./gradlew test`가 `jacocoTestReport`를 자동으로 이어 실행한다(HTML+XML). 도입 시점 기준 라인 커버리지 81.8%를 실측 확인했다. 프론트엔드는 `@vitest/coverage-v8`(설치된 vitest 3.2.7과 버전 맞춤) — `npm run test:coverage`. 둘 다 "수치가 급격히 떨어지면 눈에 띄게" 하는 목적이라 CI에서 실패시키지 않고 아티팩트로만 업로드한다(Codecov 같은 외부 SaaS 연동은 계정 개설이 필요해 A 항목 — 지금은 CI 아티팩트로 충분하다고 판단).
5. **ESLint의 `no-explicit-any`/`no-unused-vars`를 error로 올린다.** `eslint-config-next`의 typescript 프리셋은 기본으로 이 둘을 `warn`에 둔다 — 경고는 `npm run lint`/CI를 실패시키지 않아 방치되기 쉽다. 이 코드베이스는 이미 `any`를 전혀 쓰지 않고 있어 error로 올려도 당장 고칠 게 없었다. 검증 중 `coverage/`가 생성한 리포트 스크립트(`coverage/block-navigation.js`)가 eslint 대상에 잡혀 무관한 경고를 내는 것을 발견해 `coverage/`/`playwright-report/`/`test-results/`를 eslint 무시 목록에 추가했다.

## 결과 (Consequences)

- 이후 커밋의 `git blame`이 이번 재포맷 커밋을 거치게 된다 — `git blame --ignore-rev <이 커밋 SHA>`로 건너뛸 수 있다.
- detekt가 없어 복잡도/코드 스멜 검사는 당분간 사람의 코드 리뷰에 의존한다 — detekt가 Kotlin 2.2.x를 지원하는 새 릴리스를 내면 재검토한다.
- 커버리지 수치에 강제성이 없어 실제로 떨어지는 PR을 막지는 못한다 — 리뷰어가 아티팩트를 보고 판단해야 한다.

## 관련 문서

- [quality-improvement-plan.md](../../product/quality-improvement-plan.md) Q-1
- [0045-observability-logging-metrics-error-tracking.md](0045-observability-logging-metrics-error-tracking.md) (같은 종류의 "최신 버전 선행" 호환성 문제 사례)
- [PLAN.md](../../../PLAN.md) Phase 38
