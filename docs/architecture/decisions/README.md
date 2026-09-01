# Architecture Decision Records (ADR)

이 디렉터리는 Quno 개발 과정에서 내린 아키텍처/기술 결정을 기록한다. 각 ADR은 "왜 그렇게 결정했는지"와 "그 결정의 결과로 무엇을 감수했는지"를 남기는 것이 목적이다 — `system-architecture.md`/`domain-model.md`/`api-design.md`가 "현재 상태가 무엇인가"를 설명한다면, ADR은 "왜 지금 이 상태인가"를 설명한다.

## 작성 규칙

- 새 ADR은 [TEMPLATE.md](TEMPLATE.md)를 복사해서 시작한다.
- 파일명은 `NNNN-짧은-제목.md` (4자리 순번, kebab-case).
- 상태는 제안됨/승인됨/폐기됨/대체됨 중 하나. 기존 결정을 뒤집을 때는 옛 ADR을 지우지 않고 "대체됨(ADR-XXXX로)"로 표시한 뒤 새 ADR을 추가한다.
- **Claude Code는 다음 기준에 해당하는 결정을 내리거나 발견하면, 사용자가 요청하지 않아도 알아서 새 ADR을 작성한다** (2026-08-25 확정, 아래 "ADR 자동 작성 기준" 참고). 작성 후 이 README의 목록에도 추가한다.

## ADR 자동 작성 기준

다음 중 하나에 해당하면 ADR감이다:

- 기술 스택/라이브러리/인프라를 선택하거나 교체할 때 (예: 특정 DB, 캐시 전략, 메시징 방식)
- 여러 구현 대안 중 하나를 선택하고 트레이드오프를 감수할 때 (예: 락 전략, 테스트 전략, 캐시 범위)
- 버그 수정이 "이 코드 한 줄 고침"을 넘어서 앞으로 지켜야 할 규칙/정책을 만들 때 (예: 특정 필드를 조회 기준으로 통일)
- 범위를 의도적으로 줄이거나 특정 결정을 보류하기로 할 때 (보류 자체가 결정이다)
- `AskUserQuestion`으로 사용자에게 확인받은 설계/스코프 결정

반대로 다음은 ADR로 남기지 않는다: 단순 리팩터링, 오타 수정, 이미 확정된 패턴을 그대로 따르는 반복 구현, PLAN.md 체크리스트 갱신처럼 그 자체로 결정이 아닌 기록.

## 목록

| ADR | 제목 | 상태 |
|---|---|---|
| [0001](0001-tech-stack.md) | 기술 스택을 Kotlin/Spring Boot 4 + PostgreSQL/MongoDB/Redis 단일 모듈 DDD로 확정 | 승인됨 |
| [0002](0002-archive-alternative-proposals.md) | 기각된 대안 기획(StackNext, MySQL+Kafka)은 삭제 대신 참고 아카이브로 보관 | 승인됨 |
| [0003](0003-stateless-jwt-auth.md) | Stateless JWT(Access/Refresh 분리) 인증 채택 | 승인됨 |
| [0004](0004-question-version-aggregate-boundaries.md) | Question/QuestionVersion 분리, Answer는 독립 Aggregate, 리비전은 append-only | 승인됨 |
| [0005](0005-pessimistic-locking-revision-concurrency.md) | 리비전 생성 동시성 방어로 Pessimistic Locking 채택 | 승인됨 |
| [0006](0006-transactional-outbox-in-process-scheduler.md) | 비동기 이벤트 처리는 Transactional Outbox + in-process 스케줄러로 | 승인됨 |
| [0007](0007-tag-slug-uniqueness-and-error-endpoint.md) | 태그 중복 판정을 slug 기준으로 통일하고 `/error`를 permitAll 처리 | 승인됨 |
| [0008](0008-postgres-native-search.md) | 전용 검색엔진 도입 전, PostgreSQL 네이티브 전문검색으로 시작 | 승인됨 |
| [0009](0009-redis-cache-global-aggregates-only.md) | Redis 캐시는 전역 집계에만, TTL 만료로만 갱신 | 승인됨 |
| [0010](0010-metrics-read-model-skip-dto.md) | 성공 지표 스냅샷은 순수 읽기 모델로 취급해 DTO 복제 생략 | 승인됨 |
| [0011](0011-mockmvc-e2e-testing.md) | 유스케이스 직접 호출 통합 테스트에 더해 MockMvc 기반 E2E 테스트 추가 | 승인됨 |
| [0012](0012-qpr-multi-reviewer-thread-model.md) | QPR Review의 정보 요청은 다중 리뷰 요청 스레드 모델로 구현 | 승인됨 |
| [0013](0013-defer-public-read-access.md) | 질문/프로필 조회의 비로그인 공개 여부는 보류 | 일부 대체됨(ADR-0041) |
| [0014](0014-answer-target-version-auto-recorded.md) | 답변의 대상 버전은 작성 시점 최신 버전으로 자동 기록, 명시적 선택 UI는 미도입 | 승인됨 |
| [0015](0015-review-request-status-independent-of-question-status.md) | ReviewRequest.status는 Question.status를 다시 게이팅하지 않음 | 승인됨 |
| [0016](0016-manual-duplicate-marking-cluster.md) | Cluster는 자동 유사도 분석 대신 사용자 명시적 표시로 형성, 이번 Phase는 Cluster+Super Answer로 한정 | 승인됨 |
| [0017](0017-manual-outdated-marking-and-spike-detection-scope.md) | Outdated는 사용자 명시적 표시로 근사, 기술 버전 자동 감지는 범위 밖, Spike Detection은 포함 | 승인됨 |
| [0018](0018-simple-reputation-score-only.md) | Phase 9는 간단한 평판 점수만, Organization/Direct Ask는 후속 Phase로 이연 | 승인됨 |
| [0019](0019-quno-flow-and-dashboard-only-no-live-chat.md) | Phase 10은 Quno Flow+고급 Dashboard만, 실시간 질문방(Live Chat)은 후속 Phase로 이연 | 승인됨 |
| [0020](0020-frontend-scoped-to-backend-support.md) | 프론트엔드는 백엔드 지원 화면부터, Vote/Comment/Badge/모더레이션은 후속으로 이연 | 승인됨 |
| [0021](0021-tag-detail-via-search-approximation.md) | Tag Detail은 검색 결과 근사로 구현, 태그 통계/탭/Follow 상태는 후속으로 이연 | 일부 대체됨(ADR-0040) |
| [0022](0022-search-filters-client-side-tag-and-status-only.md) | 고급 검색 필터는 클라이언트 사이드 Tags/Status만, Score/Date/Sort는 보류 | 승인됨 |
| [0023](0023-vote-as-side-aggregate-no-reputation-impact.md) | Vote는 Watch와 같은 독립 side-aggregate, 평판 점수·인기순위엔 미반영 | 일부 대체됨(ADR-0032) |
| [0024](0024-comment-flat-no-edit-tombstone-delete.md) | Comment는 스레드 없는 평면 목록, 수정 불가, soft-delete tombstone | 일부 대체됨(ADR-0031) |
| [0025](0025-save-as-separate-side-aggregate-from-watch.md) | Save(북마크)는 Watch와 구조는 같지만 별도 테이블·독립 side-aggregate로 분리 | 승인됨 |
| [0026](0026-follow-user-relationship-only-no-activity-feed.md) | Follow User는 관계 기록·조회만, 활동 피드·알림은 후속 Phase로 이연 | 승인됨 |
| [0027](0027-badge-as-computed-read-model-no-award-events.md) | Badge는 Reputation처럼 영속화 없는 계산형 읽기 모델, 획득 이벤트·알림은 미도입 | 승인됨 |
| [0028](0028-moderation-mvp-report-dismiss-hide-only.md) | 모더레이션은 신고→검토 큐→Dismiss/Hide까지만, 역할 관리·Edit·정지는 후속으로 이연 | 승인됨 |
| [0029](0029-answer-revision-mirrors-question-version-no-locking.md) | Answer Revision은 Question/QuestionVersion 분리를 그대로 적용하되 동시성 잠금은 미도입 | 승인됨 |
| [0030](0030-cluster-merge-question-fork-graph-data-only.md) | Merge는 클러스터 병합으로 한정, Fork는 리비전 인프라 재사용, 지식 그래프는 데이터 API까지만 | 승인됨 |
| [0031](0031-comment-thread-mention-edit-history.md) | Comment에 1단계 대댓글, @mention 알림(생성 시점만), 수정 이력(diff 없이) 추가 | 승인됨 |
| [0032](0032-vote-score-search-sort-dashboard-reputation.md) | 검색 Score 정렬, Dashboard 인기순위, 평판 점수에 Vote 반영(모두 가중치 1) | 승인됨 |
| [0033](0033-technology-version-scan-detection-only-no-auto-outdated.md) | endoflife.date로 기술 버전 릴리스 실제 자동 감지, OUTDATED 자동 전환은 하지 않고 알림까지만 | 승인됨 |
| [0034](0034-organization-virtual-only-direct-ask-no-payment.md) | Organization은 Virtual/Community만, Direct Ask는 결제 없이 요청/수락만 구현 | 승인됨 |
| [0035](0035-verified-organization-email-domain-mailpit.md) | Verified Organization을 업무/학교 이메일 도메인 인증으로 구현, 로컬은 Mailpit으로 검증 | 승인됨 |
| [0036](0036-live-chat-websocket-mongodb-redis-presence.md) | 실시간 질문방을 STOMP/WebSocket + MongoDB(메시지) + Redis(접속자)로 구현, Spring Boot 4 Mongo prefix 변경 발견 | 승인됨 |
| [0037](0037-paid-direct-ask-toss-payments-test-mode.md) | 유료 Direct Ask를 토스페이먼츠 테스트 모드로 구현(무료 Direct Ask 대체), 지급대행은 범위 밖, RestClient 요청 직렬화 함정 발견 | 승인됨 |
| [0038](0038-organization-direct-ask-frontend-no-user-search.md) | Organization/Direct Ask 프론트엔드는 사용자 검색 없이 프로필 페이지를 진입점으로, 결제는 Toss 호스팅 체크아웃으로 구현 | 승인됨 |
| [0039](0039-live-chat-frontend-stompjs-connect-on-demand.md) | 실시간 질문방 프론트엔드는 `@stomp/stompjs`로, 연결은 "채팅 참여하기"를 누른 뒤에만 열도록 구현 | 승인됨 |
| [0040](0040-tag-detail-wiki-editable-and-real-stats.md) | 태그 상세 정보(설명/문서 링크/기여자/관련 태그)는 위키 스타일 편집과 실제 통계 쿼리로 구현, 30일 활동 요약은 범위 밖 | 승인됨 |
| [0041](0041-narrow-public-read-access.md) | 비로그인 공개 열람은 질문 상세/목록/검색으로 좁혀서 시작(ADR-0013 재검토), SEO 메타데이터는 범위 밖 | 승인됨 |
| [0042](0042-expand-public-read-access-tags-orgs-profiles.md) | 비로그인 공개 열람을 태그·조직 상세·사용자 프로필까지 확대, SEO 메타데이터는 여전히 범위 밖 | 승인됨 |
| [0043](0043-seo-metadata-question-og-and-sitemap.md) | SEO 메타데이터는 질문 상세의 동적 Open Graph + 태그·조직 sitemap까지만, 질문/프로필 sitemap 열거는 범위 밖 | 승인됨 |
