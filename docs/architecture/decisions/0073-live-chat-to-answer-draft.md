# ADR-0073: 실시간 질문방 대화를 답변으로 게시 — 서버가 초안만 만들고 게시는 기존 답변 쓰기로

- 날짜: 2026-10-22
- 상태: 승인됨

## 배경 (Context)

실시간 질문방([ADR-0036](0036-live-chat-websocket-mongodb-redis-presence.md))은 해결되면 대화가 휘발된다. 메시지는 MongoDB에 남지만, 질문 카드에는 답변으로 남지 않아 다음 사람이 찾을 수 없다. PLAN.md Phase 71은 해결 순간 메시지를 골라 답변 초안으로 편집·게시하고, 답변에 "질문방 대화에서 정리됨" 출처를 남기기로 했다. 선행 Phase가 없고 AI도 필요 없다([ADR-0069](0069-agent-era-positioning-verified-fix-memory.md)에서도 유지).

정할 것은 다음과 같다.

- 초안을 누가 만드는가
- 게시를 어떤 경로로 하는가
- 출처를 어떻게 남기는가
- 누가 할 수 있는가

## 결정 (Decision)

- **초안은 서버가 만들되 저장하지 않는다.**
  - `POST /api/v1/live-chat/{roomId}/answer-draft`(body: `messageIds`)가 고른 메시지를 시간순으로 모은다. 발신자 닉네임을 붙인 마크다운 본문을 돌려줄 뿐 아무것도 저장하지 않는다.
  - 서버가 만드는 이유: 메시지 id가 그 방에 속하는지 검증하고, 닉네임을 일괄 조회하는 일이 서버에 있어야 하기 때문이다.
  - 한 번에 고를 수 있는 메시지는 최대 50개다.
- **게시는 기존 답변 쓰기를 그대로 쓴다.**
  - `POST /questions/{id}/answers`에 선택 필드 `liveChatRoomId`를 더한다.
  - 답변 버전, 알림(Outbox `NEW_ANSWER`), 채택 흐름이 일반 답변과 완전히 같다.
  - 채택 권한은 질문 작성자에게 그대로 남는다.
- **출처는 답변의 nullable 컬럼 `source_live_chat_room_id`로 남긴다.**
  - 응답의 `sourceLiveChatRoomId`가 있으면 화면에 "질문방 대화에서 정리됨" 배지를 붙인다.
  - 방은 닫히지 않으므로([ADR-0036](0036-live-chat-websocket-mongodb-redis-presence.md)) 출처 링크는 항상 동작한다.
  - 원문 메시지는 MongoDB에 그대로 남는다.
- **권한은 방 참여자다.**
  - 참여자는 방을 연 사람이거나, 그 방에 메시지를 한 번이라도 보낸 사람이다. 초안 생성과 출처를 단 게시 모두 이 조건을 본다(아니면 403).
  - 게시 때 방이 그 질문의 방인지도 확인한다(아니면 400).
- **보류**
  - 대화 참여자별 기여 표기(공동 저자): 답변의 저자 모델을 바꿔야 해 범위가 크다. 초안 본문에 닉네임이 남는 것으로 대신한다.
  - "방 종료·해결 시점의 자동 게시 제안": 방에 종료 개념이 없다. 대신 질문이 RESOLVED가 아니어도 참여자에게 항상 "대화를 답변으로 정리" 진입점을 보여준다.

## 결과 (Consequences)

- 새 쓰기 경로가 없어 답변 관련 기존 불변식(버전, 알림, 평판, 채택)을 그대로 재사용한다.
- 초안을 저장하지 않으므로 편집 중 이탈하면 다시 만들어야 한다. 사용성 문제가 보이면 클라이언트 임시 저장을 검토한다.
- 참여자 판정에 `existsByRoomIdAndSenderId` 조회가 하나 늘어난다(MongoDB, `roomId` 인덱스 범위).
- 메시지를 고르는 화면에서 누가 한 말인지 보여야 하므로, 과거 메시지 조회(`GET /live-chat/{roomId}/messages`)에도 [ADR-0064](0064-list-responses-resolve-nicknames-in-one-batch.md)와 같은 일괄 닉네임 조회를 붙였다. 실시간 STOMP 메시지는 여전히 `senderId`만 보낸다.

## 관련 문서

- [ADR-0036](0036-live-chat-websocket-mongodb-redis-presence.md) 실시간 질문방
- [PLAN.md](../../../PLAN.md) Phase 71
- [docs/architecture/api-design.md](../api-design.md)
