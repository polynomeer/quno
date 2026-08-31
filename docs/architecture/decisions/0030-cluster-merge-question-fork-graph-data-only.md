# ADR-0030: Merge는 클러스터 병합으로 한정하고, Fork는 리비전 인프라를 재사용하며, 지식 그래프는 데이터 API까지만 제공한다

- 날짜: 2026-08-31
- 상태: 승인됨

## 배경 (Context)

mvp-scope.md 로드맵 Phase 3("질문 네트워크")는 Cluster, Merge/Fork, Super Answer, 지식 그래프 시각화를 한데 묶는다. Phase 6([ADR-0016](0016-manual-duplicate-marking-cluster.md))에서 Cluster+Super Answer만 먼저 구현하면서 나머지 셋은 "사용 패턴을 관찰한 뒤" 번호 미정으로 미뤄뒀다. 이제 그 사용 패턴 관찰 없이 착수하기로 했으므로, ADR-0016이 원래 관찰하려 했던 질문("누가 언제 병합/포크를 요청하는가")에 대한 답 없이 설계해야 한다 — 대신 이미 코드에 새겨진 신호를 근거로 삼는다: `MarkQuestionsAsSameProblemUseCase`가 서로 다른 두 클러스터를 묶으려 할 때 던지는 `ClustersAlreadyDistinctException`의 메시지 자체가 "merging clusters is not supported yet"라고 명시하고 있고, ADR-0016은 이 409가 자주 발생하면 그게 곧 Merge를 앞당길 신호라고 이미 적어뒀다. Fork는 코드베이스에 정말 아무 흔적도 없다(`parentId`/`forkedFrom`류 컬럼·메서드·문서 전무) — 완전히 새로 설계한다.

## 결정 (Decision)

**Merge는 "클러스터 병합"으로만 한정한다.** ADR-0016 본문이 "Merge(클러스터 병합/질문 병합)"이라고 두 의미를 섞어 썼지만, 이번 결정은 클러스터 병합만 다룬다 — 질문 자체를 "MERGED" 상태로 닫고 정식으로 다른 질문에 흡수시키는 것(Stack Overflow의 Close as Duplicate에 가까움)은 `Question.status`에 새 종단 상태를 추가하는 별도의 무거운 결정이라 이번 범위에서 뺀다. 구현 방식: `MarkQuestionsAsSameProblemUseCase`가 지금 두 질문이 이미 서로 다른 클러스터에 속했을 때 `ClustersAlreadyDistinctException`(409)을 던지는 분기를, **실제로 두 클러스터를 병합하는 로직으로 교체한다** — "같은 문제로 표시"라는 동일한 사용자 행동이 그 자연스러운 결말이기 때문이다(새 API 엔드포인트를 만들지 않음). `questionId` 쪽 클러스터가 살아남고, `relatedQuestionId` 쪽 클러스터의 멤버 전원이 살아남은 클러스터로 재배정된 뒤 흡수된 클러스터 행은 삭제된다(`QuestionClusterRepository`에 `delete` 능력을 새로 추가해야 함 — 지금은 없음). 흡수된 클러스터에 이미 지정된 Super Answer는 자동으로 이전하지 않는다 — 병합 후 필요하면 사람이 다시 지정하면 된다는 판단이다(이미 존재하는 재지정 API로 충분).

기존 "같은 문제로 표시"가 권한 제한이 없는 것과 동일하게, 병합도 별도 권한을 두지 않는다.

**Fork는 완전히 새로 만들되, 기존 리비전 인프라를 재사용한다.** `questions`에 `origin_question_id`(nullable, 자기 자신을 제외한 다른 질문 참조) 컬럼을 추가하고, `Question.open()`에 선택적 파라미터로 얹는다(별도 팩토리 메서드를 만들지 않음 — Fork로 만들어진 질문도 "새 질문을 만드는 것"이라는 본질은 같기 때문). `POST /questions/{id}/fork`는 origin의 **현재 최신 본문을 그대로 복사**해 실행자 명의의 새 질문(+Qv1)을 만든다 — GitHub Fork가 저장소를 있는 그대로 복제하는 것과 같은 방식이다. Fork 시점에 내용을 바꿔 제출하는 기능은 만들지 않는다 — 포크 직후 필요하면 이미 있는 `POST /questions/{id}/versions`(질문 리비전, Phase 2)를 그대로 써서 고치면 된다. 태그도 origin의 현재 태그를 그대로 복사한다. 자기 자신의 질문을 포크하는 것도 막지 않는다(자기 자신에게 투표/팔로우하는 것과 달리, 자신의 질문을 변형해 나가는 것은 의미 있는 행동이다). Fork는 대상 질문을 자동으로 같은 Cluster에 넣지 않는다 — Cluster는 "같은 문제(같은 해결책 공유)"를, Fork는 "다른 조건으로 갈라져 나간 변형(다른 해결책이 필요할 수 있음)"을 뜻해 성격이 다르기 때문이다. `origin_question_id`는 순수 계보 추적용이며, Cluster/Super Answer와 달리 outbox 이벤트를 발행하지 않는다(같은 계열의 기존 결정과 동일한 이유 — 동기 API 응답으로 충분).

**지식 그래프는 이번 Phase에서 데이터 API까지만 제공하고, 실제 시각화(그래프 다이어그램 UI)는 만들지 않는다.** ADR-0016이 시각화를 미룬 원래 이유("프론트엔드가 아직 없음")는 이제 사실이 아니지만, 인터랙티브 노드-엣지 그래프 렌더링은 그 자체로 새 다이어그램 라이브러리 도입이 필요한 별도 규모의 프론트엔드 투자라 이번 백엔드 중심 Phase에 묶지 않는다. 대신 `GET /questions/{id}/graph`가 이미 존재하는 조각들(Cluster 멤버, Fork 계보, Related Questions)을 한 응답으로 모아 반환한다 — 새로운 계산이나 저장소 없이 기존 `GetClusterUseCase`/`QuestionSearchUseCase.related`/새 Fork 조회를 조합만 한다.

## 결과 (Consequences)

- `ClustersAlreadyDistinctException`은 더 이상 던져지지 않으므로 제거하고, 그 동작을 검증하던 기존 단위 테스트(Phase 6.4)는 "두 클러스터가 병합된다"는 새 기대치로 교체해야 한다 — 이건 새 기능 추가가 아니라 기존 동작을 실제로 바꾸는 것이라는 점을 명확히 해둔다.
- 클러스터 병합은 흡수되는 쪽의 Super Answer 지정을 잃을 수 있다 — 드문 경우일 것으로 예상하지만, 실사용에서 자주 문제가 되면 자동 이전 로직을 재검토한다.
- 질문 자체를 "다른 질문에 병합됨(MERGED)"으로 명시적으로 닫는 기능은 여전히 없다 — Cluster 가입으로 근사할 수는 있지만 진짜 그 개념은 아니다. 실제 수요가 확인되면 `Question.status`에 종단 상태를 추가하는 별도 ADR이 필요하다.
- Fork로 만들어진 질문은 원본과 완전히 독립적인 생애주기를 갖는다 — 원본이 나중에 리비전되거나 Outdated로 표시돼도 포크에는 아무 영향이 없다(계보 포인터만 남을 뿐 동기화되지 않음). 이게 의도한 동작이다.
- "지식 그래프"라는 이름이 붙었지만 실제로는 그래프 시각화가 아니라 관계 데이터 조회 API다 — 프론트엔드가 이 데이터로 무엇을 보여줄지(리스트? 카드? 실제 다이어그램?)는 이 ADR이 결정하지 않는다.

## 관련 문서

- [ADR-0016](0016-manual-duplicate-marking-cluster.md) (Cluster+Super Answer 범위 결정, 이번에 이어받는 "병합 신호" 근거)
- [ADR-0008](0008-postgres-native-search.md) (벡터/임베딩 기반 유사도는 여전히 범위 밖 — 이번 Merge/Fork도 그런 자동화 없이 사용자 명시 행동으로만 동작)
- [PLAN.md](../../../PLAN.md) Phase 18
