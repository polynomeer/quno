# Quno 개발 작업계획서

Claude Code가 세션을 이어가며 순서대로 진행하기 위한 작업 목록이다. 각 단계는 [커밋 규칙](CONTRIBUTING.md)에 따라 완료 즉시 별도 커밋한다. 진행하면서 체크박스를 갱신한다.

- 제품 범위: [docs/product/mvp-scope.md](docs/product/mvp-scope.md)
- 제품 철학: [docs/product/vision.md](docs/product/vision.md)
- 시스템 설계: [docs/architecture/system-architecture.md](docs/architecture/system-architecture.md)
- 도메인 모델: [docs/architecture/domain-model.md](docs/architecture/domain-model.md)
- API 설계: [docs/architecture/api-design.md](docs/architecture/api-design.md)

## Phase 0 — 저장소/환경 설정 (완료)

- [x] `.gitignore`, `CLAUDE.md`, `CONTRIBUTING.md` 작성 및 커밋
- [x] 기획 문서 정리: `docs/product/`, `docs/architecture/` 재작성, 원본은 `docs/archive/`로 이동
- [x] 작업계획서(`PLAN.md`) 작성

## Phase 1 — 백엔드 프로젝트 스캐폴딩

기술 스택: [system-architecture.md](docs/architecture/system-architecture.md#확정-기술-스택) 참고 (Kotlin, Spring Boot 4.0.8, Java 21, Gradle Kotlin DSL, 단일 모듈 DDD). `backend/`에 프로젝트가 위치한다.

- [x] 1.1 Gradle Kotlin DSL 프로젝트 초기화 (`backend/build.gradle.kts`, `backend/settings.gradle.kts`) — Spring Initializr로 생성 (Spring Boot 4.0.8, Kotlin, Java 21). Web/Validation/Security/Data JPA/Data MongoDB/Data Redis/Flyway/PostgreSQL/Actuator 의존성 포함
- [ ] 1.2 패키지 뼈대 생성 — [system-architecture.md](docs/architecture/system-architecture.md#패키지-구조-kotlin-단일-모듈)의 `domain / application / interfaces / infrastructure` 구조. `infrastructure/config`는 먼저 생성했고(SecurityConfig), 나머지 도메인별 하위 패키지(`domain/user`, `domain/question` 등)는 빈 폴더로 미리 만들지 않고 Phase 2에서 실제 코드와 함께 생성한다
- [x] 1.3 `docker-compose.yml` 작성 — PostgreSQL 16, MongoDB 7, Redis 7 (이 머신의 다른 프로젝트와 겹치지 않도록 5442/6390 포트 사용, [system-architecture.md](docs/architecture/system-architecture.md#로컬-개발-환경) 참고)
- [x] 1.4 `application.yml` / `application-local.yml` 작성 (DB/Mongo/Redis 접속 정보, 프로필 분리, 서버 포트 8081)
- [x] 1.5 Flyway 초기 마이그레이션(`V1__init.sql`) — [domain-model.md](docs/architecture/domain-model.md#erd-postgresql--운영형)의 `users`, `questions`, `question_versions`, `answers`, `tags`, `question_tags`, `watches`, `user_tag_follows`, `notifications` 테이블
- [x] 1.6 인증 방식 확정 및 문서 반영 — JWT(Access/Refresh, Stateless)로 확정, [api-design.md](docs/architecture/api-design.md#인증-확정--2026-08-24) 갱신, 최소 `SecurityConfig` 추가 (실제 JWT 필터는 Phase 2.1)
- [x] 1.7 헬스체크/기본 실행 확인 — `docker compose up -d` + `SPRING_PROFILES_ACTIVE=local ./gradlew bootRun`으로 기동, `/actuator/health`에서 db/mongo/redis 모두 `UP` 확인, 미인증 요청은 거부됨을 확인

## Phase 2 — 코어 도메인 구현 (MVP P0)

순서는 의존관계를 따른다: User → Question/QuestionVersion → Answer → Tag → Watch/Notification → Search 기초.

- [x] 2.1 **Identity**: User 도메인/JPA 엔티티, 회원가입·로그인·리프레시 API, 기본 프로필 조회. JWT 발급/검증(`JwtTokenProvider`, `JwtAuthenticationFilter`) 포함, curl로 signup→login→me(무인증 401 확인)→refresh 전체 플로우 검증 완료
- [x] 2.2 **Question 생성**: Question + QuestionVersion(v1) 생성 유스케이스, `POST /api/v1/questions`, `GET /api/v1/questions/{id}`. 두 엔드포인트 모두 인증 필요(SecurityConfig 기본값 유지) — 질문 조회를 비로그인 사용자에게 공개할지는 아직 결정하지 않았고, 향후 Search/Discovery(P1) 설계 시 재검토 필요. curl로 인증없이 생성 시 401, 생성/조회/404/유효성검증(400) 플로우 검증 완료
- [x] 2.3 **Question Revision**: 새 QuestionVersion append, `latest_version_id` 갱신(작성자만, `Question.revise()`가 OPEN/NEEDS_INFO→UPDATED 전이, RESOLVED는 유지), 동시성 방어(`SELECT ... FOR UPDATE` 락 + DB unique 제약), LCS 기반 라인 Diff(`TextDiffer`, 순수 도메인 유틸). `POST/GET /api/v1/questions/{id}/versions`, `GET .../versions/{version}`, `GET .../versions/{version}/diff` 구현. curl로 타인 리비전 시도 403, 작성자 리비전 성공(상태 UPDATED 전이), 버전 히스토리/개별조회/404/diff 전체 플로우 검증 완료
- [x] 2.4 **Answer**: 답변 작성/조회, 채택 유스케이스(질문 작성자만 채택, 기존 채택 답변 자동 해제, `Question.resolve()`로 RESOLVED 전환). `POST/GET /api/v1/questions/{id}/answers`, `POST /api/v1/answers/{id}/accept`. curl로 답변 작성/목록, 비작성자 채택 시도 403, 채택 성공 시 질문 RESOLVED 전환, 존재하지 않는 답변 404 검증 완료
- [x] 2.5 **Question Status**: `OPEN`(생성) → `UPDATED`(리비전) → `RESOLVED`(채택) 전이는 2.2~2.4에서 이미 구현됨. `NEEDS_INFO`는 답변자의 "정보 요청" 액션이 있어야 발생하는데 이는 QPR Review 기능(mvp-scope.md에서 MVP 이후로 명시된 범위)에 속하므로 지금 만들지 않고 Phase 2(로드맵)에서 QPR과 함께 구현하기로 결정
- [x] 2.6 **Tag**: `Tag`(create/rename/softDelete), `POST /questions`가 `tags` 필드로 태그를 find-or-create해 `question_tags`에 연결(slug 기준 중복 제거), `GET /tags`(검색), `POST/DELETE /tags/{id}/follow`. 버그 발견 및 수정: 대소문자만 다른 태그명("Kotlin"/"kotlin")이 `uq_tags_slug_active` 유니크 제약을 위반하면서 예외가 `/error`로 forward되어 SecurityConfig에 막혀 401로 위장되는 문제 → find-or-create를 slug 기준 조회로 변경하고 `SecurityConfig`에 `/error` permitAll 추가. curl로 재현 및 수정 확인 완료
- [x] 2.7 **Watch(Ward)**: 구독/해제(exists-check 기반 idempotent), `POST/DELETE /questions/{id}/watch`, `GET /me/watches`. Outbox 이벤트 발행 골격(V2 마이그레이션 `outbox_events`, `domain/common/OutboxEvent`)을 추가하고 ReviseQuestion/WriteAnswer/AcceptAnswer 유스케이스가 각각 QUESTION_REVISION/NEW_ANSWER/ANSWER_ACCEPTED를 같은 트랜잭션에서 기록하도록 연결(소비자는 Phase 2.8). curl+DB 조회로 와드 등록/멱등성/404, 리비전·답변·채택 시 outbox_events row 생성 확인 완료
- [x] 2.8 **Notification**: `DispatchOutboxEventsUseCase`가 2초 주기 스케줄러(`OutboxDispatchScheduler`)로 `outbox_events`를 소비해 Watch 기반 fan-out. 이벤트별 추가 수신자(NEW_ANSWER→질문 작성자, ANSWER_ACCEPTED→답변 작성자)와 액터 제외 규칙 반영. `GET /api/v1/me/notifications`, `POST /api/v1/me/notifications/mark-read`(일괄 읽음). V3 마이그레이션으로 notifications.payload_json을 outbox_events와 동일하게 payload(TEXT)로 정리. curl로 질문작성자 자동 알림(미와드 상태에서도), 답변작성자 자동 알림, 액터 제외, 일괄 읽음 처리 전 구간 실제 스케줄러 동작으로 검증 완료
- [x] 2.9 **Search/Related Questions**: PostgreSQL `to_tsvector`/`plainto_tsquery` 기반 전문검색(제목/본문/에러로그, 최신 버전만) + 태그 ILIKE, `GET /api/v1/search?q=&limit=`. `question_tags` 자기 조인으로 공유 태그 수를 계산하는 관련 질문 추천, `GET /api/v1/questions/{id}/related`(태그 매칭 우선, mvp-scope.md 반영). curl로 제목/본문/에러로그/태그 검색과 태그 중첩 기반 관련 질문(공유 태그 없는 질문 제외) 확인 완료
- [x] 2.10 도메인 단위 테스트 — 2.1~2.9에서 이미 작성한 단위 테스트(인메모리 fake 기반)로 리비전 append-only/버전 단조증가, 채택 invariant(1문제 1채택), 와드 중복 방지, 채택 권한, 리비전 권한 등을 감사. fake로는 검증 불가능한 두 가지 실제 DB 의존 규칙을 `integration/` 패키지에 `@SpringBootTest` 통합 테스트로 보강:
  - `TagSlugUniquenessIntegrationTest` — 2.6에서 발견한 "Kotlin"/"kotlin" slug 충돌 버그의 회귀 테스트. 실제 `uq_tags_slug_active` 제약에 대해 find-or-create가 여전히 안전한지 확인 (인메모리 fake에는 이 제약이 없어 재현 불가)
  - `ReviseQuestionConcurrencyIntegrationTest` — 8개 스레드가 동일 질문을 동시에 리비전해도 `SELECT ... FOR UPDATE` 락 덕분에 버전 번호 중복/누락이 없음을 실제 Postgres 트랜잭션으로 검증
  - 범위 밖으로 확인: Question/Answer/Tag의 `softDelete`는 domain 클래스에 메서드는 있지만 이를 호출하는 유스케이스/API가 MVP P0에 없어("삭제된 질문 수정 금지" 등은 아직 실제로 도달 불가능한 코드) 지금 만들지 않음 — 삭제 기능이 실제로 추가되는 시점에 함께 테스트

## Phase 3 — MVP P1

- [x] 3.1 태그 팔로우 기반 추천 점수식 구현 ([domain-model.md](docs/architecture/domain-model.md#태그-팔로우-기반-추천-쿼리) SQL 참고). `GET /api/v1/recommendations/questions?source=tags`, 점수식 `matched_tag_count*3 + LEAST(answer_count,5)`. Search/Related와 candidate-id 랭킹 후 hydrate 패턴이 겹쳐 `application/common/QuestionSummaryHydrator`로 결과 조립 로직을 공용화(3중복 시점에 추출). curl로 팔로우 전/후 추천 변화, 본인 질문 제외(`author_id <> userId`) 확인 완료
- [x] 3.2 라이트 대시보드 API — `GET /api/v1/dashboard`가 4개 섹션을 조합(오늘의 인기 질문 Top5/내 Ward 업데이트 5건/팔로우 태그 피드 10건/태그 트렌드 10건). 대시보드 전용 로직 없이 기존 유스케이스(`ListMyNotificationsUseCase`, `RecommendQuestionsUseCase`) 재사용. 인기 질문은 조회수 미구현으로 Watch·Answer 수 기반 근사([api-design.md](docs/architecture/api-design.md#라이트-대시보드-phase-32) "알려진 단순화" 기록). curl로 실제 누적 데이터 기준 4개 섹션 모두 확인(팔로우 피드가 본인 질문 제외 규칙과 일관되게 비어있음도 확인)
- [x] 3.3 사용자 프로필 라이트 — `GET /api/v1/users/{id}/profile`(공개, 이메일 미포함). 작성 질문은 `QuestionSummaryHydrator` 재사용, 작성 답변은 기존 `AnswerResult`/`toResult()` 재사용, 팔로우 태그는 `UserTagFollowRepository`+`TagRepository` 조합. 이 참에 컨트롤러에 흩어져 있던 `toResponse()` 확장 함수들(Answer/Tag)을 각 Responses.kt로 승격해 재사용 가능하게 정리. curl로 실제 누적 데이터 기준 질문/답변/팔로우태그 전 섹션과 존재하지 않는 사용자 404 확인 완료
- [x] 3.4 Redis 캐시 적용 — `DashboardRepositoryAdapter`의 `popularQuestions`/`trendingTags`(모든 사용자 공통, 비용이 큰 native 집계 쿼리)에 cache-aside 적용. `StringRedisTemplate`+Jackson 직렬화, TTL 60초, 능동적 무효화 없음(TTL 만료로만 갱신). `wardUpdates`/`followingTagsFeed`는 사용자별 최신성이 중요해 캐시하지 않기로 결정([api-design.md](docs/architecture/api-design.md#redis-캐시-phase-34) 기록). redis-cli로 캐시 키/TTL 생성 확인, sentinel 값 주입 후 API가 그 값을 그대로 반환함을 확인해 실제로 캐시를 읽는다는 것을 증명, 키 삭제 후 재조회 시 DB에서 새로 채워지는 것도 확인 완료

## Phase 4 — 검증

- [x] 4.1 [mvp-scope.md](docs/product/mvp-scope.md#성공-지표) 지표 계측 — `GET /api/v1/metrics`가 백엔드 데이터만으로 계산 가능한 지표(Revision Rate, Answer/Accept Rate, Ward 커버리지, North Star 후보 "주간 활성 Living Questions")를 native SQL 한 번으로 집계. `MetricsLoggingScheduler`가 동일 스냅샷을 30분 주기로 INFO 로깅. CTR/D1·D7 Retention은 클라이언트 이벤트 트래킹이 필요해 프론트엔드가 없는 현재는 계측 지점이 없음을 명시하고 범위에서 제외([api-design.md](docs/architecture/api-design.md#지표-계측-phase-41) 기록). 실제 서버 기동 후 curl로 응답 확인 완료
- [x] 4.2 E2E 시나리오 테스트 — `QuestionLifecycleE2ETest`(`integration/`)를 기존 통합 테스트들과 달리 `MockMvc`로 실제 HTTP+JWT 보안 필터까지 통과시켜 작성: 질문 생성 → Ward(watch) → 리비전 → (outbox 소비 후) QUESTION_REVISION 알림 확인 → 답변 → NEW_ANSWER 알림 → 채택(RESOLVED 전환) → ANSWER_ACCEPTED 알림까지 한 번에 검증. `DispatchOutboxEventsUseCase`를 테스트에서 직접 호출해 2초 스케줄러 타이밍에 의존하지 않도록 결정적으로 구성. 실제 Postgres 대상으로 통과 확인 완료
- [x] 4.3 MVP 핵심 가설 검증 데모 — 프론트엔드 대신 `backend/scripts/demo.sh`(API 데모 플로우)를 선택: 실행 중인 서버에 대해 가입→질문 생성→Ward→리비전→diff 확인→Ward 알림→답변→채택→알림→검색→태그 팔로우 추천→공개 프로필→대시보드→지표 스냅샷까지 curl로 순서대로 호출하며 각 단계 응답을 사람이 읽을 수 있게 출력. 실제 로컬 서버에 대해 전체 스크립트 실행 및 통과 확인 완료(outbox 비동기 처리 타이밍 때문에 알림 확인 단계에 짧은 대기를 넣음)

## Phase 5 — 협업형 QPR (mvp-scope.md 로드맵 Phase 2)

GitHub PR의 Review/Re-request 개념을 질문에 적용한다 — [vision.md](docs/product/vision.md#다른-서비스에서-차용하는-개념)의 QPR(Question Pull Request) 컨셉과 [domain-model.md](docs/architecture/domain-model.md#qpr-이벤트-체인-phase-2) 이벤트 체인(`RequestMoreInfo → NEEDS_INFO → 리비전 → ReRequestReview`)을 그대로 구현 대상으로 삼는다. 여러 리뷰어가 각자 독립적으로 정보를 요청/재요청할 수 있는 **다중 리뷰 요청 스레드 모델**로 구현하기로 결정했다(2026-08-25, GitHub PR review와 동일한 형태이며 단일 NEEDS_INFO 플래그안보다 이벤트 체인·vision.md 취지에 더 부합한다고 판단).

- [x] 5.1 답변–질문버전 연결 고도화 — `answers.target_version_number` 컬럼 추가(V4 마이그레이션, 기존 행은 1로 백필). `Answer.write()`가 `targetVersionNumber`를 필수로 받고, `WriteAnswerUseCase`가 작성 시점 질문의 최신 버전 번호를 자동 계산해 넘긴다(버전 선택 UI는 만들지 않음 — MVP 단순화). `application/common/AnswerResultAssembler`를 새로 만들어 `targetVersionNumber`/`isStale`(질문이 그 이후 리비전됐는지, 저장하지 않고 조회 시점 계산) 계산을 `WriteAnswerUseCase`/`ListAnswersUseCase`/`GetUserProfileUseCase` 세 곳에서 공유(`QuestionSummaryHydrator`와 동일한 3중복 추출 패턴). 실제 서버로 질문 생성→v1 답변(isStale=false)→리비전→기존 답변 재조회(isStale=true)→v2 답변(isStale=false) 흐름 curl로 검증 완료
- [x] 5.2 ReviewRequest 도메인 — 새 Aggregate `ReviewRequest`(id, questionId, requestedBy, message, status: OPEN/ADDRESSED, questionVersionNumberAtRequest, createdAt, addressedAt, V5 마이그레이션)와 `ReviewRequestRepository`. `Question.requestMoreInfo()` 추가(RESOLVED면 `QuestionAlreadyResolvedException`, 이미 NEEDS_INFO면 no-op, 그 외에는 NEEDS_INFO로 전이). `POST /api/v1/questions/{id}/review-requests`(작성자 본인 요청 시 `SelfReviewRequestException`/403), `GET /api/v1/questions/{id}/review-requests`(전체 목록). `REVIEW_REQUESTED` outbox 이벤트를 기존 fan-out에 추가(Ward 구독자 + 질문 작성자). 실제 서버로 본인 요청 403, 정보 요청→NEEDS_INFO 전환→목록 조회→작성자 알림 수신 전체 흐름 curl 검증 완료. 재요청/상태 복귀는 5.3에서 이어감
- [x] 5.3 재요청(Re-request Review) — `POST /api/v1/questions/{id}/review-requests/{reviewRequestId}/re-request`(질문 작성자만 가능, 아니면 403 / 이미 ADDRESSED인 요청 재요청 시 409 / 요청 시점(`questionVersionNumberAtRequest`) 이후 실제 리비전이 없으면 409 `QuestionNotRevisedSinceRequestException`). 해당 요청만 ADDRESSED로 전환하고 `REVIEW_RE_REQUESTED` outbox 이벤트로 원 요청자(+ Ward 구독자)에게 알린다. **설계 수정**: 애초 계획했던 "열려있는 요청이 없으면 Question을 NEEDS_INFO→UPDATED로 복귀"는 구현 중 작성한 단위 테스트가 모순을 발견해 제거했다 — `Question.revise()`가 이미 리비전 시점에 무조건 NEEDS_INFO를 벗어나므로(열려있는 요청 수와 무관), 재요청이 허용되는 시점엔 항상 이미 UPDATED라 그 로직이 도달 불가능한 코드였다(`Question.reviewAddressed()` 삭제, [ADR-0015](docs/architecture/decisions/0015-review-request-status-independent-of-question-status.md)). 실제 서버로 리비전 전 재요청 409, 비작성자 403, 재요청 성공, 중복 재요청 409, 원 요청자 알림 수신 전체 흐름 curl 검증 완료
- [x] 5.4 알림 통합 — `REVIEW_REQUESTED`(작성자 + Ward 구독자, 요청자 본인 제외)와 `REVIEW_RE_REQUESTED`(원 요청자 + Ward 구독자, 작성자 본인 제외)를 5.2/5.3 구현 시점에 이미 `DispatchOutboxEventsUseCase`의 기존 fan-out 패턴에 추가했다 — 두 기능이 알림 없이는 완결되지 않아 별도 단계로 미루지 않고 바로 통합했다. curl로 두 알림 모두 실제 수신 확인 완료(5.2, 5.3 기록 참고)
- [x] 5.5 테스트 — 인메모리 fake 기반 단위 테스트를 5.1~5.3 구현과 함께 이미 작성함(본인 질문 요청 금지, RESOLVED 질문 요청 금지, 다중 리뷰어 독립성, 재요청 권한/상태 invariant, 리비전 전 재요청 거부, 중복 재요청 거부). 추가로 `QuestionReviewLifecycleE2ETest`(`integration/`)를 `QuestionLifecycleE2ETest`와 동일한 MockMvc+실제 JWT 패턴으로 작성해 정보요청(본인 요청 403 포함) → NEEDS_INFO 전환 → 작성자 알림 → 리비전 전 재요청 409 → 리비전 → 재요청 성공(ADDRESSED) → 원 요청자 알림 → 목록 조회까지 실제 HTTP로 검증
- [x] 5.6 문서화 — `domain-model.md`의 Bounded Context/Aggregate 표·ERD·테이블별 책임·QPR 이벤트 체인 구현 상태를 5.1~5.3 진행과 함께 갱신했고, `api-design.md`에 "답변–질문버전 연결 (Phase 5.1)"과 "QPR Review — 정보 요청/재요청 (Phase 5.2~5.3)" 섹션을 추가했다. ADR-0014(답변 대상 버전 자동 기록), ADR-0015(ReviewRequest.status는 Question.status를 다시 게이팅하지 않음)를 새로 기록해 Phase 5 전체가 완료됐다

## Phase 6 — 질문 네트워크: Cluster + Super Answer (mvp-scope.md 로드맵 Phase 3, 일부)

mvp-scope.md 로드맵 Phase 3(질문 네트워크: Cluster, Merge/Fork, Super Answer, 지식 그래프 시각화) 중 **Cluster와 Super Answer만** 이번 Phase에서 다룬다(2026-08-25 결정). Merge/Fork는 실제 요청 UX가 아직 불명확해 사용 패턴을 관찰한 뒤 후속 Phase로 미루고, 지식 그래프 "시각화"는 프론트엔드가 없는 현재 범위 밖이다(Phase 4.3과 동일한 판단).

**클러스터링 방식(2026-08-25 결정)**: 임베딩/벡터 유사도 기반 자동 클러스터링은 도입하지 않는다. 대신 Stack Overflow의 "mark as duplicate"처럼 **사용자가 두 질문이 같은 문제라고 명시적으로 표시**하면 클러스터가 생성/합류된다 — 기존 태그 중첩 기반 관련 질문 기능과 자연스럽게 이어지고, 새 ML/벡터 인프라가 필요 없다([ADR-0008](docs/architecture/decisions/0008-postgres-native-search.md)이 이미 벡터 유사도를 유보한 것과 같은 방향). 질문은 최대 하나의 클러스터에만 속한다(`questions.cluster_id`) — 이미 서로 다른 클러스터에 속한 두 질문을 하나로 합치는 것(클러스터 병합)은 지원하지 않고 명시적으로 거부한다(그 자체가 Merge 기능의 영역이므로 후속 Phase로 이연).

- [x] 6.1 QuestionCluster 도메인 — 새 Aggregate `QuestionCluster`(id, representativeAnswerId, createdAt, V6 마이그레이션: `question_clusters` 테이블 + `questions.cluster_id`)와 `QuestionClusterRepository`. `Question`에 `clusterId: Long?` 필드와 `joinCluster()` 추가. `POST /api/v1/questions/{id}/cluster`(body: `relatedQuestionId`)로 두 질문을 같은 클러스터로 묶는다: 둘 다 클러스터가 없으면 새로 생성, 하나만 있으면 다른 쪽이 합류, 이미 같은 클러스터면 no-op, 서로 다른 클러스터면 409(`ClustersAlreadyDistinctException`), 자기 자신을 지정하면 400(`CannotClusterWithSelfException`)
- [x] 6.2 클러스터 조회 — `GET /api/v1/questions/{id}/cluster`(없으면 404 `QuestionNotInAnyClusterException`), `GET /api/v1/clusters/{id}`(없으면 404 `ClusterNotFoundException`). 멤버 질문 목록은 `QuestionSummaryHydrator` 재사용, Super Answer가 지정돼 있으면 함께 반환
- [x] 6.3 Super Answer 지정 — `POST /api/v1/clusters/{id}/super-answer`(body: `answerId`). 그 답변이 (a) 클러스터에 속한 질문 중 하나의 답변이어야 하고(아니면 409 `AnswerNotInClusterException`), (b) 채택된 답변이어야 한다(아니면 409 `AnswerNotAcceptedException`). 지정 권한은 리뷰 요청과 동일하게 제한 없음(현재 역할/평판 시스템이 없음). `DesignateSuperAnswerUseCase`가 결과 조립을 `GetClusterUseCase`에 위임해 중복 없이 재사용
- [x] 6.4 테스트 — 단위 테스트(신규 생성/합류/no-op/거부 4분기, 자기 자신 지정 거부, 존재하지 않는 질문/클러스터/답변, Super Answer 조건 위반 2가지)와 `QuestionClusterLifecycleE2ETest`(`integration/`)를 다른 Lifecycle E2E 테스트와 동일한 MockMvc+실제 JWT 패턴으로 작성해 자기 자신 클러스터링 400 → 클러스터 생성 → 조회 → 미채택 답변 지정 409 → 채택 → Super Answer 지정 성공 → 재조회 반영까지 실제 HTTP로 검증
- [x] 6.5 문서화 — `domain-model.md`에 `Knowledge` Bounded Context/`QuestionCluster` Aggregate/ERD/테이블별 책임/Domain Events/"지식 진화 체인 (Phase 3)" 구현 상태를 반영(자동 유사도 분석·자동 Super Answer 후보 탐지·Cluster/Super Answer의 outbox 미발행 이유를 명시), `api-design.md`에 "질문 Cluster & Super Answer (Phase 6)" 섹션 추가, ADR-0016(클러스터링 방식·이번 Phase 범위 결정)으로 스코프 결정을 기록. 실제 서버로 자기 클러스터링 400, 클러스터 생성/조회, 미채택 답변 409, Super Answer 지정 성공까지 curl 검증 완료

## Phase 8 — 자동 유지보수: Outdated 표시 + Spike Detection (mvp-scope.md 로드맵 Phase 4, 일부)

mvp-scope.md 로드맵 Phase 4(QunoBot, 기술 버전 영향 감지, Outdated/Regression, Spike Detection) 중 **실제로 자동화 가능한 부분만** 이번 Phase에서 다룬다(2026-08-25 결정). "기술 버전 영향 감지"의 진짜 자동화는 외부 기술 버전 릴리스 데이터 피드(예: endoflife.date)가 있어야 가능한데 그런 연동은 없다 — 그 부분은 명시적으로 범위 밖에 두고, 대신 Cluster/Review와 동일한 **사용자 명시적 표시** 패턴으로 Outdated를 근사한다. 반대로 Spike Detection(특정 태그의 질문량 급증 감지)은 기존 대시보드 집계 인프라만으로 진짜 자동 감지가 가능해 이번 Phase에 포함한다.

- [x] 8.1 Question OUTDATED 상태 + 수동 표시 — `QuestionStatus`에 `OUTDATED` 추가. `Question.markOutdated()`(멱등 — 이미 OUTDATED면 no-op). `POST /api/v1/questions/{id}/outdated`(body: `reason`, 권한 제한 없음 — Cluster와 동일). `QUESTION_OUTDATED` outbox 이벤트를 기존 fan-out에 추가(Ward 구독자 + 질문 작성자, 액터 제외, `reason`은 JSON 이스케이프해 payload에 포함). `Question.revise()`는 이미 RESOLVED가 아니면 UPDATED로 전이하므로 OUTDATED→UPDATED(리비전으로 "다시 살아남")는 코드 변경 없이 자연히 성립 — 별도 REOPENED 상태는 도입하지 않는다. 실제 서버로 표시→상태 확인→작성자 알림 수신→리비전 후 UPDATED 복귀까지 curl 검증 완료
- [x] 8.2 Spike Detection — 새 `domain/qunobot` 패키지(`TagSpike` 값 객체, `SpikeDetectionRepository` 포트). 태그별 최근 1일 질문 수 vs 직전 14일 일평균을 비교하는 native SQL로 급증 비율(spikeRatio)을 계산(최근 질문 3건 미만인 태그는 제외해 노이즈 방지). `GET /api/v1/qunobot/spikes?limit=`. Dashboard의 트렌드 캐싱과 동일하게 Redis cache-aside 적용(ADR-0009 패턴 재사용, 새 ADR 불필요). 실제 태그 5건을 급증시켜 spikeRatio 계산 확인 + redis-cli 3단계(키/TTL 생성 → sentinel 주입 후 그대로 반환 → 키 삭제 후 재계산) 캐시 검증 완료
- [x] 8.3 테스트 — `markOutdated()`/`MarkQuestionOutdatedUseCase` 단위 테스트(멱등성, 리비전 시 UPDATED 복귀, outbox 이벤트, 본인 표시 허용)와 `QuestionOutdatedLifecycleE2ETest`(다른 사용자가 표시→작성자 알림→리비전 후 UPDATED 확인, 실제 HTTP+JWT). Spike Detection의 native SQL 자체는 Dashboard/Metrics와 동일하게 자동 테스트 대신 실제 서버+curl로 검증(날짜 윈도우 집계는 인메모리 fake로 재현 불가능한 영역)
- [x] 8.4 문서화 — `domain-model.md`에 `OUTDATED` 상태값·`Maintenance` Bounded Context·QunoBot 이벤트 체인 구현 상태(기술 버전 자동 감지는 미구현임을 명시)·Domain Events 반영, `api-design.md`에 "Outdated 표시 & Spike Detection (Phase 8)" 섹션 추가, ADR-0017로 스코프 결정 기록

## Phase 9 — 신뢰 네트워크: 간단한 평판 점수 (mvp-scope.md 로드맵 Phase 5, 일부)

mvp-scope.md 로드맵 Phase 5(Organization, 전문가 평판, Direct Ask) 중 **기존 데이터만으로 계산 가능한 간단한 평판 점수만** 이번 Phase에서 다룬다(2026-08-25 결정). Organization(조직 인증)과 Direct Ask(결제 포함)는 조직 인증 방식·매칭 기준·결제 처리 범위 등 핵심 설계가 문서에 없어 임의로 설계하기보다 후속 Phase로 미룬다 — mvp-scope.md의 MVP 제외 목록도 "복잡한 Reputation Economy"만 제외했지 간단한 평판까지 배제하지 않았다.

- [x] 9.1 평판 점수 계산 — 새 `domain/reputation` 패키지(`UserReputation` 값 객체 — questionCount/answerCount/acceptedAnswerCount/superAnswerCount 원시 집계 + 계산된 `score` 프로퍼티, `ReputationRepository` 포트). `score = questionCount*1 + answerCount*2 + acceptedAnswerCount*15 + superAnswerCount*10`(채택 답변과 Super Answer 지정에 가중치를 크게 둠 — Stack Overflow 평판 모델과 같은 방향). native SQL 한 번으로 사용자당 4개 카운트를 집계(Metrics와 동일한 단일 행 subquery 패턴). 캐싱은 두지 않는다(사용자별 조회라 Dashboard/Spike처럼 모두에게 같은 결과가 아님)
- [x] 9.2 API — `GET /api/v1/users/{id}/reputation`. 존재하지 않는 사용자는 404. `UserReputation`은 도메인 불변조건이 없는 순수 조회 모델이라 ADR-0010과 동일하게 DTO 복제 없이 그대로 응답으로 노출
- [x] 9.3 테스트 — 단위 테스트(점수 계산 공식, 활동 없음=0점, 존재하지 않는 사용자 404)와 실제 서버+curl 검증(질문/답변/채택/Super Answer 지정 단계별로 점수가 정확히 0→17→27로 증가하는 것을 확인 — native SQL 집계라 Dashboard/Metrics/Spike Detection과 동일하게 자동 통합 테스트 대신 실제 데이터로 검증)
- [x] 9.4 문서화 — `domain-model.md`에 Reputation Bounded Context 반영, `api-design.md`에 "전문가 평판 (Phase 9)" 섹션 추가, ADR-0018로 스코프 결정 기록

## Phase 10 — 소비 경험 강화: Quno Flow + 고급 Dashboard (mvp-scope.md 로드맵 Phase 6, 일부)

mvp-scope.md 로드맵 Phase 6(Quno Flow, Instant Question, 실시간 질문방, 고급 Daily Dashboard) 중 **Quno Flow와 고급 Dashboard만** 이번 Phase에서 다룬다(2026-08-25 결정). mvp-scope.md/vision.md에는 한 줄뿐이지만 [docs/archive/Quno 서비스 통합 기획서](docs/archive/README.md)의 원본 절(22장 "Daily Newspaper Dashboard", 23장 "Quno Flow", 19장 "실시간 질문 공간")을 참고해 구체화했다.

- **Quno Flow**는 원본 기획을 보면 사실 "살아 움직이는 Question Network의 Activity Stream"이다 — 인기 질문, 태그 급증, 재활성화(Outdated→리비전)된 질문, Cluster의 새 Super Answer 같은 **기존 신호를 하나의 카드 피드로 묶는 것**이라 새 인프라 없이 만들 수 있다.
- **고급 Daily Dashboard**도 마찬가지로 기존 Dashboard(Phase 3.2)에 헤드라인·오늘 해결된 질문·재활성화된 질문·Trending Errors 섹션을 추가하는 확장이다.
- **실시간 질문방(Live Chat, 19장)**은 WebSocket 기반 실시간 연결·현재 접속자 수 추적·메시지 영속화라는 완전히 새로운 인프라가 필요해 이번 세션의 다른 Phase들과 투자 규모가 다르다 — 범위 밖에 두고 번호 미정으로 미룬다.
- **Instant Question**은 원본 기획서에서도 별도 백엔드 설계가 없고, 이미 `POST /questions`가 필수 필드(title/body)만으로 질문을 만들 수 있어 그 자체로 "즉석 질문"을 지원한다 — 프론트엔드 UX 문제일 뿐 추가 백엔드 작업이 없다(이 세션 전체에 프론트엔드가 없다는 점도 Phase 4.3과 동일).

- [x] 10.1 재활성화/Super Answer 갱신 신호 — 새 `domain/flow` 패키지에 `FlowRepository` 포트. `findRecentlyReopenedQuestionIds(limit)`: `outbox_events`(QUESTION_OUTDATED) 발생 이후 새 `question_versions`가 생긴 질문을 찾는 native SQL(새 컬럼 없이 기존 이벤트 로그+리비전 기록만으로 "재활성화"를 도출 — Spike Detection과 같은 방식). `question_clusters`에 `updated_at` 컬럼 추가(V7 마이그레이션)하고 `QuestionCluster.designateSuperAnswer()`가 갱신하도록 수정한 뒤 `findRecentlySuperAnsweredClusterIds(limit)` 추가. 이 포트는 10.2와 10.3이 함께 재사용한다
- [x] 10.2 고급 Daily Dashboard 확장 — 기존 `GetDashboardUseCase`/`DashboardResult`/`DashboardResponse`(Phase 3.2)에 필드 추가(하위 호환 — 기존 필드는 그대로 둠): `headline`(급증 태그가 있으면 그것, 없으면 최고 인기 질문 — Quno Flow의 카드 조립을 재사용해 중복 구현 없음), `resolvedToday`(오늘 RESOLVED 전환된 질문, 캐싱 없음), `reopenedKnowledge`(10.1 재사용), `trendingErrors`(기존 `SpikeDetectionRepository.findSpikingTags` 재사용 — 알려진 단순화)
- [x] 10.3 Quno Flow — 새 `application/flow/usecase/GetActivityFeedUseCase`. `FlowCard`(type, headline: String, questionId?, clusterId?)와 `FlowCardType`(POPULAR_QUESTION/TAG_SPIKE/REOPENED_QUESTION/CLUSTER_SUPER_ANSWER) 4종을 고정 섹션 순서로 묶어 반환. **설계 단순화**: 최초 계획에 있던 `occurredAt` 필드는 구현 중 제거했다 — 카드를 하나의 타임라인으로 정렬하지 않기로 한 이상(인기/급증은 자연스러운 발생 시각이 없는 스냅샷이라) 항상 "지금"만 담길 필드는 정보 가치가 없었다. `GET /api/v1/flow?limit=`(섹션당 개수, 기본 5)
- [x] 10.4 테스트 — `GetActivityFeedUseCase`(4개 카드 타입별 조립, 신호 없을 때 빈 목록)와 `GetDashboardUseCase`(헤드라인 우선순위: 급증 > 인기, 신호 없으면 null; resolvedToday/reopenedKnowledge/trendingErrors 조립)를 인메모리 fake로 단위 테스트. reopened/super-answered native SQL과 두 기능 전체는 Dashboard/Metrics/Spike Detection과 동일하게 실제 서버+curl로 검증(질문 outdated→리비전→재활성화 카드 등장, 클러스터 Super Answer 지정→카드 등장 및 대시보드 헤드라인 반영까지 확인)
- [x] 10.5 문서화 — `domain-model.md`에 Flow Bounded Context와 `question_clusters.updated_at` 반영, `api-design.md`에 "Quno Flow & 고급 Dashboard (Phase 10)" 섹션 추가(Trending Errors 근사 방식 포함), ADR-0019로 실시간 질문방/Instant Question을 범위 밖에 둔 이유 기록

## Frontend Phase 0 — 프로젝트 셋업 + 디자인 기반

[docs/frontend/](docs/frontend/README.md) 설계서 기반 첫 프론트엔드 착수. [ADR-0020](docs/architecture/decisions/0020-frontend-scoped-to-backend-support.md)에 따라 **백엔드가 이미 지원하는 화면부터** 만든다 — Vote/Comment/Save/Follow User/Badge/모더레이션/답변 리비전은 이번 범위에서 제외한다. 코드는 [frontend/](frontend/)에 둔다(백엔드는 [backend/](backend/)).

- [x] F0.1 프로젝트 스캐폴딩 — `create-next-app`으로 Next.js 16(App Router, Turbopack)+TypeScript+Tailwind v4+ESLint로 `frontend/` 생성. TanStack Query/Zustand/React Hook Form+Zod/clsx(런타임)와 Vitest+Testing Library+Playwright(테스트)를 추가 설치. `package.json`에 dev/build/lint/test 스크립트 구성, `vitest.config.ts`+`vitest.setup.ts`로 jsdom 테스트 하네스 구성(스모크 테스트 1개로 동작 확인). Next.js가 번들링한 rolldown-vite와 vitest 자체 vite 의존성이 타입 충돌을 일으켜 `vitest.config.ts`는 `tsconfig.json` 타입체크 대상에서 제외. `run.sh` 통합은 보류(F0.4/F0.5로 실제 백엔드 연동 확인 후에도 프론트엔드는 별도 `npm --prefix frontend run dev`로 기동 — 백엔드와 프론트엔드를 하나의 스크립트로 묶을 필요성이 아직 없음)
- [x] F0.2 디렉터리 구조 — [architecture.md](docs/frontend/architecture.md#26-디렉터리코드-구조)대로 `app/(auth)/`, `features/auth/`, `shared/{api,ui,lib}/`, `widgets/app-header/`를 실제로 구성. 계획대로 `comment`/`moderation`은 만들지 않음. `entities/`는 F0 범위(인증만)에 아직 소비자가 없어 생성하지 않고 Frontend Phase 1에서 필요해지는 시점에 만들기로 함
- [x] F0.3 디자인 토큰 + 기본 컴포넌트 — [design.md](docs/frontend/design.md#7-디자인-시스템)의 Color Token을 `globals.css`의 CSS Custom Properties(`:root` + `prefers-color-scheme: dark`)로 정의하고 Tailwind v4 `@theme inline`으로 `bg-brand`/`text-text-secondary` 등 유틸리티 클래스로 재노출(`tailwind.config.js` 없이 CSS-first 설정). Button/Input/StatusBadge(백엔드 `QuestionStatus` enum과 매핑)/TagChip/Skeleton 구현
- [x] F0.4 API Client + 인증 — `shared/api/http-client.ts`(fetch 래퍼) + `api-error.ts`(`ApiError` — status/code 보존). `shared/lib/token-storage.ts`로 Access/Refresh를 localStorage에 저장(SSR 안전), 401 응답 시 `/api/v1/auth/refresh`로 자동 재발급 후 원 요청 재시도(동시 요청은 모듈 스코프 `refreshPromise`로 중복 재발급 방지, [ADR-0003](docs/architecture/decisions/0003-stateless-jwt-auth.md)). `features/auth`에 로그인 폼(`app/(auth)/login/page.tsx`)과 세션 조회(`useSession`) 구현. 백엔드에 CORS 설정이 전혀 없어 `SecurityConfig`에 `http://localhost:3000` 허용 CORS Bean을 추가(별도 커밋 `feat(security): 프론트엔드 dev 서버용 CORS 허용`). `redirectTo` 보존과 회원가입 폼은 아직 미구현 — 로그인만으로 F0 검증 범위를 한정
- [x] F0.5 AppShell — `widgets/app-header`에 Header(로고/검색창/Ask 버튼/로그인 상태에 따른 닉네임+로그아웃 또는 로그인 버튼) 구현, `app/layout.tsx`에 `Providers`(TanStack Query)와 함께 배치. 알림/프로필 드롭다운·반응형 네비게이션은 아직 없음(대상 화면이 없어 Frontend Phase 1 이후로 미룸). 실제 회원가입(`POST /api/v1/auth/signup`)→로그인→홈에서 `GET /api/v1/dashboard` 데이터 렌더링까지 브라우저로 엔드투엔드 검증 완료

## Frontend Phase 1 — 읽기 경험

Home/Search/Question Detail/Tag 4개 화면을 모두 조회 전용으로 완성한다. 백엔드가 모든 조회 API에 인증을 요구하므로([ADR-0013](docs/architecture/decisions/0013-defer-public-read-access.md)) 이 Phase의 페이지도 전부 로그인이 필요하다 — 비로그인 접근은 `redirectTo`를 보존한 채 `/login`으로 보낸다. Vote/Comment/Watch/Accept 등 쓰기·커뮤니티 액션은 다음 Phase로 미룬다.

- [x] F1.1 인증 가드 + `redirectTo` — `features/auth/hooks/useRequireAuth.ts` 추가: 미로그인 시 `/login?redirectTo=`로 리다이렉트, 로그인 성공 후 그 경로로 복귀(로그인 폼을 `Suspense`로 감싸고 `useSearchParams`로 읽음, F0.4에서 미룬 부분). **버그 발견 및 수정**: 검증 중 `useSession`이 `localStorage`를 렌더링 중 동기적으로 읽어 AppHeader의 로그인/로그아웃 분기가 SSR과 어긋나는 Hydration 에러를 발견 — `useSyncExternalStore`로 교체(서버 스냅샷은 항상 `false`, React가 하이드레이션 시 실제 값으로 스스로 보정). 이 수정 후에도 `useRequireAuth`가 로그인된 사용자를 잘못 `/login`으로 튕기는 2차 버그를 발견: disabled 쿼리가 "아직 fetch를 시작하지 않음"과 "정말 로그인 안 됨"을 `isLoading`만으로 구분하지 못해 생기는 레이스였다 — 토큰 존재 여부는 effect 안에서 직접 읽고(SSR에서 실행되지 않으므로 안전), 토큰이 있으면 `/me` 요청이 실제로 완료(`isFetched`)될 때까지 리다이렉트를 미루도록 수정. 브라우저로 로그아웃→보호된 페이지 접근→`/login?redirectTo=`→로그인→원래 페이지 복귀까지 전체 흐름 검증 완료
- [x] F1.2 Home — `features/dashboard`(dashboard.api/flow.api) + `widgets/question-feed`(QuestionCard/QuestionList) + `widgets/activity-panel`(TrendingTagsPanel/FlowFeed)로 스모크 테스트용 임시 홈을 실제 화면으로 교체: 헤드라인 배너, 인기 질문, 오늘 해결된 질문/재활성화된 지식(있을 때만 렌더링), 관심 태그 피드, Trending 태그/에러 사이드 패널, Quno Flow 활동 스트림. 실제 서버 데이터(질문 생성→리비전→답변 채택→오늘 해결/태그 급증 반영)로 각 섹션 렌더링 확인
- [x] F1.3 Questions/Search — `/questions?q=`: `features/search`로 `GET /search` 연동, `URLSearchParams` 기반 URL 동기화(뒤로가기/공유 가능), 로딩 스켈레톤/빈 결과/에러 상태. score 정렬 등 백엔드 미지원 옵션은 만들지 않음. AppHeader 검색창도 이 페이지로 연결
- [x] F1.4 Question Detail — `/questions/{id}`: `features/question`(`GET /questions/{id}`, Markdown 렌더링은 `shared/ui/MarkdownContent`—`react-markdown`+`remark-gfm`, HTML을 직접 주입하지 않아 별도 sanitizer 없이 안전), `GET /questions/{id}/related`, `features/answer`로 `GET /questions/{id}/answers`(조회 전용 — 채택 배지·`isStale` 배지, Accept/작성은 Phase 2). Vote/Watch UI는 만들지 않음(Phase 3)
- [x] F1.5 Question Revision History + Diff — `/questions/{id}/versions`: `GET .../versions` 목록 + From/To 버전 선택 UI로 `GET .../versions/{version}/diff?from=` 비교, `DiffView`가 ADDED/REMOVED/EQUAL 라인을 색상으로 표시. 리비전이 1개뿐이면 비교 UI 대신 안내 문구
- [x] F1.6 Tag Directory + Detail — `/tags`(`GET /tags?q=` 검색), `/tags/{name}`(전용 조회 API가 없어 `GET /search?q={name}` 결과를 `tags` 배열 exact match로 클라이언트 필터링하는 근사치, [ADR-0021](docs/architecture/decisions/0021-tag-detail-via-search-approximation.md)). Follow 토글은 현재 사용자의 팔로우 여부를 알려주는 API가 없어 Phase 3(커뮤니티 액션)로 미룸

## Frontend Phase 2 — 쓰기 경험

Ask/Answer/Accept 3개 흐름을 완성해 "질문→답변→채택"이라는 Q&A 핵심 루프를 프론트엔드에서 처음부터 끝까지 수행할 수 있게 한다.

- [x] F2.1 MarkdownEditor + Textarea — `shared/ui/Textarea.tsx`, `shared/ui/MarkdownEditor.tsx`(Write/Preview 탭, `MarkdownContent` 재사용)를 구현해 Ask/Answer Composer가 공유
- [x] F2.2 Ask — `/ask`: React Hook Form + Zod로 제목(필수, 300자)/본문(필수)/environment·logs(선택) 입력, `features/question/ui/TagInput`으로 태그(클라이언트에서 최대 5개). 제목 입력에 400ms debounce를 걸어 `GET /search`로 유사 질문을 보여주고(작성 중인 제목이라 `related`가 아니라 `search` 재사용), `POST /questions`로 생성 후 상세로 이동. 중복 제출 방지, localStorage 기반 draft 자동 저장/복원(초안은 성공 제출 시 삭제). **버그 발견 및 수정**: `TagInput`이 쉼표를 keydown 이벤트로만 감지해, 붙여넣기나 자동화 도구처럼 값이 한 번에 채워지는 입력에서는 태그가 전혀 분리되지 않는 것을 브라우저 검증 중 발견 — `onChange`에서도 쉼표를 분리하도록 수정
- [x] F2.3 Answer Composer — Question Detail 하단에 답변 작성 영역 추가: `POST /questions/{id}/answers`, 성공 시 답변 목록 invalidate. 답변 정렬은 Best(수락 우선)/Newest/Oldest만 제공(Score는 투표 기능이 없어 제외, [ADR-0020](docs/architecture/decisions/0020-frontend-scoped-to-backend-support.md)). 답변 수정 이력 UI는 만들지 않음(Answer에 리비전 자체가 없음)
- [x] F2.4 Accept — 질문 작성자에게만 답변 카드에 Accept 버튼 노출(`question.authorId === me.id`), `POST /answers/{id}/accept` 성공 시 답변 목록 + 질문(상태 RESOLVED 반영) invalidate. 브라우저로 Ask→Answer→Accept 전체 루프(질문 생성→답변 작성→채택→질문 상태 Solved 전환)를 실제 서버로 검증 완료

## Frontend Phase 3 — 커뮤니티 액션

design.md에는 없는(백엔드 전용) 기능들을 이번에 화면으로 처음 노출한다 — QPR Review/Cluster/Outdated는 design.md 원본 설계에 없으므로 UI는 [domain-model.md](docs/architecture/domain-model.md)/[api-design.md](docs/architecture/api-design.md)를 근거로 새로 설계한다.

- [x] F3.1 Watch — Question Detail 헤더에 `features/watch`의 WatchButton 추가. "지금 이 질문을 Watch 중인지"를 알려주는 전용 API가 없어 `GET /me/watches` 전체 목록에서 `questionId` 포함 여부로 초기 상태를 판단한다(Tag Follow와 달리 Watch는 이 목록 조회가 가능해 Phase 1의 Tag Detail Follow 보류와는 다르게 지금 구현 가능, [ADR-0021](docs/architecture/decisions/0021-tag-detail-via-search-approximation.md) 참고). `POST/DELETE /questions/{id}/watch`. 브라우저로 로그인 사용자가 Watch/Unwatch 토글 시 상태가 실제로 반영되는 것을 확인
- [x] F3.2 평판 프로필 — `/users/{id}`: `entities/user`로 `GET /users/{id}/profile`(작성 질문/답변/팔로우 태그) + `GET /users/{id}/reputation`(점수와 4개 원시 카운트) 조합. Phase 0의 AppHeader가 이미 걸어둔 `/users/{id}` 링크(빈 라우트)를 채운다. `AnswerCard`에 `questionHref`를 선택적으로 추가해 프로필 목록에서 "질문 보기" 링크를 붙인다. 태그별 세분화 Expertise·Badge는 백엔드에 없어 만들지 않음([design.md](docs/frontend/design.md#16-사용자-프로필)). 브라우저로 실제 점수(질문 3·답변 2·채택 2·Super Answer 1 → 47점)가 [ADR-0018](docs/architecture/decisions/0018-simple-reputation-score-only.md) 산식과 정확히 일치하는 것을 확인
- [x] F3.3 QPR Review UI — `features/review`로 Question Detail에 정보 요청 패널 추가: `GET /questions/{id}/review-requests` 목록(message/상태/요청자/요청 시점 버전), 작성자가 아닌 로그인 사용자만 `POST .../review-requests`로 새 요청 생성(작성자 본인 시도는 403 `SelfReviewRequestException` — 버튼 자체를 숨김), 질문 작성자만 리비전 이후 `POST .../review-requests/{id}/re-request`로 재요청(리비전 전이면 409, 이미 ADDRESSED면 409 — 조건 만족 시에만 버튼 노출). 브라우저로 정보 요청 생성(RESOLVED 질문에서는 409 에러 메시지 노출 확인)→작성자가 리비전→재요청 처리(OPEN→ADDRESSED, Question.status는 불변)까지 전체 흐름 검증
- [x] F3.4 Cluster/Super Answer 표시 — `features/cluster`로 Question Detail에 클러스터 패널 추가: `GET /questions/{id}/cluster`(404면 클러스터 없음, `useCluster`가 `null`로 취급해 섹션 자체를 숨김)로 같은 문제로 묶인 질문 목록과 Super Answer 표시. 질문 ID를 직접 입력해 `POST /questions/{id}/cluster`로 다른 질문과 묶기(질문 검색 기반 picker는 이번 범위 밖 — 백엔드 API 자체가 ID 입력만 받음). 채택된 답변에 한해 `POST /clusters/{id}/super-answer`로 Super Answer 지정 가능. 브라우저로 두 질문을 클러스터로 묶고 채택된 답변을 Super Answer로 지정해 양쪽 질문 페이지에 모두 반영되는 것을 확인
- [x] F3.5 Outdated 표시 — Question Detail에 `OutdatedAction`("Outdated로 표시" 액션, 사유 입력 필수, 권한 제한 없음 — 작성자 본인도 가능) 추가. `POST /questions/{id}/outdated`, 성공 시 질문 상태가 OUTDATED로 바뀌는 것을 기존 StatusBadge가 그대로 표시(Phase 0에서 이미 매핑됨). 브라우저로 표시 후 액션이 사라지는 것까지 확인

**검증 중 발견한 도구 이슈(코드 버그 아님)**: 브라우저 자동화 중 tab의 실제 viewport가 0x0으로 굳어 스크린샷·스크롤·접근성 트리 조회가 모두 실패하는 현상을 겪었다 — `resize_window`(preset: desktop)로 뷰포트 에뮬레이션을 초기화해 해결. 이 세션의 브라우저 검증 도구 자체의 문제이지 프론트엔드 코드 문제는 아니다.

## Frontend Phase 4 — Discovery

원본 로드맵의 "관련 질문/태그 탐색/Quno Flow/고급 Dashboard"는 이미 Frontend Phase 1에서 구현했다(Question Detail·Ask의 Related Questions, `/tags`+`/tags/{name}` Tag Directory/Detail, Home의 Quno Flow 활동 스트림과 헤드라인/오늘 해결/재활성화 등 고급 Dashboard 필드) — 다시 만들지 않는다. 이번 Phase의 실질 범위는 **고급 검색 필터**와 **검색 자동완성** 두 가지뿐이다.

- [x] F4.1 검색 필터 — `features/search/ui/SearchFilters`로 `/questions`에 Status/Tags 필터 칩 추가(클라이언트 사이드로 `GET /search` 결과를 좁힘 — Tags는 AND 결합), 선택 상태를 `?q=&tags=&status=` URL query에 반영. Answered/Date/Score 필터와 Score/Newest 정렬은 응답에 해당 데이터가 없어 만들지 않음([ADR-0022](docs/architecture/decisions/0022-search-filters-client-side-tag-and-status-only.md)). 브라우저로 Status+Tags를 함께 적용해 16→4→3건으로 좁혀지는 것과 URL 반영을 확인
- [x] F4.2 검색 자동완성 — `widgets/app-header/SearchBox` + `features/search/hooks/useAutocomplete`로 AppHeader 검색창에 입력 중 드롭다운으로 질문 제목(`GET /search`)과 태그(`GET /tags?q=`) 후보를 보여준다(design.md #10 "검색어 자동완성"). 2자 미만 입력 시 비활성화. 브라우저로 드롭다운 렌더링과 태그/질문 항목 클릭 시 이동까지 확인

## Frontend Phase 5 — Retention

알림 6종(`QUESTION_REVISION`/`NEW_ANSWER`/`ANSWER_ACCEPTED`/`REVIEW_REQUESTED`/`REVIEW_RE_REQUESTED`/`QUESTION_OUTDATED`)은 `payload`가 이벤트별로 다른 JSON 문자열이라 [domain-model.md](docs/architecture/domain-model.md)의 발행 지점을 근거로 프론트에서 직접 메시지를 조립한다(design.md에는 없는 화면).

- [x] F5.1 Notifications — `features/notification`으로 `/notifications` 구현: `GET /me/notifications` 목록(최신순 정렬은 프론트에서), `describe-notification.ts`가 6종 `payload`(각기 다른 JSON 필드)를 사람이 읽는 메시지로 조립, 읽지 않음은 행 전체 강조 대신 작은 dot으로만 표시(design.md #17), `POST /me/notifications/mark-read`로 전체 읽음 처리(백엔드가 개별 읽음 처리를 지원하지 않아 "Mark all as read" 하나만 제공). 답변 관련 알림은 `/questions/{id}#answer-{answerId}`로 이동(`AnswerCard`에 `id`+`scroll-mt-20` 추가). 동일 질문의 반복 이벤트 그룹핑은 이번 범위에서 하지 않음(design.md도 "할 수 있다" 수준의 선택 사항). 브라우저로 두 사용자 간 리비전/답변/채택/Outdated 4종 알림이 실제로 쌓이고, 읽지 않음 배지→앵커 이동→전체 읽음 처리까지 전체 흐름을 검증
- [x] F5.2 Watching 목록 — `features/watch/ui/WatchedQuestionList` + `/watching`: `GET /me/watches` 목록 전용 화면(요약 응답에 태그가 없어 `QuestionSummary` 기반 `QuestionCard`는 재사용하지 않고 전용 리스트 컴포넌트를 둠)
- [x] F5.3 Profile 확장 — `/users/{id}`가 본인 프로필일 때만(`me.id === userId`) "Watching" 섹션을 추가로 보여준다 — `GET /me/watches`는 URL의 `id`와 무관하게 항상 요청자 본인의 목록만 반환하므로 타인 프로필에는 표시할 수 없음(브라우저로 본인/타인 프로필 각각에서 섹션 노출 여부를 확인). AppHeader에 Watching/Notifications(안 읽음 개수 배지) 링크 추가
- [ ] Frontend Phase ? — 모더레이션/답변 리비전 ([ADR-0020](docs/architecture/decisions/0020-frontend-scoped-to-backend-support.md)에 따라 대응 백엔드 기능부터 먼저 설계해야 함, 번호 미정). Vote/Comment는 Phase 11~12로, Save/Follow User/Badge는 Phase 13~15로 백엔드 설계를 시작해 이 목록에서 분리함 — 완료된 프론트엔드 작업은 아래 Frontend Phase 6 참고, Save/Follow User/Badge 프론트엔드는 해당 백엔드 Phase 완료 후 착수

## Frontend Phase 6 — Vote/Comment UI

Phase 11(Vote)·Phase 12(Comment) 백엔드가 준비되어 Action Rail 투표 컨트롤·Answer 카드 Score·댓글 섹션을 프론트엔드에 붙인다(대응 백엔드가 이제 있으므로 [ADR-0020](docs/architecture/decisions/0020-frontend-scoped-to-backend-support.md)의 "후속으로 이연" 대상에서 Vote/Comment는 제외됨). Save(북마크)/Follow User/Badge/모더레이션/답변 리비전은 여전히 대응 백엔드가 없어 보류 상태 유지.

- [x] F6.1 Vote — `features/vote`(api/hooks/ui): `useMyVotes`(`GET /me/votes` 전체 조회 후 클라이언트에서 targetType+targetId로 매칭, `useMyWatches`와 동일 패턴), `useCastVote`/`useRetractVote`(같은 방향 재클릭 시 철회), `VoteControl`(▲ score ▼, 이미 투표한 방향 강조). 본인 질문/답변에는 백엔드가 `SelfVoteException`(403)을 던지므로 작성자 본인에게는 컨트롤 대신 점수만 표시. Question Detail 헤더 옆과 각 `AnswerCard`에 배치, 목록 카드(`QuestionCard`)에는 상호작용 없는 점수 텍스트만 노출(투표 UI를 목록에 넣는 것은 이번 범위 밖). 브라우저로 서로 다른 두 사용자 간 질문/답변 투표(본인 것은 숨김, 타인 것은 클릭 시 점수 반영)까지 확인
- [x] F6.2 Comment — `features/comment`(api/hooks/ui): `useComments`/`useCreateComment`/`useDeleteComment`, `CommentSection`(목록 + "Add a comment" 클릭 시 나타나는 입력창, 600자 제한, 삭제는 작성자 본인만). 마크다운 렌더링 없이 평문 표시(ADR-0024와 일치). Question Detail의 질문 본문 아래와 각 `AnswerCard` 하단에 배치. 브라우저로 댓글 작성→다른 사용자에게는 삭제 버튼 미노출→작성자 삭제 시 tombstone("삭제된 댓글입니다")까지 확인
- [x] F6.3 알림 연동 — `describe-notification.ts`에 `NEW_COMMENT` 메시지 매핑 추가(Phase 12.3에서 프론트 작업으로 남겨둔 항목), `NotificationType` 유니온에도 추가. 브라우저로 댓글 작성 시 알림 센터에 "새 댓글이 달렸습니다"가 뜨는 것까지 확인

## Phase 11 — Vote (mvp-scope.md 로드맵에 없던 새 범위)

design.md가 전제하는 Stack Overflow형 투표를 뒤늦게 추가한다. `Question`/`Answer` Aggregate는 건드리지 않고 `Watch`와 같은 독립 side-aggregate로 설계했다([ADR-0023](docs/architecture/decisions/0023-vote-as-side-aggregate-no-reputation-impact.md)) — 자기 투표 금지, 평판 점수·Dashboard 인기순위·검색 정렬에는 이번 범위에서 반영하지 않음.

- [x] 11.1 도메인 모델 — 새 `domain/vote` 패키지: `Vote`(voterId, targetType: `VoteTargetType`(QUESTION\|ANSWER), targetId, value: Int), `Vote.cast()`가 `+1`/`-1` 외 값을 `InvalidVoteValueException`(400)으로 거부. `VoteRepository` 포트(`findByVoterAndTarget`, `save`(upsert), `delete`, `sumScore(targetType, targetId): Long`, `findByVoter(voterId): List<Vote>`). PK는 `(voter_id, target_type, target_id)` 복합키로 결정(`WatchId`와 동일 패턴 — 계획 당시 "별도 id"로 적었던 것에서 변경, 유일성 제약을 PK 자체로 표현하는 편이 더 단순함). V8 마이그레이션 `votes` 테이블(+ `CHECK (value IN (-1, 1))` DB 레벨 방어까지 추가). **점수는 배치 집계(`sumScores`) 없이 항목별 `sumScore` 호출로 구현** — `QuestionSummaryHydrator`가 이미 태그도 N+1로 조회하는 기존 관례를 그대로 따름(계획 당시 배치 최적화를 언급했으나 실제 코드베이스 관례와 맞지 않아 단순화)
- [x] 11.2 API — `POST/DELETE /api/v1/questions/{id}/vote`, `POST/DELETE /api/v1/answers/{id}/vote`(body: `{value: 1|-1}`, `VoteController`, cast/retract 모두 `WatchController`처럼 204 No Content), `GET /api/v1/me/votes`(내 투표 전체 목록). 자기 자신의 질문/답변에 투표 시도하면 `SelfVoteException` 403(대상의 `authorId`를 조회해 비교)
- [x] 11.3 기존 응답에 점수 통합 — `QuestionSummaryHydrator`/`AnswerResultAssembler`에 `VoteRepository` 주입, `QuestionSearchResult`/`QuestionSearchResultResponse`/`AnswerResult`/`AnswerResponse`/`QuestionSummaryResult`/`QuestionResponse`에 `score: Long` 필드 추가. 이 두 컴포넌트가 검색·Dashboard·related·추천·Cluster 멤버·프로필 답변까지 전부 조립하는 유일한 지점이라 호출부 변경 없이 모든 응답에 점수가 붙음
- [x] 11.4 테스트 — 단위 테스트 12개(`CastVoteUseCaseTest`/`RetractVoteUseCaseTest`/`ListMyVotesUseCaseTest` — 생성/값 변경/철회/자기 투표 차단/value 검증/여러 투표자 합산)와 `VoteLifecycleE2ETest`(실제 HTTP+Postgres로 cast→score 반영→변경→철회→0 복귀, `GET /me/votes` 확인, 자기 투표 403, 잘못된 value 400). 마이그레이션 작성 중 `value SMALLINT`가 Kotlin `Int`(Hibernate 기본 `INTEGER` 매핑)와 스키마 검증에서 충돌하는 것을 실제 테스트로 발견해 `INTEGER`로 수정
- [x] 11.5 문서화 — `domain-model.md`에 Voting Bounded Context(Engagement와 분리한 별도 컨텍스트로 결정 — 구독/알림과 품질 신호는 성격이 달라서)와 ERD/삭제정책 반영, `api-design.md`에 "Vote (Phase 11)" 섹션 추가. 결정 자체는 이미 [ADR-0023](docs/architecture/decisions/0023-vote-as-side-aggregate-no-reputation-impact.md)로 기록됨

## Phase 12 — Comment (mvp-scope.md 로드맵에 없던 새 범위)

QPR `ReviewRequest`와 성격이 다른 범용 clarification 댓글을 추가한다. 스레드 없는 평면 목록, 수정 불가, soft-delete tombstone으로 범위를 좁혔다([ADR-0024](docs/architecture/decisions/0024-comment-flat-no-edit-tombstone-delete.md)).

- [x] 12.1 도메인 모델 — 새 `domain/comment` 패키지: `Comment`(targetType: `CommentTargetType`(QUESTION\|ANSWER), targetId, authorId, body — 최대 600자(`MAX_COMMENT_BODY_LENGTH`), **`isDeleted` 대신 `deletedAt: Instant?`로 구현** — Question/Answer/Tag가 이미 쓰는 soft-delete 컬럼 관례와 통일(계획 당시 표기에서 변경), `softDelete()`는 idempotent(이미 삭제됐으면 그대로 반환). `CommentRepository` 포트(`save`, `findById`, `listByTarget` — 어느 것도 `deletedAt` 필터를 걷지 않음, tombstone도 계속 조회돼야 하므로). `CommentNotFoundException`(404), `CommentAccessDeniedException`(403). V9 마이그레이션 `comments` 테이블
- [x] 12.2 API — `POST/GET /api/v1/questions/{id}/comments`, `POST/GET /api/v1/answers/{id}/comments`, `DELETE /api/v1/comments/{id}`(`CommentController`). `CommentResponse`는 삭제된 댓글의 `body`를 `null`로, `isDeleted`를 별도 필드로 반환(원문은 DB에는 남지만 어떤 응답에서도 다시 노출되지 않음) — [ADR-0024](docs/architecture/decisions/0024-comment-flat-no-edit-tombstone-delete.md) 4번. Question.status는 건드리지 않아 RESOLVED 질문에도 댓글 가능. 길이 제한은 Bean Validation(`@field:Size(max = MAX_COMMENT_BODY_LENGTH)`)으로 400 VALIDATION_ERROR
- [x] 12.3 알림 연동 — `OutboxEventTypes.NEW_COMMENT` 추가, `DispatchOutboxEventsUseCase`의 `when`에 분기 추가(질문 댓글: Ward 구독자+질문 작성자, 답변 댓글: Ward 구독자+질문 작성자+답변 작성자 — payload에 `questionAuthorId`/`answerAuthorId`를 함께 담아 둘 다 추출, 없는 필드는 자연히 스킵되는 기존 `extractLong` 동작 그대로 재사용, kdoc도 갱신). **프론트 알림 센터(F5.1)의 `describeNotification`에 `NEW_COMMENT` 메시지 매핑 추가는 별도 프론트엔드 작업으로 남겨둠**(이번 백엔드 Phase 범위 밖)
- [x] 12.4 테스트 — 단위 테스트 15개(`CreateCommentUseCaseTest`/`DeleteCommentUseCaseTest`/`ListCommentsUseCaseTest` — 작성/목록(오래된순)/삭제/idempotent 재삭제/작성자 아닌 삭제 403/빈 문자열·600자 초과 거부/대상 없음 404/outbox payload 검증)와 `CommentLifecycleE2ETest`(실제 HTTP+Postgres로 질문·답변 댓글 작성→알림 fan-out(양쪽 다 알림 확인)→삭제 후 tombstone→권한 없는 삭제 403→600자 초과 400). E2E 작성 중 `GET .../comments` 호출에 Authorization 헤더를 빠뜨려 401이 나는 것을 발견해 수정(Vote Phase에서와 같은 종류의 실수)
- [x] 12.5 문서화 — `domain-model.md`에 Discussion Bounded Context, Comment Aggregate, `NEW_COMMENT` Domain Event, ERD의 다형 연관, 테이블 삭제 정책 반영. `api-design.md`에 "Comment (Phase 12)" 섹션 추가. 이번 결정은 이미 [ADR-0024](docs/architecture/decisions/0024-comment-flat-no-edit-tombstone-delete.md)로 기록됨

## Phase 13 — Save (mvp-scope.md 로드맵에 없던 새 범위)

design.md #18이 Watch(구독)와 분리해 정의하는 개인 북마크 기능을 추가한다. `watches`와 데이터 모양은 같지만 별도 테이블·독립 side-aggregate로 분리했다([ADR-0025](docs/architecture/decisions/0025-save-as-separate-side-aggregate-from-watch.md)) — Watch/Save를 하나의 테이블에 합치지 않는다.

- [ ] 13.1 도메인 모델 — 새 `domain/save` 패키지: `SaveRepository` 포트(`save`/`unsave`(모두 idempotent)/`isSaved`/`findSavedQuestionIds(userId)` — `WatchRepository`와 동일한 시그니처 패턴, `findWatcherIds`에 대응하는 "누가 저장했는지" 조회는 두지 않음). `(user_id, question_id)` 복합 PK의 `saves` 테이블(V10 마이그레이션). 자기 자신의 질문 저장은 막지 않음(제약 없음)
- [ ] 13.2 API — `POST/DELETE /api/v1/questions/{id}/save`(204, `WatchController`와 동일 패턴), `GET /api/v1/me/saves`(`WatchedQuestionResponse`와 동일한 모양의 `SavedQuestionResponse` 목록)
- [ ] 13.3 테스트 — 단위 테스트(저장/철회/중복 저장 idempotent/목록 조회)와 실제 서버+curl 검증(저장→목록에 나타남→철회→목록에서 사라짐)
- [ ] 13.4 문서화 — `domain-model.md`에 Save Aggregate 반영(Bounded Context는 착수 시점에 Engagement 편입 여부 확정), `api-design.md`에 "Save (Phase 13)" 섹션 추가. 이번 결정은 이미 [ADR-0025](docs/architecture/decisions/0025-save-as-separate-side-aggregate-from-watch.md)로 기록됨

## Phase 14 — Follow User (mvp-scope.md 로드맵에 없던 새 범위)

design.md #18이 "후속 버전 기능"이라고 스스로 표시한 사용자 팔로우를 관계 기록·조회 범위로만 좁혀 추가한다. 팔로우한 사용자의 활동 피드·알림은 이번 범위에 포함하지 않는다([ADR-0026](docs/architecture/decisions/0026-follow-user-relationship-only-no-activity-feed.md)).

- [ ] 14.1 도메인 모델 — 새 `domain/follow` 패키지(또는 확정할 이름): `UserFollowRepository` 포트(`follow(followerId, followeeId)`/`unfollow`(idempotent)/`isFollowing`/`findFolloweeIds(followerId)` — `UserTagFollowRepository`와 동일한 관계 테이블 패턴). `SelfFollowException`(자기 자신 팔로우 시도, 403 — `SelfVoteException`/`SelfReviewRequestException`과 동일한 패턴). `(follower_id, followee_id)` 복합 PK의 `user_follows` 테이블(V11 마이그레이션)
- [ ] 14.2 API — `POST/DELETE /api/v1/users/{id}/follow`(204), `GET /api/v1/me/following`(내가 팔로우하는 사용자 목록 — id/nickname 정도만). 대상 사용자가 없으면 기존 `UserNotFoundException`(404) 재사용
- [ ] 14.3 테스트 — 단위 테스트(팔로우/언팔로우/자기 자신 팔로우 403/대상 없음 404/목록 조회)와 실제 서버+curl 검증
- [ ] 14.4 문서화 — `domain-model.md`에 UserFollow Aggregate 반영(Bounded Context는 착수 시점 확정), `api-design.md`에 "Follow User (Phase 14)" 섹션 추가. 팔로워/팔로잉 카운트 노출, 활동 피드, 알림 연동은 [ADR-0026](docs/architecture/decisions/0026-follow-user-relationship-only-no-activity-feed.md)에 따라 의도적으로 보류했음을 명시

## Phase 15 — Badge (mvp-scope.md 로드맵에 없던 새 범위)

design.md #19가 전제하는 배지 시스템을 Reputation(Phase 9, [ADR-0018](docs/architecture/decisions/0018-simple-reputation-score-only.md))과 같은 방식 — 영속화 없이 매 요청마다 계산하는 읽기 모델 — 로 추가한다. 획득 이벤트/알림/토스트는 이번 범위에서 만들지 않는다([ADR-0027](docs/architecture/decisions/0027-badge-as-computed-read-model-no-award-events.md)).

- [ ] 15.1 도메인 모델 — `domain/badge` 패키지: `BadgeType` enum(고정 6종 — `FIRST_QUESTION`/`FIRST_ANSWER`(Bronze), `PROBLEM_SOLVER`(채택 답변 5개 이상)/`WELL_RECEIVED`(받은 투표 점수 합 50점 이상, Silver), `TRUSTED_ANSWERER`(채택 답변 20개 이상)/`SUPER_ANSWER`(Super Answer 지정 1회 이상, Gold), `BadgeTier` enum(BRONZE/SILVER/GOLD). `BadgeRepository` 포트는 기존 `ReputationRepository.compute(userId)`(질문/답변/채택/Super Answer 수)에 없는 값 하나만 추가로 계산 — 사용자가 작성한 질문+답변에 대한 투표 점수 합(`votes`를 `questions`/`answers`의 `author_id`와 조인하는 새 네이티브 쿼리)
- [ ] 15.2 API — `GET /api/v1/users/{id}/badges` — 위 집계치를 `BadgeType`별 조건과 비교해 현재 획득 조건을 만족하는 배지 목록만 반환(`{type, tier}`만 포함, 이름/설명 등 표시 문구는 `NotificationType`과 같은 원칙으로 프론트가 가짐). 대상 사용자가 없으면 `UserNotFoundException`(404)
- [ ] 15.3 테스트 — 단위 테스트(6종 배지 각각의 경계값 — 조건 미달/충족, 여러 배지 동시 충족, 활동 없는 사용자는 빈 목록)와 실제 서버+curl 검증(질문/답변/채택/투표를 실제로 쌓아 배지가 나타나는지)
- [ ] 15.4 문서화 — `domain-model.md`에 Badge를 Reputation Bounded Context에 반영(Reputation과 같은 "활동 기반 신뢰 신호" 성격이라 새 컨텍스트 없이 편입), `api-design.md`에 "Badge (Phase 15)" 섹션 추가. 획득 알림/영속화를 만들지 않은 이유는 이미 [ADR-0027](docs/architecture/decisions/0027-badge-as-computed-read-model-no-award-events.md)로 기록됨

## Phase 16+ — 이후 로드맵 (착수 시점에 각 Phase 세부 계획을 이 문서에 다시 전개한다)

[mvp-scope.md](docs/product/mvp-scope.md#로드맵-phase) 로드맵과 대응한다(괄호 안이 mvp-scope.md 자체 번호). 아래는 순서 참고용이며, MVP 검증 결과에 따라 우선순위가 바뀔 수 있다.

- [ ] Phase ? — 질문 네트워크 잔여: Merge, Fork, 지식 그래프 시각화 (mvp-scope.md 로드맵 Phase 3, 나머지) — Cluster/Super Answer 사용 패턴을 관찰한 뒤 착수 시점과 번호를 정한다
- [ ] Phase ? — 기술 버전 영향 감지 실제 자동화 (mvp-scope.md 로드맵 Phase 4, 나머지) — 외부 기술 버전 릴리스 데이터 소스 연동이 실제로 필요해지고 그 소스를 정할 수 있을 때 착수
- [ ] Phase ? — 신뢰 네트워크 잔여: Organization, Direct Ask (mvp-scope.md 로드맵 Phase 5, 나머지) — 조직 인증 방식, Direct Ask의 결제 처리 범위 등 핵심 설계가 아직 없어 착수 시점에 다시 설계한다
- [ ] Phase ? — 실시간 질문방(Live Chat) (mvp-scope.md 로드맵 Phase 6, 나머지) — WebSocket 기반 실시간 연결/현재 접속자 추적/메시지 영속화 인프라를 실제로 투자할 시점에 설계한다
- [ ] Phase ? — 검색 Score 정렬/Dashboard 인기순위·평판 점수에 Vote 반영 — Phase 11에서 의도적으로 보류([ADR-0023](docs/architecture/decisions/0023-vote-as-side-aggregate-no-reputation-impact.md) 5~7번). Vote 실사용 패턴을 관찰한 뒤 가중치를 다시 설계한다
- [ ] Phase ? — Comment 대댓글/`@mention`/수정 이력 — Phase 12에서 의도적으로 보류([ADR-0024](docs/architecture/decisions/0024-comment-flat-no-edit-tombstone-delete.md) 2~3, 5번). 실사용 후 수요가 확인되면 착수

## 진행 방식

- 각 체크박스 항목(또는 자연스러운 하위 묶음)을 하나의 작업 단위로 보고 완료 시 즉시 커밋한다.
- 새로운 결정(스택 변경, 인증 방식 확정 등)은 관련 `docs/architecture/*.md`에 반영한 뒤 진행한다.
- 문서와 실제 코드가 어긋나면 코드를 진실로 보고 문서를 갱신한다.
