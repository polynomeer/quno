# ADR-0028: 모더레이션은 신고→검토 큐→Dismiss/Hide 두 액션까지만, 역할 관리·Edit·정지는 후속으로 미룬다

- 날짜: 2026-08-31
- 상태: 승인됨

## 배경 (Context)

[design.md #20](../../frontend/design.md)의 모더레이션 UI는 신고 사유별 필터, 신고 횟수, `Keep/Close as duplicate/Edit/Hide` 네 가지 액션, audit trail, 사용자 노출 사유와 내부 운영 메모의 분리까지 요구한다. 이 문서 자체가 "**신고/모더레이션 큐/역할 기반 권한이 백엔드에 전혀 없다**"([api-design.md](../api-design.md#인증-확정--2026-08-24)도 "관리자/모더레이터 API가 추가되면 Role과 세부 권한을 분리한다"고만 적어두고 미착수 상태)고 확인한 대로, 이번 설계는 사실상 백지에서 시작한다. 조사 결과 `Question`/`Answer`는 `deleted_at` 컬럼과 그걸 걸러내는 일부 조회 쿼리(`QuestionJpaRepository`)만 있을 뿐 실제 `softDelete()` 도메인 메서드나 이를 호출하는 use case는 하나도 없었다 — `domain-model.md`의 Aggregate 표가 "Question | ... softDelete"라고 적어둔 것은 실제로는 아직 구현되지 않은 계획이었다(코드가 근거, 문서가 착오).

## 결정 (Decision)

이번 범위는 **신고 → 모더레이터 검토 큐 → Dismiss/Hide 두 액션**까지로 좁힌다.

- **Role**: `users` 테이블에 `role`(`USER` | `MODERATOR`, 기본값 `USER`) 컬럼만 추가한다. **역할을 부여/회수하는 API는 만들지 않는다** — 최초 모더레이터는 운영자가 DB에서 직접 값을 바꾼다. JWT에 role을 넣지 않고(ADR-0003의 stateless JWT 구조를 건드리지 않음), 매 요청마다 `UserRepository.findById(userId).role`로 확인한다 — 역할이 바뀌면 다음 요청부터 바로 반영되고(토큰 재발급 불필요), Access Token에 role을 실어 캐싱했을 때 생기는 "강등돼도 토큰 만료 전까지 여전히 모더레이터"라는 지연 문제가 없다.
- **Report**: `domain/report` 패키지에 `Report`(reporterId, targetType: QUESTION\|ANSWER, targetId, reason: SPAM\|DUPLICATE\|LOW_QUALITY\|OTHER, message, status: PENDING\|DISMISSED\|ACTIONED, resolvedBy, resolvedAt) — Vote/Comment와 같은 독립 side-aggregate. 같은 대상에 대한 중복 신고를 병합하지 않는다 — "3 reports"는 별도 카운터가 아니라 `COUNT(*)`로 그때그때 구한다.
- **액션은 Dismiss/Hide 둘뿐**: `Keep`은 Dismiss와 동일하게 취급하고(콘텐츠를 그대로 두고 신고만 닫음), `Hide`는 대상 Question/Answer에 **처음으로 실제 `softDelete()`를 추가**해 호출한다(`deletedAt` 설정). `Close as duplicate`는 별도 상태를 새로 만들지 않는다 — 이미 있는 Cluster 기능(Phase 6, "같은 문제로 표시")이 정확히 이 개념을 담당하므로 재사용을 권장하고 모더레이션 액션에는 넣지 않는다. `Edit`(모더레이터가 남의 글을 직접 수정)은 "누가 누구의 글을 고칠 수 있는가"라는 별도 권한 모델이 필요해 이번 범위에서 뺀다.
- **Hide는 신고 대상자에게만 알린다**: `OutboxEventTypes.CONTENT_HIDDEN`을 새로 추가해 콘텐츠 작성자 1명에게만 알림을 보낸다(Ward 구독자 fan-out과는 다름 — 이건 "활동"이 아니라 그 사람에게 온 행정 조치 통보이기 때문). Hide는 계단식으로 전파하지 않는다 — 질문을 Hide해도 그 질문의 답변들은 자동으로 함께 숨겨지지 않는다(단순함을 위한 의도적 결정, 실제 사용 패턴을 보고 재검토).
- **audit trail은 `Report` 행 자체가 담당**한다 — `resolvedBy`/`resolvedAt`/`status`만으로 "누가 언제 무엇을 했는지"가 남으므로 별도 감사 로그 테이블을 만들지 않는다([ADR-0010](0010-metrics-read-model-skip-dto.md)과 같은 방향: 이미 있는 데이터로 충분하면 새 테이블을 만들지 않는다).

**범위 밖**으로 명시적으로 미루는 것: 역할 부여/회수 API(관리자 UI), Edit 액션, 사용자 정지/차단, "사용자 노출 사유"와 "내부 운영 메모"의 분리(이번 범위는 신고 메시지 하나로 겸용), Hide의 계단식 전파, 신고 사유별 자동 우선순위/스팸 필터.

## 결과 (Consequences)

- 실제로 동작하는 최소 루프(신고→검토→처리)만 존재한다 — design.md가 그리는 전체 모더레이션 경험(정지, 역할 관리 UI, 공개/비공개 메모 분리)은 여전히 없다.
- `Answer`에도 `softDelete()`가 이번에 처음 생기므로, `Answer` 조회 쿼리에 `deletedAt IS NULL` 필터가 빠져 있던 부분(`AnswerJpaRepository`, `Question`과 달리 이 필터가 전혀 없었음)을 함께 손봐야 한다 — Hide 이후에도 숨겨진 답변이 계속 노출되면 이번 기능 자체가 무의미해진다.
- 첫 모더레이터를 지정하려면 반드시 DB에 직접 접근해야 한다 — 실사용 단계에서 이게 불편해지면 최소한의 승격 API를 다시 설계한다.
- Role 확인이 매 요청 DB 조회 1번을 추가한다 — Reputation(ADR-0018 관련)과 마찬가지로 사용자별로 결과가 다르고 빈도가 낮아 캐싱하지 않는다.

## 관련 문서

- [design.md #20 모더레이션 UI](../../frontend/design.md)
- [ADR-0003](0003-stateless-jwt-auth.md) (건드리지 않는 기존 JWT 구조)
- [ADR-0016](0016-manual-duplicate-marking-cluster.md) (재사용을 권장하는 기존 Cluster/중복 표시 기능)
- [PLAN.md](../../../PLAN.md) Phase 16
