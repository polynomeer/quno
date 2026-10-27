# ADR-0075: 에러 시그니처 — 근본 원인 기준 정규화, 최신 판본만 색인, 질문 작성·검색·관련 질문에서 공통 사용

- 날짜: 2026-10-27
- 상태: 승인됨

## 배경 (Context)

PLAN.md Phase 64는 스택트레이스에서 환경에 따라 달라지는 부분을 지워 같은 에러를 같은 시그니처로 묶는다. 그리고 질문을 쓰기 전에 "같은 에러 N건"을 보여준다([competitive-analysis.md](../../product/competitive-analysis.md) 4.2).

Phase 91 실험([experiments/phase-91-mcp](../../../experiments/phase-91-mcp/README.md))에서 같은 목적의 정규화기를 JavaScript로 먼저 만들었다. 시드 105건과 실제 Gradle·Spring 로그로 다듬었다. 이 결과물을 백엔드로 옮기면서 정할 것이 남았다.

- 저장 단위
- 일치 판정
- 라인 번호를 넣을지
- 기존 질문 처리

## 결정 (Decision)

### 정규화 규칙

Phase 91 `signature.js`와 같은 규칙을 Kotlin 도메인 로직(`ErrorSignatureExtractor`, 순수 함수)으로 옮긴다. 해시도 같은 방식(SHA-256 앞 16자)으로 만든다. 그래서 실험 코퍼스와 서비스가 같은 키를 쓴다.

- **근본 원인 기준.** 체인의 마지막 `Caused by:`를 쓴다. 로그에 체인이 여러 개면 모두 낸다.
- **두 개의 키.**
  - `signatureHash` = 예외 클래스 + 정규화한 상위 5개 프레임. 프레임이 없으면 없다.
  - `messageKey` = 예외 클래스 + 정규화한 메시지.
- **라인 번호는 넣지 않는다.** 버전마다 바뀌어 같은 에러를 갈라놓기 때문이다.
- **메시지에서 지우는 것:** 프레임의 프록시 접미사(`$$...BySpringCGLIB$$`)·람다 번호·익명 클래스 번호, 메시지의 경로·IP·UUID·16진 주소·숫자·따옴표 문자열.
- **지우지 않는 것:** JDK "helpful NPE" 메시지의 따옴표 안 코드 이름과 `NoClassDefFoundError`의 클래스 경로는 식별력이 있어서 남긴다.

### 일치 판정

- `signatureHash` 일치를 강한 일치로 먼저 놓는다.
- 그다음 `messageKey`만 일치하는 질문을 놓는다.
- 해시 충돌은 16자(64비트)라 무시할 수준이다. 충돌해도 결과가 "관련 질문 후보"일 뿐이라 피해가 작다.

### 저장

- `question_error_signatures`에 **질문의 최신 판본만** 색인한다.
- 질문 작성·수정 때 본문과 로그에서 다시 계산해 그 질문의 행을 통째로 바꾼다. `version_number`는 어느 판본에서 나왔는지 기록용으로 남긴다.
- 판본별로 모두 쌓는 안은 기각했다. 수정으로 사라진 에러가 계속 일치해 잘못된 후보를 낸다.

### 쓰는 곳

- `POST /api/v1/error-signatures/preview`: 질문 작성 화면에서 로그를 붙이면 "같은 에러 N건"을 보여준다.
- 검색어에 예외가 들어 있으면 시그니처 일치를 기존 전문검색 결과보다 앞에 둔다.
- 관련 질문은 시그니처 일치를 기존 태그 겹침 결과보다 앞에 둔다.

### 기존 질문

색인 이전 질문은 수정될 때 색인된다. 일괄 백필은 운영 데이터가 생기면 별도 작업으로 한다(지금은 로컬·시드 데이터뿐).

### 보류

- "Super Answer 있음" 표시: 질문 요약에 Cluster 정보가 없어 지금은 채택된 답변 여부로 대신한다.
- 다른 언어(Python·Go) 트레이스 형식: Phase 64는 JVM과 Node 형식만 다룬다.

## 결과 (Consequences)

- 질문 작성·수정에 정규식 파싱과 행 교체가 하나 더 붙는다. 입력 크기 상한(20,000자)으로 비용을 묶는다.
- 일치는 단순 동등 비교라 인덱스 두 개(`signature_hash`, `message_key`)로 끝난다.
- Phase 65(CLI/MCP)와 Phase 91의 MCP 조회가 같은 키를 쓸 수 있다.

## 관련 문서

- [PLAN.md](../../../PLAN.md) Phase 64
- [competitive-analysis.md](../../product/competitive-analysis.md) 4.2
- [experiments/phase-91-mcp/src/signature.js](../../../experiments/phase-91-mcp/src/signature.js)
