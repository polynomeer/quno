# ADR-0074: Kotlin 2.4 업그레이드 보류 — ktlint가 따라올 때까지 2.2.21 유지

- 날짜: 2026-10-27
- 상태: 승인됨

## 배경 (Context)

Dependabot이 Kotlin Gradle 플러그인 세 개(`jvm`, `plugin.spring`, `plugin.jpa`)를 2.2.21 → 2.4.21로 올리는 PR을 따로따로 열었다(#47·#48·#49). 세 PR 모두 백엔드 CI에서 `runKtlintCheck*` 태스크가 "KtLint failed to parse file"로 실패했다.

로컬에서 세 플러그인을 한꺼번에 2.4.21로 올려 확인했다.

- ktlint Gradle 플러그인 14.2.0의 기본 엔진은 Kotlin 2.4 소스(`build.gradle.kts` 포함)를 해석하지 못한다.
- 엔진을 당시 최신인 ktlint 1.8.0으로 고정해도 실패한다. ktlint 워커 안의 Kotlin 컴파일러가 2.4 컴파일러와 충돌해 `IllegalStateException: Extensions storage is not registered`로 실패한다.

즉 지금은 Kotlin 2.4와 ktlint를 함께 쓸 조합이 없다.

## 결정 (Decision)

- **Kotlin은 2.2.21을 유지한다.** #47·#48·#49는 닫는다.
- **ktlint 검사는 빼지 않는다.** 업그레이드를 위해 코드 스타일 검사를 끄는 쪽은 기각했다. CI에서 형식 회귀를 잡는 장치가 사라지기 때문이다.
- **Dependabot 설정**
  - Kotlin 플러그인을 `kotlin` 그룹으로 묶어 항상 한 PR로 올라오게 한다.
  - `>= 2.4.0`은 무시한다.

## 결과 (Consequences)

- Kotlin 2.4의 새 기능과 컴파일러 개선은 당분간 쓰지 못한다. Spring Boot 4.0.8은 2.2.x로도 문제없이 동작한다.
- **재검토 조건:** ktlint(또는 ktlint Gradle 플러그인) 릴리스 노트에 Kotlin 2.4 지원이 나오면 이 ADR을 재검토한다. 그때 `dependabot.yml`의 ignore를 지우고, 세 플러그인과 ktlint 엔진을 함께 올린다.

## 관련 문서

- `.github/dependabot.yml`
- `backend/build.gradle.kts`
