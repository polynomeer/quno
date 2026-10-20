# Phase 91 — 실험 실행 가이드

[실험 설계](phase-91-validation-experiment.md)를 실제로 돌리는 절차다. 준비물(과제, 코퍼스, 하네스, 집계 도구)은 모두 저장소에 있고, **에이전트 실행은 사람이 로그인한 환경에서 시작해야 한다**(아래 0절). 이 문서의 도구는 `claude -p`(Claude Code 헤드리스 모드)를 대상 에이전트로 쓴다. 다른 에이전트를 쓰려면 `experiments/phase-91-tasks/scripts/run-one.py`의 명령 조립부만 바꾸면 된다.

## 0. 사전 조건

| 항목 | 내용 |
|---|---|
| 인증 | `claude`가 로그인되어 있어야 한다(터미널에서 `claude` 실행 후 `/login`) 또는 `ANTHROPIC_API_KEY`. **로그인이 만료되면 실행이 즉시 "Failed to authenticate"로 끝나고, 하네스가 이를 무효 실행(`runs/_invalid/`)으로 분리한 뒤 연속 2회에서 중단한다.** 2026-10-20에 이 환경에서 실제로 발생했다 |
| 런타임 | JDK 21, Node 20+, python3, bash. B01은 Docker(`postgres:18.1`), B04·C05는 python3(포트 점유) |
| 네트워크 | Maven Central 등. 첫 실행은 의존성을 내려받는다 |
| 코퍼스 | `experiments/phase-91-mcp/build-corpora.sh`로 `corpora/build/` 생성 |
| 다른 Gradle 작업 | 실험 중에는 돌리지 않는다(병렬 실행은 데몬이 사라지는 간헐 실패를 일으킨 적이 있다) |

## 1. 에이전트 고정

설계 2.1절: 에이전트·모델 버전을 실험 내내 바꾸지 않는다. `--model`로 고정하고, 시작 시 `claude --version`, 모델 ID, 학습 시점을 기록한다(B층 일부 과제가 학습 데이터에 있을 수 있어 A층과 구분해 해석한다). 결과 JSON의 `modelUsage`에 실제 모델이 남는다.

## 2. 격리와 공정성 (하네스가 하는 일)

- 작업 디렉터리는 `scripts/prepare.sh`가 만든 복사본이고 `answers/`가 없다.
- 코퍼스(정답·미끼 포함), MCP 설정, 호출 로그는 작업 디렉터리 밖(`/tmp/quno-runs_private/<run>/`)에 둔다.
- 파일 도구는 `./**`로 한정하고, 임의 경로를 읽는 셸 명령은 허용하지 않는다(`--permission-mode dontAsk` + `--allowedTools`). **파일럿에서 이 제한이 실제로 의도대로 동작하는지 확인한다**(예: 에이전트가 `../`를 읽으려다 거부되는지). 구멍이 있으면 정답이 노출되므로 본 실험 전에 고친다.
- 프롬프트는 모든 조건에서 같고 Quno 도구를 쓰라는 문구가 없다 — 호출 여부가 H1의 측정 대상이다.
- 판정은 에이전트의 보고가 아니라 `./CHECK`의 종료 코드로 한다(L 과제 제외).

## 3. 파일럿 (먼저)

```bash
cd experiments/phase-91-mcp && ./build-corpora.sh
```

```bash
cd experiments/phase-91-tasks && scripts/run-matrix.py --pilot
```

5회(C07·A03·A03(정답 포함)·A01(시드)·B07(L))를 돌린다. 확인할 것:

1. 에이전트가 실제로 일했는가(`runs/*.json`의 `num_turns`, `total_cost_usd`, `usage` 모두 0이 아님).
2. 정답 조건(A03-A2)에서 Quno 도구를 호출했는가(`quno_calls`), 호출 로그(`runs/*.calls.jsonl`).
3. 작업 디렉터리 밖 파일을 읽으려는 시도가 거부되는가(결과의 `result` 요약, 필요하면 세션 기록).
4. **회당 비용과 시간** → 아래 4절의 상한을 정한다.

## 4. 본 실험

```bash
scripts/run-matrix.py --runs 3 --l-runs 1 --budget 3 --max-total-usd <상한> --model <고정 모델>
```

- 행렬: R/D 26개 × 5조건(A0, A1, A2, A3a, A3b) × 3회 + L 4개 × 5조건 × 1회(B09는 A2·A3 제외) ≈ **407회**. 설계서의 "4조건 312회"에서 A3을 라벨 유/무(A3a/A3b)로 나눠 늘었다.
- 회당 `--budget`(기본 3 USD)이 단일 실행의 상한이고, `--max-total-usd`가 누적 상한이다. 상한에 닿으면 멈추고, 다시 실행하면 **이어서** 진행한다(결과가 있는 실행은 건너뜀).
- 비용이 크면 설계 2.3절대로 A·B층 20개 위주로 줄인다: `--only A01,...,B10`. 줄인 내용을 설계서 변경 이력에 남긴다.
- 실행 순서는 시드 91로 섞여 있다(조건이 시간 순서에 치우치지 않도록).

## 5. 집계와 판정

```bash
scripts/aggregate.py
```

조건·층별 해결률, 시간·비용 중앙값, 호출률, A0 대비 쌍체 시간 단축, 미끼 조건의 오염 후보를 출력한다. L 과제는 포함하지 않는다.

```bash
scripts/judge-sheet.py
```

`runs/judge-sheet.md`(조건 비공개)와 `runs/judge-key.json`(평가자에게 주지 않음)을 만든다. **평가자는 조건을 모르는 상태로** 각 기록의 관련도(2/1/0)를 매긴다. 기록 id가 `oracle-`/`decoy-`로 시작하는 것은 시트에서 이미 익명화된다. 가능하면 설계자(이 저장소의 작업자)와 다른 사람이 한다.

- **L 과제**: `runs/*.json`의 `diagnosis_md`를 `answers/<ID>.md`의 루브릭(0~4점, -1 감점)으로 채점한다. 이것도 조건을 가리고 한다.
- **오염률(H4)**: A3 조건에서 에이전트가 미끼의 수정을 실제로 적용했는지는 사람이 세션 기록으로 확인한다(집계 도구는 후보만 뽑는다).
- 설계 3.1~3.3의 판정 규칙을 그대로 적용하고, 결과 해석 시 설계 3.4의 변경 사실(기준 2 변경)과 한계를 함께 적는다.

## 6. 보고

설계 7절의 산출물: 원시 실행 로그, 층별 결과, 판정(Go/Narrow/Pivot), 결정 ADR. 원시 결과(`runs/`)에는 대화 내용이 들어 있어 커밋하지 않고(`.gitignore`), 집계 표와 판정 근거만 문서로 남긴다.
