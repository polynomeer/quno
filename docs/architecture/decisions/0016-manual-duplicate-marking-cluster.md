# ADR-0016: Cluster는 자동 유사도 분석이 아니라 사용자의 명시적 "같은 문제" 표시로 형성하고, 이번 Phase는 Cluster+Super Answer로 한정

- 날짜: 2026-08-25
- 상태: 승인됨

## 배경 (Context)

mvp-scope.md 로드맵 Phase 3(질문 네트워크)는 Cluster, Merge/Fork, Super Answer, 지식 그래프 시각화를 한데 묶어 정의한다. domain-model.md의 "지식 진화 체인"은 `SimilarityAnalyzed → QuestionClustered → ClusterThresholdReached → SuperAnswerCandidateDetected`처럼 자동 유사도 분석과 임계값 기반 클러스터링을 전제로 그려져 있다. 이를 실제로 구현하려면 임베딩 모델과 벡터 검색 인프라가 필요한데, [ADR-0008](0008-postgres-native-search.md)에서 이미 "본문 유사도 기반 추천은 MVP 이후 확장"으로 벡터 유사도를 유보한 바 있다. 이 Phase에 착수하면서 (1) 클러스터링을 자동화할지 수동화할지, (2) Cluster/Merge/Fork/Super Answer 네 가지를 한 번에 다 만들지 일부만 먼저 만들지를 사용자에게 확인했다.

## 결정 (Decision)

두 가지를 결정한다.

1. **클러스터링은 사용자가 명시적으로 표시한다.** `POST /questions/{id}/cluster`로 "이 질문은 저 질문과 같은 문제다"를 표시하면 클러스터가 생성되거나 기존 클러스터에 합류한다 — Stack Overflow의 "mark as duplicate"와 같은 형태다. 임베딩/벡터 유사도 기반 자동 클러스터링은 도입하지 않는다.
2. **이번 Phase는 Cluster + Super Answer까지만 구현한다.** Merge(클러스터 병합/질문 병합)와 Fork는 후속 Phase로 미룬다 — 아직 "언제 누가 병합/포크를 요청하는지" UX가 불명확해, 먼저 Cluster+Super Answer를 실제로 써보고 패턴을 관찰한 뒤 설계하는 것이 낫다고 판단했다. 지식 그래프 "시각화"도 프론트엔드가 없는 현재 범위 밖이다(Phase 4.3과 같은 판단).

이 결정에 따라 질문은 최대 하나의 클러스터에만 속하도록(`questions.cluster_id` 단일 FK) 단순화하고, 이미 서로 다른 클러스터에 속한 두 질문을 하나로 합치는 것(클러스터 병합)은 명시적으로 거부한다 — 그 자체가 Merge 기능의 영역이기 때문이다.

## 결과 (Consequences)

- 새 ML/벡터 인프라 없이 기존 스택(PostgreSQL만)으로 Cluster/Super Answer를 구현할 수 있다 — ADR-0001의 "모듈형 모놀리스로 시작해 필요할 때만 확장" 원칙과 일관된다.
- 태그가 없거나 검색으로 찾기 어려운 질문은 클러스터링되지 않는다 — 사용자가 직접 발견하고 표시해야만 클러스터가 생긴다. 자동 클러스터링 대비 커버리지는 낮지만 오탐(false positive)이 없다.
- 이미 다른 클러스터에 속한 두 질문을 묶으려는 시도는 409로 거부된다. 실제로 이런 상황이 자주 발생하면 Merge 기능을 앞당겨야 한다는 신호로 받아들인다.
- Merge/Fork/지식 그래프 시각화는 PLAN.md에 번호 미정 상태로 남겨뒀다 — 착수 시점에 사용 데이터를 보고 다시 설계한다.

## 관련 문서

- [ADR-0008](0008-postgres-native-search.md)
- [domain-model.md](../domain-model.md#지식-진화-체인-phase-3)
- [PLAN.md](../../../PLAN.md) Phase 6
