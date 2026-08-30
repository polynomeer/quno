# ADR-0026: Follow User는 이번 범위에서 관계 기록·조회만, 활동 피드·알림은 후속 Phase로 미룬다

- 날짜: 2026-08-30
- 상태: 승인됨

## 배경 (Context)

[design.md #18](../../frontend/design.md)의 Watch·북마크·팔로우 표는 Follow User를 "특정 사용자의 공개 활동 관심"으로 정의하면서도 UI 칸에 스스로 "후속 버전 기능"이라고 적어뒀다 — 설계 문서 작성 시점에도 1차 범위로 보지 않았다는 뜻이다. 반면 [ADR-0020](0020-frontend-scoped-to-backend-support.md)의 갭 분석에서는 Follow User 자체가 백엔드에 아예 없다는 사실만 확인했을 뿐, 어디까지 만들지는 정하지 않았다. "관심 있는 사용자의 활동을 본다"는 목표를 온전히 구현하려면 팔로우한 사용자들의 활동(새 질문/답변/채택 등)을 모으는 피드와 그에 대한 알림까지 필요한데, 이건 이미 있는 Quno Flow(Phase 10, [ADR-0019](0019-quno-flow-and-dashboard-only-no-live-chat.md))의 데이터 소스·집계 방식을 다시 설계해야 하는 별도 작업이다.

## 결정 (Decision)

이번 범위는 **관계의 기록과 조회까지만** 만든다:

- `domain/follow-user`(가칭) 패키지에 `UserFollowRepository` 포트 — `follow(followerId, followeeId)`/`unfollow`/`isFollowing`/`findFolloweeIds(followerId)`(Watch/UserTagFollow와 동일한 관계 테이블 패턴).
- `POST/DELETE /api/v1/users/{id}/follow`(204), `GET /api/v1/me/following`(내가 팔로우하는 사용자 목록).
- 자기 자신은 팔로우할 수 없다 — `SelfFollowException`(403, `SelfVoteException`/`SelfReviewRequestException`과 동일한 패턴). 대상 사용자가 없으면 `UserNotFoundException`(404, 기존 예외 재사용).

다음은 **명시적으로 이번 범위 밖**이다:

- 팔로워/팔로잉 수를 프로필에 노출하는 것(카운트 쿼리 자체는 간단하지만, 노출 여부·프라이버시 정책은 별도 결정이 필요해 미룬다).
- "팔로우한 사용자의 활동" 피드 — Quno Flow(Phase 10)를 팔로우 기반으로 필터링하는 재설계가 필요하다.
- 팔로우한 사용자가 무언가 했을 때의 알림(`OutboxEventTypes`에 새 타입 추가) — 알림 타입이 늘어날 때마다 `DispatchOutboxEventsUseCase`의 fan-out 대상 결정 로직이 복잡해지는데, 지금은 실제 사용 패턴(사람들이 이 기능을 얼마나 쓰는지)을 관찰할 데이터가 전혀 없다.

## 결과 (Consequences)

- 이번 Phase가 끝나도 "Follow User"는 사실상 프로필에 저장해두는 북마크에 가깝다 — Save(질문 북마크, [ADR-0025](0025-save-as-separate-side-aggregate-from-watch.md))의 사용자 버전이라고 보면 된다. design.md가 그리는 "관심 있는 사람의 활동을 본다"는 경험은 아직 완성되지 않는다.
- 활동 피드/알림이 실제로 필요해지는 시점에는 Quno Flow의 데이터 소스 확장과 새 알림 타입 설계를 다시 해야 한다 — 이 ADR은 "안 만든다"가 아니라 "지금은 관계 기록만 먼저 검증한다"는 뜻이다(ADR-0018/0019와 같은 방향의 스코프 축소).
- Bounded Context 배치는 착수 시점에 확정한다.

## 관련 문서

- [design.md #18 Watch·북마크·팔로우](../../frontend/design.md)
- [ADR-0019](0019-quno-flow-and-dashboard-only-no-live-chat.md) (Quno Flow가 후속으로 다시 손댈 대상이 됨)
- [PLAN.md](../../../PLAN.md) Phase 14
