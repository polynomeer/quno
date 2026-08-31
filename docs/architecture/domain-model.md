# Quno 도메인 모델

> 시스템 아키텍처는 [system-architecture.md](system-architecture.md), API는 [api-design.md](api-design.md) 참고.

## Bounded Context

| Context | 역할 | 핵심 모델 |
|---|---|---|
| Identity | 사용자 식별·프로필과 사용자 간 관계 | User, UserFollow — 관계 기록·조회만, 활동 피드·알림은 후속 Phase로 이연(Phase 14, [ADR-0026](decisions/0026-follow-user-relationship-only-no-activity-feed.md)) |
| QnA (Core) | 질문의 생애주기와 답변 | Question, QuestionVersion, Answer, ReviewRequest |
| Tagging | 분류·관심 주제 관계 | Tag, UserTagFollow |
| Engagement | 질문에 대한 개인 관계(구독·보관)와 알림 | Watch, Save, Notification |
| Search/Discovery | 검색·관련 질문·추천 | Read model / index 중심 |
| Knowledge | 질문 간 연결과 대표 지식 | QuestionCluster(+Merge), Question의 Fork 계보(`originQuestionId`), 지식 그래프 데이터 조회(Phase 18, [ADR-0030](decisions/0030-cluster-merge-question-fork-graph-data-only.md)) |
| Maintenance | 오래된 지식 표시, 이상 신호 감지 | Read model 중심(TagSpike, VersionImpact), Question 상태(OUTDATED), TechnologyRelease(외부 릴리스 스냅샷, Phase 21 [ADR-0033](decisions/0033-technology-version-scan-detection-only-no-auto-outdated.md)) |
| Reputation | 활동 기반 신뢰 신호 | Read model 중심(UserReputation, Badge — Phase 15, [ADR-0027](decisions/0027-badge-as-computed-read-model-no-award-events.md)). `UserReputation.score`는 Phase 20([ADR-0032](decisions/0032-vote-score-search-sort-dashboard-reputation.md))부터 Voting 컨텍스트의 순 투표 점수(자신의 질문/답변이 받은 것)도 최저 가중치로 반영 |
| Flow | 기존 신호를 묶은 활동 스트림 | Read model 중심(FlowCard), 새 이벤트 없음 |
| Voting | 질문/답변에 대한 품질 신호(up/down) | Vote — Watch와 같은 독립 side-aggregate (Phase 11, [ADR-0023](decisions/0023-vote-as-side-aggregate-no-reputation-impact.md)). 검색 정렬·Dashboard 인기순위·평판 점수에 순 투표 점수를 반영하는 것은 Phase 20([ADR-0032](decisions/0032-vote-score-search-sort-dashboard-reputation.md)) |
| Discussion | 질문/답변에 대한 짧은 clarification | Comment — 1단계 대댓글, 수정 이력(diff 없음), 생성 시점 `@mention` 알림, soft-delete는 tombstone(Phase 12/19, [ADR-0024](decisions/0024-comment-flat-no-edit-tombstone-delete.md)/[ADR-0031](decisions/0031-comment-thread-mention-edit-history.md)). QPR `ReviewRequest`(QnA Core 컨텍스트)와는 성격이 다른 별개 개념 |
| Moderation | 신고와 그 처리 | Report — 신고→모더레이터 검토→Dismiss/Hide 두 액션까지만(Phase 16, [ADR-0028](decisions/0028-moderation-mvp-report-dismiss-hide-only.md)). `User.role`(USER\|MODERATOR)도 이 컨텍스트의 권한 판단에 쓰이지만 필드 자체는 Identity의 User가 들고 있음 |
| Trust Network | 자기 신고형 소속 그룹과 전문가 직접 요청 | Organization(+OrganizationMembership), DirectAskRequest — 둘 다 외부 인증·결제 없이 사용자 행동만으로 성립(Phase 22, [ADR-0034](decisions/0034-organization-virtual-only-direct-ask-no-payment.md)) |

`Feed` 컨텍스트는 아직 미착수다 ([../product/mvp-scope.md](../product/mvp-scope.md) 로드맵 참고). Trust Network의 Verified Organization(실제 회사·학교 인증)과 유료 Direct Ask(보상/결제)도 Phase 22에서 명시적으로 범위 밖에 뒀다([ADR-0034](decisions/0034-organization-virtual-only-direct-ask-no-payment.md)).

## Aggregate

`Question`이 핵심 Aggregate Root다. 질문의 상태 전이, 최신 리비전 포인터, 채택 답변 같은 불변식을 관리한다. `Answer`는 독립 Aggregate다.

| 모델 | 주요 행위/규칙 |
|---|---|
| Question | create, revise, resolve/acceptAnswer, requestMoreInfo, joinCluster, markOutdated, softDelete. 삭제된 질문 수정 금지. 질문자만 채택 가능. 최대 하나의 Cluster에만 속함(Phase 6.1). `revise()`가 이미 RESOLVED가 아니면 UPDATED로 전이하므로 OUTDATED도 리비전 한 번으로 자연히 벗어남(Phase 8.1). `originQuestionId`는 생성 시점에만 설정되는 Fork 계보 포인터(Phase 18) — `clusterId`와 독립적이라 포크된 질문이 원본과 같은 Cluster에 자동으로 들어가지 않음([ADR-0030](decisions/0030-cluster-merge-question-fork-graph-data-only.md)) |
| QuestionVersion | immutable revision. `version_number` 단조 증가. 과거 버전 보존(append-only) |
| Answer | create, accept/unaccept, softDelete, withLatestVersion(리비전, Phase 17). accepted 상태 보유. `target_version_number`로 작성 시점 질문 버전을 명시(Phase 5.1) — 이건 "이 답변이 질문의 어떤 버전을 보고 작성됐는가"이고 `latestVersionId`(AnswerVersion 포인터)는 "이 답변 자체의 버전 이력"이라 서로 다른 축임. `bodyMarkdown`은 최신 `AnswerVersion`의 캐시 값(`questions.title`과 같은 패턴, [ADR-0029](decisions/0029-answer-revision-mirrors-question-version-no-locking.md)) |
| AnswerVersion | immutable revision(Av1, Av2, ...). `version_number` 단조 증가. 과거 버전 보존(append-only). `QuestionVersion`과 동일한 구조를 그대로 적용했지만 Pessimistic Locking은 채택하지 않음 — 답변은 작성자 본인만 수정할 수 있어 Question만큼의 동시 편집 압력이 없다고 판단(Phase 17, [ADR-0029](decisions/0029-answer-revision-mirrors-question-version-no-locking.md)) |
| ReviewRequest | request(open), addressed. 하나의 질문에 여러 리뷰어의 요청이 독립적으로 동시에 열릴 수 있음(Phase 5.2, [ADR-0012](decisions/0012-qpr-multi-reviewer-thread-model.md)). status는 Question.status를 다시 게이팅하지 않는 독립 부기 정보(Phase 5.3, [ADR-0015](decisions/0015-review-request-status-independent-of-question-status.md)) |
| Watch | watch/unwatch. user-question 중복 금지 |
| Save | save/unsave. user-question 중복 금지. Watch와 데이터 모양은 같지만 별도 테이블·독립 side-aggregate로 분리 — 알림도, "누가 저장했는지" 조회도 없음(Phase 13, [ADR-0025](decisions/0025-save-as-separate-side-aggregate-from-watch.md)) |
| Notification | create, markRead |
| Tag | create/rename/softDelete. 활성 name/slug 유일성 |
| QuestionCluster | create, designateSuperAnswer. 자동 유사도 분석이 아니라 사용자의 명시적 "같은 문제" 표시로만 생성/합류됨(Phase 6.1, [ADR-0016](decisions/0016-manual-duplicate-marking-cluster.md)). **서로 다른 두 클러스터를 합치는 것(Merge)이 Phase 18에서 가능해졌다** — "같은 문제로 표시" 액션이 이미 서로 다른 클러스터에 속한 두 질문을 만나면 한쪽 클러스터가 다른 쪽에 흡수되고(멤버 재배정 후 흡수된 클러스터 행 삭제), 흡수되는 쪽의 Super Answer 지정은 이전되지 않는다([ADR-0030](decisions/0030-cluster-merge-question-fork-graph-data-only.md)). `updated_at`은 "최근에 Super Answer가 지정됐는지"를 도출하기 위한 용도로 Phase 10.1에서 추가됨 |
| Vote | cast(값 변경 포함)/retract. voter+targetType+targetId 유일(다른 값으로 다시 cast하면 upsert). 자기 자신의 질문/답변에는 투표 불가(`SelfVoteException`). `score`는 저장하지 않고 항상 `SUM(value)`로 집계 — Question/Answer는 Vote의 존재를 모름(Phase 11, [ADR-0023](decisions/0023-vote-as-side-aggregate-no-reputation-impact.md)) |
| Comment | write, softDelete, edit(작성자 본인만, 권한 검사는 use case에서 — `AcceptAnswerUseCase`와 같은 패턴). 대상(Question\|Answer)당 평면 목록에 `parentCommentId`로 1단계 답글만 허용(답글의 답글은 `CommentReplyDepthExceededException`). `edit()`는 `versionNumber`를 증가시키고, 직전 상태는 use case가 별도 `CommentVersion`에 append-only로 archive(diff 없음). soft-delete는 idempotent하며 행을 지우지 않고 `deleted_at`만 세운다 — 목록에는 계속 나타나지만 응답의 `body`는 null로 tombstone 처리, 삭제된 댓글은 수정 불가(`CommentAlreadyDeletedException`). 생성 시점에만 본문의 `@nickname`을 파싱해 알림(수정 시 재파싱 안 함, Phase 12/19, [ADR-0024](decisions/0024-comment-flat-no-edit-tombstone-delete.md)/[ADR-0031](decisions/0031-comment-thread-mention-edit-history.md)) |
| CommentVersion | append-only, `edit()` 직전의 (versionNumber, body) 스냅샷만 보관 — 한 번도 수정되지 않은 댓글은 이 테이블에 행이 없다(현재 `comments.body`/`version_number` 자체가 v1). `AnswerVersion`과 달리 diff 조회 없음(Phase 19, [ADR-0031](decisions/0031-comment-thread-mention-edit-history.md)) |
| Report | file, dismiss(모더레이터), action(모더레이터 — 실제 대상 soft-delete는 use case 책임, `Report`는 상태 전이만 담당). 이미 처리된 신고를 다시 처리하려 하면 `ReportAlreadyResolvedException`(409). 같은 대상에 대한 중복 신고는 병합하지 않음(Phase 16, [ADR-0028](decisions/0028-moderation-mvp-report-dismiss-hide-only.md)) |
| UserFollow | follow/unfollow. follower-followee 중복 금지. 자기 자신 팔로우 불가(`SelfFollowException`). Watch/Save와 같은 순수 관계 데이터지만 대상이 Question이 아니라 User라 Engagement가 아닌 Identity 컨텍스트에 둠. 활동 피드·알림은 만들지 않음(Phase 14, [ADR-0026](decisions/0026-follow-user-relationship-only-no-activity-feed.md)) |
| Organization | create. 이름은 `slugify`(대소문자 정규화만, 비-ASCII 보존) 기준 중복 금지 — 생성자는 자동으로 첫 멤버. Verified 인증은 없음(Phase 22, [ADR-0034](decisions/0034-organization-virtual-only-direct-ask-no-payment.md)) |
| OrganizationMembership | join/leave(idempotent). organization-user 중복 금지. Watch/Save와 같은 순수 관계 데이터 |
| DirectAskRequest | request(PENDING), accept, decline. 대상이 `User.acceptsDirectAsk=false`면 요청 자체가 거부됨(`DirectAskNotAcceptedException`). 자기 자신에게 요청 불가(`SelfDirectAskException`). 같은 (질문, 대상)에 열린 요청은 하나만(`DuplicateDirectAskException`). 어떤 `Answer`와도 직접 연결되지 않음 — 수락 후 답변은 기존 `POST /questions/{id}/answers`를 그대로 씀 |

## Domain Events

```text
QuestionCreated
QuestionRevised (QUESTION_REVISION)
AnswerCreated (NEW_ANSWER)
AnswerAccepted (ANSWER_ACCEPTED)
QuestionWatched
QuestionResolved
ReviewRequested (REVIEW_REQUESTED)
ReviewReRequested (REVIEW_RE_REQUESTED)
QuestionMarkedOutdated (QUESTION_OUTDATED)
CommentCreated (NEW_COMMENT)
ContentHidden (CONTENT_HIDDEN)
AnswerRevised (ANSWER_REVISION)
UserMentionedInComment (MENTIONED_IN_COMMENT)
TechnologyVersionImpactDetected (TECH_VERSION_IMPACT_DETECTED)
DirectAskRequested (DIRECT_ASK_REQUESTED)
DirectAskAccepted (DIRECT_ASK_ACCEPTED)
DirectAskDeclined (DIRECT_ASK_DECLINED)
```

Cluster/Super Answer(Phase 6.1~6.3)는 outbox 이벤트로 발행하지 않는다 — 사용자가 명시적으로 호출한 API 응답으로 즉시 결과를 확인할 수 있어, Ward 알림처럼 비동기 fan-out이 필요한 시나리오가 아니라고 판단했다. Quno Flow/고급 Dashboard(Phase 10)도 같은 이유로 새 이벤트를 만들지 않는다 — 조회 시점에 기존 `outbox_events`/`question_versions`/`question_clusters` 타임스탬프를 읽어 신호를 그때그때 도출한다. Vote(Phase 11)도 이벤트를 발행하지 않는다 — 투표 하나하나에는 알림 가치가 없고(매 투표마다 알림이 생기면 스팸이 된다), score는 조회 시점에 그냥 집계하면 되기 때문이다([ADR-0023](decisions/0023-vote-as-side-aggregate-no-reputation-impact.md)). 반대로 Comment(Phase 12)는 `NEW_COMMENT` 이벤트를 발행한다 — `NEW_ANSWER`와 동일하게 `DispatchOutboxEventsUseCase`의 기존 fan-out(Ward 구독자 + "항상 알림받는 당사자")에 그대로 얹었다. 답변에 달린 댓글은 질문 작성자와 답변 작성자 둘 다를 "항상 알림받는 당사자"로 payload에 담아(`questionAuthorId`/`answerAuthorId`) 둘 다 알림을 받는다(질문 댓글은 `answerAuthorId`가 없어 자연히 스킵된다). Moderation(Phase 16)의 `CONTENT_HIDDEN`은 다른 이벤트와 반대 방향으로 예외적이다 — `DispatchOutboxEventsUseCase`가 Ward 구독자를 기본 수신자로 깔지 **않는** 유일한 이벤트 타입이고, 오직 숨겨진 콘텐츠의 작성자에게만 통보한다("활동"이 아니라 그 사람에게 온 행정 조치 통보이기 때문, [ADR-0028](decisions/0028-moderation-mvp-report-dismiss-hide-only.md)). Answer Revision(Phase 17)의 `ANSWER_REVISION`은 `NEW_ANSWER`와 완전히 동일한 수신자 규칙(Ward 구독자 + 질문 작성자)을 쓴다 — 답변 본문이 바뀌는 것도 구독 이유가 되는 변화로 보기 때문([ADR-0029](decisions/0029-answer-revision-mirrors-question-version-no-locking.md)). Comment 확장(Phase 19)에서 `NEW_COMMENT`는 답글일 때 부모 댓글 작성자(`parentCommentAuthorId`)도 기본 fan-out에 추가로 얹는다. 반면 `MENTIONED_IN_COMMENT`는 `CONTENT_HIDDEN`과 같은 예외 부류다 — Ward 구독자 기본 fan-out을 건너뛰고 `mentionedUserIds`(JSON 배열, 전용 `extractLongList` 파서로 추출)에 담긴 사용자에게만 통보한다("당신이 언급됐다"는 개별 통지이지 활동 신호가 아니기 때문). 댓글 수정(edit) 자체는 어떤 이벤트도 발행하지 않는다 — 오탈자 교정 수준으로 보고 재통보하지 않기로 했다([ADR-0031](decisions/0031-comment-thread-mention-edit-history.md)). Phase 21의 `TECH_VERSION_IMPACT_DETECTED`는 `QUESTION_OUTDATED`와 동일한 수신자 규칙(Ward 구독자 + 질문 작성자)을 쓰지만, 이 프로젝트의 다른 모든 이벤트와 달리 발행자가 사람이 아니라 스케줄러다 — payload에 `actorId`가 아예 없어 아무도 제외되지 않는다([ADR-0033](decisions/0033-technology-version-scan-detection-only-no-auto-outdated.md)). Phase 22의 `DIRECT_ASK_REQUESTED`/`DIRECT_ASK_ACCEPTED`/`DIRECT_ASK_DECLINED`은 `CONTENT_HIDDEN`/`MENTIONED_IN_COMMENT`와 같은 "당사자 전용" 부류다 — 요청은 대상 사용자에게만, 수락/거절 결과는 원 요청자에게만 통보하고 Ward 구독자는 아예 관여하지 않는다(둘 다 한 사람이 다른 한 사람에게 직접 건 사적인 상호작용이지, 질문 전체에 대한 활동 신호가 아니기 때문, [ADR-0034](decisions/0034-organization-virtual-only-direct-ask-no-payment.md)). Organization의 생성/가입/탈퇴는 outbox 이벤트를 발행하지 않는다 — Cluster/Super Answer(Phase 6)와 같은 이유로, 조인/탈퇴는 API 응답이 즉시 결과를 보여주는 동기 액션이라 비동기 알림이 필요 없다고 판단했다.

도메인 이벤트는 "DB 트랜잭션이 성공한 사실"을 외부 부수효과(Search indexing, Mongo timeline 반영, Ward 알림 fan-out)와 분리하는 경계다. Question 트랜잭션 안에서 직접 수행하지 않고 Outbox → Worker로 연결한다 ([system-architecture.md](system-architecture.md#비동기-이벤트-처리--transactional-outbox) 참고).

## ERD (PostgreSQL — 운영형)

```text
users
  ├──< questions ──< question_versions
  │       ├──< answers ──< answer_versions
  │       ├──< watches >── users
  │       ├──< saves >── users
  │       ├──< question_tags >── tags
  │       └──< review_requests >── users
  ├──< answers
  ├──< user_tag_follows >── tags
  ├──< user_follows >── users (자기 참조, Phase 14)
  ├──< organizations ──< organization_memberships >── users (Phase 22)
  ├──< direct_ask_requests (requester_id) (Phase 22)
  ├──< direct_ask_requests (target_user_id) (Phase 22)
  └──< notifications

question_clusters ──< questions (questions.cluster_id, 질문 1개당 최대 1개 클러스터)

questions.latest_version_id       ──> question_versions.id
questions.accepted_answer_id      ──> answers.id
answers.latest_version_id         ──> answer_versions.id (Phase 17, questions.latest_version_id와 동일한 패턴)
questions.cluster_id              ──> question_clusters.id
questions.origin_question_id      ──> questions.id (자기 참조, Fork 계보, Phase 18)
question_clusters.representative_answer_id ──> answers.id (Super Answer)
notifications.question_id / answer_id ──> 느슨한 참조 (선택적 FK)
votes.target_id ──> questions.id 또는 answers.id (target_type으로 구분, 다형 연관이라 FK 제약 없음, Phase 11)
comments.target_id ──> questions.id 또는 answers.id (target_type으로 구분, 다형 연관이라 FK 제약 없음, Phase 12)
comments.parent_comment_id ──> comments.id (자기 참조, 1단계 답글만 허용, Phase 19)
comment_versions.comment_id ──> comments.id (Phase 19)
reports.target_id ──> questions.id 또는 answers.id (target_type으로 구분, 다형 연관이라 FK 제약 없음, Phase 16)
technology_releases.tag_slug ──> tags.slug (느슨한 참조, FK 없음, Phase 21)
direct_ask_requests.question_id ──> questions.id (Phase 22)
```

### 테이블별 책임과 삭제 정책

| 테이블 | 핵심 컬럼 | 삭제 정책 |
|---|---|---|
| users | id, email, nickname, is_active, role, accepts_direct_ask | 비활성화 + 필요 시 익명화 (물리 삭제 지양). `role`(USER\|MODERATOR)은 Phase 16에서 추가 — 부여/회수 API 없이 DB에서 직접 변경([ADR-0028](decisions/0028-moderation-mvp-report-dismiss-hide-only.md)). `accepts_direct_ask`는 Phase 22에서 추가 — role과 달리 `PUT /me/direct-ask-settings`로 self-service 변경 가능([ADR-0034](decisions/0034-organization-virtual-only-direct-ask-no-payment.md)) |
| questions | id, author_id, title(cache), status, latest_version_id, accepted_answer_id, cluster_id, origin_question_id, deleted_at | soft delete, 핵심 FK 유지. `origin_question_id`는 Phase 18에서 추가된 Fork 계보 포인터([ADR-0030](decisions/0030-cluster-merge-question-fork-graph-data-only.md)) |
| question_versions | id, question_id, version_number, title, body_markdown, environment, logs, created_by | append-only, 보존 우선(soft delete는 예외적) |
| answers | id, question_id, author_id, body_markdown(최신 버전 캐시), is_accepted, target_version_number, latest_version_id, deleted_at | soft delete. `latest_version_id`는 Phase 17에서 추가([ADR-0029](decisions/0029-answer-revision-mirrors-question-version-no-locking.md)) |
| answer_versions | id, answer_id, version_number, body_markdown, created_by | append-only, 보존 우선(soft delete는 예외적) — question_versions와 동일한 패턴(Phase 17, [ADR-0029](decisions/0029-answer-revision-mirrors-question-version-no-locking.md)) |
| tags | name, slug, deleted_at | soft delete + active partial unique index |
| question_tags | question_id, tag_id | 관계 데이터, hard delete 허용 |
| user_tag_follows | user_id, tag_id | 관계 데이터, hard delete 허용 |
| watches | user_id, question_id | 관계 데이터, hard delete 허용 |
| saves | user_id, question_id | 관계 데이터, hard delete 허용. PK가 (user_id, question_id) — watches와 같은 구조지만 별도 테이블(Phase 13, [ADR-0025](decisions/0025-save-as-separate-side-aggregate-from-watch.md)) |
| review_requests | id, question_id, requested_by, message, status, question_version_number_at_request, addressed_at | append형, hard delete 불필요(상태만 전이) |
| question_clusters | id, representative_answer_id, created_at, updated_at | hard delete 불필요(멤버가 다른 클러스터로 옮겨가는 경로가 없음) |
| notifications | id, user_id, type, question_id?, answer_id?, payload, is_read | 대용량 주변 데이터, 느슨한 참조 + retention 정책 |
| votes | voter_id, target_type, target_id, value | 관계 데이터, hard delete 허용(retract). PK가 (voter_id, target_type, target_id) — watches와 같은 구조 |
| comments | id, target_type, target_id, author_id, parent_comment_id, body, version_number, deleted_at | soft delete(tombstone) — questions/answers와 같은 정책. 행은 남기고 응답의 body만 null 처리. `parent_comment_id`(자기 참조, 1단계만)와 `version_number`는 Phase 19에서 추가([ADR-0031](decisions/0031-comment-thread-mention-edit-history.md)) |
| comment_versions | id, comment_id, version_number, body, created_at | append-only, 보존 우선. `edit()` 직전 상태만 archive — answer_versions와 달리 생성 시점 v1 backfill/insert가 없음(Phase 19, [ADR-0031](decisions/0031-comment-thread-mention-edit-history.md)) |
| user_follows | follower_id, followee_id | 관계 데이터, hard delete 허용. PK가 (follower_id, followee_id) — users에 대한 자기 참조(Phase 14, [ADR-0026](decisions/0026-follow-user-relationship-only-no-activity-feed.md)) |
| reports | id, reporter_id, target_type, target_id, reason, message, status, resolved_by, resolved_at | append형, hard delete 불필요(review_requests와 동일하게 상태만 전이). `resolved_by`/`resolved_at`/`status`가 곧 audit trail이라 별도 로그 테이블을 두지 않음(Phase 16, [ADR-0028](decisions/0028-moderation-mvp-report-dismiss-hide-only.md)) |
| technology_releases | id, tag_slug(unique), product_slug, latest_version, latest_release_date, checked_at, updated_at | 이력이 아니라 태그당 1행 스냅샷 — 매 스캔마다 덮어쓴다. `tags`/`questions`에 대한 FK가 없다(`tag_slug`로 느슨하게 조인) — 아직 태그가 실제로 생성되기 전에도 추적을 시작할 수 있어야 하기 때문(Phase 21, [ADR-0033](decisions/0033-technology-version-scan-detection-only-no-auto-outdated.md)) |
| organizations | id, name, slug(unique), description, created_by, created_at | hard delete 불필요(삭제/보관 플로우 미도입). `slug`는 `Organization.slugify`(대소문자 정규화만) 기준 — Tag의 ASCII 전용 slugify와 달리 한글 등 비-ASCII 이름을 보존(Phase 22, [ADR-0034](decisions/0034-organization-virtual-only-direct-ask-no-payment.md)) |
| organization_memberships | organization_id, user_id, joined_at | 관계 데이터, hard delete 허용. PK가 (organization_id, user_id) — watches와 같은 구조(Phase 22) |
| direct_ask_requests | id, question_id, requester_id, target_user_id, message, status, created_at, responded_at | append형, hard delete 불필요(상태만 전이 — review_requests와 동일). (question_id, target_user_id)에 status='PENDING' 부분 유니크 인덱스로 중복 요청 방지(Phase 22, [ADR-0034](decisions/0034-organization-virtual-only-direct-ask-no-payment.md)) |

### 삭제/FK 운영 원칙

- Question/Answer/User 같은 코어 엔티티에 무분별한 `ON DELETE CASCADE`를 사용하지 않는다 (부모 한 건 삭제 시 대량 연쇄 삭제 위험 방지).
- 질문/답변은 soft delete를 기본으로 하여 복구·감사·통계·지식 그래프의 과거 관계를 보존한다.
- `question_tags`, `watches`, `saves`, `user_tag_follows`, `user_follows` 같은 순수 관계 데이터는 hard delete가 자연스럽다.
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

**구현 상태 (PLAN.md Phase 5.2~5.3)**: 전체 체인이 구현됐다 — `ReviewRequest` Aggregate가 다중 리뷰어의 독립적인 요청을 스레드로 관리하고([ADR-0012](decisions/0012-qpr-multi-reviewer-thread-model.md)), `UpdateQuestionVersion`은 기존 리비전 기능(`Question.revise()`)을 그대로 재사용한다. 단, `QuestionReadyForReview`라는 별도 상태/이벤트는 두지 않았다 — `revise()`가 이미 무조건 NEEDS_INFO를 벗어나 UPDATED로 전이시키므로 별도 신호가 필요 없다. `ReRequestReview → ReviewReRequested(REVIEW_RE_REQUESTED)`는 해당 `ReviewRequest`만 ADDRESSED로 바꾸고 원 요청자에게 알릴 뿐, Question.status는 다시 건드리지 않는다([ADR-0015](decisions/0015-review-request-status-independent-of-question-status.md)).

### 지식 진화 체인 (Phase 3)

```text
QuestionCreated/VersionCreated → SimilarityAnalyzed → QuestionClustered
  → ClusterThresholdReached → SuperAnswerCandidateDetected
  → SuperAnswerCreated/Updated → RelatedQuestionsUpdated → WatchersNotified
```

**구현 상태 (PLAN.md Phase 6.1~6.3)**: `SimilarityAnalyzed → QuestionClustered`와 `ClusterThresholdReached → SuperAnswerCandidateDetected`는 자동화하지 않았다 — 임베딩/벡터 유사도 인프라 없이, 사용자가 `POST /questions/{id}/cluster`로 명시적으로 "같은 문제"를 표시하면 `QuestionClustered`에 해당하는 결과(클러스터 생성/합류)가 즉시 일어난다([ADR-0016](decisions/0016-manual-duplicate-marking-cluster.md)). `SuperAnswerCreated`도 마찬가지로 자동 후보 탐지 없이 `POST /clusters/{id}/super-answer`로 사용자가 직접 지정한다. `RelatedQuestionsUpdated → WatchersNotified`는 만들지 않았다 — Cluster/Super Answer 액션은 outbox 이벤트로 발행하지 않고 API 응답으로 결과를 즉시 반환한다(비동기 알림이 필요한 시나리오가 아니라고 판단). Merge(클러스터 병합)와 Fork는 Phase 18([ADR-0030](decisions/0030-cluster-merge-question-fork-graph-data-only.md))에서 구현됐다 — 둘 다 새 outbox 이벤트를 만들지 않고 API 응답으로 즉시 결과를 반환하는 같은 원칙을 따른다. "지식 그래프"는 `GET /questions/{id}/graph`가 기존 조각들을 조합해 반환하는 데이터 API까지만이고, 실제 시각화 UI는 별도 프론트엔드 투자로 여전히 범위 밖이다.

### QunoBot 이벤트 체인 (Phase 4)

```text
TechnologyVersionReleased → ImpactScanRequested → AffectedKnowledgeDetected
  → QuestionOutdatedDetected / AnswerRegressionDetected
  → NotificationCreated → QuestionRevisionSuggested
```

**구현 상태 (PLAN.md Phase 8)**: `QuestionOutdatedDetected`에 해당하는 결과(`QUESTION_OUTDATED`)는 사용자가 `POST /questions/{id}/outdated`로 직접 표시하면 즉시 발생하고 `NotificationCreated`(기존 Watch fan-out)로 이어진다. `AnswerRegressionDetected`는 구현하지 않았다.

**구현 상태 (PLAN.md Phase 21, [ADR-0033](decisions/0033-technology-version-scan-detection-only-no-auto-outdated.md))**: `TechnologyVersionReleased → ImpactScanRequested → AffectedKnowledgeDetected`는 Phase 8 시점엔 외부 데이터 피드가 없어 구현하지 않았지만, 이번 Phase에서 endoflife.date v1 API를 연동해 실제로 자동화했다. `TechnologyVersionScanScheduler`가 하루 1회 `domain/qunobot/TrackedTechnologies`에 큐레이션된 기술의 최신 릴리스를 조회하고(`TechnologyReleaseFeed`), 저장된 스냅샷(`technology_releases`)과 달라졌을 때만 "새 릴리스"로 취급해 `AffectedKnowledgeDetected`에 해당하는 질문(해당 태그 + 비RESOLVED/비OUTDATED + 콘텐츠가 릴리스일보다 오래됨)을 찾는다. 여기서 이벤트 체인이 갈라진다: `QuestionOutdatedDetected`로 자동 전환하지 않고, 대신 `NotificationCreated`에 해당하는 `TECH_VERSION_IMPACT_DETECTED` outbox 이벤트로 곧장 `QuestionRevisionSuggested`(사람이 읽고 판단하도록 유도하는 알림) 역할을 겸한다 — 사람의 최종 판단 없이 시스템이 `OUTDATED` 상태를 스스로 바꾸는 것은 검증되지 않은 휴리스틱에 너무 큰 권한을 주는 것이라고 판단했다(ADR-0033). `GET /qunobot/version-impacts`로 현재 영향권 질문을 언제든 조회할 수 있다.

이 체인과 별개로, Spike Detection(태그별 질문량 급증 감지)은 진짜 자동화가 가능해 별도로 구현했다 — `SpikeDetectionRepository`가 태그별 최근 1일 질문 수와 직전 14일 일평균을 비교해 급증 비율을 계산한다(`GET /qunobot/spikes`). 이는 QunoBot 이벤트 체인의 일부가 아니라 독립된 읽기 전용 신호다 — "무엇이 심상치 않은지"만 알려주고 원인(기술 버전 변화인지 다른 이유인지)은 사람이 판단한다.
