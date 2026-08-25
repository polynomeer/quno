# ADR-0019: Phase 10은 Quno Flow + 고급 Dashboard만, 실시간 질문방(Live Chat)은 별도 Phase로 미룬다

- 날짜: 2026-08-25
- 상태: 승인됨

## 배경 (Context)

mvp-scope.md 로드맵 Phase 6은 "Quno Flow, Instant Question, 실시간 질문방, 고급 Daily Dashboard"를 한데 묶지만 정작 mvp-scope.md/vision.md에는 한 줄씩만 있다. 착수 전 [docs/archive/Quno 서비스 통합 기획서](../../archive/README.md)의 원본 절을 다시 확인했다 — 22장 "Daily Newspaper Dashboard", 23장 "Quno Flow", 19장 "실시간 질문 공간(Live Chat)"에 구체적인 설명이 있었다. 이를 보면 네 항목의 실제 구현 난이도가 서로 크게 다르다는 것이 드러났다.

- **Quno Flow**: 원문 표현으로 "살아 움직이는 Question Network의 Activity Stream" — 인기 질문·태그 급증·재활성화된 질문·Cluster의 새 Super Answer 같은 예시 카드가 전부 이미 확보된 신호(Dashboard, Spike Detection, Outdated, Cluster)의 조합이다.
- **고급 Daily Dashboard**: 헤드라인/오늘 해결된 질문/재활성화된 질문/Trending Errors 섹션 추가 — 기존 Dashboard(Phase 3.2)의 확장이다.
- **실시간 질문방(Live Chat)**: "현재 17명이 이 질문을 보고 있습니다" 같은 실시간 접속자 수 추적과 WebSocket 기반 Live Chat 생성·메시지 영속화가 필요하다 — 이 세션에서 지금까지 다룬 어떤 기능보다 큰 기술적 투자(양방향 실시간 연결, presence tracking)가 필요하다.
- **Instant Question**: 원본 기획서에도 별도 설계가 없다. 이미 `POST /questions`가 필수 필드(title/body)만으로 질문을 만들 수 있어 그 자체로 "즉석 질문"을 지원한다.

## 결정 (Decision)

이번 Phase는 **Quno Flow + 고급 Dashboard**만 구현한다. 기존 신호(인기 질문, 태그 급증, Outdated→리비전으로 도출한 재활성화, Cluster Super Answer 지정)를 조합해 두 기능을 만든다. **실시간 질문방(Live Chat)**은 WebSocket 인프라가 실제로 필요해지는 시점까지 명시적으로 범위 밖에 둔다. **Instant Question**은 이미 기존 API로 충족되므로 별도 백엔드 작업이 없다 — 프론트엔드가 없는 이 세션에서는 사실상 완료된 것으로 간주한다.

"재활성화(Reopened)" 신호는 새 컬럼 없이 기존 `outbox_events`(QUESTION_OUTDATED)와 `question_versions`의 타임스탬프 순서만으로 도출한다 — Spike Detection이 새 인프라 없이 기존 타임스탬프로 급증을 도출한 것과 같은 접근이다. "Trending Errors"는 별도 에러 텍스트 추출 없이 기존 태그 급증(Spike Detection)으로 근사한다 — Phase 3.3에서 "오늘의 인기 질문"을 조회수 없이 Watch/Answer 수로 근사한 것과 같은 종류의 알려진 단순화다.

## 결과 (Consequences)

- Quno Flow/고급 Dashboard는 새 테이블 없이(단, `question_clusters.updated_at` 한 컬럼만 추가) 기존 read model들을 조합하는 것만으로 완성된다 — 이 세션 전체의 "실제로 설계 가능한 부분만 구현한다" 원칙(ADR-0008, ADR-0016, ADR-0017, ADR-0018)과 일관된다.
- "Trending Errors"는 진짜 에러 메시지 분석이 아니라 태그 급증의 근사치다 — 향후 실제 에러 텍스트 추출/분류가 필요해지면 재검토한다.
- 실시간 질문방은 PLAN.md에 번호 미정으로 남았다 — Merge/Fork, 기술 버전 자동 감지, Organization/Direct Ask와 함께 "새 인프라 투자가 실제로 정당화될 때" 착수할 항목 목록에 합류한다.

## 관련 문서

- [docs/archive/README.md](../../archive/README.md)
- [ADR-0016](0016-manual-duplicate-marking-cluster.md), [ADR-0017](0017-manual-outdated-marking-and-spike-detection-scope.md) (같은 방향의 "기존 신호 재사용" 판단)
- [PLAN.md](../../../PLAN.md) Phase 10
