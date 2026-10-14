# ADR-0066: Playwright e2e를 워커 1개로 순차 실행한다

- 날짜: 2026-10-14
- 상태: 승인됨

## 배경 (Context)

로컬 e2e에서 소스를 고친 직후의 첫 실행만 가끔 실패하는 문제가 반복됐다. 콜드 dev 서버에서 여러 워커가 서로 다른 라우트를 동시에 처음 열면, Turbopack의 Fast Refresh가 다른 탭에서 입력 중이던 `/ask` 폼을 비웠다(진단: [technical-deep-dives.md 15절](../../engineering/technical-deep-dives.md#15-로컬에서만-첫-실행이-깨지는-e2e--병렬-워커와-fast-refresh)).

`playwright.config.ts`는 이미 "백엔드 상태를 공유하는 테스트가 섞일 수 있어 순차 실행이 더 예측 가능하다"며 `fullyParallel: false`를 두고 있었다. 하지만 이 옵션은 파일 내부만 순차로 만들 뿐, 파일 간 병렬은 막지 않는다.

## 결정 (Decision)

- `workers: 1`을 명시해 모든 e2e를 한 워커에서 순서대로 실행한다.
- 기각한 대안:
  - **e2e를 프로덕션 빌드(`next build && next start`)에서 돌리기**: HMR이 없어 이 문제는 사라진다. 하지만 `NEXT_PUBLIC_*`가 빌드 시점에 고정되므로 로컬·CI 실행 방식과 빌드 시간이 크게 바뀐다. 순차 실행은 원래 설정이 의도한 동작이기도 해서 이쪽을 택했다.
  - **테스트마다 hydration·컴파일 완료 대기 추가**: 증상만 가리고, 다른 탭의 Fast Refresh는 언제든 다시 끼어들 수 있다.

## 결과 (Consequences)

- 로컬 e2e가 콜드 스타트에서도 안정적으로 통과한다. 수정 후 콜드 실행 2회 모두 19/19였다.
- 로컬 실행 시간이 늘어난다. 콜드 약 1~2.5분, 웜 약 40초 수준이다.
- CI는 러너 코어가 적어 원래도 워커가 사실상 1개였으므로 동작이 바뀌지 않는다.
- 테스트 수가 크게 늘어 실행 시간이 문제가 되면, 파일별 데이터 격리를 갖춘 뒤 프로덕션 빌드 기반 병렬 실행을 재검토한다.

## 관련 문서

- [technical-deep-dives.md 15절](../../engineering/technical-deep-dives.md)
