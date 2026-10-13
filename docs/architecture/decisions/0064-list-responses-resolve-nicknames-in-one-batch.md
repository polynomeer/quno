# ADR-0064: 목록 응답의 사용자 닉네임을 컨트롤러에서 일괄 조회해 붙인다

- 날짜: 2026-10-13
- 상태: 승인됨

## 배경 (Context)

답변·댓글·QPR 요청·질문 타임라인·질문/답변 수정 이력 응답에는 작성자 id만 있고 닉네임은 없었다. 그래서 화면 곳곳에 "사용자 #26"으로 표시됐다. 디자인 시안(ADR-0061)은 닉네임을 보여주는데, 이 차이가 구현과의 대표적인 격차로 남아 있었다.

기존에 닉네임을 붙이던 곳(Direct Ask 목록, 팔로우 목록)은 항목마다 `userRepository.findById`를 호출했다. 이 방식을 목록마다 그대로 쓰면 항목 수만큼 쿼리가 나간다(N+1).

## 결정 (Decision)

- **일괄 조회 포트를 추가한다**: `UserRepository.findNicknamesByIds(ids): Map<Long, String>`. JPA `findAllById` 한 번으로 조회한다.
- **응용 계층 컴포넌트를 둔다**: `application/common/NicknameResolver`가 null을 걸러내고 id를 중복 제거한 뒤, 위 포트를 목록당 한 번 호출한다.
- **붙이는 위치는 목록 컨트롤러다**: 유스케이스와 그 결과 DTO는 건드리지 않는다. 컨트롤러가 유스케이스 결과의 사용자 id로 resolver를 한 번 부르고, 응답 매퍼에 닉네임을 넘긴다.
  - 대상: `GET /questions/{id}/answers`, `/questions/{id}/comments`, `/answers/{id}/comments`, `/questions/{id}/review-requests`, `/questions/{id}/timeline`, `/questions/{id}/versions`, `/answers/{id}/versions`.
  - 응답 필드: `authorNickname`, `requestedByNickname`, `actorNickname`, `createdByNickname`.
  - 기각한 대안: 유스케이스마다 `UserRepository`를 주입해 결과 DTO에 닉네임을 넣는 방식. 유스케이스 6개와 그 테스트 픽스처·가짜 저장소를 모두 바꿔야 한다. 그런데 닉네임은 표시용 데이터라 도메인 판단에 쓰이지 않는다.
- **목록이 아닌 응답은 `null`이다**: 생성·수정 응답(`toResponse(nickname = null)` 기본값)이 해당한다. 프론트 `userLabel()`이 이 경우 "사용자 #id"로 표시한다.
- **실시간 질문방 메시지는 범위 밖이다**: 메시지를 보낼 때마다 닉네임을 조회하면 전송 경로에 DB 왕복이 붙는다. 이를 피하려고 ADR-0036/0038에서 의도적으로 닉네임을 뺐기 때문에, 그 결정을 그대로 둔다.
- 탈퇴한 사용자는 행이 남고 닉네임만 익명화되므로(ADR-0046) 그대로 조회된다.

## 결과 (Consequences)

- 질문 상세의 답변 작성자(프로필 링크), 댓글, 타임라인, QPR 요청, 수정 이력에 닉네임이 보인다. 응답 하나당 추가 쿼리는 1회다.
- 응답 필드가 추가만 됐으므로(nullable) 기존 클라이언트는 깨지지 않는다.
- 다른 목록(예: 프로필의 작성 답변)에 닉네임이 필요해지면 같은 컨트롤러 패턴을 재사용한다.

## 관련 문서

- [docs/architecture/api-design.md — 목록 응답의 닉네임](../api-design.md#목록-응답의-닉네임-adr-0064)
- [ADR-0061](0061-frontend-visual-refresh-from-design-canvas.md), [ADR-0046](0046-reliability-account-withdrawal-data-export-n-plus-1.md)
