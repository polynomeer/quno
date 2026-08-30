# ADR-0027: Badge는 Reputation과 같은 방식으로 매 요청마다 계산하는 읽기 모델로 두고, 획득 이벤트·알림·영속화는 만들지 않는다

- 날짜: 2026-08-30
- 상태: 승인됨

## 배경 (Context)

[design.md #19](../../frontend/design.md)는 배지(Badge)를 "획득 시 작은 toast/notification으로 알린다"고 전제하지만, 지금 백엔드에는 배지 자체가 없다([ADR-0020](0020-frontend-scoped-to-backend-support.md) 갭 분석). "획득했을 때 알린다"를 그대로 구현하려면 (1) 배지 획득 여부를 영속화해서 "이미 알렸는지"를 구분하고, (2) 질문/답변/투표 등 관련 액션이 일어날 때마다 배지 조건을 재평가해 새로 획득했는지 판정하고, (3) 새로 획득했다면 알림을 만드는 이벤트 기반 시스템이 필요하다. 반면 이미 있는 `GET /users/{id}/reputation`(Phase 9, [ADR-0018](0018-simple-reputation-score-only.md))은 정반대 방식이다 — 영속화 없이 매 요청마다 질문/답변/채택 수를 집계해서 돌려주는 순수 읽기 모델([ADR-0010](0010-metrics-read-model-skip-dto.md)). 배지 조건("답변 5개 채택" 등)은 이 Reputation이 이미 계산하는 값과 대부분 겹친다.

## 결정 (Decision)

Badge도 **Reputation과 동일하게 영속화 없는 순수 읽기 모델**로 만든다. `GET /api/v1/users/{id}/badges`가 매 요청마다 그 사용자의 활동 집계치를 다시 계산해서 "지금 기준으로 획득 조건을 만족하는 배지 목록"을 반환한다 — 어떤 배지를 "언제 처음 획득했는지"는 저장하지 않는다.

고정 카탈로그(하드코딩된 `BadgeType` enum, Reputation 점수 공식처럼 설정 불가능한 초기값)로 시작한다:

| Badge | Tier | 조건 |
|---|---|---|
| `FIRST_QUESTION` | Bronze | 질문 1개 이상 작성 |
| `FIRST_ANSWER` | Bronze | 답변 1개 이상 작성 |
| `PROBLEM_SOLVER` | Silver | 채택된 답변 5개 이상 |
| `WELL_RECEIVED` | Silver | 본인이 작성한 질문+답변에 대한 투표 점수 합(Phase 11 Vote) 50점 이상 |
| `TRUSTED_ANSWERER` | Gold | 채택된 답변 20개 이상 |
| `SUPER_ANSWER` | Gold | Super Answer로 지정된 답변 1개 이상 |

질문/답변/채택 수는 기존 `ReputationRepository.compute(userId)`를 그대로 재사용하고, 투표 점수 합만 새 집계 쿼리(`votes`를 사용자가 작성한 `questions`/`answers`와 조인)로 추가한다. 배지의 이름/설명/토스트 문구 같은 표시 텍스트는 백엔드가 만들지 않는다 — `NotificationType`/`describeNotification`과 같은 원칙으로, 백엔드는 `type`(enum 식별자)과 `tier`만 반환하고 실제 문구는 프론트가 갖는다.

"획득 시 토스트 알림"(design.md가 원하는 것)은 **이번 범위에서 만들지 않는다.** 순수 읽기 모델은 태생적으로 "새로 획득했다"는 순간을 알 수 없다 — 그걸 알려면 결국 영속화가 필요해서, 이 ADR의 핵심 결정(계산만 하고 저장하지 않는다)과 정면으로 충돌한다.

## 결과 (Consequences)

- 배지 시스템 전체가 기존 인프라(Vote, Reputation)의 재사용만으로 끝난다 — 새 테이블, 새 이벤트 타입, 새 알림 타입이 하나도 필요 없다. 코드 변경 범위가 Vote/Comment보다도 작다.
- 대신 "배지를 획득했다"는 순간을 사용자에게 알려줄 방법이 없다 — 배지는 프로필에서 "지금 갖고 있는 것"으로만 확인 가능하고, 실시간 축하 연출은 없다. design.md 19장의 이 부분은 구현하지 않은 채로 남는다.
- 카탈로그가 고정 6종이라 배지를 늘리거나 조건을 바꾸려면 코드 배포가 필요하다 — 운영진이 배지를 직접 추가/조정하는 관리 UI는 없다(Reputation 점수 공식과 동일한 트레이드오프).
- 실제로 "언제 획득했는지"가 필요해지면(토스트, 획득 이력, 정렬 등) 이 ADR을 재검토하고 영속화 계층을 추가해야 한다.

## 관련 문서

- [ADR-0018](0018-simple-reputation-score-only.md) (Reputation 점수 공식 — 같은 트레이드오프 선례)
- [ADR-0010](0010-metrics-read-model-skip-dto.md) (읽기 전용 모델은 DTO 복제 생략 패턴)
- [ADR-0023](0023-vote-as-side-aggregate-no-reputation-impact.md) (이번에 재사용하는 투표 점수 집계)
- [PLAN.md](../../../PLAN.md) Phase 15
