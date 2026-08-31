# ADR-0034: Organization은 Virtual/Community만, Direct Ask는 결제 없이 요청/수락만 구현한다

- 날짜: 2026-08-31
- 상태: 승인됨

## 배경 (Context)

mvp-scope.md 로드맵 Phase 5(신뢰 네트워크)는 "Organization, 전문가 평판, Direct Ask"를 한데 묶는다. 전문가 평판은 이미 Phase 9([ADR-0018](0018-simple-reputation-score-only.md))에서 구현했고, Organization과 Direct Ask만 "핵심 설계가 아직 없어 착수 시점에 다시 설계한다"며 `PLAN.md`에 번호 미정 상태로 남아 있었다.

원본 기획서([docs/archive/Quno 서비스 통합 기획서](../../archive/README.md) 24~29장)를 보면 두 개념 모두 실제로는 여러 하위 개념을 묶고 있다.

- **Organization**: 실제 회사·학교처럼 "인증 가능한 조직"(Verified Organization)과, 사용자가 임의로 만드는 스터디·커뮤니티(Virtual Organization/Community)로 나뉜다. "같은 조직 개발자들이 자주 질문한 Topic" 같은 소셜 그래프 기반 추천 기능도 함께 제안된다.
- **Direct Ask**: 특정 전문가에게 질문에 대한 답변을 직접 요청하는 기능. "전문가는 Direct Ask를 받을지 여부를 설정할 수 있다"는 옵트인 개념과 함께, "전문가가 Direct Ask를 받고 보상을 받을 수 있는 구조로 발전시킬 수 있다"는 **유료 Direct Ask**도 부가 아이디어로 등장한다.

이 프로젝트에는 결제(PG) 연동이 전혀 없고, 실제 카드·계좌 정보를 다루는 통합은 안전상의 이유로 신중해야 한다. Verified Organization도 실제 회사·학교 소속을 검증하는 외부 신원 확인 인프라가 없다.

## 결정 (Decision)

두 기능 모두 실사용 데이터 없이 "가장 저위험·저비용으로 핵심 개념을 검증할 수 있는 부분집합"만 구현한다.

1. **Organization은 Virtual/Community만 구현한다.** 사용자가 이름·설명만으로 조직을 직접 만들고(`POST /organizations`), 생성자는 자동으로 첫 멤버가 된다. 외부 신원 검증이 필요한 Verified Organization(이메일 도메인 매칭 등)은 이번 범위에서 제외한다 — Tag를 누구나 만들 수 있는 것과 같은 신뢰 수준이다.
   - 조직 이름은 `Organization.slugify`(대소문자 정규화만, `Tag.slugify`와 달리 비-ASCII 문자를 제거하지 않음)로 중복을 막는다 — 원본 기획의 조직명 예시("대구 백엔드 개발자 모임")가 흔히 한글이라, Tag의 ASCII 전용 slugify를 그대로 썼다면 한글 전용 이름이 전부 빈 문자열로 충돌했을 것이다.
   - "같은 조직 개발자들이 자주 질문한 Topic" 같은 조직 기반 추천/피드는 이번 범위에서 만들지 않는다 — 실제 조직 데이터가 쌓이기 전에는 검증할 방법이 없다.
2. **Direct Ask는 결제 없이 요청→수락/거절까지만 구현한다.** 대상 사용자가 `User.acceptsDirectAsk`로 옵트인해야 요청을 받을 수 있고(기본값 false — 스팸 방지), 요청은 자기 자신에게 금지되며, 같은 (질문, 대상) 쌍에 열린(PENDING) 요청은 하나만 허용한다. 수락 후 실제 답변은 기존 `POST /questions/{id}/answers`를 그대로 쓴다 — `DirectAskRequest`는 어떤 `Answer`와도 직접 연결되지 않는다. "유료 Direct Ask"(보상/결제 구조)는 명시적으로 범위 밖에 둔다.

## 결과 (Consequences)

- Organization의 "신뢰"는 순수하게 자기 신고(self-declared)다 — 실제 회사 소속을 사칭해도 시스템이 걸러내지 않는다. 이는 Tag/Cluster/Outdated에 이미 적용된 "자동 검증 대신 사용자 명시적 행동" 철학과 같은 방향이지만, Organization은 "소속"이라는 사회적 신뢰를 함의하는 만큼 악용 리스크가 더 크다 — 실사용에서 문제가 되면 재검토한다.
- Direct Ask 요청은 알림만 발생시킬 뿐, 전문가가 수락을 강제당하지 않는다(거절 가능). "전문가 추천"(Topic Expert 등 원본 기획의 추천 화면)은 만들지 않았다 — 요청자가 대상 사용자 ID를 직접 알아야 한다는 뜻이며, UX 관점의 "누구에게 물어볼지 추천"은 후속 과제다.
- `users.accepts_direct_ask`는 DB에 새 컬럼을 추가하지만, `User.role`(Phase 16)과 달리 self-service API(`PUT /me/direct-ask-settings`)로 사용자가 직접 켜고 끌 수 있다 — role은 DB-only로 남겨둔 것과 다른 결정이다(role은 권한 상승이라 self-service가 부적절하지만, Direct Ask 수신 여부는 순수 개인 설정이라 부적절하지 않다).
- "유료 Direct Ask"와 Verified Organization은 여전히 PLAN.md에 명시적으로 범위 밖으로 남는다 — 실제 PG 연동이나 조직 인증 방식이 필요해지는 시점에 별도로 설계한다.

## 관련 문서

- [domain-model.md](../domain-model.md#trust-network-phase-22)
- [api-design.md](../api-design.md#organization--direct-ask-phase-22)
- [ADR-0016](0016-manual-duplicate-marking-cluster.md) (동일한 "자동화 대신 사용자 명시적 표시" 철학)
- [ADR-0018](0018-simple-reputation-score-only.md) (Phase 9가 이미 미룬 Organization/Direct Ask 스코프)
- [PLAN.md](../../../PLAN.md) Phase 22
