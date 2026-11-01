# ADR-0080: `quno` CLI와 MCP 서버 — TypeScript npm 패키지 하나, MCP는 읽기 전용 4개 도구와 출력 상한, 스택 첨부는 미리 보여주고 확인받는다

- 날짜: 2026-11-01
- 상태: 승인됨

## 배경 (Context)

[ADR-0079](0079-personal-access-tokens-and-scopes.md)로 브라우저 밖에서 쓸 개인 액세스 토큰이 생겼다. PLAN.md Phase 65의 남은 작업은 두 가지다.

- **65.2 CLI:** `ask`(파이프로 로그 입력, 스택 지문 자동 첨부), `search`, 에러 시그니처 사전 조회
- **65.3 MCP 서버:** 읽기 전용 도구부터

PLAN은 구현 언어와 배포 방식을 ADR로 정하라고 했다. Phase 91 실험은 이미 Node + 공식 MCP SDK로 읽기 전용 MCP 프로토타입(`experiments/phase-91-mcp`)을 만들었다.

## 결정 (Decision)

### 언어와 배포: TypeScript, npm 패키지 하나(`cli/`)

- 저장소 루트에 `cli/`를 두고 `@quno/cli` 패키지를 만든다. 실행 파일은 `quno` 하나다. MCP 서버는 같은 패키지의 `quno mcp` 하위 명령으로 띄운다.
  - 설정(API 주소·토큰)과 HTTP 클라이언트를 두 진입점이 공유한다.
  - 코딩 에이전트 설정에는 `npx -y @quno/cli mcp` 한 줄이면 된다.
- 근거
  - 공식 MCP SDK는 TypeScript 구현이 가장 성숙하고, Phase 91 프로토타입과 같은 SDK·버전(1.32.1)을 쓴다.
  - 프론트엔드와 같은 언어라 API 타입을 옮겨 오기 쉽다.
  - Node 20 이상이 있는 개발 환경이면 따로 설치하지 않고 `npx`로 실행할 수 있다.
- 기각한 대안
  - **Kotlin/JVM(백엔드와 같은 언어):** 시작이 느리고 배포물이 무겁다. MCP 클라이언트가 도구 호출 때마다 프로세스를 띄우는 상황에서 불리하다.
  - **Go 단일 바이너리:** 배포는 가장 깔끔하다. 하지만 이 저장소에 Go 도구 체인이 없고, MCP SDK도 TypeScript보다 뒤처진다.
- 런타임 의존성은 MCP SDK와 zod 둘뿐이다. 인자 파싱은 `node:util`의 `parseArgs`, HTTP는 내장 `fetch`로 한다.
- npm 레지스트리 게시는 이번 범위가 아니다. 지금은 저장소에서 빌드해 `npm link`로 쓴다. 게시 절차와 버전 정책은 실제 사용자가 생길 때 정한다.

### 설정과 인증

- 우선순위: 환경 변수(`QUNO_API_URL`, `QUNO_TOKEN`) → 설정 파일(`~/.config/quno/config.json`, 권한 600) → 기본 주소.
- `quno login`은 토큰을 표준 입력으로만 받는다. 명령 인자로 받으면 셸 기록에 남기 때문이다. 받은 토큰은 `GET /me`로 확인한 뒤 저장한다.

### `quno ask`의 스택 첨부: 자동으로 찾되, 게시 전에 보여주고 확인받는다

- 현재 디렉터리에서 `build.gradle.kts`·`build.gradle`·`pom.xml`·`package.json`을 찾는다. 서버의 파싱 API로 의존성을 읽어 질문에 붙인다.
- 웹에서는 미리보기 체크박스로 사내 패키지를 뺄 수 있다([ADR-0076](0076-stack-fingerprint-dependency-parsing.md)). CLI에서는 그 역할을 다음이 대신한다.
  - `--exclude <패턴>`(반복 가능, `com.acme:*`처럼 끝에 `*`)과 `--no-stack`으로 뺀다.
  - 게시 직전에 제목과 붙일 의존성 목록을 출력하고 `y`를 받아야 게시한다.
  - 표준 입력이 터미널이 아니면(파이프로 로그를 넘기는 경우) `--yes`가 있어야 게시한다. 확인 없이 사내 패키지명이 외부로 나가는 일을 막는다.
- 표준 입력으로 받은 로그는 질문의 `logs`가 된다. 게시 전에 같은 에러를 다룬 기존 질문을 먼저 보여준다(에러 시그니처 미리보기).

### MCP 도구: 읽기 전용 4개와 출력 상한(65.4의 나머지)

| 도구 | 하는 일 |
|---|---|
| `search_known_issues` | 에러 텍스트로 에러 시그니처를 계산하고 같은 에러를 다룬 질문을 찾는다 |
| `search_questions` | 키워드 검색 |
| `get_question` | 질문 본문, 스택 지문, 답변(채택 답변 먼저, 적용 범위 포함) |
| `get_my_stack` | 사용자의 "내 스택" |

- 모두 `readOnlyHint`이고, READ 스코프 토큰으로 충분하다.
- **출력 상한:** 본문 필드는 4,000자, 도구 응답 전체는 20,000자에서 자르고 `truncated: true`를 남긴다. 상한은 서버 API가 아니라 도구 쪽에 둔다(ADR-0079). 에이전트의 컨텍스트를 지키는 것이 목적이기 때문이다.
- **기록은 미검증이라는 점을 도구 설명에 적는다.** 답변의 적용 범위와 현재 환경을 비교한 뒤 쓰라고 안내한다. Phase 91 프로토타입의 문구를 이어받는다.
- **"내 스택 기준 OUTDATED 확인"(PLAN 65.3)은 이번에 하지 않는다.** 답변 적용 범위와 내 스택을 비교해 판정하는 규칙은 Phase 73(내 스택 렌즈)이 서버에 둘 예정이다. 클라이언트에 버전 비교기를 하나 더 만들면 판정이 두 곳으로 갈라진다. 그동안은 `get_question`의 적용 범위와 `get_my_stack`을 에이전트가 직접 비교한다.
- **쓰기 도구(질문 초안 등)는 두지 않는다.** PLAN 65.3은 쓰기에 사용자 승인을 요구하고, ADR-0069는 쓰기 도구를 Phase 92·93·95로 미뤘다.

## 결과 (Consequences)

- 터미널에서 `cat error.log | quno ask --title "..."` 한 줄로 질문하고, 코딩 에이전트에서 Quno를 검색할 수 있다.
- 저장소에 Node 패키지가 하나 더 생긴다. 그래서 CI에 `cli` 작업을 추가하고, Dependabot npm 대상에도 넣는다.
- Phase 91 프로토타입(`experiments/phase-91-mcp`)은 실험용 시드 데이터로 동작하는 별도 코드로 남는다. 이 패키지는 실제 API를 부른다.

## 관련 문서

- [PLAN.md](../../../PLAN.md) Phase 65
- [ADR-0069](0069-agent-era-positioning-verified-fix-memory.md) 에이전트 시대 포지셔닝
- [ADR-0075](0075-error-signature-normalization-and-matching.md) 에러 시그니처
- [ADR-0079](0079-personal-access-tokens-and-scopes.md) 개인 액세스 토큰
