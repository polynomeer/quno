# Quno API 설계 (MVP)

> 도메인 모델은 [domain-model.md](domain-model.md), 시스템 아키텍처는 [system-architecture.md](system-architecture.md) 참고.

## 주요 REST API

| Method | Endpoint | 설명 |
|---|---|---|
| POST | `/api/v1/auth/signup` | 회원가입 |
| POST | `/api/v1/auth/login` | 로그인, Access/Refresh Token 발급 |
| POST | `/api/v1/auth/refresh` | Refresh Token으로 Access/Refresh Token 재발급 |
| GET | `/api/v1/me` | 내 기본 프로필 조회 |
| POST | `/api/v1/questions` | 질문과 Qv1 생성 (`tags: string[]` 선택 — find-or-create, slug 기준 중복 제거) |
| GET | `/api/v1/questions/{id}` | 질문 최신본/버전 요약 조회 |
| GET | `/api/v1/questions/{id}/versions` | 질문 버전 히스토리(요약) 목록 |
| GET | `/api/v1/questions/{id}/versions/{version}` | 특정 질문 버전 조회 |
| POST | `/api/v1/questions/{id}/versions` | 새 질문 리비전 생성 (작성자만) |
| GET | `/api/v1/questions/{id}/versions/{version}/diff?from={version}` | 두 버전의 본문 라인 diff (기본: 직전 버전과 비교) |
| GET | `/api/v1/questions/{id}/related?limit=` | 태그 중첩 기반 유사 질문 추천 (공유 태그 수 내림차순) |
| POST | `/api/v1/questions/{id}/answers` | 답변 등록 |
| GET | `/api/v1/questions/{id}/answers` | 답변 목록 |
| POST | `/api/v1/answers/{id}/accept` | 답변 채택 및 질문 RESOLVED 전환 |
| POST | `/api/v1/questions/{id}/watch` | 와드 등록 |
| DELETE | `/api/v1/questions/{id}/watch` | 와드 해제 |
| GET | `/api/v1/me/watches` | 내 와드 목록 |
| GET | `/api/v1/me/notifications` | 내 알림 목록 |
| POST | `/api/v1/me/notifications/mark-read` | 알림 일괄 읽음 |
| GET | `/api/v1/tags` | 태그 검색 |
| POST | `/api/v1/tags/{id}/follow` | 태그 팔로우 |
| DELETE | `/api/v1/tags/{id}/follow` | 태그 언팔로우 |
| GET | `/api/v1/search?q=...&limit=` | 질문/태그/에러 검색 (PostgreSQL 전문검색 + 태그 부분일치) |
| GET | `/api/v1/recommendations/questions?source=tags` | 태그 기반 추천 |
| GET | `/api/v1/dashboard` | 대시보드 집계 |

## 인증 (확정 — 2026-08-24)

**Spring Security + JWT (Access/Refresh Token 분리), Stateless 세션**을 사용한다. `system-architecture.md`의 React Web Client가 백엔드와 별도로 배포되는 구조이므로 서버 세션 공유보다 stateless 토큰 인증이 스케일링에 유리하다.

- Access Token은 짧은 만료 시간(예: 15~30분), Refresh Token은 별도 저장소/만료 정책으로 관리한다.
- 비밀번호는 BCrypt로 단방향 해시한다.
- 요청에서 `authorId`/`userId`를 클라이언트가 직접 지정하지 않는다. 인증 Principal(SecurityContext)에서 사용자 식별자를 얻는다.
- 관리자/모더레이터 API가 추가되면 Role과 세부 권한을 분리한다.
- 기본 필터 체인(`SecurityConfig`)은 `/error`, `/actuator/health`, `/actuator/info`, `/api/v1/auth/**`만 공개하고 나머지는 인증을 요구한다. `JwtAuthenticationFilter`가 `Authorization: Bearer <token>`을 검증해 SecurityContext에 사용자 id를 principal로 설정한다 (Phase 2.1에서 구현 완료).
  - `/error`를 막아두면 컨트롤러에서 uncaught exception이 발생했을 때 Boot의 내부 forward가 이 필터 체인에서 다시 미인증 처리되어, 실제 원인(예: 500)이 아니라 **401로 위장**되어 클라이언트에 보인다 (Phase 2.6에서 `uq_tags_slug_active` 제약 위반이 401로 보이는 문제로 실제 발견함). 새 예외 타입을 추가할 때 GlobalExceptionHandler에 매핑을 빠뜨리면 이 문제가 재현되니 주의.
- Refresh Token은 서버 측 저장/revocation 목록 없이 서명·만료만 검증하는 순수 stateless 방식이다. 탈취 대응(조기 폐기 등)이 필요해지면 Redis 기반 revocation을 후속 단계에서 추가한다.

## 페이지네이션

목록형 API(`GET /api/v1/questions`, `/api/v1/search`, `/api/v1/me/notifications` 등)는 페이지 번호 기반보다 **cursor pagination**을 권장한다. 예: `created_at` 또는 `last_activity_at` + `id`를 커서로 사용하면 데이터가 계속 추가되는 상황에서도 중복/누락을 줄일 수 있다. 현재 `/search`, `/related`는 `limit`만 받는 단순 형태이며, cursor는 MVP 이후 트래픽이 실제로 커졌을 때 도입한다.

## 검색·관련 질문 구현 (Phase 2.9)

전용 검색엔진(OpenSearch/Elasticsearch, [system-architecture.md](system-architecture.md#확정-기술-스택) "이후 검토") 도입 전 단계로, PostgreSQL 네이티브 기능만으로 구현한다.

- `GET /search`: `to_tsvector('simple', title || body_markdown || logs) @@ plainto_tsquery('simple', q)`로 최신 버전의 제목/본문/에러로그를 전문검색하고, `tags.name ILIKE '%q%'`를 OR로 결합한다. 형태소 분석기는 `simple`(토큰화만, 어간 추출 없음) — 한국어 등 비영어 검색 품질이 필요해지면 `pg_bigm`/외부 검색엔진으로 교체한다.
- `GET /questions/{id}/related`: `question_tags` 자기 조인으로 공유 태그 수를 계산해 내림차순 정렬한다 (mvp-scope.md "태그 매칭 우선"). 태그가 없는 질문은 관련 질문이 비어 있을 수 있다 — 본문 유사도 기반 추천은 MVP 이후 확장.
- 두 기능 모두 soft-delete된 질문/태그는 제외한다.

## 태그 팔로우 기반 추천 (Phase 3.1)

`GET /recommendations/questions?source=tags`는 [domain-model.md](domain-model.md#태그-팔로우-기반-추천-쿼리)의 점수식을 그대로 구현한다.

- 후보: 요청자가 팔로우한 태그가 달린 질문 중 **본인이 작성하지 않은** 질문(`author_id <> :userId`)
- 점수: `matched_tag_count * 3 + LEAST(answer_count, 5)`, 동점이면 최신순
- `source` 파라미터는 향후 다른 추천 전략을 위해 받아두지만 MVP는 태그 팔로우 전략 하나뿐이라 현재는 무시한다.
- 검색/관련 질문과 동일하게 candidate id를 랭킹한 뒤 `QuestionSummaryHydrator`(`application/common`)로 결과를 조립한다 — 세 기능이 이 조립 로직을 공유한다.

## 라이트 대시보드 (Phase 3.2)

`GET /dashboard`는 4개 섹션을 한 번에 조합한다. 대시보드 전용 비즈니스 로직은 두지 않고 기존 유스케이스/포트를 재사용한다.

| 섹션 | 소스 |
|---|---|
| `popularQuestions` (Top 5) | `DashboardRepository.findPopularQuestionIds` — `watch_count*3 + answer_count*2` 내림차순, 최신순 tiebreak |
| `wardUpdates` (최근 5건) | `ListMyNotificationsUseCase` 재사용 |
| `followingTagsFeed` (최대 10건) | `RecommendQuestionsUseCase` 재사용 (Phase 3.1과 동일 로직) |
| `trendingTags` (최대 10건) | `DashboardRepository.findTrendingTags` — 최근 7일 내 생성된 질문에 달린 태그를 질문 수 기준 집계 |

**알려진 단순화**: MVP는 조회수(view count)를 추적하지 않는다. "오늘의 인기 질문"은 문서에서 이상적으로 언급한 view/watch/answer/freshness 조합 대신 현재 확보된 신호(Watch 수, Answer 수)만으로 근사한다. 실제 조회 추적이 추가되면 이 랭킹 쿼리에 반영한다.

## 입력 검증 공통 원칙

- Markdown 본문은 렌더링 시 XSS Sanitization을 적용한다.
- 질문/답변 작성은 Redis 기반 레이트 리밋 적용을 검토한다 (스팸 방지).
- 첨부파일(MVP 이후)은 Object Storage에 저장하고 API/DB에는 metadata만 보관한다.
