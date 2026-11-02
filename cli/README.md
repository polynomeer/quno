# quno CLI · MCP 서버

터미널과 코딩 에이전트에서 Quno를 쓰는 도구입니다(PLAN.md Phase 65, [ADR-0080](../docs/architecture/decisions/0080-quno-cli-and-mcp-server-typescript-package.md)). 실행 파일은 `quno` 하나이고, MCP 서버는 `quno mcp`로 띄웁니다.

## 설치

Node 20 이상이 필요합니다. 아직 npm에 게시하지 않았으므로 저장소에서 빌드해 연결합니다.

```bash
cd cli && npm ci && npm run build && npm link
```

## 로그인

웹의 **프로필 → 액세스 토큰**(`/settings/tokens`)에서 토큰을 만듭니다.

- 검색과 MCP만 쓰려면 "읽기" 권한이면 충분합니다.
- `quno ask`로 질문까지 올리려면 "질문 작성" 권한도 고릅니다.

토큰은 셸 기록에 남지 않도록 표준 입력으로만 넘깁니다.

```bash
pbpaste | quno login --api-url https://api.quno.example --web-url https://quno.example
```

- 설정은 `~/.config/quno/config.json`(권한 600)에 저장됩니다.
- 환경 변수 `QUNO_API_URL`·`QUNO_WEB_URL`·`QUNO_TOKEN`이 있으면 설정 파일보다 우선합니다.

## 명령

| 명령 | 하는 일 |
|---|---|
| `quno whoami` | 토큰 주인 확인 |
| `quno search hikari pool` | 질문 검색(`--limit`, `--json`) |
| `cat error.log \| quno errors` | 같은 에러(예외 클래스 + 상위 프레임)를 다룬 질문 찾기. `--file`로도 줄 수 있음 |
| `cat error.log \| quno ask --title "..." --body "..."` | 질문 작성. 표준 입력은 로그가 됨 |
| `quno capture` | 질문에 첨부될 환경 스냅샷(Tier 0)을 그대로 보여줌(`--json`이면 JSON만) |

`quno ask`는 게시 전에 다음을 보여줍니다.

- 현재 디렉터리의 빌드 파일(`build.gradle.kts`·`build.gradle`·`pom.xml`·`package.json`)에서 읽은 스택 지문
- 같은 에러를 다룬 기존 질문

그 뒤 터미널에서 `y`를 받아야 게시합니다. 파이프 등으로 확인을 받을 수 없으면 `--yes`가 있어야 게시합니다.

- 사내 패키지는 `--exclude 'com.acme:*'`(반복 가능, 끝에 `*`면 접두사 일치)로 뺍니다.
- 스택 지문을 아예 붙이지 않으려면 `--no-stack`을 씁니다.
- 이 컴퓨터의 환경 스냅샷(OS·JDK·Node·컨테이너·로케일·도구 버전·git 커밋·잠금 파일의 해석된 의존성)도 함께 붙습니다. `--no-snapshot`으로 뺄 수 있습니다.
  - 홈 경로·사용자명·호스트명은 지우고, git 원격은 호스트만 남깁니다.
  - 환경 변수는 CI 표시 같은 정해진 이름이 있는지만 보고, 값은 읽지 않습니다.
  - 정확히 무엇이 나가는지는 `quno capture`로 확인할 수 있습니다([ADR-0083](../docs/architecture/decisions/0083-environment-snapshot-tier0-collector.md)).
- 원본 빌드 파일은 서버에 저장되지 않습니다.

## MCP 서버

읽기 전용 도구 4개를 제공합니다. 출력은 본문 필드당 4,000자, 응답 전체 20,000자에서 잘립니다.

| 도구 | 하는 일 |
|---|---|
| `search_known_issues` | 에러 텍스트로 같은 에러를 다룬 질문 찾기 |
| `search_questions` | 키워드 검색 |
| `get_question` | 질문과 답변(채택 답변 먼저, 적용 버전 범위 포함) |
| `get_my_stack` | 웹에서 저장한 "내 스택" |

Claude Code에 연결하는 예:

```bash
claude mcp add quno -- quno mcp
```

설정 파일로 연결하는 클라이언트라면 다음과 같이 씁니다.

```json
{
  "mcpServers": {
    "quno": { "command": "quno", "args": ["mcp"], "env": { "QUNO_TOKEN": "quno_pat_..." } }
  }
}
```

Quno의 답변은 사람이 쓴 기록이고 자동으로 검증되지 않았습니다. 도구 설명도 에이전트에게 답변의 적용 범위와 현재 버전을 비교한 뒤 쓰라고 안내합니다.

## 개발

```bash
npm test          # vitest — 가짜 fetch로 명령과 MCP 도구를 검증
npm run typecheck
npm run build     # dist/
```
