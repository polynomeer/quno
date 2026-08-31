# ADR-0031: Comment에 1단계 대댓글, @mention 알림, 수정 이력을 추가(ADR-0024 일부 대체)

- 날짜: 2026-08-31
- 상태: 승인됨

## 배경 (Context)

[ADR-0024](0024-comment-flat-no-edit-tombstone-delete.md)는 Comment를 "스레드 없는 평면 목록·수정 불가·`@mention` 파싱 없음"으로 의도적으로 좁혀 설계하면서, "대댓글·멘션·수정 이력이 실제로 필요해지면 각각 새 ADR로 재설계한다"고 명시적으로 남겨뒀다(ADR-0024 결과 항목 3번). Phase 19+ 백로그 검토 중 사용자가 이 세 가지를 한 번에 착수하기로 선택했다. 세 기능 모두 외부 의존성(별도 데이터 소스, 결제/인증 정책 등)이 없어 지금 바로 설계·구현할 수 있다.

## 결정 (Decision)

1. **대댓글은 1단계까지만 허용한다** — `comments.parent_comment_id`(nullable, 자기 참조)를 추가한다. 답글의 답글은 만들 수 없다(`CommentReplyDepthExceededException`, 400) — 부모의 `parentCommentId`가 이미 null이 아니면 즉시 거부. 답글은 부모와 같은 `targetType`/`targetId`를 가져야 한다(다른 대상으로 답글을 다는 것은 의미가 없음). `ListCommentsUseCase`는 여전히 평면 목록을 반환하고(백엔드에 트리 조립 로직을 두지 않음), 프론트엔드가 `parentCommentId`로 그룹핑해 부모 아래 들여쓰기로 표시한다. Stack Overflow의 완전 평면 댓글보다는 한 단계 더 유연하지만, 무한 depth 관리(재귀 조회, depth 제한 정책)까지는 여전히 과하다고 판단해 1단계로 제한한다.
2. **수정(edit)을 허용하되, Question/Answer의 diff 기반 리비전보다 단순화한다** — `PUT /api/v1/comments/{id}`(작성자 본인만, `CommentAccessDeniedException` 403). 이미 삭제된 댓글은 수정할 수 없다(`CommentAlreadyDeletedException`, 409). 수정할 때마다 `comments.version_number`를 증가시키고, 새 `comment_versions`(id, comment_id, version_number, body, created_at) 테이블에 이전 본문을 append-only로 남긴다 — `AnswerVersion`과 같은 append-only 패턴이지만, **diff 엔드포인트는 만들지 않는다**: 댓글은 최대 600자라 두 시점의 본문을 나란히 보여주는 것만으로 충분하고, `TextDiffer`를 재사용할 만큼의 가치가 없다고 판단했다. `GET /api/v1/comments/{id}/versions`로 과거 본문 목록만 반환한다. **수정은 알림을 발생시키지 않는다** — Answer Revision(ADR-0029)과 달리 댓글 수정은 오탈자 교정 수준의 가벼운 변경으로 보고, Ward 구독자에게 다시 통보할 만한 활동 신호로 취급하지 않는다.
3. **`@mention`은 정규식 파싱 + 정확한 닉네임 일치로 구현하고, 알림 목적에만 쓴다** — 댓글 **생성 시점에만** 본문에서 `@([\w-]+)` 패턴(영숫자/밑줄/하이픈)을 추출해 `UserRepository.findByNickname`으로 정확히 일치하는 사용자를 찾는다. 공백이나 그 외 문자가 포함된 닉네임은 이 패턴으로 멘션할 수 없다(회원가입 시 닉네임 형식 제약이 아예 없어 생기는 한계 — 닉네임 정책이 생기면 재검토). **수정 시에는 멘션을 재계산하지 않는다** — 매 수정마다 이전 멘션 집합과 diff를 유지해야 알림 중복을 막을 수 있는데, 이는 지금 범위에 비해 과한 상태 추적이다. 자동완성 UI는 이번에도 추가하지 않는다(ADR-0024 5번과 같은 이유 — 닉네임 검색 API가 아직 없음). 프론트엔드는 렌더링 시 `@단어` 토큰을 스타일링만 하고(굵게/색상), 실제 사용자로 링크하지 않는다 — 백엔드가 파싱 결과(어떤 유저ID로 해석됐는지)를 읽기 응답에 노출하지 않기 때문이다(멘션은 쓰기 시점의 알림 부작용일 뿐, 조회 가능한 구조화 데이터로 저장/노출하지 않는다).
4. **알림은 두 갈래로 분리한다** — (a) 답글 생성 시 기존 `NEW_COMMENT` 이벤트 payload에 `parentCommentAuthorId`를 추가해 부모 댓글 작성자에게도 통보(Ward 구독자 fan-out에 편승, `DispatchOutboxEventsUseCase`의 `NEW_COMMENT` 분기에 한 줄 추가). (b) 멘션은 새 `OutboxEventTypes.MENTIONED_IN_COMMENT` 이벤트로 분리하고, `CONTENT_HIDDEN`과 마찬가지로 **Ward 구독자 기본 fan-out을 건너뛰고 멘션된 사용자에게만** 통보한다(활동 신호가 아니라 "당신이 언급됐다"는 개별 통지이므로). payload는 `mentionedUserIds`를 JSON 배열로 담고, `DispatchOutboxEventsUseCase`에 배열 파싱용 `extractLongList` 헬퍼를 추가한다(기존 `extractLong`은 단일 숫자 필드 전용이라 재사용 불가).
5. **버전 이력 UI는 별도 페이지를 만들지 않는다** — Question/Answer는 `/questions/[id]/versions`, `/answers/[answerId]/versions` 전용 페이지를 갖지만, 댓글은 그 정도로 무겁게 다룰 콘텐츠가 아니라고 판단해 `CommentItem` 안에서 "edited (v2)" 같은 표시를 누르면 인라인으로 과거 본문 목록이 펼쳐지는 방식으로 구현한다(새 라우트 없음).

## 결과 (Consequences)

- ADR-0024의 결정 2(대댓글 없음)·3(수정 불가)·5(멘션 없음)는 이 ADR로 대체된다. ADR-0024 파일 자체는 지우거나 고치지 않고 상태만 "일부 대체됨(ADR-0031로)"으로 갱신한다 — 나머지 결정(대상 범위, tombstone 삭제, 600자 제한, 평판 미반영)은 그대로 유효하다.
- 댓글이 이제 두 개의 신규 테이블(`comment_versions`)과 두 개의 신규 컬럼(`parent_comment_id`, `version_number`)을 갖게 되지만, `Comment` 자체의 불변식(작성자만 수정/삭제, 600자 제한)은 그대로 유지된다.
- 멘션 알림이 닉네임 형식 제약 부재로 인해 일부 사용자에게는 작동하지 않을 수 있다는 알려진 한계가 생긴다 — 실사용 후 닉네임 정책이 필요해지면 재검토한다.
- 답글 depth가 2단계 이상 필요해지거나, 멘션 자동완성이 실제로 요청되면(사용자 검색 API가 생긴 뒤) 각각 후속 ADR로 확장한다.

## 관련 문서

- [ADR-0024](0024-comment-flat-no-edit-tombstone-delete.md)(일부 대체 대상)
- [ADR-0029](0029-answer-revision-mirrors-question-version-no-locking.md)(append-only 버전 패턴의 원형)
- [ADR-0028](0028-moderation-mvp-report-dismiss-hide-only.md)(Ward 구독자 fan-out을 건너뛰는 알림 분기의 선례 — `CONTENT_HIDDEN`)
- [PLAN.md](../../../PLAN.md) Phase 20
