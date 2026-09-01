# ADR-0036: 실시간 질문방을 STOMP/WebSocket + MongoDB(메시지) + Redis(접속자)로 구현한다

- 날짜: 2026-09-01
- 상태: 승인됨

## 배경 (Context)

[ADR-0019](0019-quno-flow-and-dashboard-only-no-live-chat.md)는 실시간 질문방(Live Chat)을 "이 세션에서 지금까지 다룬 어떤 기능보다 큰 기술적 투자(양방향 실시간 연결, presence tracking)"라는 이유로 범위 밖에 두고 `PLAN.md`에 번호 미정으로 남겨뒀다. 원본 기획서([docs/archive](../../archive/README.md) 19장 "실시간 질문 공간")는 두 가지를 요구한다: 질문 상세에 "현재 N명이 이 질문을 보고 있습니다"를 표시하는 것(presence), 그리고 질문자·답변자·관심 사용자·전문가가 함께 논의할 수 있는 Live Chat을 필요할 때 즉시 열 수 있는 것. "실시간 대화 → 구조화된 지식"이라는 문구는 논의 내용을 사람이 읽고 기존 리비전/답변 API로 정제하는 것을 뜻하며, 자동 요약·변환 기능을 요구하지 않는다.

이 프로젝트는 기술 스택에 MongoDB를 확정했지만([system-architecture.md](../system-architecture.md#확정-기술-스택)) 지금까지 실제로 한 번도 사용한 적이 없었다 — `docs/architecture/domain-model.md`의 "MongoDB 문서 모델" 섹션은 계획만 있고 구현이 없는 상태였다.

## 결정 (Decision)

1. **STOMP-over-WebSocket**(`spring-boot-starter-websocket`)으로 실시간 연결을 구현한다. WebSocket 핸드셰이크(`/ws`) 자체는 인증하지 않고, STOMP `CONNECT` 프레임의 `Authorization: Bearer <token>` 네이티브 헤더를 `StompAuthChannelInterceptor`가 기존 `TokenProvider`로 검증해 세션의 Principal을 설정한다 — REST의 stateless JWT 인증([ADR-0003](0003-stateless-jwt-auth.md))과 같은 토큰, 다른 전달 경로다.
2. **질문당 최대 1개의 `LiveChatRoom`**(PostgreSQL)만 존재한다. "필요한 경우 즉시 생성"은 find-or-create를 의미하며, 방을 닫는 기능은 두지 않는다 — 질문이 RESOLVED가 된 뒤에도 논의 이력은 그대로 남는다.
3. **메시지는 MongoDB에 저장한다** — 이 프로젝트가 MongoDB를 실제로 채택한 첫 사례다. 고빈도 append-only 쓰기이고 관계형 불변조건이 없어, domain-model.md가 애초에 MongoDB의 용도로 상정했던 "구조가 자주 바뀌거나 대량 쓰기가 발생하는" 컬렉션의 전형이다. `LiveChatRoom`(메타데이터, 저빈도, 고정 스키마)은 계속 PostgreSQL에 둔다.
4. **접속자(presence)는 Redis Set**(`RedisLiveChatPresenceTracker`)으로 추적한다. STOMP `SUBSCRIBE`(`/topic/questions/{id}/presence`)에서 join, `UNSUBSCRIBE`/연결 종료에서 leave — 별도 heartbeat 없이 WebSocket 연결 종료 이벤트에 얹는다. 세션→구독 매핑은 인메모리(단일 인스턴스 전제, 결과 섹션에 기록).
5. **채팅 메시지는 outbox 이벤트를 발행하지 않는다**(Vote가 매 투표마다 이벤트를 안 만드는 것과 같은 이유 — 스팸). 대신 방이 **새로 생성될 때만** `LIVE_CHAT_STARTED`를 Ward 구독자 + 질문 작성자에게 통보한다(`QUESTION_OUTDATED`와 동일한 수신자 규칙).
6. **"실시간 대화 → 구조화된 지식" 자동 변환은 만들지 않는다** — 사람이 채팅을 읽고 기존 `POST /questions/{id}/versions`/`POST /questions/{id}/answers`를 호출하는 것으로 충분하다고 판단했다.

### 부수 발견: Spring Boot 4의 MongoDB 연결 프로퍼티 prefix 변경

MongoDB를 처음 실사용하며 발견한 문제: `application-local.yml`에 원래 있던 `spring.data.mongodb.uri: mongodb://localhost:27017/quno`는 **아무 값도 바인딩하지 못하고 조용히 무시됐다** — 메시지가 실제로는 `mongodb://localhost/test`(드라이버 기본값)에 저장되고 있었다. `GET /actuator/configprops`로 확인한 결과, Spring Boot 4는 MongoDB 연결 프로퍼티(host/port/uri/database/username/password)를 다루는 클래스를 `spring.data.mongodb.*` prefix의 `DataMongoProperties`(gridfs/representation만 남음)에서 `spring.mongodb.*` prefix의 새 `MongoProperties`(`org.springframework.boot.mongodb.autoconfigure.MongoProperties`)로 분리했다. 존재하지 않는 프로�터티 키에 값을 써도 Spring Boot는 에러를 내지 않고 조용히 무시하므로, 이 오류는 애플리케이션 기동 로그만으로는 드러나지 않는다 — 실제로 삽입된 문서가 어느 DB에 들어갔는지 확인해야만 발견된다.

## 결과 (Consequences)

- `application-local.yml`을 `spring.mongodb.host`/`port`/`database`로 수정했다. 이 prefix 변경은 Live Chat에만 국한되지 않는 이 프로젝트 전체의 MongoDB 설정 규칙이므로, 향후 다른 기능이 MongoDB를 쓸 때도 같은 함정에 빠지지 않도록 `domain-model.md`의 MongoDB 섹션에도 남겼다.
- Presence의 세션→구독 매핑은 단일 인스턴스에서만 정확하다. 여러 인스턴스로 수평 확장하면 Redis의 실제 멤버십(Set)은 인스턴스 간에 공유되지만, "이 세션이 disconnect됐을 때 어떤 questionId를 leave해야 하는지"를 아는 로컬 상태는 그 세션이 연결된 인스턴스에만 있다 — 그 인스턴스가 죽으면 좀비 멤버가 Redis Set에 남을 수 있다. 실제로 여러 인스턴스가 필요해지는 시점에 공유 저장소로 옮기는 재검토가 필요하다.
- 실제 STOMP 프로토콜 왕복(CONNECT 인증 → SUBSCRIBE presence → 카운트 증가 브로드캐스트 → SEND 메시지 → MongoDB 저장 → 다른 세션에 실시간 브로드캐스트 → 연결 종료 → 카운트 감소)을 Python 클라이언트 스크립트로 curl 대신 검증했다 — WebSocket은 curl로 검증할 수 없어 이 프로젝트에서 처음 쓴 검증 방식이다.
- Live Chat 자체의 REST 엔드포인트(방 열기/조회/히스토리)는 인증을 요구하지만, WebSocket 세션의 인증 실패(잘못된/누락된 토큰)는 CONNECT 프레임 거부로 나타난다 — REST의 401과 다른 실패 모양이라는 점을 클라이언트가 알아야 한다.

## 관련 문서

- [domain-model.md](../domain-model.md#live-chat-phase-24)
- [api-design.md](../api-design.md#live-chat-phase-24)
- [ADR-0019](0019-quno-flow-and-dashboard-only-no-live-chat.md) (이번 ADR이 채워 넣는 범위 밖 결정)
- [ADR-0003](0003-stateless-jwt-auth.md) (재사용한 JWT 토큰 검증)
- [PLAN.md](../../../PLAN.md) Phase 24
