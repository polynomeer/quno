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
- [ ] 2.8 **Notification**: Watch 기반 알림 fan-out(리비전/새 답변/채택), `GET /api/v1/me/notifications`, 읽음 처리
- [ ] 2.9 **Search/Related Questions**: 제목·본문·태그·에러 텍스트 lexical search, 기본 유사 질문 추천 (태그 매칭 우선)
- [ ] 2.10 도메인 단위 테스트 — 리비전 append-only, 채택 invariant, 와드 중복 방지, 태그 유일성 등 핵심 규칙 검증

## Phase 3 — MVP P1

- [ ] 3.1 태그 팔로우 기반 추천 점수식 구현 ([domain-model.md](docs/architecture/domain-model.md#태그-팔로우-기반-추천-쿼리) SQL 참고)
- [ ] 3.2 라이트 대시보드 API — 오늘의 인기 질문, 내 Ward 업데이트, 팔로우 태그 피드, 태그 트렌드
- [ ] 3.3 사용자 프로필 라이트 — 작성 질문/답변, 관심 태그 노출
- [ ] 3.4 Redis 캐시 적용 — 대시보드/인기 질문 조회 경로

## Phase 4 — 검증

- [ ] 4.1 [mvp-scope.md](docs/product/mvp-scope.md#성공-지표) 지표(Revision Rate, Ward Adoption 등) 계측 포인트 구현/로깅
- [ ] 4.2 E2E 시나리오 테스트: 질문 생성 → 리비전 → 답변 → 채택 → Ward 알림
- [ ] 4.3 MVP 핵심 가설 검증을 위한 최소 프론트엔드 또는 API 데모 플로우 확인

## Phase 5+ — MVP 이후 로드맵 (착수 시점에 세부 계획 별도 수립)

[mvp-scope.md](docs/product/mvp-scope.md#로드맵-phase) 로드맵과 1:1 대응한다. 아래는 순서 참고용이며, MVP 검증 결과에 따라 우선순위가 바뀔 수 있다.

- [ ] Phase 2(로드맵): QPR Review / Needs Info / Re-request, 답변-질문버전 연결 고도화
- [ ] Phase 3(로드맵): Question Cluster, Merge/Fork, Super Answer
- [ ] Phase 4(로드맵): QunoBot, 기술 버전 영향 감지, Outdated/Regression
- [ ] Phase 5(로드맵): Organization, 전문가 평판, Direct Ask
- [ ] Phase 6(로드맵): Quno Flow, Instant Question, 실시간 질문방, 고급 Daily Dashboard

## 진행 방식

- 각 체크박스 항목(또는 자연스러운 하위 묶음)을 하나의 작업 단위로 보고 완료 시 즉시 커밋한다.
- 새로운 결정(스택 변경, 인증 방식 확정 등)은 관련 `docs/architecture/*.md`에 반영한 뒤 진행한다.
- 문서와 실제 코드가 어긋나면 코드를 진실로 보고 문서를 갱신한다.
