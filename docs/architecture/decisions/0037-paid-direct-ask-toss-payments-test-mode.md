# ADR-0037: 유료 Direct Ask를 토스페이먼츠 테스트 모드로 구현하고, 전문가 지급대행은 범위 밖에 둔다

- 날짜: 2026-09-01
- 상태: 승인됨

## 배경 (Context)

[ADR-0034](0034-organization-virtual-only-direct-ask-no-payment.md)는 실제 카드·계좌 정보를 다루는 PG 연동을 안전상의 이유로 범위 밖에 두고 Direct Ask를 무료 요청/수락만으로 구현했다. `PLAN.md`는 "PG 연동, 결제 처리 범위 등 핵심 설계가 아직 없어 착수 시점에 다시 설계한다"고 남겨뒀다.

이번 Phase 착수 전 사용자에게 결제 방식을 확인했다(2026-09-01): 토스페이먼츠 테스트 모드 연동(실 계좌 없이 진짜 결제 프로토콜을 끝까지 구현·검증) vs 내부 포인트/크레딧 경제(외부 PG 없음) 중 전자를 선택했다.

## 결정 (Decision)

1. **모든 Direct Ask 요청에 정액 수수료(`quno.direct-ask.fee-amount`, 기본 1,000원)를 부과한다** — Phase 22의 무료 흐름을 대체한다(같은 액션에 무료/유료 두 경로를 병행하지 않기로 함, 복잡도 대비 가치가 낮다고 판단).
2. **결제가 확인되기 전까지 요청은 대상에게 보이지 않는다.** `DirectAskRequest`에 `AWAITING_PAYMENT` 상태를 추가해 `PENDING` 앞에 둔다 — `POST /questions/{id}/direct-asks`는 요청과 결제(`DirectAskPayment`, PENDING)를 같은 트랜잭션에서 만들 뿐, `DIRECT_ASK_REQUESTED` 알림은 발행하지 않는다. 클라이언트가 토스 결제위젯을 거쳐 돌아온 뒤 `POST /direct-asks/payments/confirm`을 호출해야 결제가 실제로 확인되고, 그 시점에야 요청이 `PENDING`으로 전이되며 알림이 나간다. 이 게이트가 없으면 결제 없이 요청을 스팸할 수 있다.
3. **거절하면 자동으로 환불한다.** 대상이 받지도 않을 요청에 돈을 내게 하는 건 부당하다고 판단해, `RespondToDirectAskRequestUseCase`가 거절 시 토스 취소 API를 호출해 결제를 `CANCELLED`로 되돌린다. 수락 시에는 결제를 그대로 `PAID`로 유지한다.
4. **전문가에게 실제로 돈을 지급하는 것은 범위 밖이다.** 요청자의 결제는 Quno가 받을 뿐, 그 돈을 전문가 계좌로 실제 송금하는 지급대행(정산) 흐름은 만들지 않았다 — 이는 KYC·세무 신고 같은 완전히 다른 규모의 규제 대응이 필요한 별개 사업 문제다. 원본 기획의 "전문가가 보상을 받을 수 있는 구조로 발전시킬 수 있다"는 표현도 필수가 아니라 발전 방향으로만 제시했었다.
5. **웹훅은 만들지 않는다.** 리다이렉트+승인(confirm) 흐름만으로 카드 결제(이 기능의 유일한 결제 수단)를 완결할 수 있다 — 웹훅은 가상계좌 입금처럼 비동기로 완료되는 결제 수단에 필요한데 다루지 않는다.

### 실제 검증에서 확인한 한계: 결제창 자체는 사람이 완료해야 한다

토스페이먼츠 테스트 모드는 "가상 테스트 카드번호를 제공하지 않는다" — 실제 카드 정보를 입력해야 하며(돈은 안 빠져나간다고 명시함), 오직 그 방식으로만 실제 `paymentKey`가 발급된다. 카드 정보를 어떤 필드에도 입력하지 않는 것은 예외 없이 지켜야 할 안전 원칙이라, 결제위젯을 통한 실제 체크아웃(진짜 `paymentKey` 발급)은 Claude가 직접 완료할 수 없다 — "테스트 모드라 실제 돈이 안 나간다"는 사실이 이 제약을 바꾸지 않는다.

대신 검증은 두 갈래로 나눠 진행했다:
- **비즈니스 로직**(상태 전이, 금액 위조 방지, 중복 요청 방지, 거절 시 환불)은 `FakePaymentGateway`로 단위 테스트했다.
- **실제 아웃바운드 HTTP 계약**(URL, Basic Auth 헤더, JSON 바디 형태)은 로컬 Python HTTP 서버로 만든 Toss 모크(진짜 Toss 서버에는 절대 요청하지 않음)를 세워, `quno.toss.api-base-url`을 그쪽으로 임시 오버라이드해 실제 프로토콜 왕복(승인→상태 전이→알림, 거절→취소 호출→환불)을 curl로 전 구간 검증했다.

실제 Toss 테스트 환경을 통한 카드 결제 완료(진짜 `paymentKey` 발급)까지 확인한 사람 검증은 아직 없다 — 사용자가 직접 한 번 결제위젯을 완주해보면 이 갭이 닫힌다.

### 부수 발견: `RestClient.builder()` 정적 팩토리는 요청 바디를 직렬화하지 못했다

로컬 모크 서버로 검증하는 과정에서 실제 버그를 발견했다: `TossPaymentGateway`가 `RestClient.builder()`(정적 팩토리)로 만든 클라이언트로 POST 요청을 보내면, 요청 바디가 **조용히 빈 `{}`로 직렬화**됐다(예외 없음). 처음엔 요청/응답 DTO가 `TossPaymentGateway` 내부의 `private data class`(nested)라서 Jackson이 리플렉션 접근을 못 하는 것으로 추정해 파일 최상위 `private data class`로 옮겼지만 재현이 계속됐다 — 이 첫 진단은 틀렸다.

실제 원인은 `RestClient.builder()` 정적 팩토리가 Spring Boot의 메시지 컨버터 자동구성(이 프로젝트의 Jackson 3 `tools.jackson` 컨버터 포함)을 거치지 않는다는 것이었다. Spring Boot가 자동구성한 `RestClient.Builder` 빈을 주입받아 쓰는 표준 해법을 시도했으나, 이 프로젝트가 쓰는 세분화된 스타터(`spring-boot-starter-webmvc`)로는 `RestClient.Builder` 빈 자체가 등록되지 않아 애플리케이션이 기동조차 되지 않았다(`RestClientAutoConfiguration`이 발동하지 않음).

최종 해법: 컨버터 자동 선택에 기대지 않고, 이미 이 코드베이스 곳곳에서 검증된 Jackson 3 `ObjectMapper` 빈을 직접 주입받아 요청/응답을 수동으로 `writeValueAsString`/`readValue`하고, `RestClient`에는 순수 문자열만 주고받게 했다. [Phase 21](0033-technology-version-scan-detection-only-no-auto-outdated.md)의 `EndOfLifeDateTechnologyReleaseFeed`도 같은 정적 `RestClient.builder()`를 쓰지만 GET 응답 파싱만 하고(요청 바디 없음) 실제로 잘 동작해왔다 — 즉 이 프로젝트의 정적 `RestClient.builder()`는 응답 디코딩은 되지만 요청 바디 인코딩은 안 되는 비대칭적 함정으로 보인다. 향후 이 코드베이스에서 outbound POST/PUT을 만들 때는 이 ADR의 해법(ObjectMapper 직접 사용)을 기본으로 삼아야 한다.

## 결과 (Consequences)

- Direct Ask는 이제 항상 유료다 — Phase 22의 무료 흐름은 더 이상 존재하지 않는다.
- `DuplicateDirectAskException`을 막는 부분 유니크 인덱스가 `AWAITING_PAYMENT`도 포함하도록 넓어졌다 — 결제하지 않고 방치된 요청은 만료·정리 로직 없이 그 자리에 영구히 남아 같은 (질문, 대상) 쌍의 새 요청을 막는다. 실사용에서 문제가 되면 만료 처리를 추가한다(EmailDomainVerification과 동일한 종류의 알려진 단순화).
- 프로덕션 토스페이먼츠 자격증명(실제 가맹점 키)은 이 코드베이스가 구성하지 않는다 — JWT secret·Mailpit과 같은 패턴으로 배포 시점에 환경변수로 주입해야 한다.
- 결제창 UI 자체(프론트엔드)는 이번 Phase에 포함하지 않는다 — 다른 모든 Phase와 동일하게 백엔드까지만([ADR-0020](0020-frontend-scoped-to-backend-support.md)).

## 관련 문서

- [domain-model.md](../domain-model.md#trust-network-phase-22)
- [api-design.md](../api-design.md#유료-direct-ask-phase-25)
- [ADR-0034](0034-organization-virtual-only-direct-ask-no-payment.md) (이번 ADR이 대체하는 무료 Direct Ask 결정)
- [ADR-0033](0033-technology-version-scan-detection-only-no-auto-outdated.md) (같은 정적 `RestClient.builder()` 패턴을 쓰지만 이번에 발견된 함정에 걸리지 않은 선례)
- [PLAN.md](../../../PLAN.md) Phase 25
