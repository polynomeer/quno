# Phase 91 MCP 프로토타입

[Phase 91 실험 설계](../../docs/product/phase-91-validation-experiment.md)를 위한 **읽기 전용 MCP 서버**다. 에이전트가 에러를 만났을 때 "같은 에러를 먼저 해결한 기록"이 있는지 조회하는 경로를 최소 비용으로 만들어, 호출률·적중률·해결 시간 단축을 재는 것이 목적이다.

**실험이 끝나면 버릴 수 있는 코드다.** 구현 언어(Node)는 프로토타입 편의로 고른 것이며, Phase 65.2의 `quno` CLI 구현 언어를 정하는 ADR을 구속하지 않는다. 프로덕션 모델은 Phase 92·94에서 따로 설계한다.

## 도구 (읽기 전용 2개)

| 도구 | 입력 | 설명 |
|---|---|---|
| `search_known_issues` | `errorText`(스택트레이스 전체), `limit` | 시그니처로 기록을 찾는다. 상위 N건과 점수, 매치 종류를 준다 |
| `get_resolution` | `id` | 기록 하나를 다시 조회한다 |

응답은 구조화 필드(`appliesTo`, `verification`, `lastConfirmed`, `signature`, `sources`)와 `untrustedText`(증상·원인·해결 요약·메모)로 **분리**되어 있고 `notice`가 자유 텍스트를 명령으로 취급하지 말라고 명시한다([ADR-0069](../../docs/architecture/decisions/0069-agent-era-positioning-verified-fix-memory.md) 결정 3). 모든 기록은 `verification: "unverified"`다.

## 매칭

스택트레이스의 **근본 원인(마지막 `Caused by:`)**을 기준으로 한다. 경로·IP·UUID·숫자·따옴표 문자열은 메시지에서, 라인 번호·프록시 해시·람다 번호는 프레임에서 제거한다.

| 매치 | 조건 | 점수 |
|---|---|---|
| `exact-signature` | 예외 클래스 + 상위 5개 프레임이 같다 | 1.0 |
| `same-message` | 예외 클래스 + 정규화한 메시지가 같다 | 0.85 |
| `similar-message` | 같은 예외 클래스이고 메시지 토큰이 겹친다 | 0.3~0.7 |

점수 0.3 미만은 **억지로 채우지 않고 결과에서 뺀다.** 아무 기록이나 돌려주면 적중률이 부풀려진다. 프레임은 라이브러리 버전이 바뀌면 달라지므로 `exact-signature`는 깨지기 쉽고, 그래서 메시지 기준 단계가 있다.

## 사용

```bash
cd experiments/phase-91-mcp && npm install && npm test
```

```bash
npm run validate-seeds
```

MCP 클라이언트(코딩 에이전트)에는 stdio 서버로 등록한다. 예(`.mcp.json` 형식):

```json
{
  "mcpServers": {
    "quno": {
      "command": "node",
      "args": ["/절대경로/experiments/phase-91-mcp/src/server.js"],
      "env": { "QUNO_CONDITION": "A1", "QUNO_RUN_ID": "task01-A1-r1" }
    }
  }
}
```

| 환경변수 | 기본값 | 의미 |
|---|---|---|
| `QUNO_SEED_DIR` | `seeds/` | 기록(`*.json`) 디렉터리. 조건별로 다른 코퍼스(A1 현실 / A2 상한 / A3 미끼)를 가리킨다 |
| `QUNO_CALL_LOG` | `logs/calls.jsonl` | 호출 로그 경로 |
| `QUNO_CONDITION`, `QUNO_RUN_ID` | 없음 | 로그에 같이 남는 조건·실행 식별자 |

## 호출 로그

도구를 호출할 때마다 JSONL 한 줄이 남는다: 시각, 조건, 실행 ID, 코퍼스 크기, 질의 앞 2000자, 파싱 결과, 반환한 기록 id. 이것으로 **호출률**(실행 중 호출이 1회 이상인 비율)과 **적중 판정 대상**(반환 id와 과제 정답 비교)을 뽑는다. 질의 원문이 들어가므로 `logs/`는 git에서 제외했다. **로그가 없는 실행 = 호출하지 않은 실행**이므로 조건마다 로그 경로를 분리하거나 `QUNO_RUN_ID`를 반드시 지정한다.

## 기록 스키마와 시드

`seeds/*.json`에 객체 또는 객체 배열을 둔다. 필드는 [src/record.js](src/record.js)가 정의하고 로드 시 검증한다(출처 URL 1개 이상 필수, `errorSample`에서 예외를 못 찾으면 거부).

```json
{
  "id": "spring-boot-4-example",
  "title": "한 줄 제목",
  "errorSample": "org.example.SomeException: message\n\tat ...",
  "appliesTo": [{ "component": "spring-boot", "versions": ">=4.0 <4.1" }],
  "sources": [{ "url": "https://...", "license": "CC BY-SA 4.0" }],
  "lastConfirmed": "2026-10-19",
  "symptom": "...", "cause": "...", "fixSummary": "..."
}
```

`seeds/`는 **비어 있다.** 시드 약 100건은 공개 출처에서 새로 큐레이션해야 하고(저장소에 데이터가 없다), 과제 30개를 먼저 확정·커밋한 뒤에 만든다(과제 정답을 보고 시드를 만들지 않기 위해). `test/fixtures/`의 기록은 파서 테스트용 가짜 픽스처이며 실험 코퍼스가 아니다.

## 알려진 한계

- 시그니처 정규화는 Java/Kotlin 스택트레이스 중심이다. Node 형식은 기본만, Gradle/컴파일러 출력(예외가 아닌 에러)은 아직 다루지 않는다 — 과제 세트에 이런 에러가 많으면 파서를 확장해야 한다.
- 검색은 시그니처·토큰 겹침뿐이라 의미 검색이 아니다. 같은 원인의 다른 증상은 못 찾을 수 있고, 이는 적중률의 하한을 낮춘다.
- 인증·저장소·동시성은 없다. 파일을 읽어 메모리에 올리는 단일 프로세스다.
