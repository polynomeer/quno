# ADR-0017: Outdated는 사용자 명시적 표시로 근사하고, 진짜 기술 버전 자동 감지는 범위 밖으로 둔다. Spike Detection은 포함한다

- 날짜: 2026-08-25
- 상태: 승인됨

## 배경 (Context)

mvp-scope.md 로드맵 Phase 4는 "QunoBot, 기술 버전 영향 감지, Outdated/Regression, Spike Detection"을 한데 묶는다. domain-model.md의 "QunoBot 이벤트 체인"은 `TechnologyVersionReleased → ImpactScanRequested → AffectedKnowledgeDetected → QuestionOutdatedDetected/AnswerRegressionDetected`처럼 외부에서 기술 버전 릴리스 이벤트가 들어온다는 것을 전제로 한다. 이 프로젝트에는 그런 외부 데이터 소스(예: 각 기술의 릴리스 노트/체인지로그 피드)가 없고, 지금 새로 만들려면 스크래핑이나 외부 API 연동 같은 상당한 인프라 투자가 필요하다. 반면 Spike Detection(특정 태그의 질문량 급증 감지)은 이미 확보된 질문/태그 생성 시각 데이터만으로 계산 가능하다.

## 결정 (Decision)

두 가지로 나눠 결정한다.

1. **Outdated는 Cluster/Review와 동일하게 사용자가 명시적으로 표시**한다. `POST /questions/{id}/outdated`로 누구나(작성자 포함, 권한 제한 없음) 질문을 OUTDATED로 전환할 수 있다. 진짜 "기술 버전 변화를 자동으로 감지해 Outdated를 판정"하는 것은 외부 데이터 연동이 실제로 필요해지는 시점까지 명시적으로 범위 밖에 둔다.
2. **Spike Detection은 이번 Phase에 포함**한다. 기존 대시보드 트렌드 집계(Phase 3.2)와 같은 방식으로, 태그별 "최근 1일 질문 수 vs 직전 14일 일평균"을 비교하는 native SQL로 급증 비율을 계산한다 — 새 인프라 없이 진짜 자동 감지가 가능하다.

## 결과 (Consequences)

- `QuestionStatus.OUTDATED`가 새로 생기지만, 여기로의 전이는 전적으로 사람의 판단에 의존한다 — "정말 이 질문이 기술 버전 변화로 낡았는지"를 시스템이 검증하지 않는다. 오탐/악용(부적절하게 outdated 표시)을 막을 별도 안전장치(예: 최소 근거 요구, 되돌리기)는 아직 없다 — 실제 오남용 사례가 나오면 재검토한다.
- Spike Detection은 "질문량 급증"만 감지하지, 그것이 실제로 기술 버전 변화 때문인지는 알 수 없다 — 단순히 "이 태그에 무슨 일이 있다"는 신호일 뿐이며, 원인 분석은 사람이 한다.
- "기술 버전 영향 감지"의 진짜 자동화(외부 릴리스 피드 연동)는 PLAN.md에 번호 미정 상태로 남겨뒀다 — Merge/Fork와 마찬가지로 착수 시점에 다시 설계한다.

## 관련 문서

- [domain-model.md](../domain-model.md#qunobot-이벤트-체인-phase-4)
- [ADR-0009](0009-redis-cache-global-aggregates-only.md) (Spike Detection이 재사용하는 캐싱 패턴)
- [ADR-0016](0016-manual-duplicate-marking-cluster.md) (동일한 "자동화 대신 사용자 명시적 표시" 철학)
- [PLAN.md](../../../PLAN.md) Phase 8
