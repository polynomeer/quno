# ADR-0033: 기술 버전 영향 감지를 endoflife.date로 실제 자동화하되, 감지는 알림까지만 하고 OUTDATED 자동 전환은 하지 않는다

- 날짜: 2026-08-31
- 상태: 승인됨

## 배경 (Context)

[ADR-0017](0017-manual-outdated-marking-and-spike-detection-scope.md)은 "기술 버전 영향 감지"의 진짜 자동화(`TechnologyVersionReleased → ImpactScanRequested → AffectedKnowledgeDetected`, [domain-model.md](../domain-model.md#qunobot-이벤트-체인-phase-4))를 외부 릴리스 데이터 피드가 없다는 이유로 범위 밖에 두고, `PLAN.md`에 번호 미정 상태로 남겨뒀다. 이번 Phase에서 그 외부 데이터 소스 연동을 실제로 시작한다.

무료로 인증 없이 쓸 수 있는 [endoflife.date](https://endoflife.date) v1 API(`GET /api/v1/products/{slug}`)가 이 프로젝트의 태그 대부분(kotlin, spring-boot, redis, kafka(→apache-kafka), postgresql, mongodb, docker(→docker-engine))을 제품 단위로 커버하는 것을 확인했다. 다만 "java"처럼 endoflife.date가 벤더별 빌드(amazon-corretto, eclipse-temurin, ...)로만 제공해 하나의 제품으로 특정할 수 없는 기술도 있다.

탐지 자체가 가능해진 뒤에도 남는 질문은: 새 버전이 감지되면 시스템이 관련 질문을 자동으로 `OUTDATED`로 전환해야 하는가? ADR-0017은 이미 사용자 명시적 표시(`POST /questions/{id}/outdated`)에 "오탐/악용을 막을 안전장치가 없다"는 리스크를 남겼다. 자동 전환은 그 리스크를 사람의 판단 없이 시스템 혼자 감수하는 것이다 — "태그가 일치하고 콘텐츠가 릴리스보다 오래됐다"는 휴리스틱은 실제로 그 릴리스가 질문 내용에 영향을 주는지 전혀 검증하지 않는다(예: Kotlin 문법 기초를 묻는 질문은 마이너 릴리스와 대부분 무관하다).

## 결정 (Decision)

1. **외부 데이터 소스로 endoflife.date v1 API를 쓴다.** 태그 slug → 제품 slug 매핑(`domain/qunobot/TrackedTechnologies`)은 코드에 하드코딩된 수동 큐레이션이다 — Cluster([ADR-0016](0016-manual-duplicate-marking-cluster.md))와 같은 "자동 탐색 대신 명시적 큐레이션" 철학이다. 모호한 매핑(java 등)은 넣지 않는다.
2. **하루 1회 스케줄러(`TechnologyVersionScanScheduler`)가 각 추적 기술의 최신 릴리스를 조회**해 `technology_releases` 테이블(태그당 1행, 이력이 아니라 스냅샷)과 비교한다. 처음 보는 태그는 베이스라인으로만 저장하고 알림을 보내지 않는다(롤아웃 시점에 기존 모든 태그가 "새 릴리스"로 오인되는 것을 막기 위해) — 저장된 버전과 실제로 달라졌을 때만 "새 릴리스"로 취급한다.
3. **새 릴리스가 감지되면 해당 태그가 달리고, `RESOLVED`/`OUTDATED`가 아니며, 마지막 콘텐츠 변경(question_versions 최신 `created_at` — `questions.updated_at`은 클러스터 합류 등 콘텐츠와 무관한 상태 변경에도 움직여 신뢰할 수 없다)이 릴리스일보다 이전인 질문**을 찾아 `TECH_VERSION_IMPACT_DETECTED` outbox 이벤트를 발행한다(Ward 구독자 + 질문 작성자, 기존 QUESTION_OUTDATED와 동일한 수신자 규칙).
4. **질문을 자동으로 `OUTDATED`로 전환하지 않는다.** 이 이벤트는 사람이 읽고 필요하면 직접 `POST /questions/{id}/outdated`를 호출하도록 유도하는 알림일 뿐이다 — ADR-0017이 이미 사람의 판단에 맡긴 것과 같은 자리에, 검증되지 않은 자동 판단을 추가하지 않기 위해서다.
5. **`GET /qunobot/version-impacts?limit=`으로 현재 영향권에 있는 질문 목록을 언제든 조회**할 수 있게 한다(Spike Detection과 같은 "읽기 전용 신호" 패턴, [ADR-0010](0010-metrics-read-model-skip-dto.md)). Spike Detection과 달리 **Redis 캐싱은 하지 않는다** — 이 프로젝트에 Instant/LocalDate를 포함한 모델을 Redis JSON 캐시로 왕복시킨 전례가 없어 그 리스크를 감수할 만큼 조회 비용이 크지 않다고 판단했다(추적 기술 수가 작아 쿼리가 가볍다).

## 결과 (Consequences)

- `domain/question/QuestionStatus.kt`의 "OUTDATED는 오직 사용자 명시적 표시로만 도달한다"는 기존 주석은 여전히 참이다 — 이번 Phase는 그 규칙을 바꾸지 않고, 그 표시를 유도하는 새 알림 경로 하나를 추가했을 뿐이다.
- 실제 외부 서비스(endoflife.date)에 대한 첫 아웃바운드 HTTP 의존성이 생긴다. 한 제품 조회 실패(알 수 없는 slug, 타임아웃, 장애)는 그 제품만 건너뛰고 나머지 스캔에 영향을 주지 않도록 `TechnologyReleaseFeed.fetchLatest`가 예외 대신 null을 반환하게 설계했다 — 하지만 endoflife.date 자체가 사라지거나 API 계약이 바뀌면 이 기능 전체가 조용히 멈춘다(에러 알림 없음). 실사용에서 문제가 되면 헬스체크나 스캔 실패 알림을 추가로 고려한다.
- `TrackedTechnologies.MAPPING`은 수동으로 확장해야 한다 — 새 태그가 실제로 많이 쓰이기 시작해도 자동으로 추적 대상에 들어오지 않는다. 태그-제품 자동 매칭(예: 태그명으로 endoflife.date를 검색)은 오탐 위험이 커서 이번 Phase에 포함하지 않았다.
- 프론트엔드는 `TECH_VERSION_IMPACT_DETECTED` 알림 타입의 설명 문구만 추가했다(`describe-notification.ts`) — Spike Detection이 처음엔 백엔드만 있다가 나중에 Dashboard(Phase 10)로 소비된 것과 같은 순서로, `/qunobot/version-impacts`를 소비하는 전용 화면은 이번 Phase 범위 밖이다.

## 관련 문서

- [domain-model.md](../domain-model.md#qunobot-이벤트-체인-phase-4)
- [api-design.md](../api-design.md#기술-버전-영향-감지-phase-21)
- [ADR-0017](0017-manual-outdated-marking-and-spike-detection-scope.md) (이번 ADR이 채워 넣는 범위 밖 결정)
- [ADR-0009](0009-redis-cache-global-aggregates-only.md) / [ADR-0010](0010-metrics-read-model-skip-dto.md) (Spike Detection이 세운 패턴 재사용/미재사용 근거)
- [PLAN.md](../../../PLAN.md) Phase 21
