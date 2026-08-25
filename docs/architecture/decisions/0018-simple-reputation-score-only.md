# ADR-0018: Phase 9는 간단한 평판 점수만 다루고, Organization과 Direct Ask는 후속 Phase로 미룬다

- 날짜: 2026-08-25
- 상태: 승인됨

## 배경 (Context)

mvp-scope.md 로드맵 Phase 5는 "Organization, 전문가 평판, Direct Ask"를 한데 묶는다. 그런데 이 셋 중 Organization(조직 인증)과 Direct Ask(결제 포함)는 vision.md/mvp-scope.md 어디에도 구체적인 메커니즘이 없다 — 조직을 어떻게 인증할지(이메일 도메인 기반? 수동 승인?), Direct Ask에서 결제를 어느 범위까지 다룰지 등은 이 세션에서 확인 가능한 문서 근거가 전혀 없다. 반면 "전문가 평판"은 이미 확보된 질문/답변/Cluster 데이터만으로 근사치를 계산할 수 있고, mvp-scope.md의 MVP 제외 목록도 "복잡한 Reputation Economy"라고만 적어 간단한 버전까지 배제하지는 않았다.

## 결정 (Decision)

이번 Phase는 **간단한 평판 점수 계산**만 구현한다. `GET /users/{id}/reputation`이 질문 수·답변 수·채택된 답변 수·Super Answer 지정 횟수를 집계해 `score = questionCount*1 + answerCount*2 + acceptedAnswerCount*15 + superAnswerCount*10` 공식으로 점수를 낸다(채택 답변과 Super Answer 지정에 가중치를 크게 둬 "전문가"에 해당하는 실제 기여를 더 반영). Organization과 Direct Ask(결제 포함)는 핵심 설계가 없는 채로 임의로 만들지 않고, 번호 미정 상태로 후속 Phase에 남겨둔다.

## 결과 (Consequences)

- 평판 점수는 순전히 활동량 기반 근사치다 — 답변의 실제 품질, 동료 평가(peer review), 악용 방지(스팸성 답변 양산 등) 장치는 없다. 실제 오남용이 관찰되면 재검토한다.
- Organization/Direct Ask가 없으므로 "회사·학교 기반 전문가 추천"이나 "결제 기반 1:1 질문"은 아직 제품에 존재하지 않는다 — 이 세션 전체에서 유지해온 "실제로 설계 가능한 부분만 구현한다"는 원칙(ADR-0008, ADR-0016, ADR-0017)을 그대로 따른 것이다.
- 평판 점수 공식(가중치)은 하드코딩된 초기값이다 — 실제 사용자 행동을 관찰한 뒤 조정이 필요할 수 있다.

## 관련 문서

- [ADR-0010](0010-metrics-read-model-skip-dto.md) (재사용하는 "읽기 전용 모델은 DTO 생략" 패턴)
- [ADR-0016](0016-manual-duplicate-marking-cluster.md), [ADR-0017](0017-manual-outdated-marking-and-spike-detection-scope.md) (같은 방향의 스코프 축소 선례)
- [PLAN.md](../../../PLAN.md) Phase 9
