# ADR-0083: 환경 스냅샷 1차 — Tier 0 스키마 v1, CLI 수집기가 로컬에서 정제·미리보기, 서버는 다시 정제하고 환경 지문을 직접 계산, "서명"은 출처 표시로 대신한다

- 날짜: 2026-11-01
- 상태: 승인됨

## 배경 (Context)

[ADR-0070](0070-question-as-evidence-environment-snapshot-and-verification-tiers.md)은 질문에 환경 스냅샷을 자동 첨부하는 원칙을 정했다. 원칙은 다음과 같다.

- 이벤트 기반으로 캡처한다.
- 정제·동의는 로컬에서 한다.
- 민감도를 3계층으로 나눈다.
- 환경 지문으로 같은 환경을 묶는다.
- 수집기 서명으로 신뢰를 표시한다.

PLAN.md Phase 93은 이를 7개 항목으로 나눴다. 첫 PR은 그 위에 모든 것이 얹히는 뼈대다.

- 93.1 설계
- 93.2 Tier 0 스키마
- 93.5 로컬 수집기 중 Tier 0
- 93.6 서버 저장

Tier 1·2 진단(93.3), 기준선 델타(93.4), MCP `capture_environment`는 다음 단계다.

## 결정 (Decision)

### 스키마 `quno.snapshot/1`

JSON 문서 하나다. 최상위에 다음 필드를 둔다.

- `schemaVersion`
- `source`(`COLLECTOR`|`MANUAL`)
- `collector`(이름·버전)
- `capturedAt`
- `tiers`(포함한 계층)
- `tier0`

`tier0`의 필드는 다음과 같다.

| 필드 | 내용 |
|---|---|
| `os` | 이름·버전·아키텍처·커널 |
| `runtimes` | `java`(벤더·버전), `node` 등 감지된 런타임 |
| `container` | Docker 안인지, WSL인지, Rosetta 번역 중인지 |
| `locale` | 언어·인코딩·타임존 |
| `tools` | gradle·mvn·npm·git 버전 |
| `ide` | 감지되면 이름 |
| `ci` | CI 여부·제공자 |
| `git` | 커밋 SHA, dirty 여부, 원격 **호스트만** |
| `dependencies` | 아래 참고 |
| `jvmArgs` | 정제한 JVM 인자 |

- **의존성은 "해석된" 버전을 우선한다.** 잠금 파일(`package-lock.json`, `gradle.lockfile`)이 있으면 그 버전을 쓰고 `resolution: "lockfile"`로 표시한다.
  - 없으면 선언 버전을 `resolution: "declared"`로 담는다.
  - Gradle 의존성 해석을 실행하지 않는 이유: 수집이 수십 초 걸리고 네트워크가 필요할 수 있다. 실패하는 순간에 빠르게 첨부한다는 목적과 맞지 않는다.
- **스키마는 추가만 한다.** 서버는 모르는 필드를 보존하고 아는 필드만 읽는다. 호환되지 않는 변경은 `schemaVersion`을 올린다. 서버가 모르는 버전은 400이다.
- 크기 상한은 64KB, 의존성은 500개다.

### 정제: 로컬에서 하고, 서버가 한 번 더

- **수집기(로컬)**
  - 홈 경로는 `~`로 바꾼다.
  - 사용자명·호스트명은 문자열에서 지운다.
  - git 원격은 호스트만 남긴다. 저장소 경로와 자격 증명은 남기지 않는다.
  - JVM 인자에서 `password|secret|token|key|credential`이 이름에 든 시스템 프로퍼티는 값을 `[REDACTED]`로 바꾼다.
- **수집 금지:** 환경 변수는 허용목록에 든 **이름의 존재 여부**만 본다(`CI`, `GITHUB_ACTIONS` 등). 그 외 환경 변수 값, 소스 파일 내용, 사용자·호스트 식별자는 읽지 않는다.
- **미리보기·동의:** `quno capture`는 보낼 JSON을 그대로 보여준다. `quno ask`는 스냅샷 요약을 다른 첨부물과 함께 보여주고 게시 확인을 받는다([ADR-0080](0080-quno-cli-and-mcp-server-typescript-package.md)). `--no-snapshot`으로 뺄 수 있다.
- **서버(방어선):** 받은 문서의 모든 문자열 값에 [ADR-0081](0081-engine-suggestion-layer-and-secret-masking.md)의 `SecretDetector`를 다시 돌려 `[REDACTED]`로 바꾸고, 바꾼 개수를 기록한다.
  - 수집기 버그나 수동 입력에서 새는 경로를 막는다.
  - 거절하지 않고 바꾸기만 하는 이유: 질문 게시 전체를 막는 것보다 낫고, 원본은 어차피 서버에 남지 않는다.

### 환경 지문: 서버가 직접 계산한다

- 지문은 정규화한 튜플(OS 이름, 아키텍처, 컨테이너 여부, 런타임별 이름·벤더·**메이저 버전**)의 SHA-256 앞 16자리다.
- 클라이언트가 보낸 지문은 믿지 않는다. 서버가 같은 규칙으로 다시 계산해야 지문이 묶음 키로 쓸모가 있다.
- 메이저 버전까지만 넣는 이유: 패치 버전마다 지문이 달라지면 같은 환경끼리 묶이지 않는다. 영향 매트릭스(Phase 72)가 쓰는 단위와 같다.

### "수집기 서명"은 출처 표시로 대신한다

- ADR-0070은 공식 수집기 스냅샷을 서명하고 수동 입력에는 낮은 신뢰를 표시하라고 했다.
- 하지만 **오픈 클라이언트에 넣은 서명 키는 증명력이 없다.** 키를 꺼내면 누구나 "공식" 서명을 만들 수 있다.
- 그래서 1차에서는 `source`(수집기/수동)와 수집기 이름·버전을 그대로 보여준다. 화면에는 "Quno 수집기 0.2.0" / "수동 입력"으로 표시하고 "서명"이라는 말은 쓰지 않는다.
- 위조할 수 없는 신뢰는 서버가 직접 재현하는 러너(Phase 80)가 만든 스냅샷에서 시작한다. 그때 서버 키로 서명한다.

### 저장과 노출

- `question_environment_snapshots`에 질문당 최신 하나를 둔다([ADR-0076](0076-stack-fingerprint-dependency-parsing.md)과 같은 이유). 함께 저장하는 필드는 다음과 같다.
  - 판본 번호
  - 스키마 버전, 출처, 수집기
  - 서버가 계산한 지문
  - 서버 정제 개수
  - 문서(JSONB)
- 질문 수정에서 스냅샷을 생략하면 유지하고, `null`을 명시적으로 지우는 경로는 두지 않는다. 스냅샷은 "그때의 환경" 증거라서 지우기보다 새로 덮어쓰는 것이 맞다.
- **Fork는 복사하지 않는다.** Fork한 사람의 환경은 원 질문과 다르다.
- 질문 상세 응답과 MCP `get_question`에 요약이 들어가고, 화면에는 시안의 "환경 스냅샷" 접힘 패널로 보인다.

## 결과 (Consequences)

- `cat error.log | quno ask ...` 한 줄로 OS·JDK·컨테이너·로케일·해석된 의존성이 질문에 붙는다. 사람이 빠뜨리던 단서가 기본값이 된다.
- 지문이 생겨 영향 매트릭스(72)·깨짐 레이더(69)·러너 이미지 레시피(81)가 쓸 키가 마련된다.
- 출처 표시는 신뢰를 증명하지 않는다. 그래서 검증 등급(Phase 94)에서 L1 조건의 "환경 스냅샷"은 수동 입력도 인정하되, L2 이상은 러너 결과로만 올린다.
- 정제 누수 테스트(93.7)의 첫 코퍼스는 서버 측 재정제 테스트다. 수집기 쪽 코퍼스는 Tier 1(로그 창)을 수집할 때 함께 넓힌다.

## 관련 문서

- [PLAN.md](../../../PLAN.md) Phase 93
- [ADR-0070](0070-question-as-evidence-environment-snapshot-and-verification-tiers.md) 환경 스냅샷 원칙
- [ADR-0080](0080-quno-cli-and-mcp-server-typescript-package.md) CLI
- [ADR-0081](0081-engine-suggestion-layer-and-secret-masking.md) SecretDetector
