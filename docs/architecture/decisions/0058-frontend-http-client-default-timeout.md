# ADR-0058: 프론트엔드 `httpClient`에 기본 요청 타임아웃(15초)을 추가한다

- 날짜: 2026-09-24
- 상태: 승인됨

## 배경 (Context)

[failure-scenario-testing.md](../../operations/failure-scenario-testing.md) F2 시나리오(`docker pause postgres`로 백엔드는 떠 있지만 DB 의존 엔드포인트만 응답이 없는 상태를 재현)를 실행하다가, `frontend/src/shared/api/http-client.ts`의 `request()`가 `fetch()`를 호출할 때 `AbortController`/`signal`을 전혀 넘기지 않는다는 것을 발견했다 — 클라이언트 자체 타임아웃이 어디에도 없다.

지금은 이 문제가 겉으로 드러나지 않는다. [ADR-0054](0054-hikari-connection-timeout-fast-fail.md)(HikariCP connection-timeout)와 [ADR-0055](0055-mongo-server-selection-timeout-fast-fail.md)(Mongo `serverSelectionTimeout`)가 각각 3초로 고정돼 있어서, 백엔드가 의존 컴포넌트 장애 상황에서도 항상 몇 초 안에 응답(에러든 성공이든)을 보내기 때문이다. 그래서 F2를 실제로 재현해도 화면이 무한정 멈추지 않고 3초 안팎에 에러로 전환됐다.

하지만 이건 프론트엔드의 회복력이 백엔드의 타임아웃 설정에 암묵적으로 전적으로 의존한다는 뜻이다 — 새로 추가되는 API 경로가 이 두 바운드(HikariCP/Mongo) 밖에 있는 외부 호출(예: 향후 서드파티 API를 백엔드가 아니라 프론트에서 직접 부르는 경우, 혹은 프록시/CDN 계층의 지연)을 갖게 되면, 백엔드 설정과 무관하게 프론트가 무한 대기할 수 있다. 이 상태는 정책적으로 남겨둘 문제가 아니라 방어선을 하나 더 두는 게 맞다고 판단했다.

## 결정 (Decision)

`httpClient`의 `request()`가 `fetch()`에 기본적으로 `AbortSignal.timeout(15_000)`을 `signal`로 넘기도록 한다(호출자가 직접 `signal`을 넘기면 그것을 우선한다). 15초는 ADR-0054/0055가 보장하는 3초 바운드에 네트워크 왕복 지연·JSON 파싱 여유를 넉넉히 얹은 값으로, 정상 상황에서는 절대 걸리지 않지만 그 바운드를 벗어난 요청에도 UI가 무한정 멈추지 않게 하는 마지막 방어선이다.

타임아웃으로 인한 abort는 `ApiError`가 아니라 별도의 `RequestTimeoutError`(`frontend/src/shared/api/api-error.ts`)로 던진다 — `ApiError`는 서버가 실제로 응답한 HTTP status/code를 의미하는데, 타임아웃은 서버 응답 자체가 없는 별개의 실패 모드이기 때문이다. `AbortSignal.timeout()`이 만드는 `DOMException("TimeoutError")`를 `request()`의 `catch`에서 감지해 변환한다. 호출자(`QuestionDetailContent.tsx` 등, `error instanceof ApiError`로 404 등을 분기하던 곳)가 `RequestTimeoutError`를 별도로 분기해 "요청 시간이 초과됐습니다" 같은 메시지를 보여줄 수 있게 했다.

전역 재시도(retry)는 추가하지 않았다 — 이미 401 재발급 재시도 경로가 있는 상태에서 타임아웃까지 자동 재시도를 얹으면 실패 사유(진짜 장애 vs 일시적 지연)가 섞여 디버깅이 어려워지고, React Query가 쿼리 레벨에서 이미 재시도 정책을 갖고 있어 중복이다.

## 결과 (Consequences)

- 백엔드 타임아웃 설정이 실수로 풀리거나(예: 새 커스터마이저 없는 외부 연동 추가), 백엔드 자체가 완전히 응답을 멈추는 상황에서도 프론트엔드는 15초 후 명확한 에러로 전환된다 — 더 이상 화면이 무한정 멈추는 경로가 없다.
- `RequestTimeoutError`를 신경 쓰지 않는 기존 호출자는 그대로 동작한다 — `Error`의 서브클래스이므로 기존의 `catch`/에러 바운더리는 이전과 동일하게 잡는다. `FormError` 같은 공용 컴포넌트는 `ApiError`가 아닌 에러에 대해 이미 `fallback` 문구로 대체 표시하므로 별도 분기 없이도 깨지지 않는다.
- 15초는 임의의 값이다 — 실제 운영 트래픽에서 느린 네트워크(모바일 등) 요청이 이 값을 넘겨 정상 요청이 타임아웃으로 오인되는 사례가 나오면 재검토한다.
- 이 타임아웃은 `httpClient`를 거치는 모든 요청에 일괄 적용된다. 파일 업로드처럼 시간이 오래 걸릴 수 있는 요청이 이 경로를 타게 되면(현재는 없음) 호출자가 직접 더 긴 `signal`을 넘겨 재정의해야 한다.

## 관련 문서

- [failure-scenario-testing.md](../../operations/failure-scenario-testing.md) F2
- [0054-hikari-connection-timeout-fast-fail.md](0054-hikari-connection-timeout-fast-fail.md)
- [0055-mongo-server-selection-timeout-fast-fail.md](0055-mongo-server-selection-timeout-fast-fail.md)
- `frontend/src/shared/api/http-client.ts`, `frontend/src/shared/api/api-error.ts`
