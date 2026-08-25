# Quno 도메인 모델

> 시스템 아키텍처는 [system-architecture.md](system-architecture.md), API는 [api-design.md](api-design.md) 참고.

## Bounded Context

| Context | 역할 | 핵심 모델 |
|---|---|---|
| Identity | 사용자 식별과 프로필 | User |
| QnA (Core) | 질문의 생애주기와 답변 | Question, QuestionVersion, Answer, ReviewRequest |
| Tagging | 분류·관심 주제 관계 | Tag, UserTagFollow |
| Engagement | 질문 변화 구독과 알림 | Watch, Notification |
| Search/Discovery | 검색·관련 질문·추천 | Read model / index 중심 |

MVP 이후 확장 시 `Knowledge`(Cluster, SuperAnswer), `Direct Ask`, `Feed` 컨텍스트가 추가된다 ([../product/mvp-scope.md](../product/mvp-scope.md) 로드맵 참고).

## Aggregate

`Question`이 핵심 Aggregate Root다. 질문의 상태 전이, 최신 리비전 포인터, 채택 답변 같은 불변식을 관리한다. `Answer`는 독립 Aggregate다.

| 모델 | 주요 행위/규칙 |
|---|---|
| Question | create, revise, resolve/acceptAnswer, requestMoreInfo, softDelete. 삭제된 질문 수정 금지. 질문자만 채택 가능 |
| QuestionVersion | immutable revision. `version_number` 단조 증가. 과거 버전 보존(append-only) |
| Answer | create, edit, softDelete. accepted 상태 보유. `target_version_number`로 작성 시점 질문 버전을 명시(Phase 5.1) |
| ReviewRequest | request(open), addressed. 하나의 질문에 여러 리뷰어의 요청이 독립적으로 동시에 열릴 수 있음(Phase 5.2, [ADR-0012](decisions/0012-qpr-multi-reviewer-thread-model.md)) |
| Watch | watch/unwatch. user-question 중복 금지 |
| Notification | create, markRead |
| Tag | create/rename/softDelete. 활성 name/slug 유일성 |

## Domain Events

```text
QuestionCreated
QuestionRevised (QUESTION_REVISION)
AnswerCreated (NEW_ANSWER)
AnswerAccepted (ANSWER_ACCEPTED)
QuestionWatched
QuestionResolved
ReviewRequested (REVIEW_REQUESTED)
```

도메인 이벤트는 "DB 트랜잭션이 성공한 사실"을 외부 부수효과(Search indexing, Mongo timeline 반영, Ward 알림 fan-out)와 분리하는 경계다. Question 트랜잭션 안에서 직접 수행하지 않고 Outbox → Worker로 연결한다 ([system-architecture.md](system-architecture.md#비동기-이벤트-처리--transactional-outbox) 참고).

## ERD (PostgreSQL — 운영형)

```text
users
  ├──< questions ──< question_versions
  │       ├──< answers
  │       ├──< watches >── users
  │       ├──< question_tags >── tags
  │       └──< review_requests >── users
  ├──< answers
  ├──< user_tag_follows >── tags
  └──< notifications

questions.latest_version_id  ──> question_versions.id
questions.accepted_answer_id ──> answers.id
notifications.question_id / answer_id ──> 느슨한 참조 (선택적 FK)
```

### 테이블별 책임과 삭제 정책

| 테이블 | 핵심 컬럼 | 삭제 정책 |
|---|---|---|
| users | id, email, nickname, is_active | 비활성화 + 필요 시 익명화 (물리 삭제 지양) |
| questions | id, author_id, title(cache), status, latest_version_id, accepted_answer_id, deleted_at | soft delete, 핵심 FK 유지 |
| question_versions | id, question_id, version_number, title, body_markdown, environment, logs, created_by | append-only, 보존 우선(soft delete는 예외적) |
| answers | id, question_id, author_id, body_markdown, is_accepted, target_version_number, deleted_at | soft delete |
| tags | name, slug, deleted_at | soft delete + active partial unique index |
| question_tags | question_id, tag_id | 관계 데이터, hard delete 허용 |
| user_tag_follows | user_id, tag_id | 관계 데이터, hard delete 허용 |
| watches | user_id, question_id | 관계 데이터, hard delete 허용 |
| review_requests | id, question_id, requested_by, message, status, question_version_number_at_request, addressed_at | append형, hard delete 불필요(상태만 전이) |
| notifications | id, user_id, type, question_id?, answer_id?, payload, is_read | 대용량 주변 데이터, 느슨한 참조 + retention 정책 |

### 삭제/FK 운영 원칙

- Question/Answer/User 같은 코어 엔티티에 무분별한 `ON DELETE CASCADE`를 사용하지 않는다 (부모 한 건 삭제 시 대량 연쇄 삭제 위험 방지).
- 질문/답변은 soft delete를 기본으로 하여 복구·감사·통계·지식 그래프의 과거 관계를 보존한다.
- `question_tags`, `watches`, `user_tag_follows` 같은 순수 관계 데이터는 hard delete가 자연스럽다.
- Notification처럼 대량·약결합 데이터는 FK를 생략하거나 `SET NULL` 정책을 허용한다.
- 영구 삭제가 필요하면 즉시 cascade하지 않고 retention/배치 purge 정책으로 통제한다.

## MongoDB 문서 모델

코어 도메인의 Source of Truth는 PostgreSQL이며, MongoDB는 구조가 자주 바뀌는 문서에 제한적으로 사용한다.

### Question Timeline

```json
{
  "_id": "q_123",
  "questionId": 123,
  "events": [
    {"type": "CREATED", "by": 10, "at": "..."},
    {"type": "REVISION_ADDED", "version": 2, "by": 10, "at": "..."},
    {"type": "ANSWER_ADDED", "answerId": 777, "by": 30, "at": "..."}
  ]
}
```

운영 단계에서는 이벤트 배열이 무한히 커질 수 있으므로, 질문별 단일 거대 문서보다 **event-per-document 또는 bucket 전략**을 검토한다.

### Architecture Snapshot (MVP 이후)

```json
{
  "questionId": 123,
  "questionVersion": 2,
  "nodes": [
    {"id": "api", "type": "service", "label": "payment-api"},
    {"id": "db", "type": "db", "label": "postgres"}
  ],
  "edges": [{"from": "api", "to": "db", "label": "JPA"}],
  "meta": {"env": "prod", "region": "ap-northeast-2"}
}
```

### Personalization Profile

```json
{
  "userId": 10,
  "tagAffinity": {"kotlin": 0.92, "spring-boot": 0.88},
  "clusterAffinity": {"coroutine-cancel": 0.94}
}
```

사용자가 직접 팔로우한 태그처럼 원천 정합성이 필요한 데이터는 PostgreSQL을 source of truth로 유지한다.

## 핵심 SQL 트랜잭션 흐름

### 질문 생성

```sql
BEGIN;

INSERT INTO questions(author_id, title, status)
VALUES (:authorId, :title, 'OPEN')
RETURNING id;

INSERT INTO question_versions(
  question_id, version_number, title, body_markdown,
  environment, logs, created_by
)
VALUES (:questionId, 1, :title, :body, :environment, :logs, :authorId)
RETURNING id;

UPDATE questions
SET latest_version_id = :versionId, updated_at = NOW()
WHERE id = :questionId;

COMMIT;
```

### Revision 생성 (동시성 주의)

단순 `MAX(version_number)+1`은 동시 리비전 요청에서 충돌할 수 있다. Question row를 `SELECT ... FOR UPDATE`로 잠그거나, 낙관적 락(version column) + `(question_id, version_number)` unique constraint + 재시도 전략을 사용한다.

```sql
BEGIN;

SELECT id, latest_version_id
FROM questions
WHERE id = :questionId AND deleted_at IS NULL
FOR UPDATE;

SELECT version_number FROM question_versions WHERE id = :latestVersionId;

INSERT INTO question_versions(...)
VALUES (..., :currentVersion + 1, ...)
RETURNING id;

UPDATE questions
SET latest_version_id = :newVersionId, title = :newTitle, updated_at = NOW()
WHERE id = :questionId;

-- 같은 트랜잭션에서 Outbox event 기록 권장

COMMIT;
```

### Watch 등록 (idempotent)

```sql
INSERT INTO watches(user_id, question_id)
VALUES (:userId, :questionId)
ON CONFLICT (user_id, question_id) DO NOTHING;
```

### Watch 사용자 알림 fan-out

**구현 상태 (Phase 2.8)**: 원안의 단일 INSERT SELECT 대신, `outbox_events`를 폴링하는 `DispatchOutboxEventsUseCase`(2초 주기 스케줄러, [system-architecture.md](system-architecture.md#비동기-이벤트-처리--transactional-outbox))가 Watch 목록을 조회해 애플리케이션 코드에서 `notifications`에 건별 INSERT한다. `payload` 컬럼은 JSONB가 아니라 앱이 직접 쓰고 읽는 TEXT다(SQL JSON 연산자로 조회하지 않으므로).

이벤트 타입별로 수신자가 "질문 액터 제외 Watch 목록"만은 아니다:

- `QUESTION_REVISION`: Watch 목록 − 리비전 작성자(항상 질문 작성자와 동일)
- `NEW_ANSWER`: Watch 목록 ∪ **질문 작성자**(명시적으로 Watch하지 않았어도 항상 알림) − 답변 작성자
- `ANSWER_ACCEPTED`: Watch 목록 ∪ **채택된 답변의 작성자**(항상 알림) − 채택을 수행한 질문 작성자

개념을 보여주는 참고용 SQL(실제로는 애플리케이션 코드가 이 조합을 계산한다):

```sql
INSERT INTO notifications(user_id, type, question_id, payload)
SELECT w.user_id, 'QUESTION_REVISION', :questionId,
       jsonb_build_object('version_number', :versionNumber)::text
FROM watches w
WHERE w.question_id = :questionId
  AND w.user_id <> :actorId;
```

### 답변 채택

```sql
BEGIN;

UPDATE answers
SET is_accepted = FALSE, updated_at = NOW()
WHERE question_id = :questionId AND is_accepted = TRUE;

UPDATE answers
SET is_accepted = TRUE, updated_at = NOW()
WHERE id = :answerId AND question_id = :questionId AND deleted_at IS NULL;

UPDATE questions
SET accepted_answer_id = :answerId, status = 'RESOLVED', updated_at = NOW()
WHERE id = :questionId AND author_id = :actorId AND deleted_at IS NULL;

COMMIT;
```

운영 구현에서는 답변이 해당 질문 소속인지, 요청자가 질문 작성자인지를 애플리케이션 계층과 트랜잭션에서 함께 검증해야 한다.

## 태그 팔로우 기반 추천 쿼리

MVP 추천은 복잡한 ML보다 설명 가능한 후보 생성과 랭킹으로 시작한다.

```sql
WITH matched AS (
  SELECT q.id, q.title, q.created_at,
         COUNT(DISTINCT qt.tag_id) AS matched_tag_count
  FROM user_tag_follows utf
  JOIN question_tags qt ON qt.tag_id = utf.tag_id
  JOIN questions q ON q.id = qt.question_id
  WHERE utf.user_id = :userId
    AND q.deleted_at IS NULL
    AND q.author_id <> :userId
  GROUP BY q.id, q.title, q.created_at
),
answer_stats AS (
  SELECT question_id, COUNT(*) AS answer_count
  FROM answers WHERE deleted_at IS NULL
  GROUP BY question_id
)
SELECT m.*, COALESCE(a.answer_count, 0) AS answer_count,
       (m.matched_tag_count * 3 + LEAST(COALESCE(a.answer_count, 0), 5)) AS score
FROM matched m
LEFT JOIN answer_stats a ON a.question_id = m.id
ORDER BY score DESC, m.created_at DESC
LIMIT 20;
```

향후 명시적 태그 팔로우뿐 아니라 조회·Ward·답변·검색 행동을 암묵적 관심도 feature로 추가하고, MongoDB personalization profile과 vector similarity를 조합한 hybrid ranking으로 확장한다.

## Event Storming 요약 (MVP 이후 전체 그림)

> 원본 다이어그램: [../archive/quno-event-storming.png](../archive/quno-event-storming.png)

| 색상 역할 (Miro 컨벤션) | 예시 |
|---|---|
| Command (파랑) | CreateQuestion, UpdateQuestionVersion, RequestMoreInfo |
| Aggregate (노랑) | Question, Answer, Cluster, Watch |
| Domain Event (주황) | QuestionCreated, QuestionVersionCreated, AnswerAccepted |
| Policy (보라/분홍) | NotifyWatchers, AnalyzeSimilarity, MarkOutdated |
| Read Model (초록) | QuestionTimeline, RelatedQuestions, DashboardFeed |

### 핵심 이벤트 체인 (MVP 범위)

```text
CreateQuestion → QuestionCreated → QuestionVersionCreated
  → AnalyzeSimilarity → SimilarQuestionDetected
  → AnswerCreated → AnswerAccepted → QuestionResolved → NotifyWatchers
```

### QPR 이벤트 체인 (Phase 2)

```text
RequestMoreInfo → AdditionalInfoRequested → QuestionStatusChanged(NEEDS_INFO)
  → UpdateQuestionVersion → QuestionVersionCreated → QuestionReadyForReview
  → ReRequestReview → ReviewReRequested → AnswerCreated
```

**구현 상태 (PLAN.md Phase 5.2)**: `RequestMoreInfo → AdditionalInfoRequested(REVIEW_REQUESTED) → QuestionStatusChanged(NEEDS_INFO)`까지 구현됐다 — `ReviewRequest` Aggregate가 다중 리뷰어의 독립적인 요청을 스레드로 관리한다([ADR-0012](decisions/0012-qpr-multi-reviewer-thread-model.md)). `UpdateQuestionVersion`(기존 리비전 기능 재사용) 이후의 `QuestionReadyForReview → ReRequestReview → ReviewReRequested`는 Phase 5.3에서 추가한다.

### 지식 진화 체인 (Phase 3)

```text
QuestionCreated/VersionCreated → SimilarityAnalyzed → QuestionClustered
  → ClusterThresholdReached → SuperAnswerCandidateDetected
  → SuperAnswerCreated/Updated → RelatedQuestionsUpdated → WatchersNotified
```

### QunoBot 이벤트 체인 (Phase 4)

```text
TechnologyVersionReleased → ImpactScanRequested → AffectedKnowledgeDetected
  → QuestionOutdatedDetected / AnswerRegressionDetected
  → NotificationCreated → QuestionRevisionSuggested
```
