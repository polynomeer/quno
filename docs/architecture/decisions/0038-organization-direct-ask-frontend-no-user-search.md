# ADR-0038: Organization/Direct Ask 프론트엔드는 사용자 검색 없이 프로필 페이지를 진입점으로 삼는다

- 날짜: 2026-09-01
- 상태: 승인됨

## 배경 (Context)

Phase 22([ADR-0034](0034-organization-virtual-only-direct-ask-no-payment.md))·23([ADR-0035](0035-verified-organization-email-domain-mailpit.md))·25([ADR-0037](0037-paid-direct-ask-toss-payments-test-mode.md))는 Organization/Direct Ask를 백엔드까지만 구현하고 프론트엔드는 매번 범위 밖에 뒀다(Spike Detection·기술 버전 감지와 같은 순서). Phase 25로 유료 Direct Ask까지 백엔드가 끝난 뒤, 남은 프론트엔드 격차 세 가지(태그 상세 정보 보강 / Organization·Direct Ask / 실시간 질문방) 중 어느 것을 다음으로 진행할지 사용자에게 확인했고, Organization/Direct Ask(결제 포함)를 선택했다.

구현 과정에서 진짜 설계 문제가 하나 나왔다: Direct Ask는 "특정 사용자에게 이 질문에 답해달라고 요청"하는 기능인데, 이 프로젝트에는 **사용자를 이름으로 검색하는 API가 없다**(`@mention`도 자동완성 없이 정확히 일치하는 닉네임만 텍스트로 받는다, [roadmap.md §7](../../frontend/roadmap.md)). 요청 생성 화면에서 대상을 어떻게 고를지 정해야 했다.

## 결정 (Decision)

1. **Direct Ask 요청의 진입점은 대상 사용자의 프로필 페이지(`/users/{id}`)로 한다.** 사용자 검색 UI를 새로 만드는 대신, 이미 여러 화면(답변 작성자 링크, Follow 목록, 댓글 `@mention` 등)에서 도달 가능한 프로필 페이지에 `RequestDirectAskPanel`을 배치한다 — `FollowUserButton`이 프로필 페이지에서 대상을 특정하는 것과 같은 패턴이다.
2. **요청에 붙일 질문은 요청자 자신의 질문 중에서 고른다.** 백엔드는 요청자가 질문 작성자인지 검증하지 않지만(`CreateDirectAskRequestUseCase`), 남의 질문에 대해 제3자를 Direct Ask로 부르는 흐름은 UX상 의미가 없어 프론트에서 `useUserProfile(내 id).questions`로 후보를 자기 질문으로만 좁힌다.
3. **결제는 Toss의 호스팅 체크아웃("결제창") API로 처리하고, 성공/실패 리다이렉트를 모두 한 페이지(`/direct-asks/checkout`)로 받는다.** Toss SDK는 CDN 스크립트(`js.tosspayments.com`)로 동적 로드하며, `successUrl`/`failUrl`을 동일한 경로로 지정해 두 경우를 쿼리 파라미터(`paymentKey`/`orderId`/`amount` vs `code`/`message`)로만 구분한다 — 별도의 성공/실패 페이지를 만들 필요가 없다.
4. **`GET /me/direct-asks`를 비정규화한다.** `questionId`/`requesterId`/`targetUserId`만 있는 기존 응답으로는 프론트가 사람이 읽을 수 있는 목록을 그릴 수 없어, `SavedQuestionResponse`가 이미 쓰던 패턴대로 `questionTitle`/`requesterNickname`/`targetUserNickname`을 추가했다(`ListMyDirectAsksUseCase`가 `QuestionRepository`/`UserRepository`로 N+1 조회 — 이 프로젝트의 다른 목록 read model과 동일한 비용 프로파일).
5. **Organization 멤버십 여부는 `OrganizationResponse`가 아니라 내 프로필의 `organizations` 목록으로 판단한다.** `POST /{id}/join`이 멱등이라 백엔드에 "멤버인가" 플래그가 없다 — `FollowUserButton`이 `useMyFollowing`으로 판단하는 것과 같은 패턴을 `JoinOrganizationButton`에도 적용했다(`useUserProfile`에 `enabled` 옵션을 추가해 재사용).

## 결과 (Consequences)

- 사용자 검색 API가 생기기 전까지, Direct Ask는 "프로필을 먼저 찾아가야" 요청할 수 있다 — Question Detail 화면에서 곧바로 특정인을 지목해 요청하는 흐름은 없다. 실사용에서 불편이 확인되면 사용자 검색 API를 새로 설계해야 한다.
- Toss 결제창 자체(카드 정보 입력)는 안전 정책상 자동화된 브라우저 검증이 완주할 수 없다는 ADR-0037의 한계가 프론트엔드 검증에도 그대로 이어진다 — 요청 생성까지는 실제 API로, confirm 이후 흐름은 백엔드 검증과 동일하게 로컬 모크 Toss 서버로 확인했다(PLAN.md F12.5).
- Verified 조직에 `POST /{id}/join`으로 직접 가입을 시도하는 경로는 프론트에서 아예 버튼을 감추는 방식으로 막았다(백엔드도 `VerifiedOrganizationJoinRequiresEmailException`으로 별도 방어).

## 관련 문서

- [api-design.md](../api-design.md#유료-direct-ask-phase-25)
- [domain-model.md](../domain-model.md#trust-network-phase-22)
- [ADR-0034](0034-organization-virtual-only-direct-ask-no-payment.md), [ADR-0035](0035-verified-organization-email-domain-mailpit.md), [ADR-0037](0037-paid-direct-ask-toss-payments-test-mode.md) (이번 프론트엔드가 붙이는 백엔드 결정들)
- [PLAN.md](../../../PLAN.md) Phase 26
