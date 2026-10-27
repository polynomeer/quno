# ADR-0076: Stack Fingerprint — 의존성 파일을 서버에서 파싱하되 원본은 저장하지 않고, 사용자가 고른 항목만 질문에 붙인다

- 날짜: 2026-10-27
- 상태: 승인됨

## 배경 (Context)

질문의 환경 정보는 지금 `environment` 자유 텍스트뿐이다. 그래서 어느 버전에서 생긴 문제인지, 어느 버전에서 유효한 답인지를 기계가 알 수 없다([competitive-analysis.md](../../product/competitive-analysis.md) 3절 약점 2). PLAN.md Phase 63은 `build.gradle.kts`·`pom.xml`·`package.json`을 (생태계, 패키지, 버전)으로 구조화한다. 그 위에 답변 적용 범위(63.3)와 정밀한 영향 감지(63.4)를 얹는 키스톤이다.

의존성 파일에는 사내 패키지 이름이나 저장소 주소가 섞일 수 있다. 그래서 무엇을 저장하고 누가 고르는지가 핵심 결정이다.

## 결정 (Decision)

### 지원 형식 (1차)

| 형식 | 읽는 것 | 생태계 |
|---|---|---|
| Gradle(Kotlin/Groovy DSL) | `implementation`·`api`·`testImplementation` 등의 `group:name:version` 좌표, `platform(...)` | `MAVEN` |
| Gradle 플러그인 | `id("…") version "…"`, `kotlin("…") version "…"` | `GRADLE_PLUGIN` |
| Maven | `pom.xml`의 parent, `dependencies`, `dependencyManagement`. `${property}`·`${project.version}` 치환 | `MAVEN` |
| npm | `package.json`의 `dependencies`·`devDependencies`·`peerDependencies` | `NPM` |

- 버전이 없는 좌표(BOM 관리)는 버전 없이 남긴다.
- pip·go.mod는 이후에 다룬다.

### 파싱 위치와 저장

- **파싱은 서버가 한다.** `POST /api/v1/stack-fingerprints/parse`는 결과만 돌려주고 아무것도 저장하지 않는다.
- **사용자가 고른 항목만 질문에 붙는다.** 미리보기에서 사내 패키지 같은 항목을 빼고, 남은 항목을 질문 작성·수정 요청의 `dependencies`로 보낸다. 서버는 받은 목록만 `question_dependencies`에 저장하고, 원본 파일은 어디에도 남지 않는다.
- **XML 파싱은 DTD와 외부 엔티티를 끈다**(XXE 방지).
- **파서는 도메인 포트(`DependencyFileParser`)로 둔다.** JSON·XML 라이브러리를 쓰는 구현은 인프라에 둬서 도메인이 프레임워크에 기대지 않게 한다.
- **최신 판본만 저장한다**([ADR-0075](0075-error-signature-normalization-and-matching.md)의 시그니처와 같은 이유).
  - 수정 요청에 `dependencies`가 없으면 이전 목록을 유지한다.
  - 빈 목록을 보내면 지운다.
  - Fork는 원 질문의 목록을 복사한다.
- **상한:** 질문당 최대 200개, 파일 입력은 100,000자까지 받는다.
- **기존 데이터:** `environment` 자유 텍스트는 그대로 둔다. 마이그레이션하지 않고 함께 쓴다.

### 버전 비교

이 단계에서는 하지 않는다. 버전 문자열을 선언된 그대로(`^3.2.0` 포함) 저장한다. 범위 표현과 비교기는 답변 적용 범위(63.3)에서 정한다.

### `TrackedTechnologies`와의 관계

지금은 연결하지 않는다. 영향 감지(63.4)에서 태그 slug 매핑 대신 이 구조화 항목을 쓰도록 바꾼다.

## 결과 (Consequences)

- 질문 상세에서 환경을 칩으로 보여줄 수 있다. 이후 같은 패키지·버전을 가진 질문끼리 묶을 수 있는 데이터가 생긴다.
- 사용자가 미리보기를 거치지 않으면 저장되는 것이 없다. 개인정보 위험이 작은 대신, 구조화 데이터의 양은 사용자의 수고에 달려 있다. CLI(Phase 65)와 환경 스냅샷 수집기(Phase 93)가 이 수고를 줄이는 경로다.
- 정규식 기반 Gradle 파서는 변수로 조립한 좌표(`"$group:$name:$ver"`)를 읽지 못한다. 미리보기에서 빠진 항목이 보이므로 사용자가 알 수 있다.

## 관련 문서

- [PLAN.md](../../../PLAN.md) Phase 63
- [ADR-0033](0033-technology-version-scan-detection-only-no-auto-outdated.md) 자동 OUTDATED 전환 안 함
- [ADR-0070](0070-question-as-evidence-environment-snapshot-and-verification-tiers.md) 환경 스냅샷 Tier 0
