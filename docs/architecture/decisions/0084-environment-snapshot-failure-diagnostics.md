# ADR-0084: 환경 스냅샷 2차 — `quno run`이 실패를 로컬에만 기록하고, 증상 프로파일이 Tier 1 항목을 고르며, Tier 2는 `detailed` 프리셋에서만 담는다

- 날짜: 2026-11-02
- 상태: 승인됨

## 배경 (Context)

[ADR-0083](0083-environment-snapshot-tier0-collector.md)으로 Tier 0 스냅샷(OS·런타임·컨테이너·잠금 파일 의존성)이 질문에 붙게 됐다. Phase 93의 남은 항목은 다음과 같다.

- 93.3: 실패를 설명하는 Tier 1(로그 창·명령·종료 코드·환경 변수·설정 키·실패한 테스트)과 옵트인 Tier 2(메모리·디스크·cgroup 등)
- 93.4: 마지막으로 성공한 뒤 바뀐 것
- 93.5 나머지: 링 버퍼, MCP `capture_environment`, 필드별 포함/제외
- 93.7: 정제 누수 코퍼스

[ADR-0070](0070-question-as-evidence-environment-snapshot-and-verification-tiers.md)의 원칙은 여기서도 그대로다: 이벤트 기반(상시 모니터링 아님), 로컬 정제·동의, 민감도 3계층.

이번에 답할 질문은 셋이다.

- 실패 직전 출력을 어떻게 잡는가
- 어떤 진단을 언제 담는가
- 사용자가 무엇을 통제하는가

## 결정 (Decision)

### 실패를 잡는 방법: `quno run -- <명령>`

- 명령을 자식 프로세스로 실행하고 출력은 터미널에 그대로 흘려보낸다. 마지막 200줄(줄당 500자)만 **메모리의 링 버퍼**에 둔다. 종료 코드는 그대로 돌려준다.
- **성공하면** 이 프로젝트의 기준선을 기록한다: 커밋, 해석된 의존성 버전, 설정 파일 해시.
- **실패하면** 그 순간을 동결한다: 정제한 스냅샷과 정제한 출력 꼬리를 `last-failure.json`으로 남긴다.
- 기록은 `$XDG_STATE_HOME/quno/projects/<cwd 해시>/`에 하고, 권한은 디렉터리 700·파일 600이다. **아무 데도 보내지 않는다.**
- 사용자는 `quno capture --last-failure`로 확인하고, `quno ask --last-failure`로 올린다.
- 셸 훅이나 상시 감시는 하지 않는다. 사용자가 감싼 명령만 본다.
  - 셸 훅은 모든 명령을 엿보게 되어 "이벤트 기반" 원칙과 맞지 않는다.
  - 설치도 셸마다 다르다.
- **출력 꼬리는 스냅샷 문서에 넣지 않고 질문의 로그 칸으로 보낸다.** 문서에는 `logWindow{keptLines, totalLines, attachedAs:"logs"}`만 남긴다.
  - 로그 칸은 이미 에러 시그니처 색인(같은 에러 찾기)과 읽기 시점 비밀값 마스킹(ADR-0081)을 거친다.
  - 같은 텍스트를 두 군데 두면 한쪽만 고쳐지는 일이 생긴다.

### 증상 프로파일

출력과 종료 코드로 증상을 하나 판별한다. 증상마다 담는 Tier 1 항목이 다르다.

| 증상 | 판별 예 | 담는 Tier 1 항목 |
|---|---|---|
| `OUT_OF_MEMORY` | `OutOfMemoryError`, 종료 코드 137 | 환경 변수 (Tier 2를 권한다) |
| `DB_TIMEOUT` | `Connection is not available`, `SQLTimeoutException` | 환경 변수, 설정 키 |
| `DEPENDENCY_RESOLUTION` | `Could not resolve`, `ERESOLVE` | 환경 변수 (델타가 핵심) |
| `TEST_FAILURE` | `There were failing tests`, 실패한 JUnit 리포트 | 환경 변수, 설정 키, 실패한 테스트 |
| `BUILD_FAILURE` | `BUILD FAILED`, `Execution failed for task` | 환경 변수 |
| 판별 불가 | — | 환경 변수, 실패한 테스트 |

- 증상에 상관없는 항목은 모으지 않는다. 의존성 해석 실패에 애플리케이션 설정 키는 필요 없다. 덜 모으면 덜 샌다.
- 판별은 첫 일치 하나만 쓴다. 출력에 여러 신호가 섞여 있으면 순서상 앞선 것(메모리 → DB → 의존성 → 테스트 → 빌드)이 이긴다. 더 구체적인 원인이 앞에 오도록 순서를 정했다.
- 서버는 `tier1.symptom`을 `symptom` 컬럼(V33)으로 꺼내 `(symptom, fingerprint)`에 색인한다. 문서의 첫 색인 필드이고, 환경별 집계(69·72)가 쓴다. 서버가 모르는 증상 이름은 거절하지 않고 null로 둔다(수집기가 먼저 새 증상을 배울 수 있다).

### Tier 1 항목의 범위

- **환경 변수:** 허용목록 이름만 값을 읽는다.
  - 대상: `SPRING_PROFILES_ACTIVE`, `NODE_ENV`, `TZ`, `LANG`, `LC_ALL`, `*_OPTS`/`NODE_OPTIONS`/`JAVA_TOOL_OPTIONS`
  - `*_OPTS`류는 민감한 `-D` 값을 지운다.
  - 그 밖의 변수는 여전히 읽지 않는다.
- **설정 키:** `application*.yml|yaml|properties`의 **키만** 담는다(프로젝트 루트, `src/main/resources`, `config/`). 값은 정제해서라도 보내지 않는다. 키 이름만으로 "풀 크기를 설정했는가, 어떤 프로필이 있는가"를 알 수 있고, 값은 정제 규칙이 놓치는 순간 사고가 된다.
- **명령 인자:** `--password x`, `--token=x`, `-Dkey=x`는 이름을 남기고 값을 지운다. 나머지도 비밀값 탐지기를 거친다.
- **실패한 테스트:** 실행 시작 뒤에 쓰인 JUnit XML(Gradle `build/test-results`, Maven `surefire/failsafe-reports`)에서 최대 20개를 담는다. 담는 것은 클래스·이름·메시지 첫 줄이다. 시작 전 리포트는 지난 실행의 것이라 제외한다.

### Tier 2와 프리셋

- **Tier 2는 `--preset detailed`를 고를 때만 모은다.** 담는 것은 메모리 총량·여유, CPU 수, 디스크 여유, cgroup 메모리 한도다.
  - 증상이 메모리 부족이면 CLI가 "detailed로 다시 실행하라"고 **안내만** 한다. 사용자의 선택 없이 자동으로 올리지 않는다.
  - 서버는 `tiers`에 2가 없는데 `tier2`가 있으면 400으로 거절한다. 옵트인 표시 없는 런타임 상태는 수집기 버그다.
- **프리셋은 셋이다.**
  - `minimal`: 환경을 식별하는 정보(OS·런타임·컨테이너·로케일·CI)와 명령·종료 코드·증상만 담는다. 의존성·git·도구·JVM 인자·환경 변수·설정 키·실패한 테스트·델타는 뺀다.
  - `standard`: 기본값.
  - `detailed`: standard에 Tier 2를 더한다.
  - 조직 정책(Phase 79)이 프리셋을 강제하는 것은 79에서 한다.
- **필드별 제외:** `--omit tier0.git`처럼 점으로 구분한 경로를 뺀다(반복 가능).
  - 뺀 경로는 문서의 `omitted`에 남긴다. 읽는 사람이 "수집기가 못 모았다"와 "작성자가 뺐다"를 구분할 수 있어야 한다.
  - `tier0` 전체와 메타 필드(`schemaVersion` 등)는 뺄 수 없다. 지문을 계산할 수 없게 되기 때문이다.
- 이번에 하지 않는 것:
  - 스레드 덤프: 대상 프로세스 pid가 필요하고, 덤프 안의 문자열 정제가 어렵다.
  - OOM 커널 이벤트: `dmesg`는 권한이 필요하다.
  - 의존 서비스(DB·Redis) 버전: 접속 정보를 읽어야 한다.
  - 이 셋은 재현 러너(Phase 80)처럼 서버가 통제하는 환경에서 먼저 한다.

### 마지막 성공 이후 바뀐 것 (93.4)

실패 스냅샷의 `delta`에 다음을 담는다.

- 기준선 시각·커밋
- 그 뒤 커밋 수
- 바뀐 **파일 이름**(작업 트리 포함, 최대 50개 + 총수)
- 해석된 의존성 버전 변화(from → to, 추가·제거 포함)
- 바뀐 설정 파일 이름

규칙:

- 커밋 메시지와 파일 내용은 담지 않는다.
- 기준선 커밋이 리베이스로 사라졌으면 커밋 수는 `null`이다.
- 설정 파일 해시는 로컬 기준선 파일에만 있고, 밖으로는 "바뀌었다"만 나간다.
- 기준선은 `quno run`이 성공할 때만 갱신된다. CI의 마지막 성공과 비교하는 것은 CI 연동이 생길 때 한다.

### 로컬 정제: 서버 탐지기의 복사본, 공유 코퍼스

- Tier 1에서 처음으로 자유 텍스트(명령 줄·출력·테스트 메시지)를 모은다. 그래서 서버의 `SecretDetector` 규칙을 CLI에 옮겼다(`cli/src/snapshot/secrets.ts`).
- 처리 순서: 문서 전체 문자열에 비밀값 정제 → 신원 정제(홈·사용자·호스트) → 프리셋·사용자 제외.
- 출력 꼬리는 줄로 나누기 전에 통째로 정제한다. 여러 줄에 걸친 개인 키가 줄 단위 검사에서 빠지지 않게 하기 위해서다.
- 두 구현이 따로 놀지 않도록 **누수 코퍼스를 저장소 루트 `testdata/secret-leak-corpus.json`에 하나만 둔다.** CLI 테스트와 서버 테스트가 같은 파일을 읽는다.
  - CLI: 명령 인자·출력·환경 변수·설정 파일·실패 메시지·git 원격에 심는다.
  - 서버: 문서 곳곳에 심는다.
  - 둘 다 정제 결과에 값이 하나도 남지 않아야 한다(93.7 목표 0건).
- 이미 `[REDACTED]`로 바뀐 URL 비밀번호는 새 발견으로 세지 않는다. 그렇지 않으면 서버 재정제 개수가 부풀어 "수집기가 놓쳤다"는 잘못된 신호가 된다.

### MCP `capture_environment`

- 에이전트용 `quno capture`다. 이 컴퓨터에서 스냅샷을 만들어 **보여 주기만** 하고 보내지 않는다.
- 에이전트는 명령을 직접 실행하므로, `failure{argv, exitCode, output}`를 넘기면 `quno run`과 같은 링 버퍼·증상 판별·델타를 거친다. 결과는 `last-failure`로 동결한다.
  - 그래야 사용자가 같은 내용을 `quno capture --last-failure`로 다시 보고 `quno ask --last-failure`로 올릴 수 있다.
  - 게시는 여전히 사람이 한다(ADR-0080: MCP에는 쓰기 도구가 없다).
- 로컬 파일을 쓰므로 `readOnlyHint: false`다. 다만 `destructiveHint: false`, `openWorldHint: false`다(외부로 나가지 않는다).

## 결과 (Consequences)

- `quno run -- ./gradlew test`로 감싸 두면, 실패했을 때 증상·명령·종료 코드·실패한 테스트·마지막 성공 이후 바뀐 의존성이 한 번에 질문에 붙는다. 사람이 "어제는 됐는데"를 증거로 대신 말해 준다.
- 질문 상세 패널은 "실패 진단 / 마지막으로 성공한 뒤 바뀐 것 / 런타임 상태 / 환경" 순서로 보인다. 증상은 접힌 상태에서도 칩으로 보인다.
- 정제 규칙이 CLI와 서버 두 곳에 있다. 코퍼스가 둘을 묶지만, 규칙을 바꿀 때는 두 곳을 함께 고쳐야 한다.
- 증상 판별은 정규식이라 놓치는 경우가 있다. 판별하지 못하면 "알 수 없는 실패"로 기본 항목만 담는다. 틀린 증상보다 낫다.
- `quno run`으로 감싸지 않은 실패는 잡지 못한다. 이는 의도된 한계다(상시 감시 안 함).

## 관련 문서

- [PLAN.md](../../../PLAN.md) Phase 93
- [ADR-0070](0070-question-as-evidence-environment-snapshot-and-verification-tiers.md) 환경 스냅샷 원칙
- [ADR-0080](0080-quno-cli-and-mcp-server-typescript-package.md) CLI·MCP
- [ADR-0081](0081-engine-suggestion-layer-and-secret-masking.md) SecretDetector
- [ADR-0083](0083-environment-snapshot-tier0-collector.md) 환경 스냅샷 1차
