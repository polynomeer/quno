# Quno API 설계 (MVP)

> 도메인 모델은 [domain-model.md](domain-model.md), 시스템 아키텍처는 [system-architecture.md](system-architecture.md) 참고.

## 주요 REST API

| Method | Endpoint | 설명 |
|---|---|---|
| POST | `/api/v1/auth/signup` | 회원가입 |
| POST | `/api/v1/auth/login` | 로그인, Access/Refresh Token 발급 |
| POST | `/api/v1/auth/refresh` | Refresh Token으로 Access/Refresh Token 재발급 |
| GET | `/api/v1/me` | 내 기본 프로필 조회 (이메일 포함, 비공개) |
| GET | `/api/v1/users/{id}/profile` | 공개 프로필 — 작성 질문/답변, 팔로우 태그 (이메일 미포함) |
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
| GET | `/api/v1/metrics` | MVP 성공 지표 스냅샷 (내부/운영용) |
| POST | `/api/v1/questions/{id}/review-requests` | QPR 정보 요청(Review) 생성 — 질문을 NEEDS_INFO로 전환 |
| GET | `/api/v1/questions/{id}/review-requests` | 질문에 걸린 정보 요청 전체 목록 (상태 포함) |
| POST | `/api/v1/questions/{id}/review-requests/{reviewRequestId}/re-request` | 정보 요청 재요청 — 해당 요청을 ADDRESSED로 전환 (작성자만) |

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

### Redis 캐시 (Phase 3.4)

`popularQuestions`/`trendingTags` 두 섹션만 캐시한다 — 이 둘은 **모든 사용자에게 동일한** 결과이자 비용이 큰 native 집계 쿼리이기 때문이다. `wardUpdates`/`followingTagsFeed`는 사용자별로 다르고 최신성이 정확도에 직결돼(내 알림이 오래된 값으로 보이면 버그처럼 느껴짐) 캐시하지 않는다.

- 방식: cache-aside. `DashboardRepositoryAdapter`가 `StringRedisTemplate`으로 키를 먼저 조회하고, miss 시 native 쿼리를 실행한 뒤 결과를 Jackson으로 직렬화해 저장한다.
- 키: `dashboard:popular-questions:{limit}`, `dashboard:trending-tags:{limit}`
- TTL: 60초 — "트렌드"류 데이터는 약간의 지연이 자연스러우므로 능동적 무효화(질문/답변/와드 생성 시 캐시 삭제)는 두지 않고 TTL 만료로만 갱신한다. 더 강한 신선도가 필요해지면 이 부분을 재검토한다.
- 값 직렬화는 `readValue(String, Class)`/`writeValueAsString`만 사용한다 (Jackson 3의 `TypeReference`/`JavaType` 제네릭 API는 배열 타입(`Array<T>::class.java`)으로 우회해 불필요한 복잡도를 피했다).

## 사용자 프로필 라이트 (Phase 3.3)

`GET /users/{id}/profile`은 작성 질문 전체(`QuestionSummaryHydrator`로 조립), 작성 답변 전체, 팔로우 태그 목록을 반환한다. `GET /me`와 달리 이메일을 포함하지 않는 공개용 응답이다.

- 다른 조회 API와 동일하게 현재는 인증을 요구한다 — 비로그인 공개 열람 여부는 [PLAN.md](../../PLAN.md) Phase 2.2에서 미룬 "질문 조회 공개 여부" 결정과 함께 Search/Discovery 설계 시 재검토한다.
- 질문/답변 목록은 개수 제한 없이 전체를 반환한다 — 사용자당 활동량이 많아지면 커서 페이지네이션이 필요해질 수 있다(제한 없음은 MVP 단순화).

**알려진 단순화**: MVP는 조회수(view count)를 추적하지 않는다. "오늘의 인기 질문"은 문서에서 이상적으로 언급한 view/watch/answer/freshness 조합 대신 현재 확보된 신호(Watch 수, Answer 수)만으로 근사한다. 실제 조회 추적이 추가되면 이 랭킹 쿼리에 반영한다.

## 지표 계측 (Phase 4.1)

`GET /metrics`는 [mvp-scope.md](../product/mvp-scope.md#성공-지표)의 성공 지표 중 **백엔드 데이터만으로 계산 가능한 것들**을 하나의 native SQL로 집계해 반환한다. 응답은 원시 카운트와 0.0~1.0 사이의 비율(rate)을 함께 담는다.

| 필드 | 정의 | 대응 지표 |
|---|---|---|
| `revisionRate` | `question_versions.version_number >= 2`인 질문 비율 | Question Revision Rate |
| `answerRate` / `acceptRate` | 답변이 달린/채택된 질문 비율 | Answer Rate / Accept Rate |
| `wardCoverageRate` | 최소 1명 이상이 watch한 질문 비율 | Ward Adoption의 질문 단위 근사치 |
| `livingQuestionRate` | 최근 7일 내 `outbox_events`(리비전/답변/채택) 또는 `watches` 생성 이벤트가 있었던 질문 비율 | North Star 후보 "주간 활성 Living Questions" |

**의도적으로 제외한 지표**: Related Question CTR, Tag Feed CTR, D1/D7 Retention은 클라이언트 이벤트 트래킹이 필요한데 이 세션까지 프론트엔드가 없어 계측 지점이 없다 — 프론트엔드 도입 시 재검토한다.

- `MetricsSnapshot`(`domain/metrics`)은 도메인 불변조건이 없는 순수 조회 모델이라 application/interfaces 계층에서 별도 DTO로 복제하지 않고 그대로 재사용한다(다른 기능들과 다른 의도적 예외).
- `MetricsLoggingScheduler`(`infrastructure/observability`)가 30분 주기로 같은 스냅샷을 INFO 레벨로 로깅한다 — 별도 메트릭 백엔드 없이도 로그 기반 관측 도구로 추적 가능하게 하기 위함.
- 다른 조회 API와 동일하게 인증을 요구한다.

## 답변–질문버전 연결 (Phase 5.1)

`AnswerResponse`(답변 작성/목록/공개 프로필 응답 공통)에 `targetVersionNumber`와 `isStale` 필드를 추가했다 — [vision.md](../product/vision.md)가 지적한 "답변이 어느 시점의 질문을 대상으로 했는지 불명확" 문제에 대한 최소 대응이다([ADR-0012](decisions/0012-qpr-multi-reviewer-thread-model.md) 관련, PLAN.md 5.1).

- `targetVersionNumber`: 답변 작성 시점의 질문 최신 버전 번호를 자동 기록한다. 버전을 사용자가 직접 고르는 UI는 만들지 않았다 — 항상 "지금 최신 버전"을 대상으로 한다는 단순한 규칙이다.
- `isStale`: 질문이 그 이후 리비전되어(`question_versions`의 최신 버전 번호가 `targetVersionNumber`보다 커짐) 이 답변이 더 이상 최신 질문 내용을 반영하지 못할 수 있음을 나타낸다. 저장하지 않고 조회 시점에 계산한다.
- `application/common/AnswerResultAssembler`가 이 계산을 전담한다 — `WriteAnswerUseCase`/`ListAnswersUseCase`/`GetUserProfileUseCase` 세 곳에서 공유한다(`QuestionSummaryHydrator`와 같은 이유로 3중복 시점에 추출).

## QPR Review — 정보 요청과 재요청 (Phase 5.2~5.3)

[ADR-0012](decisions/0012-qpr-multi-reviewer-thread-model.md)에서 결정한 **다중 리뷰 요청 스레드 모델**의 첫 단계다. 여러 사람이 같은 질문에 각자 독립적으로 정보를 요청할 수 있다 — GitHub PR review와 동일하게, 하나의 질문에 열린(OPEN) 요청이 여러 개 동시에 존재할 수 있다.

- `POST /questions/{id}/review-requests`: 질문 작성자가 아닌 사용자가 `message`와 함께 정보 요청을 연다. 성공하면 `ReviewRequest`(status=OPEN, `questionVersionNumberAtRequest`=요청 시점의 질문 최신 버전 번호)를 생성하고, `Question.requestMoreInfo()`로 질문을 NEEDS_INFO로 전환한다(이미 NEEDS_INFO면 no-op, RESOLVED면 409 Conflict).
  - 작성자 본인이 요청하면 403(`SelfReviewRequestException`).
  - RESOLVED 질문에 요청하면 409(`QuestionAlreadyResolvedException`).
- `GET /questions/{id}/review-requests`: 해당 질문의 모든 요청(OPEN/ADDRESSED 무관)을 최신순으로 반환한다.
- `REVIEW_REQUESTED` outbox 이벤트를 기존 Watch/Notification fan-out 파이프라인에 태운다 — 수신자는 Ward 구독자 + 질문 작성자(요청자 본인 제외), `NEW_ANSWER`와 같은 규칙이다.
- 재요청(Re-request Review, Phase 5.3): `POST .../review-requests/{reviewRequestId}/re-request`는 질문 작성자만 호출할 수 있고(작성자 아니면 403), 요청 시점(`questionVersionNumberAtRequest`) 이후 실제 리비전이 있어야 허용된다(없으면 409 `QuestionNotRevisedSinceRequestException`). 이미 ADDRESSED인 요청을 다시 재요청하면 409다. 성공하면 그 `ReviewRequest`만 ADDRESSED로 바뀌고 `REVIEW_RE_REQUESTED` 이벤트가 원 요청자(+ Ward 구독자)에게 알림을 보낸다.
  - **중요**: 재요청은 **Question.status를 건드리지 않는다.** `Question.revise()`가 이미 리비전 시점에 무조건 NEEDS_INFO를 벗어나므로(열려있는 요청 개수와 무관), 재요청이 호출될 때는 항상 이미 UPDATED 상태다. `ReviewRequest.status`(OPEN/ADDRESSED)는 리뷰어별 독립 부기 정보이지 Question.status를 다시 게이팅하는 값이 아니다 — 구현 중 유닛 테스트로 이 모순을 발견하고 정리했다([ADR-0015](decisions/0015-review-request-status-independent-of-question-status.md)).

## 입력 검증 공통 원칙

- Markdown 본문은 렌더링 시 XSS Sanitization을 적용한다.
- 질문/답변 작성은 Redis 기반 레이트 리밋 적용을 검토한다 (스팸 방지).
- 첨부파일(MVP 이후)은 Object Storage에 저장하고 API/DB에는 metadata만 보관한다.
