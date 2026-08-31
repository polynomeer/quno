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
| POST | `/api/v1/questions/{id}/cluster` | 다른 질문과 "같은 문제"로 표시 — 클러스터 생성/합류 |
| GET | `/api/v1/questions/{id}/cluster` | 이 질문이 속한 클러스터 조회 (멤버 질문 + Super Answer) |
| GET | `/api/v1/clusters/{id}` | 클러스터 직접 조회 |
| POST | `/api/v1/clusters/{id}/super-answer` | 클러스터의 Super Answer 지정 (채택된 답변만 가능) |
| POST | `/api/v1/questions/{id}/outdated` | 질문을 OUTDATED로 표시 (사용자 명시적 판단, 권한 제한 없음) |
| GET | `/api/v1/qunobot/spikes?limit=` | 최근 질문량이 급증한 태그 목록 (자동 감지) |
| GET | `/api/v1/users/{id}/reputation` | 활동 기반 평판 점수 조회 |
| GET | `/api/v1/flow?limit=` | Quno Flow 활동 스트림 (섹션당 개수, 기본 5) |
| POST | `/api/v1/questions/{id}/vote` | 질문에 투표 (`value: 1\|-1`, upsert — 다시 호출하면 값이 바뀜) |
| DELETE | `/api/v1/questions/{id}/vote` | 질문 투표 철회 |
| POST | `/api/v1/answers/{id}/vote` | 답변에 투표 (`value: 1\|-1`, upsert) |
| DELETE | `/api/v1/answers/{id}/vote` | 답변 투표 철회 |
| GET | `/api/v1/me/votes` | 내 투표 전체 목록 |
| POST | `/api/v1/questions/{id}/comments` | 질문에 댓글 작성 |
| GET | `/api/v1/questions/{id}/comments` | 질문의 댓글 목록(삭제된 댓글도 tombstone으로 포함) |
| POST | `/api/v1/answers/{id}/comments` | 답변에 댓글 작성 |
| GET | `/api/v1/answers/{id}/comments` | 답변의 댓글 목록 |
| DELETE | `/api/v1/comments/{id}` | 댓글 삭제 (작성자 본인만, soft-delete) |

## 인증 (확정 — 2026-08-24)

**Spring Security + JWT (Access/Refresh Token 분리), Stateless 세션**을 사용한다. `system-architecture.md`의 React Web Client가 백엔드와 별도로 배포되는 구조이므로 서버 세션 공유보다 stateless 토큰 인증이 스케일링에 유리하다.

- Access Token은 짧은 만료 시간(예: 15~30분), Refresh Token은 별도 저장소/만료 정책으로 관리한다.
- 비밀번호는 BCrypt로 단방향 해시한다.
- 요청에서 `authorId`/`userId`를 클라이언트가 직접 지정하지 않는다. 인증 Principal(SecurityContext)에서 사용자 식별자를 얻는다.
- Role(`USER`\|`MODERATOR`, Phase 16)은 이 stateless 구조를 그대로 둔 채로 추가했다 — JWT에 role을 싣지 않고, 모더레이터 전용 use case가 매 호출마다 `UserRepository`로 최신 role을 조회한다("Moderation (Phase 16)" 섹션 참고, [ADR-0028](decisions/0028-moderation-mvp-report-dismiss-hide-only.md)).
- 기본 필터 체인(`SecurityConfig`)은 `/error`, `/actuator/health`, `/actuator/info`, `/api/v1/auth/**`만 공개하고 나머지는 인증을 요구한다. `JwtAuthenticationFilter`가 `Authorization: Bearer <token>`을 검증해 SecurityContext에 사용자 id를 principal로 설정한다 (Phase 2.1에서 구현 완료).
  - `/error`를 막아두면 컨트롤러에서 uncaught exception이 발생했을 때 Boot의 내부 forward가 이 필터 체인에서 다시 미인증 처리되어, 실제 원인(예: 500)이 아니라 **401로 위장**되어 클라이언트에 보인다 (Phase 2.6에서 `uq_tags_slug_active` 제약 위반이 401로 보이는 문제로 실제 발견함). 새 예외 타입을 추가할 때 GlobalExceptionHandler에 매핑을 빠뜨리면 이 문제가 재현되니 주의.
- Refresh Token은 서버 측 저장/revocation 목록 없이 서명·만료만 검증하는 순수 stateless 방식이다. 탈취 대응(조기 폐기 등)이 필요해지면 Redis 기반 revocation을 후속 단계에서 추가한다.

## 페이지네이션

목록형 API(`GET /api/v1/questions`, `/api/v1/search`, `/api/v1/me/notifications` 등)는 페이지 번호 기반보다 **cursor pagination**을 권장한다. 예: `created_at` 또는 `last_activity_at` + `id`를 커서로 사용하면 데이터가 계속 추가되는 상황에서도 중복/누락을 줄일 수 있다. 현재 `/search`, `/related`는 `limit`만 받는 단순 형태이며, cursor는 MVP 이후 트래픽이 실제로 커졌을 때 도입한다.

## 검색·관련 질문 구현 (Phase 2.9)

전용 검색엔진(OpenSearch/Elasticsearch, [system-architecture.md](system-architecture.md#확정-기술-스택) "이후 검토") 도입 전 단계로, PostgreSQL 네이티브 기능만으로 구현한다.

- `GET /search`: `to_tsvector('simple', title || body_markdown || logs) @@ plainto_tsquery('simple', q)`로 최신 버전의 제목/본문/에러로그를 전문검색하고, `tags.name ILIKE '%q%'`를 OR로 결합한다. 형태소 분석기는 `simple`(토큰화만, 어간 추출 없음) — 한국어 등 비영어 검색 품질이 필요해지면 `pg_bigm`/외부 검색엔진으로 교체한다. `sort=relevance|score` 파라미터 지원(Phase 20, [ADR-0032](decisions/0032-vote-score-search-sort-dashboard-reputation.md)) — `relevance`가 기본값이며 **실제로는 `ts_rank` 없이 `id DESC`(최신순 근사)다**, `score`는 질문이 받은 순 투표 점수 내림차순(동점이면 id 내림차순). 후보 질문 집합은 두 모드 동일, 정렬 기준만 다르다. 인식하지 못하는 값은 조용히 `relevance`로 처리한다.
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
| `popularQuestions` (Top 5) | `DashboardRepository.findPopularQuestionIds` — `watch_count*3 + answer_count*2 + vote_score*1` 내림차순(투표 항은 Phase 20, [ADR-0032](decisions/0032-vote-score-search-sort-dashboard-reputation.md) — 순 투표 점수, 음수 허용), 최신순 tiebreak |
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

## 질문 Cluster & Super Answer (Phase 6)

[ADR-0016](decisions/0016-manual-duplicate-marking-cluster.md)에서 결정한 대로, 자동 유사도 분석 없이 **사용자가 명시적으로 "같은 문제"라고 표시**해야만 Cluster가 만들어진다. 질문은 최대 하나의 클러스터에만 속한다(`questions.cluster_id`).

- `POST /questions/{id}/cluster` (body: `relatedQuestionId`): 두 질문을 같은 문제로 표시한다. 권한 제한은 없다(작성자가 아니어도, 심지어 두 질문의 작성자가 서로 달라도 누구나 표시 가능 — 커뮤니티 모더레이션 성격).
  - 자기 자신을 지정하면 400(`CannotClusterWithSelfException`).
  - 둘 다 클러스터가 없으면 새로 만들어 함께 합류시키고, 하나만 있으면 다른 쪽이 그 클러스터에 합류하고, 이미 같은 클러스터면 아무 것도 바뀌지 않는다(no-op, 200 응답은 동일).
  - **이미 서로 다른 클러스터에 속해 있으면 Phase 18부터 두 클러스터를 병합한다**(더 이상 409를 던지지 않음) — 자세한 내용은 아래 "Cluster Merge & Question Fork (Phase 18)" 참고.
  - 응답은 `{clusterId, memberQuestionIds, representativeAnswerId}` — 멤버는 id만 반환한다(요약 정보가 필요하면 아래 조회 API를 쓴다).
- `GET /questions/{id}/cluster`: 이 질문이 속한 클러스터를 조회한다. 클러스터가 없으면 404(`QuestionNotInAnyClusterException`).
- `GET /clusters/{id}`: 클러스터를 직접 조회한다(북마크된 클러스터 링크 등에서 사용). 없으면 404(`ClusterNotFoundException`).
- 두 조회 API 모두 응답은 `{clusterId, members: QuestionSearchResultResponse[], representativeAnswerId}` — 멤버 질문 요약은 `QuestionSummaryHydrator`를 재사용한다(Search/Related/Recommendation과 동일한 조립 로직).
- `POST /clusters/{id}/super-answer` (body: `answerId`): 클러스터의 "Super Answer"(vision.md)를 지정한다. 자동 선정 없이 사용자가 직접 고른다.
  - 그 답변이 클러스터 멤버 질문에 속하지 않으면 409(`AnswerNotInClusterException`).
  - 그 답변이 채택(accepted)되지 않았으면 409(`AnswerNotAcceptedException`) — Super Answer는 검증된 해결책이어야 한다는 취지.
  - 지정 권한 제한은 없다(현재 역할/평판 시스템이 없어 ReviewRequest와 동일하게 인증된 사용자 누구나 가능).
- Cluster/Super Answer 액션은 outbox 이벤트를 발행하지 않는다 — 호출자가 API 응답으로 결과를 바로 확인하므로 Ward 알림처럼 비동기 fan-out이 필요한 시나리오가 아니라고 판단했다.
- **범위 밖(Phase 6 시점)**: Merge(두 클러스터/질문 병합), Fork(질문 파생), 지식 그래프 시각화는 이 Phase에 포함하지 않았었다 — Phase 18에서 Merge와 Fork를 구현했다(아래 섹션 참고). 지식 그래프 시각화는 여전히 데이터 API까지만이다.

## Outdated 표시 & Spike Detection (Phase 8)

[ADR-0017](decisions/0017-manual-outdated-marking-and-spike-detection-scope.md)에서 결정한 대로, "기술 버전 변화로 인한 Outdated"의 진짜 자동 감지(외부 릴리스 피드 연동)는 범위 밖이다. 대신 Cluster/Review와 같은 **사용자 명시적 표시** 패턴을 재사용하고, 별도로 기존 데이터만으로 진짜 자동화가 가능한 Spike Detection을 추가했다.

- `POST /questions/{id}/outdated` (body: `reason`): 질문을 `OUTDATED` 상태로 전환한다. 권한 제한이 없다 — 작성자 본인을 포함해 누구나 표시할 수 있다(Cluster와 동일한 커뮤니티 판단 모델).
  - 이미 `OUTDATED`면 멱등하게 그대로 둔다(에러 아님).
  - `RESOLVED`를 포함해 어떤 상태에서도 표시 가능하다 — vision.md의 "RESOLVED → ... → OUTDATED → 새 Revision → REOPENED" 흐름 중 RESOLVED 이후 단계를 반영한다.
  - `QUESTION_OUTDATED` outbox 이벤트가 기존 fan-out에 실려 Ward 구독자 + 질문 작성자(액터 제외)에게 알림을 보낸다. `reason`은 알림 payload에 그대로 담긴다(JSON 이스케이프 처리).
  - **되살아나는 법은 새 상태를 만들지 않는다**: `Question.revise()`가 이미 RESOLVED가 아닌 모든 상태를 UPDATED로 전이시키므로, OUTDATED 질문을 리비전하면 자동으로 UPDATED가 된다 — vision.md가 말하는 별도의 `REOPENED` 상태는 도입하지 않았다.
- `GET /qunobot/spikes?limit=`: 최근 1일 질문 수가 자기 자신의 직전 14일 일평균 대비 급증한 태그를 `spikeRatio` 내림차순으로 반환한다. 노이즈 방지를 위해 최근 질문이 3건 미만인 태그는 제외한다(하드코딩된 임계값).
  - `TagSpike`(`domain/qunobot`)는 도메인 불변조건이 없는 순수 조회 모델이라 [ADR-0010](decisions/0010-metrics-read-model-skip-dto.md)과 동일하게 별도 DTO로 복제하지 않고 API 응답까지 그대로 재사용한다.
  - `SpikeDetectionRepositoryAdapter`가 [ADR-0009](decisions/0009-redis-cache-global-aggregates-only.md)와 동일한 cache-aside 패턴(모든 사용자에게 동일한 결과, TTL 60초)을 그대로 재사용한다 — 새 ADR 없이 기존 결정을 적용한 것.
  - 급증 자체가 원인을 설명하지 않는다 — "이 태그에 무슨 일이 있다"는 신호일 뿐이고, 실제 원인(기술 버전 변화 등) 분석은 사람이 한다.

## 전문가 평판 (Phase 9)

[ADR-0018](decisions/0018-simple-reputation-score-only.md)에서 결정한 대로, Organization·Direct Ask는 핵심 설계가 없어 미루고 활동 기반 평판 점수만 구현했다.

- `GET /users/{id}/reputation`: 질문 수·답변 수·채택된 답변 수·Super Answer 지정 횟수·**자신의 질문/답변이 받은 순 투표 점수**를 집계해 `score = questionCount*1 + answerCount*2 + acceptedAnswerCount*15 + superAnswerCount*10 + voteScoreReceived*1`로 계산한다(투표 항은 Phase 20, [ADR-0032](decisions/0032-vote-score-search-sort-dashboard-reputation.md) — Badge의 `BadgeRepository.sumVoteScoreReceived`를 재사용, 새 쿼리 없음). 존재하지 않는 사용자는 404.
- `UserReputation`(`domain/reputation`)은 도메인 불변조건이 없는 순수 조회 모델이라 [ADR-0010](decisions/0010-metrics-read-model-skip-dto.md)과 동일하게 별도 DTO로 복제하지 않고 API 응답까지 그대로 재사용한다.
- Metrics와 동일하게 native SQL 서브쿼리 4개를 한 번에 집계한다. Dashboard/Spike Detection과 달리 **캐시하지 않는다** — 결과가 사용자마다 다르고(모두에게 같은 결과가 아님) 개별 조회 비용이 이미 작아, 캐싱이 필요할 만큼 비싸지 않다고 판단했다.
- 채택 답변과 Super Answer 지정에 가중치를 크게 둬(각각 15점, 10점) 단순 활동량보다 "실제로 검증된 기여"를 더 반영한다 — 다만 동료 평가나 악용 방지 장치는 없는 순수 근사치다.
- **범위 밖**: Organization(조직 인증), Direct Ask(결제 포함)는 이번 Phase에 포함하지 않았다 — 핵심 설계가 문서에 없어 착수 시점에 다시 설계한다([PLAN.md](../../PLAN.md) Phase 11+).

## Quno Flow & 고급 Dashboard (Phase 10)

[ADR-0019](decisions/0019-quno-flow-and-dashboard-only-no-live-chat.md)에서 결정한 대로, [docs/archive/](../../archive/README.md) 원본 기획서(22~23장)를 참고해 기존 신호를 조합하는 두 기능만 구현했다. 실시간 질문방(Live Chat)은 WebSocket 인프라가 필요해 범위 밖이고, Instant Question은 기존 `POST /questions`로 이미 충족된다.

### 재활성화/Super Answer 갱신 신호 (Phase 10.1)

`domain/flow`의 `FlowRepository`가 새 이벤트 로그 없이 기존 타임스탬프만으로 두 신호를 도출한다 — Spike Detection이 새 인프라 없이 기존 타임스탬프로 급증을 도출한 것과 같은 접근이다.

- **재활성화(Reopened)**: 어떤 질문에 `QUESTION_OUTDATED` outbox 이벤트가 있고, 그 이후에 새 `question_versions`가 생겼으면 "재활성화됨"으로 본다.
- **최근 Super Answer 지정**: `question_clusters.updated_at`(Phase 10.1에서 추가한 컬럼 — `designateSuperAnswer()` 호출 시 갱신)이 최근인 클러스터를 찾는다.

### 고급 Dashboard (Phase 10.2)

기존 `GET /dashboard`(Phase 3.2) 응답에 필드만 추가했다(하위 호환, 기존 4개 섹션은 그대로 유지):

| 필드 | 내용 |
|---|---|
| `headline` | 가장 두드러진 신호 하나. 급증 태그가 있으면 그것을, 없으면 최고 인기 질문을 사용한다("무언가 심상치 않은 일"이 "늘 그렇듯 인기 있음"보다 헤드라인감이 크다는 판단). Quno Flow(10.3)의 카드 조립 로직을 그대로 재사용해 중복 구현하지 않는다 |
| `resolvedToday` | 오늘(자정 이후) RESOLVED로 전환된 질문 |
| `reopenedKnowledge` | 10.1의 재활성화 신호 재사용 |
| `trendingErrors` | 10.1이 아니라 기존 `SpikeDetectionRepository.findSpikingTags` 재사용. **알려진 단순화**: 실제 에러 텍스트 추출/분류 없이 태그 급증으로 근사한다 — Phase 3.3의 "인기 질문을 조회수 없이 Watch/Answer 수로 근사"한 것과 같은 종류의 단순화 |

`resolvedToday`는 단순 인덱스 조회라 Dashboard의 인기 질문/태그 트렌드와 달리 캐싱하지 않는다.

### Quno Flow (Phase 10.3)

`GET /flow?limit=`(섹션당 개수, 기본 5)가 4가지 카드를 **고정 섹션 순서**(인기 질문 → 태그 급증 → 재활성화 → Cluster Super Answer)로 이어붙여 반환한다. 하나의 타임라인으로 정렬하지 않는다 — 인기 질문/태그 급증은 "지금 이 순간의 상태" 스냅샷이라 자연스러운 발생 시각이 없고, 이걸 재활성화/Super Answer 같은 실제 이벤트와 억지로 한 타임라인에 섞으면 의미가 왜곡된다.

- `FlowCard`(`domain/flow`)는 `{type, headline, questionId?, clusterId?}` — 도메인 불변조건이 없는 순수 조회 모델이라 [ADR-0010](decisions/0010-metrics-read-model-skip-dto.md)과 동일하게 API 응답까지 그대로 재사용한다.
- `headline`은 사람이 읽는 완성된 문장이다(예: `"spring-boot 관련 질문이 평소보다 6.0배 늘었습니다"`) — 클라이언트가 별도로 조립하지 않는다.

## Vote (Phase 11)

[ADR-0023](decisions/0023-vote-as-side-aggregate-no-reputation-impact.md)에서 결정한 대로, Vote는 `Watch`와 같은 독립 side-aggregate다 — Question/Answer는 Vote의 존재를 모른다.

- `POST /questions/{id}/vote`, `POST /answers/{id}/vote`(body: `{"value": 1 | -1}`): 자기 자신의 질문/답변에 투표하면 403(`SelfVoteException`). `value`가 1/-1이 아니면 400(`InvalidVoteValueException`) — Bean Validation(`@Min`/`@Max`)으로는 "1 또는 -1"을 표현할 수 없어(0을 막지 못함) 도메인에서 직접 검증한다. 이미 투표한 상태에서 다시 호출하면 값을 덮어쓴다(upsert) — 별도의 "변경" API는 없다.
- `DELETE /questions/{id}/vote`, `DELETE /answers/{id}/vote`: 투표 철회. `Watch.unwatch`와 동일하게 멱등적이다(투표한 적이 없어도 204).
- `GET /me/votes`: 내가 투표한 전체 목록(`{targetType, targetId, value}[]`) — `GET /me/watches`와 같은 패턴으로, 프론트엔드가 "이 질문/답변에 내가 투표했는지"를 N+1 요청 없이 판단할 수 있게 한다.
- **점수는 저장하지 않고 항상 집계한다.** `votes` 테이블에 개별 투표만 두고, `score`는 `SUM(value)`를 그때그때 계산한다. `QuestionSummaryHydrator`/`AnswerResultAssembler`에 통합했기 때문에 `GET /questions/{id}`, `GET /questions/{id}/answers`, `GET /search`, `GET /dashboard`, `GET /questions/{id}/related`, 추천, Cluster 멤버 목록까지 응답에 `score` 필드가 함께 나온다.
- **투표는 알림을 발생시키지 않는다** — 매 투표마다 Notification이 쌓이면 스팸이 되므로 `outbox_events`에 새 이벤트 타입을 추가하지 않았다.
- 평판 점수·Dashboard 인기 질문 순위·`GET /search`의 Score 정렬 반영은 ADR-0023이 처음에 보류했지만, Phase 20([ADR-0032](decisions/0032-vote-score-search-sort-dashboard-reputation.md))에서 모두 가중치 1로 반영했다 — 각 절 참고.

## Comment (Phase 12)

[ADR-0024](decisions/0024-comment-flat-no-edit-tombstone-delete.md)에서 결정한 대로, Comment는 QPR `ReviewRequest`와 다른 별개 개념이다 — "정보 요청 → 리비전 → 재요청" 워크플로가 아니라 그냥 짧은 clarification이다.

- `POST /questions/{id}/comments`, `POST /answers/{id}/comments`(body: `{"body": "...", "parentCommentId"?: number}`, 최대 600자 — Stack Overflow와 동일한 제한): `parentCommentId`를 생략하면 평면 목록의 최상위 댓글, 지정하면 그 댓글에 대한 답글이 된다. **답글은 1단계까지만 허용** — 이미 답글인 댓글을 부모로 지정하면 `CommentReplyDepthExceededException`(400, Phase 19, [ADR-0031](decisions/0031-comment-thread-mention-edit-history.md)). 어떤 Question.status에서도(RESOLVED 포함) 작성 가능하고, 자기 자신의 질문/답변에도 댓글을 달 수 있다(Vote/QPR과 달리 권한 제한 없음).
- `GET /questions/{id}/comments`, `GET /answers/{id}/comments`: 대상의 댓글을 오래된 순으로 평면 목록으로 전부 반환한다(응답에 `parentCommentId`가 포함되므로 트리 조립은 클라이언트 책임). 삭제된 댓글도 목록에서 사라지지 않는다 — `isDeleted: true`와 함께 `body: null`로 나온다(tombstone).
- `PUT /comments/{id}`(body: `{"body": "..."}`, Phase 19): 작성자 본인만 가능(403). 이미 삭제된 댓글은 수정할 수 없다(`CommentAlreadyDeletedException`, 409). 응답에 갱신된 `body`와 증가한 `versionNumber`가 담긴다. **알림을 발생시키지 않는다** — 오탈자 수준의 변경으로 보고 Ward 구독자에게 재통보하지 않는다.
- `GET /comments/{id}/versions`(Phase 19): 수정 이전의 본문들을 `[{versionNumber, body, createdAt}]`로 오래된 순 반환한다. 한 번도 수정되지 않은 댓글은 빈 배열 — `diff` 파라미터는 없다(Answer/Question 리비전과 달리 diff 엔드포인트를 만들지 않기로 결정, [ADR-0031](decisions/0031-comment-thread-mention-edit-history.md)).
- `DELETE /comments/{id}`: 작성자 본인만 가능(`CommentAccessDeniedException`, 403). 이미 삭제된 댓글을 다시 삭제해도 idempotent하게 그대로 둔다(에러 아님). soft-delete는 `deleted_at`만 세우고 원문은 DB에 남지만, **API 응답에서는 어떤 경로로도 삭제된 댓글의 원문을 다시 노출하지 않는다.**
- **새 댓글은 알림을 발생시킨다** — `NEW_ANSWER`와 동일한 `DispatchOutboxEventsUseCase` fan-out을 재사용한다. 질문 댓글은 Ward 구독자 + 질문 작성자, 답변 댓글은 Ward 구독자 + 질문 작성자 + 그 답변의 작성자, 답글이면 그 부모 댓글의 작성자도 추가된다(모두 payload에 담아 기존 `extractLong` 추출 로직을 재사용 — 해당하지 않는 필드는 자연히 스킵된다).
- **`@mention` 알림**(Phase 19): 댓글 **생성 시점에만** 본문에서 `@([\w-]+)` 패턴을 파싱해 정확히 일치하는 닉네임의 사용자를 찾아 `MENTIONED_IN_COMMENT` 이벤트로 통보한다. `CONTENT_HIDDEN`과 같은 예외 부류로 Ward 구독자 기본 fan-out을 건너뛰고 멘션된 사용자에게만 간다. 수정 시에는 멘션을 재계산하지 않는다. 자동완성 UI는 없다(닉네임 검색 API 부재) — 프론트는 `@단어` 토큰을 스타일링만 하고 실제 사용자로 링크하지 않는다. 공백이 포함된 닉네임은 이 패턴으로 멘션할 수 없다는 알려진 한계가 있다([ADR-0031](decisions/0031-comment-thread-mention-edit-history.md)).
- **여전히 범위 밖**: 2단계 이상의 답글 depth, 멘션 자동완성. 실제 수요가 확인되면 각각 별도로 재설계한다.

## Save (Phase 13)

[ADR-0025](decisions/0025-save-as-separate-side-aggregate-from-watch.md)에서 결정한 대로, Save는 `Watch`와 데이터 모양은 같지만(별도 테이블) 별개의 side-aggregate다 — 구독(Watch)과 개인 보관(Save)은 서로 다른 개념이라는 design.md #18의 구분을 그대로 따른다.

- `POST /questions/{id}/save`, `DELETE /questions/{id}/save`: `WatchController`와 동일하게 둘 다 204, 둘 다 멱등적이다(이미 저장했거나 저장한 적이 없어도 에러 아님). 자기 자신의 질문도 제한 없이 저장할 수 있다(Vote와 달리 자기 대상 금지 규칙이 없음).
- `GET /me/saves`: 내가 저장한 질문 전체 목록(`{questionId, title, status}[]`, `GET /me/watches`와 완전히 같은 모양). "누가 이 질문을 저장했는지" 조회하는 API는 없다 — Watch의 `findWatcherIds`(알림 fan-out용)에 대응하는 쓰임이 Save에는 없기 때문이다.
- **Save는 알림을 발생시키지 않는다** — 순수 개인 보관이라 다른 사용자에게 알릴 이유가 없다.
- **범위 밖**: 저장한 질문 전용 정렬/폴더링, 저장 수 노출. 필요해지면 별도로 설계한다.

## Follow User (Phase 14)

[ADR-0026](decisions/0026-follow-user-relationship-only-no-activity-feed.md)에서 결정한 대로, 이번 범위는 팔로우 관계의 기록·조회까지만이다 — 팔로우한 사용자의 활동 피드나 알림은 만들지 않는다.

- `POST /users/{id}/follow`, `DELETE /users/{id}/follow`: 둘 다 204, 둘 다 멱등적이다(`WatchController`/`TagController.follow`와 동일한 패턴). 자기 자신을 팔로우하면 403(`SelfFollowException`). 대상 사용자가 없으면 404(`UserNotFoundException`, 기존 예외 재사용).
- `GET /me/following`: 내가 팔로우하는 사용자 목록(`{userId, nickname}[]`).
- **범위 밖**(모두 [ADR-0026](decisions/0026-follow-user-relationship-only-no-activity-feed.md)에서 의도적으로 보류): 팔로워/팔로잉 수를 프로필에 노출하는 것, 팔로우한 사용자의 활동을 모으는 피드(Quno Flow 재설계 필요), 팔로우 대상의 활동에 대한 알림. 실제 사용 패턴이 확인되면 각각 별도로 설계한다.

## Badge (Phase 15)

[ADR-0027](decisions/0027-badge-as-computed-read-model-no-award-events.md)에서 결정한 대로, Badge는 Reputation(Phase 9)과 동일하게 영속화 없는 계산형 읽기 모델이다 — "언제 처음 획득했는지"는 저장하지 않는다.

- `GET /users/{id}/badges`: 고정 6종 배지(`domain/badge/BadgeType`) 중 지금 시점의 활동 집계로 조건을 만족하는 것만 `{type, tier}[]`로 반환한다. 존재하지 않는 사용자는 404.
- 조건 판정에 쓰는 집계치 중 질문/답변/채택 답변/Super Answer 수는 기존 `ReputationRepository.compute(userId)`를 그대로 재사용하고, 사용자가 작성한 질문+답변에 대한 투표 점수 합만 새 쿼리(`BadgeRepository.sumVoteScoreReceived`)로 추가한다 — `votes`를 `target_type`별로 `questions`/`answers`의 `author_id`와 조인.
- 배지 카탈로그: `FIRST_QUESTION`/`FIRST_ANSWER`(Bronze, 질문·답변 1개 이상), `PROBLEM_SOLVER`(Silver, 채택 답변 5개 이상)/`WELL_RECEIVED`(Silver, 받은 투표 점수 합 50점 이상), `TRUSTED_ANSWERER`(Gold, 채택 답변 20개 이상)/`SUPER_ANSWER`(Gold, Super Answer 지정 1회 이상). 임계값은 Reputation 점수 공식과 같은 방식으로 하드코딩돼 있다.
- 응답은 `type`(enum 식별자)과 `tier`만 담는다 — 배지 이름/설명 같은 표시 문구는 `NotificationType`/`describeNotification`과 같은 원칙으로 프론트가 갖는다.
- **범위 밖**(모두 [ADR-0027](decisions/0027-badge-as-computed-read-model-no-award-events.md)에서 의도적으로 보류): 배지 획득 시점 영속화, 획득 시 토스트/알림, 카탈로그를 운영자가 직접 추가/조정하는 기능.

## Moderation (Phase 16)

[ADR-0028](decisions/0028-moderation-mvp-report-dismiss-hide-only.md)에서 결정한 대로, 이번 범위는 신고→모더레이터 검토 큐→Dismiss/Hide 두 액션까지다.

- `POST /questions/{id}/reports`, `POST /answers/{id}/reports`(body: `{"reason": "SPAM"|"DUPLICATE"|"LOW_QUALITY"|"OTHER", "message"?: string}`, 201 + 생성된 신고 반환): 로그인한 사용자라면 누구나, 자기 자신의 글도 신고 제한 없이 가능하다. 같은 대상에 대한 중복 신고는 병합되지 않고 각각 별개 행으로 쌓인다.
- `GET /moderation/reports?status=`(기본 `PENDING`), `POST /moderation/reports/{id}/dismiss`(204), `POST /moderation/reports/{id}/hide`(204) — 셋 다 모더레이터 전용. 모더레이터가 아니면 403(`ModeratorAccessDeniedException`). Role은 매 호출마다 `UserRepository`로 조회하고 JWT에는 담지 않는다.
- `dismiss`는 design.md의 `Keep`과 같은 취급이다 — 콘텐츠는 그대로 두고 신고만 `DISMISSED`로 닫는다. `hide`는 대상 Question/Answer에 실제 `softDelete()`를 호출하고(이번에 처음 구현 — 이전에는 `deleted_at` 컬럼만 있고 호출하는 코드가 전혀 없었다) 신고를 `ACTIONED`로 닫는다. 이미 처리된 신고를 다시 처리하려 하면 409(`ReportAlreadyResolvedException`) — 대상이 이미 지워져 404가 먼저 뜨지 않도록 신고 자신의 상태 전이를 콘텐츠 조회보다 먼저 검증한다.
- `Close as duplicate`는 새 상태를 만들지 않는다 — 이미 있는 Cluster 기능(Phase 6, "같은 문제로 표시")이 정확히 이 개념을 담당하므로 그걸 쓰라고 안내할 뿐 모더레이션 액션에 넣지 않았다. `Edit`(모더레이터가 남의 글을 직접 수정)도 이번 범위에 없다.
- **Hide는 콘텐츠 작성자에게만 알린다**(`CONTENT_HIDDEN`) — `DispatchOutboxEventsUseCase`가 Ward 구독자를 기본으로 깔지 않는 유일한 이벤트 타입이다. 계단식으로 전파하지도 않는다 — 질문을 Hide해도 그 질문의 답변들은 자동으로 숨겨지지 않는다.
- **범위 밖**(모두 [ADR-0028](decisions/0028-moderation-mvp-report-dismiss-hide-only.md)에서 의도적으로 보류): 역할 부여/회수 API, 사용자 정지, "사용자 노출 사유"와 "내부 운영 메모"의 분리, Hide의 계단식 전파. 실제 필요해지면 각각 별도로 설계한다.

## Answer Revision (Phase 17)

[ADR-0029](decisions/0029-answer-revision-mirrors-question-version-no-locking.md)에서 결정한 대로, Question/QuestionVersion 분리를 그대로 적용하되 Pessimistic Locking은 채택하지 않는다.

- `POST /answers/{id}/versions`(body: `{"body": "..."}`, 작성자 본인만 — 아니면 403 `AnswerAccessDeniedException`): 새 `AnswerVersion`을 append하고 `answers.body_markdown`(캐시)을 최신 내용으로 갱신한다. `GET /questions/{id}/answers` 등 기존 응답은 코드 변경 없이 항상 최신 본문을 보여준다.
- `GET /answers/{id}/versions`, `GET /answers/{id}/versions/{version}`, `GET /answers/{id}/versions/{version}/diff?from=` — Question의 동일 API(`GET /questions/{id}/versions...`)를 그대로 미러링한다. `diff`는 `from`을 생략하면 바로 이전 버전과 비교한다. 두 엔드포인트 모두 같은 `TextDiffer.diffLines` 유틸을 재사용한다.
- **동시성 잠금이 없다** — Question 리비전과 달리 `findByIdForUpdate` 류의 `SELECT ... FOR UPDATE`를 쓰지 않는다. 답변은 작성자 본인만 고칠 수 있어 QPR처럼 여러 참여자가 동시에 리비전을 만드는 경합이 없다고 판단했기 때문이다.
- **리비전은 알림을 발생시킨다**(`ANSWER_REVISION`) — `NEW_ANSWER`와 완전히 같은 수신자(Ward 구독자 + 질문 작성자)에게 통보한다. 질문 자체가 이미 사라진 경우(예: 모더레이션 Hide)에는 `questionAuthorId`가 payload에서 자연히 빠져 그 필드만 스킵된다 — 본인 답변을 고치는 것 자체는 막지 않는다.
- 기존 `targetVersionNumber`/`isStale`(Phase 5.1, "이 답변이 질문의 어떤 버전을 보고 작성됐는가")과 이번에 추가한 `latestVersionId`("이 답변 자체의 버전 이력")는 서로 다른 축이라 이번 변경으로 건드리지 않는다.

## Cluster Merge & Question Fork (Phase 18)

[ADR-0030](decisions/0030-cluster-merge-question-fork-graph-data-only.md)에서 결정한 대로, Merge는 클러스터 병합으로만 한정하고, Fork는 완전히 새로 설계했다. 지식 그래프는 데이터 API까지만 제공한다.

- **Merge**: 새 엔드포인트 없이 기존 `POST /questions/{id}/cluster`가 그대로 처리한다. 두 질문이 이미 서로 다른 클러스터에 속해 있으면(과거엔 409였던 경우), `questionId` 쪽 클러스터가 살아남고 `relatedQuestionId` 쪽 클러스터의 멤버 전원이 그리로 재배정된 뒤 흡수된 클러스터 행은 삭제된다. 흡수되는 쪽에 이미 지정된 Super Answer는 자동으로 이전되지 않는다(살아남은 쪽에 없었다면 병합 후에도 없는 채로 남는다) — 필요하면 `POST /clusters/{id}/super-answer`로 다시 지정한다. 응답 모양은 기존과 동일한 `ClusterResponse`.
- `POST /questions/{id}/fork`(201, 인증만 필요, 별도 body 없음): origin 질문의 **현재 최신 제목·본문·환경·로그·태그를 그대로 복사**해 실행자 명의의 새 질문(+Qv1)을 만든다. 응답은 `POST /questions`/`POST /questions/{id}/versions`와 같은 `QuestionMutationResponse`. 자기 자신의 질문을 포크하는 것도 허용한다. 포크된 질문은 origin과 **같은 Cluster에 자동으로 들어가지 않는다** — Cluster는 "같은 문제(같은 해결책 공유)", Fork는 "다른 조건의 변형(다른 해결책이 필요할 수 있음)"이라 성격이 다르다. 포크 직후 내용을 바꾸고 싶으면 이미 있는 `POST /questions/{id}/versions`(질문 리비전)를 그대로 쓴다 — Fork 자체에는 "내용을 바꿔 포크"하는 기능이 없다.
- `GET /questions/{id}/forks`: 이 질문에서 파생된 포크 목록. 응답은 `QuestionSearchResultResponse[]`(Related/Cluster 멤버와 동일한 요약 형태).
- `GET /questions/{id}/graph`: Cluster 멤버, Fork 계보(`forkedFrom`+`forks`), Related Questions을 한 응답으로 조합한 읽기 전용 뷰 — `{questionId, clusterMembers, forkedFrom, forks, relatedQuestions}`. 새 계산이나 저장 없이 기존 `GetClusterUseCase`/`QuestionSearchUseCase.related`/`GET .../forks`가 이미 하던 조회를 한 번에 묶은 것뿐이다. **"지식 그래프"라는 이름과 달리 실제 그래프 시각화(노드-엣지 다이어그램) UI는 아니다** — 그건 별도 프론트엔드 투자로 이번 범위 밖이다.
- Merge/Fork 모두 outbox 이벤트를 발행하지 않는다 — Cluster/Super Answer(Phase 6)와 같은 이유로, API 응답이 즉시 결과를 알려주는 동기 액션이라 비동기 알림이 필요 없다고 판단했다.
- Merge/Fork에 별도 권한 제한은 없다 — 기존 "같은 문제로 표시"가 누구나 가능한 것과 동일한 커뮤니티 모더레이션 성격을 유지한다.

## Vote 반영: 검색 Score 정렬, Dashboard 인기순위, 평판 점수 (Phase 20)

[ADR-0023](decisions/0023-vote-as-side-aggregate-no-reputation-impact.md) 5~7번이 보류했던 세 가지를 [ADR-0032](decisions/0032-vote-score-search-sort-dashboard-reputation.md)로 재검토했다. 실사용 데이터 없이 정한 판단이라 세 공식 모두 투표 가중치를 1로 시작한다.

- 검색 정렬(`GET /search?sort=`), Dashboard 인기순위, 평판 점수 각각의 구체적인 변경 내용은 위 해당 절(검색·관련 질문 구현, 고급 Dashboard, 전문가 평판) 참고.
- 셋 다 조회 시점 집계일 뿐 스키마 변경이나 새 이벤트가 없다 — `votes` 테이블은 Phase 11에서 이미 있었다.
- 어뷰징 방지(투표 속도 제한, 봇 탐지 등)는 이번 범위에 포함하지 않는다 — 자기 투표 금지(`SelfVoteException`)만 여전히 유일한 방어다.

## 입력 검증 공통 원칙

- Markdown 본문은 렌더링 시 XSS Sanitization을 적용한다.
- 질문/답변 작성은 Redis 기반 레이트 리밋 적용을 검토한다 (스팸 방지).
- 첨부파일(MVP 이후)은 Object Storage에 저장하고 API/DB에는 metadata만 보관한다.
