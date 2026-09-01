# ADR-0039: 실시간 질문방 프론트엔드는 `@stomp/stompjs`로, 연결은 참여를 누른 뒤에만 연다

- 날짜: 2026-09-01
- 상태: 승인됨

## 배경 (Context)

Phase 24([ADR-0036](0036-live-chat-websocket-mongodb-redis-presence.md))가 백엔드까지만 구현하고 미뤄둔 실시간 질문방 프론트엔드를 붙인다. 남은 프론트엔드 격차 중 마지막 하나로, [ADR-0038](0038-organization-direct-ask-frontend-no-user-search.md)에 이어 사용자에게 진행을 확인받았다.

이 프로젝트 프론트엔드에는 지금까지 WebSocket 클라이언트가 전혀 없었다 — 라이브러리 선택과, "질문 상세 페이지를 열기만 해도 접속자로 잡히는가"라는 연결 시점 설계를 새로 정해야 했다.

## 결정 (Decision)

1. **STOMP 클라이언트로 `@stomp/stompjs`를 채택한다.** 백엔드가 SockJS 폴백 없이 순수 WebSocket(`registry.addEndpoint("/ws")`, `withSockJS()` 미호출)으로 STOMP를 노출하므로, `sockjs-client`는 필요 없다. `@stomp/stompjs`는 네이티브 WebSocket 위에서 동작하는 표준 STOMP 클라이언트로 재연결(`reconnectDelay`)과 CONNECT 프레임에 커스텀 네이티브 헤더(`Authorization: Bearer <token>`)를 넣는 기능을 그대로 지원해, `StompAuthChannelInterceptor`가 기대하는 인증 방식과 정확히 맞는다.
2. **연결은 "채팅 참여하기"를 눌러야 시작한다 — 질문 상세 페이지를 여는 것만으로는 WebSocket을 열지 않는다.** presence("현재 N명이 보고 있습니다")는 `/topic/questions/{id}/presence` 구독 자체가 트리거이므로(`PresenceEventListener`), 모든 방문자에게 항상 켜두면 질문 상세 페이지를 볼 때마다 WebSocket 연결이 하나씩 생긴다. Direct Ask 결제창을 "누른 뒤에만 연다"고 정한 ADR-0038의 절제 원칙을 그대로 따라, 접속자 수를 "미리 보여주는 값"이 아니라 "채팅에 참여한 사람 수"로 좁혔다.
3. **채팅방이 없으면 "실시간 질문방 시작하기", 있으면 "채팅 참여하기" 두 단계로 나눈다.** `POST /questions/{id}/live-chat`은 find-or-create라 사실 하나의 버튼으로 합칠 수도 있었지만, `GET`이 성공하는 경우(이미 열려 있음)와 404인 경우(새로 여는 행위)의 문구가 다른 편이 사용자에게 더 정확하다고 판단했다. 방을 새로 여는 클릭은 곧바로 참여까지 이어지고(같은 액션의 연장), 이미 열려 있는 방은 별도 클릭으로 참여를 확인받는다(둘러보러 왔다가 얼떨결에 접속자로 잡히지 않도록).
4. **메시지에는 닉네임 대신 `사용자 #{senderId}`를 보여준다.** `LiveChatMessageResponse`에 닉네임이 없고, 이걸 채워 넣으려면 메시지 전송·조회 경로(초당 여러 번 발생할 수 있는 hot path)마다 Postgres 조회가 하나씩 늘어난다 — Direct Ask 목록처럼 한 번만 조립하면 되는 read model과는 비용 구조가 다르다고 판단해, 이번 범위에서는 ID만 보여주고(자신은 "나"로 표시, 프로필 링크는 제공) 넘어간다.

## 결과 (Consequences)

- 질문 상세 페이지를 그냥 열어보는 것만으로는 접속자 수에 잡히지 않는다 — "몇 명이 지금 이 질문을 보고 있는지" 앰비언트 신호로 쓰려면 별도 설계(예: 항상 열리는 경량 presence-only 소켓)가 필요하며, 지금은 하지 않는다.
- 메시지 발신자가 `사용자 #{id}`로만 보인다는 점은 실사용에서 불편이 확인되면 재검토한다 — 그때는 메시지 브로드캐스트 payload에 닉네임을 실어 보내는 쪽(조회가 아니라 전송 시점에 한 번 붙이는 방식)이 매 조회마다 조인하는 것보다 나은 절충안이 될 수 있다.
- 이 프로젝트 최초의 WebSocket 클라이언트 의존성이 생겼다 — 향후 다른 실시간 기능도 같은 `@stomp/stompjs` 연결 패턴(`features/live-chat/lib/websocket-url.ts`, `useLiveChatSocket.ts`)을 재사용할 수 있다.

## 관련 문서

- [domain-model.md](../domain-model.md) (Live Chat Bounded Context, Live Chat Message 섹션)
- [api-design.md](../api-design.md#live-chat-phase-24)
- [ADR-0036](0036-live-chat-websocket-mongodb-redis-presence.md) (백엔드 결정)
- [ADR-0038](0038-organization-direct-ask-frontend-no-user-search.md) (같은 "연결은 필요할 때만" 절제 원칙의 선례)
- [PLAN.md](../../../PLAN.md) Phase 27
